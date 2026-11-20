# Stryker4s mutation-testing trial on Gryphon

Trial run 2026-11-19. The companion write-up for the earlier Visitor trial is not in this
repository; it lives with that session's notes.

**Verdict: adopt, and the yield is materially better than Visitor's — for a structural
reason, not an accidental one.** Issues filed: #78–#84. Three PRs came out of the trial and
all three are merged: #85 (the Stryker4s configuration itself, closing #84), #87 (the
self-loop bug, closing #78) and #88 (the self-merge guards, closing #83).

Run it with `sbt stryker`; scope and thresholds are in `stryker4s.conf` at the repository
root. It is deliberately not wired into CI — see the last section for why.

## Setup

Identical to Visitor and it worked first time. Gryphon is Scala 3.9.0 on sbt 1.12.13, same
as Visitor, so `strykerScalaDialect := scala.meta.dialects.Scala3` transfers unchanged.

54 files / 7109 lines of `src/main/scala` against Visitor's 13 / 1563. Gryphon also has a
Java façade (`src/main/java`, 10 files) and JUnit 5 tests (`src/test/java`, 18 files);
Stryker4s mutates only Scala, and the Java side compiled and ran inside the instrumented
build with no special handling.

"Found 54 file(s) to be mutated" — **zero parse failures**. `-Werror` with `-Wunused:all`,
`-Wvalue-discard` and `-Wnonunit-statement` did not break the instrumented compile.

One near-miss worth recording: a `BooleanLiteral` mutant in `UndirectedGraph.scala` made a
`case (found @ (true, _), _)` unreachable and produced E029/E030 *warnings*. They were not
promoted to errors, so `-Werror` survived — but a project where such a mutant produced an
error-level diagnostic would lose mutants to spurious compile failures.

## Cost

| | Visitor | Gryphon |
|---|---|---|
| main sources | 13 files / 1563 lines | 54 files / 7109 lines |
| tests | 249 | 652 (448 ScalaTest + 204 JUnit) |
| baseline `sbt test` wall clock | 24.3s | 42.0s (cold compile) |
| baseline test **execution** | 1.2s | 2.4s |
| mutants generated | 113 | 279 |
| active after exclusions | 90 | 181 (98 StringLiteral excluded) |
| mutants tested | — | 157 |
| mutant phase | 7.1s | 39.1s |
| **wall clock** | **40.6s** | **95.2s** |

Affordable, and still worth running whole-library. But the shape of the cost changed, and
this is the number that matters for the rest of the stack:

- Visitor: mutant phase 18% of the run, essentially free, independent of mutant count.
- Gryphon: mutant phase 41%, and it clearly scales with mutant count.

Stryker4s runs three test runners in parallel *and* uses coverage analysis to run only the
tests covering each mutant — which is why 157 × 2.4s ÷ 3 ≈ 125s came in at 39s. That
optimisation is doing the work that keeps this tolerable.

**The figure to check before running this on a new project is the test *execution* time,
not the mutant count and not `sbt test`'s wall clock** (which is dominated by compilation).
Under ~5s this stays a coffee-break diagnostic. A 30-second suite with Gryphon's mutant
count would be heading for eight minutes. Stated as an extrapolation, not a measurement.

One `CompileError`, the same `MethodExpression` misfire Visitor hit: `.filterNot(...)` →
`.filter(...)` in `core/VertexMap.scala:74` on `Adjacencies[V]`, which defines only
`filter`. Stryker drops it and recompiles — that extra round is why the run costs 95s
rather than ~80s.

## Results

```
run 1 (baseline suite)
279 generated, 98 excluded -> 181 active
  1 CompileError, 23 NoCoverage, 157 tested -> 132 detected, 24 Survived
  73.74% of total, 84.62% of covered code

run 2 (+7 verification tests)
  139 detected, 40 undetected
  77.65% of total, 88.54% of covered code      mutant phase 41.4s
```

scoverage: **81.83% statement, 78.33% branch**. (Visitor: 77.75% / 88.24%.)

## The decisive comparison against scoverage

This is where Gryphon diverges from Visitor, and it is the real result of the trial.

On Visitor, all three behavioural gaps were already visible in scoverage's **branch**
report; mutation testing's unique yield was one minor gap plus two redundancy findings.

On Gryphon I cross-referenced every survivor site against `scoverage.xml`:

- **Only 4 of 17 distinct survivor sites had an uncovered branch** — `Boruvka:82`,
  `UndirectedGraph:95`, `UndirectedGraph:161`, `Connectivity:129`.
- **13 were fully covered with every branch taken**, and the mutant still lived.

And there is a structural reason, which is what makes this generalisable rather than an
accident of this codebase: **scoverage records no branch at all for `&&` and `||`
short-circuits.** `Parseable.scala:226` (`input != null && input.nonEmpty`) and
`GraphParser.scala:66` (`bad.isEmpty || bad.forall {...}`) both report as fully covered
with *zero* branch statements. Mutation testing finds those; branch coverage cannot, by
construction. `TopologicalSort.scala:123` is the same story for `forall` over a comparison.

So the deflating Visitor conclusion does not generalise. Robin's expectation that this
would pay off better on the larger, less uniformly-tested libraries is borne out — and the
mechanism is not "the score is worse" (73.74% vs 84.34%, worse but not dramatically), it is
that the survivors sit on algorithm invariants and short-circuit predicates rather than on
log messages.

## Verification — what was actually demonstrated

The Visitor trial's decisive move was writing one test and confirming a mutant flipped.
Repeated here: a throwaway spec, re-run, **detected 132 → 139, undetected 47 → 40**.

**Seven mutants flipped Survived → Killed:**

| site | mutation | finding |
|---|---|---|
| `UndirectedGraph.scala:95:8` and `:95:24` | guard → `false`, `true` → `false` | empty graph never passed to `isConnected` |
| `GraphParser.scala:66:14` | `bad.isEmpty` → `nonEmpty` | malformed-input path unasserted |
| `GraphParser.scala:71:9` | `ok` → `true` | ditto |
| `GraphParser.scala:68:17` | was `NoCoverage` | the `case _ => false` arm had never run |
| `Parseable.scala:226:31` | `&&` → `||` | null and `""` both untested |
| `TopologicalSort.scala:123:53` | `<=` → `<` | see the bug below |

**One real bug** (Gryphon#78). The `TopologicalSort.scala:123` survivor turned out not to
be a test gap at all — **the mutant was the correct code**. `position(white) <= position(black)`
and `position(white) < position(black)` differ on exactly one input, a self-loop `v → v`.
I wrote the test asserting `sort` returns `None` for a graph containing `1 → 1`; it
**failed**, returning `Some(List(0, 1))`. So `TopologicalSort.sort` hands back a
topological order for a graph that has none. Invisible to scoverage: the line has five
covered statements and no branch statements at all.

**Two survivors I could NOT kill, reported as such:**

- `GraphParser.scala:66:29` (`forall` → `exists`) survives even with the malformed-input
  test. Killing it needs an input *mixing* an `EmptyStringException` with some other
  failure — that is the real distinction ("blank lines tolerated, malformed lines not")
  and nothing asserts it.
- **`Boruvka.scala:82` resisted everything.** My reasoning said tied edge weights would
  reach the guard; two deliberately-tied graphs (an equal-weight triangle, and three tied
  pairs joined by three tied crossing edges) both passed and both left the mutant alive.
  So I instrumented the guard with a `System.err.println` and ran the whole suite: **zero
  occurrences across 652 tests.** It never executes. Instrumenting one level up showed why
  my constructions missed: the triangle's round-1 unique edges were `0-1, 1-2` (the third
  edge was never any component's cheapest), and the six-vertex graph's round 2 gave only
  `5-0, 1-2`, because two components chose the *same* crossing edge and
  `uniqueCrossingEdges` collapsed them.
  The open question — is the guard reachable at all, given the keying at `:109`? — is the
  substance of Gryphon#79. If it is reachable it depends on which vertex wins a tie in
  `cheapestCrossingEdges`, which is driven by `vertexMap` hash order, so no test could
  construct it reliably anyway.

That last one is worth dwelling on: **the honest outcome of a survivor can be "I could not
kill this, and here is the evidence it never runs"**, which is a more useful finding than
either "genuine gap" or "equivalent mutant" would have been if asserted without checking.

## The 24 survivors, classified

- **6 provably equivalent / redundant code** (Gryphon#83): `DisjointSet:114`
  (`doMerge` already guards `v1 == v2`), `Connectivity:129` and `:271` (both `throw`s are
  unreachable from any public path), `UndirectedGraph:131` (bipartite DFS start colour is
  symmetric), `BellmanFord:82` (`count < vertexCount` → `<=` merely loosens a safety bound).
- **12 genuine gaps**, filed as #78–#82 plus `DisjointSet:257` (not equivalent, unlike
  `:114` — removing it forces path compression, and `meanDepth`/`maxDepth` are public) and
  `BellmanFord:79` (duplicate enqueues inflate the counter; needs a bigger graph).
- **6 already known or by design**: four at `AbstractAdjunctGraph:117/:119`, which is
  `checkAdjacencies` warning and carrying on — **already filed as Gryphon#57**, with a
  `TODO see issue #57` two lines above. Independent rediscovery; good corroboration of the
  method, not a new finding. Plus `UndirectedGraph:161`, inside a member already annotated
  `@unused`. (It reports as Survived rather than NoCoverage because the `PartialFunction`
  *value* is constructed at class initialisation though its body never runs — a gotcha when
  reading these reports.)

The 23 `NoCoverage` mutants are not survivors; scoverage already reports those lines. They
cluster in `util/FP.scala` (10), the `TicTacToe`/`Sudoku` demo stubs (6), and singletons
elsewhere. `FP.scala` is worth a look on its own.

## Issues filed

- **#84** — adoption (closed by the PR)
- **#78** — BUG: `TopologicalSort.sort` accepts a self-loop as acyclic
- **#79** — Borůvka's in-round cycle guard is never exercised: reachable or dead?
- **#80** — `GraphParser.parseStrings`: malformed-input failure path unasserted
- **#81** — `Parseable[String]`: documented null/empty handling untested
- **#82** — `UndirectedGraph.isConnected`: empty-graph case untested
- **#83** — redundant and unreachable self-merge guards in `DisjointSet`/`Connectivity`

The verified test code is inline in each issue, so nothing is lost by the diagnostic spec
having been deleted before the commit.

## What is still open

#78, #83 and #84 were closed by the three merged PRs. The rest are the trial's unfinished
business, in the order I would pick them up:

- **#79** — Borůvka's in-round cycle guard. The one that needs a judgement rather than a
  test: I could show it never fires across 652 tests, not that it *cannot* fire. If it is
  genuinely unreachable it should go; if it is reachable only on a hash-order coincidence,
  that is worth knowing too.
- **#80** — `GraphParser.parseStrings`. Partly addressed by the verification tests, but
  `66:29` (`forall` → `exists`) still survives. Killing it needs an input that mixes an
  `EmptyStringException` with some other failure.
- **#81** — `Parseable[String]`: the documented null/empty handling. Small and
  self-contained; the test code is already in the issue.
- **#82** — `UndirectedGraph.isConnected` on an empty graph. Likewise small.
- **#86** — improve the `Graph` type hierarchy. Not a mutation-testing finding, but the
  trial gave it two concrete customers: the self-loop constraint that #87 worked around at
  the point of use rather than at construction, and the same hole in `UndirectedGraph`.
  `DisjointSet.connect` has a related asymmetry pinned but not resolved — `connect(x, y)`
  rejects an unknown `x` while `connect(x, x)` quietly does not.

Not filed, and low value: `BellmanFord.scala:79` (duplicate enqueues inflate the counter;
needs a larger graph than any test has), `UndirectedGraph.scala:161` (inside an `@unused`
member), and the `NoCoverage` cluster in `util/FP.scala`.

## For the remaining libraries

The rule that falls out of two trials:

1. Time the test **execution** (not `sbt test`'s wall clock). Under ~5s, run it.
2. Run the whole library; scoping by file saves almost nothing.
3. Exclude `StringLiteral` — 98 of 279 mutants here, 11 of 17 survivors on Visitor.
4. Expect the unique-over-scoverage yield to track how much the code leans on `&&`/`||`
   short-circuits and on predicates inside `forall`/`exists`, because those are precisely
   scoverage's structural blind spots.
5. Never a CI gate. The survivors need a human to read them — this trial produced one real
   bug, one "I could not kill it and here is why", and six equivalent mutants, and no
   threshold would have told those apart.
