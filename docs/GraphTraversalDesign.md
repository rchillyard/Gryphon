# Gryphon / Visitor — GraphTraversal Design Document

## Overview

This document captures the design for a unified `GraphTraversal` abstraction covering
four classic graph algorithms: DFS, BFS, Dijkstra, and Prim. It also documents the
required enhancements to the Visitor library to support efficient priority-queue-based
traversal with cost updates.

---

## Current State (as of Gryphon 0.2.4 / Visitor V1.3.0)

### Visitor Library

- `Traversal` engine with `traverse`, `dfs`, `bfs`, `bestFirst`, `bestFirstMax`,
  `bestFirstWeighted` (new)
- Three-type priority queue hierarchy: `BinaryHeap` → `PrioQueue` → `IndexedPrioQueue`
  (see §Priority Queue Design Decision below)
- `Frontier[F[_]]` typeclass with implementations for `Stack`, `Queue`, `PrioQueue`,
  and `IndexedPrioQueue` (new)
- `CostUpdate[W, F[_]]` typeclass (new) with default no-op given
- `TupleVisitedSet[E, V]` and `given [E, V]: VisitedSet[(E, V)]` (new) — supports
  weighted tuple frontiers for Dijkstra/Prim
- `Evaluable[V, R]`, `Neighbours[V, V]`, `VisitedSet[V]` typeclasses (unchanged)
- `JournaledVisitor` with `ListJournal` (prepend) and `QueueJournal` (append/FIFO)

### Gryphon

- `GraphTraversal[V, E, R]` trait — in `com.phasmidsoftware.gryphon.traverse`
- `DFSTraversal[V]` — delegates to `Traversal.dfs`
- `BFSTraversal[V]` — delegates to `Traversal.bfs`
- `DijkstraTraversal[V, E]` — **pending refactor** to `Traversal.bestFirstWeighted`
- `PrimTraversal[V, E]` — **pending refactor** to `Traversal.bestFirstWeighted`
- `ShortestPaths.dijkstra` delegates to `DijkstraTraversal.run`
- `ConnectedComponents` — computes connected components of undirected graphs
- `TopologicalSort`, `ShortestPaths` — existing, working

### Known Issues / Pending Work

- `DijkstraTraversal` and `PrimTraversal` refactor to tuple-frontier approach
  is **not yet done** — this is Phase 2 (see below).
- Six tests in `PrimSpec` are currently pending.
- `ShortestPathsSpec` uses `Edge` return type (changed from `DirectedEdge`).

---

## Priority Queue Design Decision

### Context

The original `PrioQueue` was a single class backed by an immutable `BinaryHeap`.
To support `decreaseKey` for Dijkstra/Prim, we needed to add an index map
(`Map[T, Int]`: element → heap-array position). Several design options were considered.

### Options Considered

#### Option A: Single `PrioQueue` with embedded index (rejected)

Add `Map[T, Int]` directly to `BinaryHeap` / `PrioQueue`. Deduplicate on `insert`
(no-op if element already present).

**Pro:** Simple, single type, minimal API surface.

**Con:** Breaks general-purpose use — a priority queue is a valid ADT that permits
duplicates. The deduplication guard (`if index.contains(t) then this`) caused the
duplicate-elements test to fail with `removeMin on empty heap`. The `index` map has
ambiguous semantics when duplicates exist (tracks only the most-recently-inserted
position). Mixes two concerns (data structure policy and structural mechanics) in
one class.

#### Option B: Two types — `BinaryHeap` (pure) + `PrioQueue` (with index) (rejected)

Move the index into `PrioQueue`, keep `BinaryHeap` pure. `PrioQueue` rebuilds the
index via `heap.data.zipWithIndex.toMap` (O(n)) after each structural operation.

**Pro:** Clean separation of data structure from policy. `BinaryHeap` becomes simple.

**Con:** Still conflates two different `PrioQueue` use cases: general-purpose
(duplicates OK) and indexed (no duplicates, `decreaseKey` needed). A single
`PrioQueue` type cannot honestly serve both.

#### Option C: Three types — `BinaryHeap` + `PrioQueue` + `IndexedPrioQueue` ✓ CHOSEN

```
BinaryHeap[T]        — pure data structure; sift-up/down, insert, removeMin only
PrioQueue[T]         — ADT; delegates to heap; duplicates permitted; no index
IndexedPrioQueue[T]  — ADT; adds Map[T,Int] index; no duplicates; decreaseKey/contains
```

**Pro:** Each type is an honest ADT with clear invariants. Pedagogically valuable —
directly illustrates the textbook principle that a priority queue ADT delegates to
a binary heap. `BinaryHeap` is visibly a pure data structure. `PrioQueue` and
`IndexedPrioQueue` are visibly policy layers. `Frontier[PrioQueue]` (for
`bestFirst`/`bestFirstMax`) and `Frontier[IndexedPrioQueue]` (for
`bestFirstWeighted`) are clearly distinct at the call site.

**Con:** Three types instead of one; two `Frontier` given instances;
`bestFirstWeighted` uses `IndexedPrioQueue` while `bestFirst`/`bestFirstMax` use
`PrioQueue` — callers must choose the right entry point.

**Why chosen:** The teaching context makes the clean three-way separation a feature,
not a cost. The type-level distinction between a plain priority queue and an indexed
one also enforces correct usage at compile time — it's impossible to accidentally
use a `decreaseKey`-enabled queue where a simple one was intended, or vice versa.

### Implementation Detail: Index Rebuild Strategy

`IndexedPrioQueue` rebuilds its `Map[T, Int]` from scratch after each `offer` or
`take` via `heap.data.zipWithIndex.toMap` — O(n). An alternative would be to have
`BinaryHeap` expose position information from sift operations so the index could be
maintained incrementally (O(log n)). This was rejected because:

- The O(n) rebuild cost is dominated by the O(E log V) traversal cost in Dijkstra/Prim
- Incremental maintenance would require threading index-awareness back into
  `BinaryHeap`, partially defeating the purpose of the split
- O(n) rebuild keeps `BinaryHeap` pure and the overall design simple

`decreaseKey` in `IndexedPrioQueue` removes the old element by patching the raw
array, rebuilds the heap from scratch via a fold of `insert`s, then inserts the new
element. This is O(n log n) rather than O(log n) but correct. Can be optimised later
if profiling ever justifies it.

---

## CostUpdate Typeclass

### Purpose

After a node is settled (dequeued and marked visited) and its neighbours offered to
the frontier, `CostUpdate` provides a hook for updating the priorities of frontier
entries that have improved. This keeps all domain knowledge (cost maps, edge weights,
the notion of "improvement") out of `Traversal` itself.

```scala
trait CostUpdate[W, F[_]]:
  def update(frontier: F[W], w: W): F[W]

// Default no-op — resolved automatically for DFS and BFS
given [W, F[_]]: CostUpdate[W, F] with
  def update(frontier: F[W], w: W): F[W] = frontier
```

`Traversal.traverse` calls `cu.update` in two places:
1. After the seed (`cu.update(seedOffered, start)`)
2. After each `offerAll` in the main loop (`cu.update(offered, node)`)

### Relationship to IndexedPrioQueue

For Dijkstra and Prim, `DijkstraTraversal` / `PrimTraversal` provide a
`given CostUpdate[W, IndexedPrioQueue]` (where `W = (E, V)`) that calls
`decreaseKey` for any frontier entry whose cost has improved since it was
first offered. The `CostUpdate` instance closes over a secondary `Map[V, E]`
(the only remaining mutable state) that tracks the current best known cost
per vertex — needed to look up the old frontier entry for `decreaseKey`.

---

## Tuple Frontier Approach for Dijkstra/Prim (Phase 2)

### Motivation

The original Dijkstra/Prim in `GraphTraversal.scala` used `Ordering[V]` derived
from a mutable cost map, with the frontier holding plain `V` vertices. This was
rejected in favour of an explicit `(E, V)` tuple frontier:

- **Correctness:** Cost is encoded in the frontier element itself — no implicit
  dependency on external mutable state for ordering.
- **Purity:** `Ordering[(E, V)]` is `Ordering.by(_._1)`, derived purely from
  `E`'s `Ordering`. No mutable state leaks into the ordering.
- **Clarity:** The frontier element type makes the cost explicit at every step.

### Design

The frontier element type is `W = (E, V)` where `E` is the cost type and `V` is
the vertex type.

**`Neighbours[(E, V), (E, V)]`** — cost accumulation lives here:
```scala
// Dijkstra: accumulate cost
given Neighbours[(E, V), (E, V)] = (ev: (E, V)) =>
  graph.filteredAdjacencies(...)(ev._2)
    .flatMap(adj => adj.maybeEdge[E].map(e => (en.plus(ev._1, e.attribute), e.black)))

// Prim: edge weight only, no accumulation
given Neighbours[(E, V), (E, V)] = (ev: (E, V)) =>
  graph.filteredAdjacencies(...)(ev._2)
    .flatMap(adj => adj.maybeEdge[E].map(e => (e.attribute, e.other(ev._2))))
```

**`VisitedSet[(E, V)]`** — tracks visited-ness on `V` alone, ignoring the cost
component, so stale `(higherCost, v)` frontier entries are correctly skipped:
```scala
given [E, V]: VisitedSet[(E, V)] = TupleVisitedSet(Set.empty[V])

case class TupleVisitedSet[E, V](visited: Set[V]) extends VisitedSet[(E, V)]:
  def isVisited(ev: (E, V)): Boolean              = visited.contains(ev._2)
  def markVisited(ev: (E, V)): VisitedSet[(E, V)] = copy(visited + ev._2)
```

**`Traversal.bestFirstWeighted`** — the entry point for Dijkstra/Prim:
```scala
def bestFirstWeighted[W: Ordering, R, J <: Appendable[(W, Option[R])]](
    start:   W,
    visitor: Visitor[W, R, J],
    goal:    W => Boolean = (_: W) => false
  )(using
    nbrs:    GraphNeighbours[W],
    ev:      Evaluable[W, R],
    vs:      VisitedSet[W],
    cu:      CostUpdate[W, IndexedPrioQueue],
    initial: IndexedPrioQueue[W]
  ): Visitor[W, R, J]
```

The external `TraversalResult[V, DirectedEdge[E, V]]` API is unchanged —
tuple unwrapping (`._2`) happens inside `DijkstraTraversal.run` / `PrimTraversal.run`.

---

## Four-Algorithm Comparison

| Algorithm | Frontier type | Entry point | Cost function | CostUpdate |
|-----------|--------------|-------------|---------------|------------|
| DFS | `Stack` (List) | `Traversal.dfs` | none | no-op |
| BFS | `Queue` | `Traversal.bfs` | none | no-op |
| Dijkstra | `IndexedPrioQueue[(E,V)]` | `Traversal.bestFirstWeighted` | cumulative path cost | `decreaseKey` |
| Prim | `IndexedPrioQueue[(E,V)]` | `Traversal.bestFirstWeighted` | edge weight only | `decreaseKey` |

All four share the same `Traversal.traverse` loop — they differ only in the
`Frontier`, `Neighbours`, `Evaluable`, and `CostUpdate` given instances provided.

---

## Key Files

### Visitor Library (V1.3.0)

| File | Change |
|------|--------|
| `PrioQueue.scala` | Three-type hierarchy: `BinaryHeap` (pure), `PrioQueue` (duplicates OK), `IndexedPrioQueue` (indexed, no duplicates, `decreaseKey`) |
| `Behaviours.scala` | Add `CostUpdate` typeclass + no-op given; add `TupleVisitedSet` + `given [E,V]: VisitedSet[(E,V)]`; add `Frontier[IndexedPrioQueue]` given |
| `CostUpdate.scala` | New file: `CostUpdate[W, F[_]]` typeclass + no-op given |
| `Traversal.scala` | Add `cu: CostUpdate[V, F]` to `traverse`; call `cu.update` after `offerAll` and at seed; add `bestFirstWeighted` entry point |

### Gryphon (Phase 2 — not yet done)

| File | Change |
|------|--------|
| `GraphTraversal.scala` | Rewrite `DijkstraTraversal` and `PrimTraversal` with `(E,V)` tuple frontier, `Neighbours[(E,V),(E,V)]`, `CostUpdate[…,IndexedPrioQueue]` |
| `ShortestPaths.scala` | Return type updated to `TraversalResult[V, Edge[E, V]]` |
| `PrimSpec.scala` | 6 tests currently pending; should pass after Phase 2 complete |
| `ShortestPathsSpec.scala` | May need updating for `Edge` return type |

---

## Tests Needed (Visitor V1.3.0)

### `BinaryHeapSpec`

- insert / removeMin ordering (min-heap)
- duplicate elements permitted
- single element; empty heap guard

### `PrioQueueSpec`

- offer / take in ascending order
- duplicate elements permitted and ordered correctly
- `emptyMax` variant dequeues in descending order

### `IndexedPrioQueueSpec`

- offer / take in ascending order
- duplicate offer is a no-op (`contains` before and after)
- `decreaseKey` repositions an existing entry correctly
- `decreaseKey` on absent element is a no-op
- `decreaseKey` with non-improving priority is a no-op
- `contains` returns true/false correctly
- `emptyMax` variant

### `FrontierSpec` additions

- `Frontier[IndexedPrioQueue]` offer/take ordering
- `Frontier[PrioQueue]` duplicate behaviour

### `TraversalSpec` additions

- `bestFirstWeighted` with a trivial graph and a `CostUpdate` that calls `decreaseKey`
- `TupleVisitedSet` correctly ignores cost component

---

## Future Work

- **Phase 2:** Refactor `DijkstraTraversal` and `PrimTraversal` to tuple-frontier
  approach (see §Tuple Frontier Approach above)
- **Kosaraju's SCC** — Strongly Connected Components for directed graphs
  - Two DFS passes: first on original graph (post-order), then on reversed graph
  - Needs `DirectedGraph.reverse` method
  - Lives alongside `ConnectedComponents` in `traverse` package
- **Kruskal's MST** — requires Union-Find
  - `UnionFindSpec` is currently entirely commented out
  - `WeightedUnionFind` also in attic
- **Verify book coverage** — systematically check all graph algorithms from
  Sedgewick & Wayne are implemented in Gryphon

---

## Package Structure

```
com.phasmidsoftware.gryphon
  .core          — Graph, VertexMap, Vertex, Edge, Adjacency, Traversable
  .adjunct       — DirectedGraph, UndirectedGraph, AttributedDirectedEdge, UndirectedEdge
  .traverse      — GraphTraversal, ShortestPaths, TopologicalSort,
                   TraversalResult, ConnectedComponents
  .parse         — GraphParser
  .util          — TryUsing, FP, GraphException

com.phasmidsoftware.visitor.core
                 — Traversal, BinaryHeap, PrioQueue, IndexedPrioQueue,
                   Frontier, CostUpdate, Evaluable, Neighbours, VisitedSet,
                   TupleVisitedSet, Visitor, JournaledVisitor, Journal, DfsOrder
```