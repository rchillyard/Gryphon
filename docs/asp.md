# The Arbitrary Substitution Principle — Formal Statement (Draft)

## Definition

Let $P$ be a program containing an expression $e$, and let $\tau$ be a transformation
such that $\tau(e)$ is *semantically equivalent* to $e$ with respect to the problem
specification — that is, $e$ and $\tau(e)$ are interchangeable in any correct solution.
We say that $e$ exhibits an **unjustified asymmetry** if substituting $\tau(e)$ for $e$
in $P$ produces observably different runtime behavior (in correctness, performance,
or generality) and no explicit justification for the choice of $e$ over $\tau(e)$ is given.

**The Arbitrary Substitution Principle (ASP):** Any unjustified asymmetry in a program
constitutes a code smell. The author of the asymmetry *must* either:

1. **justify** the choice explicitly (in a comment, a proof, or a documented convention), or
2. **eliminate** the asymmetry by making the program invariant under $\tau$.

## The Escape Clause

A justification is valid if it appeals to one or more of the following:

- **Intentional laziness / short-circuit evaluation:** the asymmetry is the feature
  (e.g., placing the cheaper predicate first in `&&` deliberately avoids evaluating
  the more expensive one).
- **Proven behavioral equivalence in context:** a proof that, despite the asymmetry
  in $e$, the observable outputs of $P$ are identical under $\tau$ (e.g., a sorting
  algorithm whose internal tie-breaking is arbitrary but whose output is still a
  valid sorted sequence).
- **Established convention:** the asymmetry follows a well-known, documented convention
  that a competent reader would recognize (e.g., `compareTo` returning negative when
  `this < other`).

Absent one of these justifications, the asymmetry *must* be treated as a defect.

## Corollary: The Peer Review Obligation

An unjustified asymmetry should never survive peer review. When reviewing code,
the reviewer's obligation is to ask: *"Why this direction and not the other?"*
If the author cannot answer, the code must be revised — either to eliminate the
asymmetry or to document its justification. Silence is not a justification.

## The Unified Nature of ASP Violations

All ASP violations share the same underlying structure: an expression of the form
$a \oplus b$ where $b \oplus a$ is equally valid under the problem specification,
yet the two produce different runtime behavior. This covers both superficially
distinct cases:

- **Operand order:** `union(v1, v2)` in naive Quick-Union, where attaching root $v_1$
  under $v_2$ vs. $v_2$ under $v_1$ is an arbitrary choice with consequences for
  tree depth.
- **Dual formulation:** Hibbard deletion using successor vs. predecessor, or searching
  forward vs. backward in a reversible problem. These are symmetric under a
  problem-level duality (left/right reflection of a tree, or time-reversal of a
  move function), but are still expressible as a choice between $a \oplus b$ and
  $b \oplus a$ under the appropriate interpretation of $\oplus$.

The surface form differs; the obligation is identical: justify or eliminate.

## Consequences of Violation

The consequences of an unjustified asymmetry range in severity:

| Severity | Consequence | Example |
|----------|-------------|---------|
| Cosmetic | Code is harder to reason about | Arbitrary successor/predecessor choice with equivalent performance |
| Performance | Asymptotically worse complexity | Naive Quick-Union: $O(N)$ vs. Weighted Quick-Union: $O(\log N)$ |
| Practical correctness | Program cannot terminate in practice | Searching forward only in a problem requiring backward search: exponential vs. polynomial time |

The WeightedQuickUnion case is particularly instructive. Noticing the ASP violation
— that `union(v1, v2)` makes an arbitrary choice — immediately reveals *what
information the program was lacking*: namely, the relative sizes of the two trees.
Acquiring that information requires a new data structure (tracking size or rank at
each root). The improvement from $O(N)$ to $O(\log N)$ then follows directly. The
ASP violation did not merely signal a missed optimisation; it pointed precisely at
the missing ingredient.

On the question of "practical correctness": a program that is theoretically correct
but requires exponential time to return an answer is, in any engineering sense,
incorrect. The philosophical distinction between "too slow to terminate" and
"incorrect" is real but operationally moot. We therefore retain "correctness" in
the severity table, qualified as *practical* correctness, with the understanding
that this represents an extreme point on the performance axis rather than a
categorically different failure mode.