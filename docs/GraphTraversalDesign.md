# Gryphon / Visitor — GraphTraversal Design Document

## Overview

This document captures the design for a unified `GraphTraversal` abstraction covering
four classic graph algorithms: DFS, BFS, Dijkstra, and Prim. It also documents the
required enhancements to the Visitor library to support efficient priority-queue-based
traversal with cost updates.

---

## Current State (as of Gryphon 0.2.4 / Visitor V1.2.0)

### Visitor Library
- `Traversal` engine with `traverse`, `dfs`, `bfs`, `bestFirst`, `bestFirstMax`
- `PrioQueue` backed by an immutable `BinaryHeap` (no index map, no `decreaseKey`)
- `Frontier[F[_]]` typeclass with implementations for `Stack`, `Queue`, `PrioQueue`
- `Evaluable[V, R]`, `Neighbours[V, V]`, `VisitedSet[V]` typeclasses
- `JournaledVisitor` with `ListJournal` (prepend) and `QueueJournal` (append/FIFO)

### Gryphon
- `GraphTraversal[V, E, R]` trait — **new**, in `com.phasmidsoftware.gryphon.traverse`
- `DFSTraversal[V]` — delegates to `Traversal.dfs`
- `BFSTraversal[V]` — delegates to `Traversal.bfs`
- `DijkstraTraversal[V, E]` — delegates to `Traversal.bestFirst` (currently buggy — no decreaseKey)
- `PrimTraversal[V, E]` — delegates to `Traversal.bestFirst` (currently buggy — no decreaseKey)
- `ShortestPaths.dijkstra` delegates to `DijkstraTraversal.run`
- `ConnectedComponents` — computes connected components of undirected graphs
- `TopologicalSort`, `ShortestPaths` — existing, working

### Known Issues
- `DijkstraTraversal` and `PrimTraversal` do not correctly handle cost improvements
  after a vertex has already been offered to the PrioQueue. This is because `PrioQueue`
  has no `decreaseKey` operation and `Traversal.traverse` has no hook for cost updates.
- Six tests in `PrimSpec` are currently pending.
- `ShortestPathsSpec` uses `Edge` return type (changed from `DirectedEdge`).

---

## Planned Enhancements

### Phase 1: Visitor Library (bump to V1.3.0)

#### 1. Indexed `BinaryHeap` with `decreaseKey`

Replace the current `BinaryHeap` with an indexed version that maintains a
`Map[T, Int]` of element → heap-array position.

```scala
private case class BinaryHeap[T](data: Vector[T], index: Map[T, Int])(using ord: Ordering[T]):
  def insert(t: T): BinaryHeap[T]           // O(log n) — no-op if already present
  def removeMin: (T, BinaryHeap[T])          // O(log n)
  def decreaseKey(oldT: T, newT: T): BinaryHeap[T]  // O(log n)
  def isEmpty: Boolean                        // O(1)
  def head: T                                 // O(1)
```

Key invariant: `index(t) == i` iff `data(i) == t` for all elements t.

#### 2. `decreaseKey` and `contains` on `PrioQueue`

```scala
case class PrioQueue[T] private(private val heap: BinaryHeap[T]):
  def offer(t: T): PrioQueue[T]
  def take: (T, PrioQueue[T])
  def decreaseKey(oldT: T, newT: T): PrioQueue[T]  // new
  def contains(t: T): Boolean                        // new
  def isEmpty: Boolean
  def head: T
```

#### 3. `CostUpdate[V, F[_]]` Typeclass

Add to `Behaviours.scala`:

```scala
trait CostUpdate[V, F[_]]:
  def update(frontier: F[V], v: V): F[V]

// Default no-op — used by DFS and BFS automatically
given [V, F[_]]: CostUpdate[V, F] with
  def update(frontier: F[V], v: V): F[V] = frontier
```

#### 4. Hook in `Traversal.traverse`

Add `cu: CostUpdate[V, F]` to context parameters and call after `offerAll`:

```scala
def traverse[V, R, J <: Appendable[(V, Option[R])], F[_]](
    start: V,
    visitor: Visitor[V, R, J],
    goal: V => Boolean = (_: V) => false
  )(using
    nbrs: Neighbours[V, V],
    ev: Evaluable[V, R],
    vs: VisitedSet[V],
    fr: Frontier[F],
    initial: F[V],
    cu: CostUpdate[V, F]   // new — defaults to no-op for DFS/BFS
  ): Visitor[V, R, J]
```

The loop gains one extra line:
```scala
val newFrontier = fr.offerAll(rest)(nbrs.neighbours(node).filterNot(newVisited.isVisited).toList)
val updatedFrontier = cu.update(newFrontier, node)  // new
loop(updatedFrontier, newVisitor, newVisited)
```

Also apply `cu.update` to the seed frontier after the start node.

---

### Phase 2: Gryphon — Fix `DijkstraTraversal` and `PrimTraversal`

Each provides a `given CostUpdate[V, PrioQueue]` that closes over the mutable
cost map and calls `decreaseKey` for any neighbour whose cost has improved:

```scala
given CostUpdate[V, PrioQueue] with
  def update(frontier: PrioQueue[V], v: V): PrioQueue[V] =
    graph.filteredAdjacencies(_ => true)(v).foldLeft(frontier) { (pq, adj) =>
      adj.maybeEdge[E].fold(pq) { e =>
        val destination = e match
          case ue: UndirectedEdge[E, V] => ue.other(v)
          case de                        => de.black
        if pq.contains(destination) then
          pq.decreaseKey(destination, destination)
        else pq
      }
    }
```

Note: `decreaseKey(destination, destination)` works because `Ordering[V]` is
derived from the mutable cost map — the same vertex object compares differently
after `relax` has updated its cost entry.

---

## Four-Algorithm Comparison

| Algorithm | Frontier | Cost function | CostUpdate |
|-----------|----------|---------------|------------|
| DFS | Stack (List) | none | no-op |
| BFS | Queue | none | no-op |
| Dijkstra | PrioQueue | cumulative path cost | decreaseKey |
| Prim | PrioQueue | edge weight only | decreaseKey |

All four share the same `Traversal.traverse` loop — they differ only in
the `Frontier`, `Evaluable`, and `CostUpdate` given instances provided.

---

## Key Files

### Visitor Library
| File | Change |
|------|--------|
| `PrioQueue.scala` | Replace BinaryHeap with indexed version; add decreaseKey/contains to PrioQueue |
| `Behaviours.scala` | Add CostUpdate typeclass and default no-op given |
| `Traversal.scala` | Add cu: CostUpdate parameter to traverse; call cu.update after offerAll |

### Gryphon
| File | Change |
|------|--------|
| `GraphTraversal.scala` | Add CostUpdate given instances to DijkstraTraversal and PrimTraversal |
| `ShortestPaths.scala` | Return type updated to TraversalResult[V, Edge[E, V]] |
| `PrimSpec.scala` | 6 tests currently pending; should pass after Phase 1+2 complete |
| `ShortestPathsSpec.scala` | May need updating for Edge return type |

---

## Future Work (after Prim is working)

- **Kosaraju's SCC** — Strongly Connected Components for directed graphs
    - Two DFS passes: first on original graph (post-order), then on reversed graph
    - Needs `DirectedGraph.reverse` method
    - Lives alongside `ConnectedComponents` in `traverse` package

- **Kruskal's MST** — requires Union-Find
    - `UnionFindSpec` is currently entirely commented out
    - `WeightedUnionFind` also in attic

- **Verify book coverage** — systematically check all graph algorithms from
  Robin's book are implemented in Gryphon

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
                 — Traversal, PrioQueue, BinaryHeap, Frontier, CostUpdate,
                   Evaluable, Neighbours, VisitedSet, Visitor, JournaledVisitor,
                   Journal, DfsOrder
```