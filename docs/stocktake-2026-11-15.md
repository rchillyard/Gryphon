# Gryphon: taking stock after 1.8.0

Written 2026-11-15, the day 1.8.0 was published, prompted by Robin asking two questions: whether
the deferred **capability parameter** (plan question 5) was worth taking up, and what the library
looks like as a whole.

Answering the first turned up a bug, so it comes first. Every claim below was checked against the
code or run, not inferred; where something was measured, the measurement is given so it can be
re-taken later.

---

## 1. A bug: `UndirectedGraph.unit` built a `DirectedGraph`

```scala
UndirectedGraph[Int, Double](VertexMap[Int]).addVertex(v)   // => DirectedGraph
```

`unit` is how every derived graph is rebuilt, so returning the wrong kind silently turned an
undirected graph into a directed one. It was a copy-paste from the sibling class, Javadoc and all —
the comment inside `UndirectedGraph` read *"Creates a new directed graph"*. Present in every
released version up to and including 1.8.0.

**Fixed** (branch `undirected-unit`). All 644 tests pass, so nothing depended on the old behaviour.

**Why nothing caught it.** `unit` is declared to return the base `Graph[V]`, so the compiler could
not see the swap, and no test exercised `addVertex` on an undirected graph and then asked what it
got back. The regression test added with the fix does exactly that, for both orientations.

**Why it matters beyond itself.** This is the same family as the `addEdge` bug that 1.8.0 fixed:
the types claim an orientation that construction does not honour. It bears directly on §8 of the
1.8.0 plan — the class is supposed to *be* the proof of the property, and here a method on the
class erased that proof in its own return type.

---

## 2. A second finding, not fixed: `VertexMap.edgeCount`

`VertexMap` holds `private var edgeCount`, incremented by `+`. Measured by a throwaway spec (run,
then deleted rather than committed, since it documents an unfixed bug):

| | expected | actual |
|---|---|---|
| `(VertexMap[Int] + e1 + e2).M` | 2 | **0** |
| original map's `.M` after deriving from it | unchanged | **incremented** |

So `+` mutates the receiver — a value the caller still holds — and the map it returns always counts
zero, because `ensure` and `modifyVertex` build fresh `VertexMap`s whose counter starts at 0.

**Severity: latent, not live.** Both concrete graph classes override `M` with `edges.size`
(`DirectedGraph.scala:111`, `UndirectedGraph.scala:45`), so no user-facing number is wrong today.
What is wrong is the inherited default at `Graph.scala:155` (`def M: Int = vertexMap.M`), which any
future graph class would pick up, and `VertexMap.M` itself, which is public.

**Fix:** delete `edgeCount` and `VertexMap.M`, and make `Graph.M` abstract — both concrete classes
already implement it. That is a `ReversedMissingMethodProblem`, i.e. a binary break, so it wants a
minor release and a MiMa filter, exactly as `inDegree`/`outDegree` did in 1.8.0.

---

## 3. The capability parameter (plan question 5): recommend closing it

The question was whether to record self-loops and multi-edges as type-level facts, as JGraphT's
`GraphType` records them — the case where type parameters start to beat class proliferation, since
one binary capability needs two classes, two need four, three need eight.

**No algorithm in Gryphon has such a precondition.** Checked individually:

- Self-loops are **counted**, never forbidden: `numberOfSelfLoops` (`Graph.scala:235`).
- `degree` handles them deliberately — the Java test is named `a_self_loop_counts_once_each_way`.
- `isBipartite` handles them correctly by construction: `dfs` colours `v`, then tests each
  neighbour, so a self-loop reaches an already-coloured `v` and yields `color(v) != c` → false.
  A self-loop is an odd cycle, so `false` is the right answer.
- Multi-edges have no concept at all. `Bag` is explicitly a multi-set and nothing objects to
  duplicates.

So a capability parameter would enforce preconditions that do not exist. That is the opposite of
the 1.8.0 situation, which had three real ones waiting to be expressed (`addEdge`, `prim`/`dijkstra`,
and the two undirected-only predicates). **Recommend closing question 5** rather than deferring it
again — on the same grounds as the Scala 3.3.8 build: not "later", but "not this".

If the answer ever changes it will be because an algorithm is *added* that needs a simple graph, and
that algorithm — not the type system — should drive the design.

---

## 4. The library as a whole

### Size

| | files | lines |
|---|---|---|
| `src/main/scala` | 53 | 7,017 |
| `src/main/java` (façade) | 11 | 2,103 |
| tests (Scala + Java) | 56 | 7,881 |

By package: `core` 2,364 · `traverse` 1,351 · `adjunct` 1,218 · `java` (bridge) 690 · `parse` 623 ·
`util` 514. Roughly 1:1 code to tests, which is healthy.

### What is good

- **The algorithm set is complete** for Sedgewick & Wayne Chapter 4, and the implementations are
  clean rather than merely correct.
- **The typeclass separation between structure and traversal is real**, not decorative — the
  Visitor engine genuinely carries the traversal and the graph types genuinely carry the structure.
- **Documentation is unusually thorough**, and since 1.8.0 both Scaladoc and the façade's Javadoc
  are published. The README's examples are compiled rather than transcribed.
- **Seven of ten algorithms reject the wrong graph at compile time**, and since 1.8.0 all ten do.
- **The Java façade is now genuinely tested** — 152 JUnit tests that had been compiled but never
  run before 1.7.0.

### What could be better, in the order worth tackling

1. **The two graph classes are ~80% identical.** Normalising the names away leaves 253 differing
   lines out of 1,218. That duplication is what produced the bug in §1, and it has already produced
   four copies of the same `// TODO find another way to handle this anomaly`
   (`DirectedGraph.scala:185,197`, `UndirectedGraph.scala:204,219`). A shared base holding the
   common half would remove a whole class of defect rather than one instance of it.

2. **"Purely functional" is not quite true, and the exceptions that matter are not the obvious
   ones.** `BellmanFord`, `AcyclicShortestPaths` and `GraphTraversal` use mutable maps internally
   behind pure interfaces — a deliberate and defensible performance choice. The two that are not
   defensible are `VertexMap.edgeCount` (§2) and `RelaxableVertex`, which has a public
   `var maybeR`, a `relax` documented as *"this vertex (mutated in place)"*, and therefore a
   `compare` whose result depends on mutable state.

3. **`RelaxableVertex` is dead code.** Nothing in `src/main` uses it but its own factories; the only
   test that touches it is `VertexSpec`. It is public, mutable, and one of the last two places a
   `Monoid` context bound survives. Same category as `util.HasZero`, deleted in 1.8.0.

4. **`VertexMap` carries two "implement this properly" TODOs** (`:38`, `:153`) in the single most
   important type in the library.

Nineteen `TODO`/`CONSIDER`/`FIXME` markers in `src/main` overall, and 22 `asInstanceOf` casts, of
which the `DisjointSet` ones are documented as safe with reasons.

### A coherent 1.9.0

Items 1–3 are one release, because they are one theme — and it is §8's theme: the gap between what
the types promise and what the implementation guarantees.

1. Deduplicate the graph classes into a shared base (removes the §1 class of bug).
2. Delete `RelaxableVertex` and `Vertex.createRelaxableWithSet`.
3. Make `M` honest: delete `edgeCount` and `VertexMap.M`, make `Graph.M` abstract.

All three are binary breaks, so they belong together in one minor release with one set of MiMa
filters and one migration section — and, per the 1.8.0 lesson, with a version-history row that says
only what that release actually changed.

---

## 5. What was decided elsewhere, recorded here so it is not re-opened

- **Scala 3.3.8 build: closed** (2026-11-15). No Visitor release has ever been 3.3-consumable — all
  are 3.7.4 or 3.9.0, and TASTy only reads forward — Gryphon's `E: {A, B}` syntax is a 3.6 feature
  now in 12 places, and both consumers are Java, which never reads TASTy.
- **Visitor's `Monoid`/`Zero`: delete, don't migrate.** Verified against the published
  `visitor_3-1.7.0.jar`: they appear in no signature in Visitor's API, and nothing outside their own
  given instances references them. Visitor needs no `Monoid` from Cats; it needs to stop publishing
  one. Not urgent — Gryphon already routes around it with `{Monoid as _, *, given}` — but it is a
  binary break when it happens, so it belongs in whatever PR next touches Visitor.
