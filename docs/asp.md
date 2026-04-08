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

## Taxonomy of Asymmetry

ASP violations fall into two broad categories:

**Type 1 — Operand order:** The expression $e$ is of the form $a \oplus b$, and
swapping to $b \oplus a$ produces different behavior. The transformation $\tau$ here
is simply argument transposition. Example: `union(v1, v2)` in naive Quick-Union,
where the choice of which root is attached to which is arbitrary but consequential.

**Type 2 — Dual formulation:** Two logically symmetric formulations of an algorithm
exist (e.g., using a node's successor vs. its predecessor in Hibbard deletion,
or searching forward vs. backward in a reversible problem). The choice between
them is arbitrary with respect to correctness but may differ in performance,
generality, or tractability. The transformation $\tau$ here is the duality mapping
between the two formulations.

In both cases, the obligation is the same: justify or eliminate.

## Consequences of Violation

The consequences of an unjustified asymmetry range in severity:

| Severity | Consequence | Example |
|----------|-------------|---------|
| Cosmetic | Code is harder to reason about | Arbitrary successor/predecessor choice with equivalent performance |
| Performance | Asymptotically worse complexity | Naive Quick-Union: $O(N)$ vs. Weighted Quick-Union: $O(\log N)$ |
| Correctness | Problem becomes unsolvable or solutions missed | Searching forward only in a problem that requires backward search |

The WeightedQuickUnion case is particularly instructive: the *only* change required
to move from $O(N)$ to $O(\log N)$ is to replace an arbitrary choice with a
principled one. No new data structure is needed; no new algorithm. The improvement
is a direct consequence of obeying the ASP.