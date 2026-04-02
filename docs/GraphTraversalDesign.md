# Gryphon / Visitor — GraphTraversal Design Document

## Overview

This document captures the design for a unified `GraphTraversal` abstraction covering
four classic graph algorithms: DFS, BFS, Dijkstra, and Prim. It also documents the
required enhancements to the Visitor library to support efficient priority-queue-based
traversal with cost updates.

---

## Current State (as of Gryphon 0.2.5 / Visitor V1.3.0)

### Visitor Library

- `Traversal` engine with `traverse`, `dfs`, `bfs`, `bestFirst`, `bestFirstMax`,
  `bestFirstWeighted`
- Three-type priority queue hierarchy: `BinaryHeap` → `PrioQueue` → `IndexedPrioQueue`
  (see §Priority Queue Design Decision below)
- `Frontier[F[_]]` typeclass with implementations for `Stack`, `Queue`, `PrioQueue`,
  and `IndexedPrioQueue`
- `CostUpdate[W, F[_]]` typeclass with default no-op given
- `TupleVisitedSet[E, V]` and `given [E, V]: VisitedSet[(E, V)]` — supports
  weighted tuple frontiers for Dijkstra/Prim
- `Evaluable[V, R]`, `Neighbours[V, V]`, `VisitedSet[V]` typeclasses (unchanged)
- `JournaledVisitor` with `ListJournal` (prepend) and `QueueJournal` (append/FIFO)

### Gryphon

- `GraphTraversal[V, E, R]` trait — in `com.phasmidsoftware.gryphon.traverse`
- `DFSTraversal[V]` — delegates to `Traversal.dfs`
- `BFSTraversal[V]` — delegates to `Traversal.bfs`
- `DijkstraTraversal[V, E]` — delegates to `Traversal.bestFirstWeighted` ✓
- `PrimTraversal[V, E]` — delegates to `Traversal.bestFirstWeighted` ✓
- `ShortestPaths.dijkstra` delegates to `DijkstraTraversal.run`
- `ConnectedComponents` — computes connected components of undirected graphs
- `TopologicalSort`, `ShortestPaths` — existing, working

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
one also enforces correct usage at compile time.

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
first offered. The `CostUpdate` instance closes over two mutable maps:
- `pred: mutable.Map[V, Edge]` — the cheapest known incoming edge per vertex
- `bestCost: mutable.Map[V, E]` — current best known cost per vertex, needed
  to locate the old frontier entry for `decreaseKey`

### Bookkeeping Ownership

A critical design invariant: **`Neighbours` is pure — it generates `(cost, vertex)`
tuples with no side effects**. All writes to `bestCost` and `pred` are owned
exclusively by `CostUpdate.update`, which handles two cases:
- `None` — first discovery of vertex `w`; `offerAll` has already offered the
  tuple, `CostUpdate` records `bestCost(w)` and `pred(w)`
- `Some(oldCost)` with improvement and `w` still in frontier — calls `decreaseKey`
  and updates both maps

This separation was discovered through debugging: when `Neighbours` also wrote to
`bestCost`, the `CostUpdate` `decreaseKey` check would find a stale `oldCost`
value and either miss improvements or look up the wrong frontier entry.

---

## Tuple Frontier Approach for Dijkstra/Prim

### Motivation

The original Dijkstra/Prim used `Ordering[V]` derived from a mutable cost map,
with the frontier holding plain `V` vertices. This was replaced with an explicit
`(E, V)` tuple frontier:

- **Correctness:** Cost is encoded in the frontier element itself — no implicit
  dependency on external mutable state for ordering.
- **Purity:** `Ordering[(E, V)]` is `Ordering.by(_._1)`, derived purely from
  `E`'s `Ordering`. No mutable state leaks into the ordering.
- **Clarity:** The frontier element type makes the cost explicit at every step.

### Design

The frontier element type is `W = (E, V)` where `E` is the cost type and `V` is
the vertex type.

**`Neighbours[(E, V), (E, V)]`** — pure cost expansion, no side effects.

**`VisitedSet[(E, V)]`** — tracks visited-ness on `V` alone, ignoring the cost
component, so stale `(higherCost, v)` frontier entries are correctly skipped:
```scala
given [E, V]: VisitedSet[(E, V)] = TupleVisitedSet(Set.empty[V])
```

The external `TraversalResult[V, DirectedEdge[E, V]]` API is unchanged —
tuple unwrapping happens inside `DijkstraTraversal.run` / `PrimTraversal.run`.

### Lazy Evaluation Bug (Heisenbug)

During implementation, a subtle Scala lazy evaluation bug was discovered. Inside
a `given Neighbours` that returns an `Iterator`, tuple pattern matching:

```scala
val (accCost, v) = ev  // WRONG — can generate lazy binding in Iterator context
```

caused `accCost` to not be materialised at the point of destructuring but at the
point of use inside the `map` lambda — by which time `ev` may have been rebound.
This caused `en.plus(accCost, e.attribute)` to return just `e.attribute` (as if
`accCost = en.zero`), producing wrong cumulative costs in Dijkstra.

The bug was discovered because adding a `println(s"accCost=$accCost")` inside
the lambda forced strict evaluation as a side effect and made the bug disappear —
a classic Heisenbug.

**Fix:** Always use strict `val` extraction with explicit type ascriptions when
destructuring tuples inside `given` instances that return lazy collections:

```scala
val accCost: E = ev._1  // CORRECT — strict binding
val v: V       = ev._2
```

---

## Undirected Graph Construction Fixes

Several bugs in undirected graph construction were exposed by the Prim tests,
which were the first tests to exercise a properly bidirectional undirected graph.

### `VertexMap.createVerticesFromTriplet` — reverse adjacency orientation

The reverse adjacency was constructed by calling `g(vv2, vv1, ...)` — swapping
vertex arguments — which produced a new `UndirectedEdge` with swapped endpoints
and `flipped=false`. This meant all adjacencies had `flipped=false`, so reverse
traversal was incorrect.

**Fix:** Construct the reverse adjacency as `AdjacencyEdge(connexion, flipped=true)`
on the original connexion, rather than calling `g` again with swapped arguments.

### `UndirectedGraph.triplesToTryGraph` — hardcoded `condition=false`

The call to `createVerticesFromTriplet` passed `false` as the `condition` parameter,
meaning the reverse adjacency was never added. Every undirected edge was only stored
in one direction.

**Fix:** Pass `!t._4.oneWay` as the condition — `true` for undirected edges,
`false` for directed ones.

### `UndirectedGraph.edges` — threw on flipped adjacencies

`getUndirectedEdgeFromAdjacency` matched `AdjacencyEdge(e, false)` and threw
`GraphException` on `AdjacencyEdge(e, true)`. Once both directions were stored,
`edges` threw on every reverse entry.

**Fix:** Use `collect` to silently skip `flipped=true` entries:
```scala
def edges: Iterator[UndirectedEdge[E, V]] =
  adjacencies.collect { case AdjacencyEdge(e: UndirectedEdge[E, V] @unchecked, false) => e }
```

### `Traversable.getConnexions` — VertexPair not accepted by addConnexion

For flipped adjacencies, `getConnexions` constructed `VertexPair(connexion.black, child)`
and passed it to `Connexions.addConnexion`, which only accepts `AttributedDirectedEdge`
and `UndirectedEdge` — throwing `GraphException` on properly bidirectional graphs.

**Fix:** Pass `connexion` directly to `addConnexion` regardless of `flipped`.
`addConnexion` already handles `UndirectedEdge` correctly via `u.other(v)`.

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
| `Traversal.scala` | Add `cu: CostUpdate[V, F]` to `traverse`; call `cu.update` after `offerAll` and at seed; add `bestFirstWeighted` entry point; extract neighbours as strict val |

### Gryphon (V0.2.5)

| File | Change |
|------|--------|
| `GraphTraversal.scala` | Rewrite `DijkstraTraversal` and `PrimTraversal` with `(E,V)` tuple frontier, pure `Neighbours[(E,V),(E,V)]`, `CostUpdate[…,IndexedPrioQueue]`; strict `ev._1`/`ev._2` extraction |
| `VertexMap.scala` | Fix `createVerticesFromTriplet` reverse adjacency orientation |
| `UndirectedGraph.scala` | Fix `triplesToTryGraph` condition; fix `edges` to use `collect`; override `M` with `edges.size` |
| `Traversable.scala` | Fix `getConnexions` to pass connexion directly to `addConnexion` |
| `PrimSpec.scala` | 11 new tests covering graph structure, MST correctness, and start-vertex independence |

---

## Future Work

- **Kosaraju's SCC** — Strongly Connected Components for directed graphs
  - Two DFS passes: first on original graph (post-order), then on reversed graph
  - Needs `DirectedGraph.reverse` method
  - Lives alongside `ConnectedComponents` in `traverse` package
- **Kruskal's MST** — requires Union-Find
  - `UnionFindSpec` is currently entirely commented out
  - `WeightedUnionFind` also in attic
- **Verify book coverage** — systematically check all graph algorithms from
  Sedgewick & Wayne are implemented in Gryphon
- **Merge `DijkstraTraversal` and `PrimTraversal`** — the two implementations are
  nearly identical; the only difference is the cost function (`en.plus(acc, w)` vs
  `w` alone). An abstract `WeightedTraversal` base class parameterised on a cost
  function would eliminate the duplication. Defer until both are confirmed stable.
- **Performance** — `IndexedPrioQueue.decreaseKey` is O(n log n); `offer`/`take`
  rebuild index in O(n). Acceptable for current graph sizes but could be optimised
  if needed.

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