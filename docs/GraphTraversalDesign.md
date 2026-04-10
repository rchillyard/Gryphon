# Gryphon / Visitor — GraphTraversal Design Document

## Overview

This document captures the design for the unified `GraphTraversal` abstraction
covering four classic graph algorithms: DFS, BFS, Dijkstra, and Prim. It also
documents the enhancements to the Visitor library that support efficient
priority-queue-based traversal with cost updates, and the current state of the
full Gryphon algorithm suite.

---

## Current State (Gryphon V1.3.0 / Visitor V1.5.0)

### Visitor Library

- `Traversal` engine with `traverse`, `dfs`, `bfs`, `bestFirst`, `bestFirstMax`,
  `bestFirstWeighted`
- Three-type priority queue hierarchy: `BinaryHeap` → `PrioQueue` → `IndexedPrioQueue`
  (see §Priority Queue Design Decision below)
- `Frontier[F[_]]` typeclass with implementations for `Stack`, `Queue`, `PrioQueue`,
  and `IndexedPrioQueue`
- `CostUpdate[W, F[_]]` typeclass with default no-op given
- `TupleVisitedSet[(E,V)]` and `given [E, V]: VisitedSet[(E, V)]` — supports
  weighted tuple frontiers for Dijkstra/Prim
- `Evaluable[V, R]`, `Neighbours[V, V]`, `VisitedSet[V]` typeclasses (unchanged)
- `JournaledVisitor` with `ListJournal` (prepend) and `QueueJournal` (append/FIFO)
- `Monoid[A]` typeclass (mirroring Cats: `identity` + `combine`) replaces `Numeric[E]`
  as the context bound for weighted traversals; `given` instances for `Int`, `Long`,
  `Double`, `Float`
- `Tracer[V]` typeclass for optional debug tracing at configurable verbosity levels

### Gryphon

- `GraphTraversal[V, E, R]` trait — in `com.phasmidsoftware.gryphon.traverse`
- `DFSTraversal[V]` — delegates to `Traversal.dfs`
- `BFSTraversal[V]` — delegates to `Traversal.bfs`
- `WeightedTraversal[V, E, R]` — abstract base class for Dijkstra and Prim,
  parameterised on `edgeCost`, `destination`, and `filterEdge`
- `DijkstraTraversal[V, E]` — extends `WeightedTraversal`; cumulative path cost
- `PrimTraversal[V, E]` — extends `WeightedTraversal`; edge weight only
- `ShortestPaths.dijkstra` — delegates to `DijkstraTraversal.run`
- `MST.prim` — delegates to `PrimTraversal.run`
- `Kruskal.mst` — greedy edge sort + Union-Find; requires `Ordering[E]` only
- `Kosaraju.stronglyConnectedComponents` — two-pass DFS on original + reversed graph
- `ConnectedComponents` — connected components of undirected graphs
- `TopologicalSort` — DFS post-order on directed graphs
- `BellmanFord` — shortest paths with negative weights
- `AcyclicShortestPaths` — shortest paths on DAGs
- `TraversalResult[V, T]` — result type for all traversals
- `Connexions[V, E]` — DFS-based came-from map

### Java Façade (V1.3.0)

- `Graph<V>`, `WeightedGraph<V,E>`, `Edge<V>`, `WeightedEdge<V,E>`
- `ShortestPaths` (Dijkstra), `MinimumSpanningTree` (Prim, Kruskal)
- `StronglyConnectedComponents` (Kosaraju), `Connectivity<V>`
- `JavaFacadeBridge` — internal Scala bridge; see `JavaFacadeDesign.md`

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

For Dijkstra and Prim, `WeightedTraversal` provides a
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

The external `TraversalResult[V, DirectedEdge[V, E]]` API is unchanged —
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

## WeightedTraversal — Unified Dijkstra/Prim Base Class

`DijkstraTraversal` and `PrimTraversal` share the same `Traversal.bestFirstWeighted`
backbone, differing in only three methods:

| Method | Dijkstra | Prim |
|--------|----------|------|
| `edgeCost(accCost, e, v)` | `Monoid.combine(accCost, e.attribute)` — cumulative | `e.attribute` — edge weight only |
| `destination(v, e)` | `e.black` — directed | `e.other(v)` — undirected |
| `filterEdge(e)` | Only `AttributedDirectedEdge` | All edge types |

The result type differs accordingly: Dijkstra produces
`TraversalResult[V, AttributedDirectedEdge[V, E]]`; Prim produces
`TraversalResult[V, Edge[V, E]]`.

---

## Came-From Semantics

The result of a BFS or DFS traversal is a **came-from map** — for each discovered
vertex, the vertex from which it was discovered during traversal. This is sometimes
called a "parent map" but that term implies a tree structure that pre-exists the
traversal. In a graph, the came-from relationship is an artifact of traversal order:
it records which vertex we happened to be visiting when we first discovered the
current vertex.

The Java façade (`GraphTraversal.bfs` / `dfs`) returns `Map<V, V>` as a came-from
map. The Scala traversal engine currently journals `(vertex, evaluatedValue)` pairs
in visit order but does not record came-from pointers. A `CameFromJournal[V]`
extension to Visitor would allow the Java façade to delegate BFS/DFS fully to the
Scala engine. This is tracked as
[Visitor Issue #10](https://github.com/rchillyard/Visitor/issues/10).

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
def edges: Iterator[UndirectedEdge[V, E]] =
  adjacencies.collect { case AdjacencyEdge(e: UndirectedEdge[V, E] @unchecked, false) => e }
```

### `Traversable.getConnexions` — VertexPair not accepted by addConnexion

For flipped adjacencies, `getConnexions` constructed `VertexPair(connexion.black, child)`
and passed it to `Connexions.addConnexion`, which only accepts `AttributedDirectedEdge`
and `UndirectedEdge` — throwing `GraphException` on properly bidirectional graphs.

**Fix:** Pass `connexion` directly to `addConnexion` regardless of `flipped`.
`addConnexion` already handles `UndirectedEdge` correctly via `u.other(v)`.

### `DirectedGraph.addEdge` — destination vertex not ensured

`DirectedGraph.addEdge` calls `vertexMap.modifyVertex` which only touches the
`from` vertex. Destination-only vertices (reachable but never a source) are
silently absent from the `VertexMap`, causing `key not found` errors when any
traversal attempts to expand them. `UndirectedGraph.addEdge` is correct — it
delegates to `VertexMap.+[E]` which calls `ensure` for both endpoints.

**Workaround:** `JavaFacadeBridge` materialisation methods fold directly over
`VertexMap.+[E]` rather than through `DirectedGraph.addEdge`. The underlying
bug is tracked as
[Gryphon Issue #16](https://github.com/rchillyard/Gryphon/issues/16).

---

## Four-Algorithm Comparison

| Algorithm | Frontier type | Entry point | Cost function | CostUpdate |
|-----------|--------------|-------------|---------------|------------|
| DFS | `Stack` (List) | `Traversal.dfs` | none | no-op |
| BFS | `Queue` | `Traversal.bfs` | none | no-op |
| Dijkstra | `IndexedPrioQueue[(E,V)]` | `Traversal.bestFirstWeighted` | cumulative path cost | `decreaseKey` |
| Prim | `IndexedPrioQueue[(E,V)]` | `Traversal.bestFirstWeighted` | edge weight only | `decreaseKey` |
| Kruskal | none (sort) | `Kruskal.mst` | edge weight (sort key) | n/a |

All four traversal algorithms share the same `Traversal.traverse` loop — they
differ only in the `Frontier`, `Neighbours`, `Evaluable`, and `CostUpdate` given
instances provided. Kruskal is not a traversal but a greedy sort-and-union algorithm.

---

## Key Files

### Visitor Library (V1.5.0)

| File | Description |
|------|-------------|
| `PrioQueue.scala` | Three-type hierarchy: `BinaryHeap` (pure), `PrioQueue` (duplicates OK), `IndexedPrioQueue` (indexed, no duplicates, `decreaseKey`) |
| `Behaviours.scala` | `CostUpdate` typeclass + no-op given; `TupleVisitedSet`; `Frontier[IndexedPrioQueue]`; `Monoid[A]` + given instances; `Tracer[V]` |
| `Traversal.scala` | `traverse` with `CostUpdate`; `bestFirstWeighted`; strict tuple extraction |
| `Journal.scala` | `Appendable`, `Journal`, `ListJournal`, `QueueJournal` |

### Gryphon (V1.3.0)

| File | Description |
|------|-------------|
| `GraphTraversal.scala` | `GraphTraversal` trait; `DFSTraversal`, `BFSTraversal`, `WeightedTraversal`, `DijkstraTraversal`, `PrimTraversal` |
| `ShortestPaths.scala` | `ShortestPaths.dijkstra` entry point |
| `MST.scala` | `MST.prim` entry point |
| `Kruskal.scala` | `Kruskal.mst`; greedy sort + `Connectivity` |
| `Kosaraju.scala` | Two-pass DFS SCC; `SCCResult[V]` type alias |
| `ConnectedComponents.scala` | `ConnectedComponents.components` |
| `TopologicalSort.scala` | Post-order DFS on DAGs |
| `BellmanFord.scala` | Shortest paths with negative weights |
| `AcyclicShortestPaths.scala` | Shortest paths on DAGs |
| `TraversalResult.scala` | `TraversalResult[V,T]`, `VertexTraversalResult`, `Connexions` |
| `VertexMap.scala` | Fixed `createVerticesFromTriplet`; `keysOnly` for graph reversal |
| `UndirectedGraph.scala` | Fixed `triplesToTryGraph`, `edges`; `isCyclic`, `isBipartite`, `isConnected`, degree methods |
| `DirectedGraph.scala` | `reverse`; `isCyclic` via `TopologicalSort`; `shortestPaths` via `BellmanFord` |
| `JavaFacadeBridge.scala` | Internal Scala bridge for the Java façade |

---

## Future Work

- **`CameFromJournal[V]` in Visitor** — would allow `GraphTraversal.bfs`/`dfs`
  in the Java façade to delegate fully to the Scala engine rather than being
  reimplemented in Java. See [Visitor Issue #10](https://github.com/rchillyard/Visitor/issues/10).

- **Fix `DirectedGraph.addEdge`** — ensure destination vertex exists in `VertexMap`.
  See [Gryphon Issue #16](https://github.com/rchillyard/Gryphon/issues/16).

- **Decouple `Monoid` from `Prim`** — `PrimTraversal` requires `Monoid[E]` but
  only uses `identity`; `combine` is never called. Decoupling would clean up
  `JavaFacadeBridge.primCustom`'s stub `Monoid`.
  See [Visitor Issue #9](https://github.com/rchillyard/Visitor/issues/9).

- **GraphML support** — add a `GraphMLParser` in the `parse` package (using
  `scala.xml`) and a corresponding Java `GraphReader`. GraphML is the standard
  interchange format supported by NetworkX, Gephi, JGraphT, and SNAP.

- **Scala `Graph` unification** — collapse `DirectedGraph[V,E]` and
  `UndirectedGraph[V,E]` into a single `Graph[V,E](directed: Boolean)`.
  Would simplify `JavaFacadeBridge` materialisation and remove the "architectural
  dinosaur" distinction.

- **Performance** — `IndexedPrioQueue.decreaseKey` is O(n log n); `offer`/`take`
  rebuild index in O(n). Acceptable for current graph sizes but could be optimised
  if needed.

- **In-order DFS** — for binary trees only; requires `BinaryNeighbours[H,V]`
  typeclass yielding exactly `(left, right)` and a separate `dfsInOrder` entry
  point in Visitor.

---

## Package Structure

```
com.phasmidsoftware.gryphon
  .core          — Graph, VertexMap, Vertex, Edge, Adjacency, Traversable
  .adjunct       — DirectedGraph, UndirectedGraph, AttributedDirectedEdge,
                   UndirectedEdge, Connectivity, ConnectivityOptimized
  .traverse      — GraphTraversal, WeightedTraversal, DijkstraTraversal,
                   PrimTraversal, ShortestPaths, MST, Kruskal, Kosaraju,
                   TopologicalSort, ConnectedComponents, BellmanFord,
                   AcyclicShortestPaths, TraversalResult, Connexions
  .parse         — GraphParser
  .util          — TryUsing, FP, GraphException
  .java          — Graph, WeightedGraph, Edge, WeightedEdge, ShortestPaths,
                   MinimumSpanningTree, StronglyConnectedComponents,
                   Connectivity, JavaFacadeBridge

com.phasmidsoftware.visitor.core
                 — Traversal, BinaryHeap, PrioQueue, IndexedPrioQueue,
                   Frontier, CostUpdate, Evaluable, Neighbours, VisitedSet,
                   TupleVisitedSet, Visitor, JournaledVisitor, Journal,
                   ListJournal, QueueJournal, DfsOrder, Monoid, Tracer
```