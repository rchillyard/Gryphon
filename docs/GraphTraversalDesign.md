# Gryphon — GraphTraversal Design Document

## Overview

This document captures the design of Gryphon's graph algorithm suite and its
integration with the Visitor traversal engine. It covers the `WeightedTraversal`
abstraction, came-from semantics, graph construction fixes, the Java façade, and
the full algorithm inventory.

For the design of the Visitor library itself (priority queues, `CostUpdate`,
`Zero`/`Monoid`, `CameFromJournal`, the tuple frontier approach, and the Heisenbug)
see `VisitorDesign.md` in the Visitor repository.

---

## Current State (Gryphon V1.5.0 / Visitor V1.6.0)

### Scala Algorithm Suite

- `GraphTraversal[V, E, R]` trait — in `com.phasmidsoftware.gryphon.traverse`
- `DFSTraversal[V]` — delegates to `Traversal.dfs`
- `BFSTraversal[V]` — delegates to `Traversal.bfs`
- `WeightedTraversal[V, E: {Zero, Ordering}, R]` — abstract base class for Dijkstra
  and Prim, parameterised on `edgeCost`, `destination`, and `filterEdge`
- `DijkstraTraversal[V, E: {Monoid, Ordering}]` — extends `WeightedTraversal`;
  cumulative path cost
- `PrimTraversal[V, E: {Zero, Ordering}]` — extends `WeightedTraversal`; edge weight only
- `ShortestPaths.dijkstra` — delegates to `DijkstraTraversal.run`
- `MST.prim` — delegates to `PrimTraversal.run`
- `Kruskal.mst` — greedy edge sort + `Connectivity`; requires `Ordering[E]` only
- `Boruvka.mst` — parallel-round edge selection + `Connectivity`; requires `Monoid[E]` and `Ordering[E]`
- `Kosaraju.stronglyConnectedComponents` — two-pass DFS on original + reversed graph
- `ConnectedComponents` — connected components of undirected graphs
- `TopologicalSort` — DFS post-order on directed graphs
- `BellmanFord` — shortest paths with negative weights
- `AcyclicShortestPaths` — shortest paths on DAGs
- `TraversalResult[V, T]` — result type for all traversals
- `Connexions[V, E]` — DFS-based came-from map (Scala-side)

### Java Façade (V1.4.0)

- `Graph<V>`, `WeightedGraph<V,E>`, `Edge<V>`, `WeightedEdge<V,E>`
- `ShortestPaths` (Dijkstra Option 1 and Option 3)
- `MinimumSpanningTree` (Prim, Kruskal, and Borůvka, Option 1 and Option 3)
- `StronglyConnectedComponents` (Kosaraju)
- `Connectivity<V>`
- `JavaFacadeBridge` — internal Scala bridge; see `JavaFacadeDesign.md`
- All BFS and DFS (Option 1 and Option 3) delegate to Scala Visitor engine
  via `CameFromJournal`; start vertex absent from came-from map

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

### Context Bound Rationale

`WeightedTraversal` requires `E: {Zero, Ordering}` — the base class uses only
`Zero.identity` (to seed the frontier) and `Ordering` (to compare costs).
`DijkstraTraversal` requires `E: {Monoid, Ordering}` because `edgeCost` calls
`Monoid.combine`. Since `Monoid extends Zero`, `DijkstraTraversal`'s bound
satisfies `WeightedTraversal`'s. `PrimTraversal` stays at `E: {Zero, Ordering}` —
it genuinely never combines costs, and requiring `Monoid` for it would be dishonest.

---

## Came-From Semantics

The result of a BFS or DFS traversal is a **came-from map** — for each discovered
vertex, the vertex from which it was discovered during traversal. This is sometimes
called a "parent map" but that term implies a tree structure that pre-exists the
traversal. In a graph, the came-from relationship is an artifact of traversal order:
it records which vertex we happened to be visiting when we first discovered the
current vertex.

The Java façade returns `Map<V, V>` as a came-from map. **The start vertex is
absent** — it has no predecessor. This is consistent with all algorithm result maps
in the Java façade: Dijkstra SPT, Prim MST, and Kosaraju SCC all have the
source/start vertex absent.

**Path reconstruction** walks `map.get(v)` until the key is absent:
```java
List<V> path = new ArrayList<>();
V v = target;
path.add(v);
while (map.containsKey(v)) { v = map.get(v); path.add(0, v); }
// path starts with start vertex, ends with target
```

The Scala engine's `CameFromJournal` is wired into the Java façade via
`JavaFacadeBridge.bfs` / `dfs` / `bfsWithNeighbours` / `dfsWithNeighbours`.
All four entry points use `withQueueJournalAndCameFrom` or
`withListJournalAndCameFrom` and extract the came-from map via
`JournaledVisitor.cameFrom`.

---

## Four-Algorithm Comparison

| Algorithm | Frontier type | Entry point | Cost function | CostUpdate |
|-----------|--------------|-------------|---------------|------------|
| DFS | `Stack` (List) | `Traversal.dfs` | none | no-op |
| BFS | `Queue` | `Traversal.bfs` | none | no-op |
| Dijkstra | `IndexedPrioQueue[(E,V)]` | `Traversal.bestFirstWeighted` | cumulative path cost | `decreaseKey` |
| Prim | `IndexedPrioQueue[(E,V)]` | `Traversal.bestFirstWeighted` | edge weight only | `decreaseKey` |
| Kruskal | none (sort) | `Kruskal.mst` | edge weight (sort key) | n/a |
| Borůvka | none (rounds) | `Boruvka.mst` | edge weight (min per component) | n/a |

All four traversal algorithms share the same `Traversal.traverse` loop — they
differ only in the `Frontier`, `Neighbours`, `Evaluable`, and `CostUpdate` given
instances provided. Kruskal is not a traversal but a greedy sort-and-union algorithm.

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

## Key Files

### Visitor Library (V1.6.0)

| File | Description |
|------|-------------|
| `PrioQueue.scala` | `BinaryHeap` (pure), `PrioQueue` (duplicates OK), `IndexedPrioQueue` (indexed, `decreaseKey`) |
| `Behaviours.scala` | `Zero`, `Monoid`, `Evaluable`, `Neighbours`, `VisitedSet`, `Frontier`, `CostUpdate`; all given instances |
| `Visitor.scala` | `Visitor` trait (with `discover`); `JournaledVisitor` with optional `CameFromJournal` |
| `Journal.scala` | `Appendable`, `Journal`, `ListJournal`, `QueueJournal`, `CameFromJournal` |
| `Traversal.scala` | `traverse` (with `discover` calls); `dfs` (with `discover` calls); `bestFirstWeighted`; `DfsOrder` |
| `Tracer.scala` | `Tracer[V]` typeclass |

### Gryphon (V1.4.0)

| File | Description |
|------|-------------|
| `GraphTraversal.scala` | `GraphTraversal` trait; `DFSTraversal`, `BFSTraversal`, `WeightedTraversal`, `DijkstraTraversal`, `PrimTraversal` |
| `ShortestPaths.scala` | `ShortestPaths.dijkstra` entry point |
| `MST.scala` | `MST.prim` entry point; `E: {Zero, Ordering}` |
| `Kruskal.scala` | `Kruskal.mst`; greedy sort + `Connectivity` |
| `Boruvka.scala` | `Boruvka.mst`; parallel-round min-edge selection + `Connectivity`; direct algorithm, no traversal engine |
| `Kosaraju.scala` | Two-pass DFS SCC; `SCCResult[V]` type alias |
| `ConnectedComponents.scala` | `ConnectedComponents.components` |
| `TopologicalSort.scala` | Post-order DFS on DAGs |
| `BellmanFord.scala` | Shortest paths with negative weights |
| `AcyclicShortestPaths.scala` | Shortest paths on DAGs |
| `TraversalResult.scala` | `TraversalResult[V,T]`, `VertexTraversalResult`, `Connexions` |
| `VertexMap.scala` | Fixed `createVerticesFromTriplet`; `keysOnly` for graph reversal |
| `UndirectedGraph.scala` | Fixed `triplesToTryGraph`, `edges`; `isCyclic`, `isBipartite`, `isConnected` |
| `UndirectedEdge.scala` | Symmetric `equals`/`hashCode`: `UndirectedEdge(a,u,v) == UndirectedEdge(a,v,u)` |
| `DirectedGraph.scala` | `reverse`; `isCyclic` via `TopologicalSort`; `shortestPaths` via `BellmanFord` |
| `JavaFacadeBridge.scala` | All bridge methods; `bfs`/`dfs`/`bfsWithNeighbours`/`dfsWithNeighbours` via `CameFromJournal` |

---

## Future Work

- **Fix `DirectedGraph.addEdge`** — ensure destination vertex exists in `VertexMap`.
  See [Gryphon Issue #16](https://github.com/rchillyard/Gryphon/issues/16).

- **GraphML support** — add a `GraphMLParser` in the `parse` package (using
  `scala.xml`) and a corresponding Java `GraphReader`. GraphML is the standard
  interchange format supported by NetworkX, Gephi, JGraphT, and SNAP. Replaces
  the bespoke `.graph` format for test resources.

- **Scala `Graph` unification** — collapse `DirectedGraph[V,E]` and
  `UndirectedGraph[V,E]` into a single `Graph[V,E](directed: Boolean)`.
  Would simplify `JavaFacadeBridge` materialisation and remove the "architectural
  dinosaur" distinction. Also resolves Issue #16 naturally.

- **`Graph.reverse()` in Java façade** — expose as a public method on `Graph<V>`
  returning a new `Graph<V>` with all edge directions flipped. Currently
  `DirectedGraph.reverse` is called internally by Kosaraju but not accessible
  from Java.

- **`ConnectedComponents` Java façade** — `List<Set<V>> ConnectedComponents.find(Graph<V> g)`

- **`TopologicalSort` Java façade** — `List<V> TopologicalSort.sort(Graph<V> g)`

- **Java façade graph file reader** — expose a `GraphBuilder.fromFile(path)` or
  `GraphBuilder.fromResource(name)` factory so Java students can load `.graph`
  resource files directly, rather than building graphs programmatically via
  `addEdge`. Currently `GraphParser` is Scala-only with no Java wrapper.

- **Verify Sedgewick & Wayne book coverage** — systematically check all graph
  algorithms from the course textbook are implemented in Gryphon.

---

## Package Structure

```
com.phasmidsoftware.gryphon
  .core          — Graph, VertexMap, Vertex, Edge, Adjacency, Traversable
  .adjunct       — DirectedGraph, UndirectedGraph, AttributedDirectedEdge,
                   UndirectedEdge, Connectivity, ConnectivityOptimized
  .traverse      — GraphTraversal, WeightedTraversal, DijkstraTraversal,
                   PrimTraversal, ShortestPaths, MST, Kruskal, Boruvka, Kosaraju,
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
                   ListJournal, QueueJournal, CameFromJournal, DfsOrder,
                   Zero, Monoid, Tracer
```