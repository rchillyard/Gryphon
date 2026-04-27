# Gryphon Java Façade — Design Document

## Overview

The Gryphon Java façade provides a Java-idiomatic API over Gryphon's purely
functional Scala graph library. Its primary audience is students in INFO6205
(Data Structures, Algorithms, and Invariants: a Practical Guide) at
Northeastern University who are not familiar with Scala.

The façade lives in `src/main/java` and `src/test/java` within the root Gryphon
project (no separate subproject). Java students interact only with Java types;
all Scala machinery is hidden.

---

## Design Principles

1. **Hide all Scala-isms.** No `Option`, no typeclasses, no `given`/`implicit`,
   no companion object syntax. Java students see only Java.

2. **Mutable façade over immutable core.** Gryphon's Scala types are persistent
   and immutable. The Java façade is mutable in the Java idiom — mutating
   operations update an internal reference rather than requiring the student to
   capture a return value.

3. **Lazy materialisation.** `Graph<V>` accumulates edges and vertices in Java
   collections and builds the underlying Scala graph on demand when a weighted
   algorithm is requested. The Scala graph is an implementation detail — a cache
   that is invalidated on every mutation.

4. **Two-tier traversal API.** Every traversal method is provided in two forms:
    - **Option 1** — sensible defaults, works out of the box.
    - **Option 3** — caller supplies a functional interface (neighbour function,
      weight extractor, combiner, etc.), matching the typeclass abstraction of
      the Scala API.

   Option 2 (builder pattern) was considered and rejected as adding complexity
   without pedagogical value.

5. **`Graph<V>` for unweighted, `WeightedGraph<V,E>` for weighted.** Directed
   and undirected variants are distinguished by factory methods (`directed()` /
   `undirected()` on `Graph<V>`, `directedWeighted()` / `undirectedWeighted()`
   on `WeightedGraph<V,E>`). There is no separate `DirectedGraph` /
   `UndirectedGraph` hierarchy.

6. **`WeightedGraph<V,E>` is a subtype of `Graph<V>`.** A weighted graph is a
   more constrained graph (all edges must carry an attribute of type `E`).
   Anywhere a `Graph<V>` is accepted, a `WeightedGraph<V,E>` can be substituted
   (Liskov). The reverse is not true. This is the correct direction: `Graph<V>`
   is the more general type; `WeightedGraph<V,E>` is the specialisation.

7. **No `Void` avoidance.** `WeightedEdge<V, Void>` (with `null` attribute) is
   a legitimate type for contexts that require the `WeightedEdge` type but carry
   no actual weight. Students are not shielded from `Void`; it is more honest
   than defaulting to weight `1.0`.

8. **BFS/DFS stay in Java.** Plain BFS and DFS are implemented directly in Java
   (`GraphTraversal`) and do not delegate to the Scala engine. The Scala engine
   is used for weighted algorithms (Dijkstra, Prim, Kruskal, Kosaraju) that have
   no natural Java equivalent. The reason: the Scala traversal engine journals
   visited vertices in order but does not record parent pointers — the
   `Map<V, V>` parent-tree result needed by the Java façade is not producible
   from the Scala journal without a Visitor library change (see Deferred Work).

9. **Dijkstra needs Monoid; Prim and Kruskal need only Ordering.** Dijkstra
   accumulates path costs along a route, requiring both a `combine` function
   (e.g. addition) and a `zero` identity element. Prim and Kruskal compare
   individual edge attributes but never combine them, so their Option 3 API
   requires only a `Comparator<E>`. This asymmetry is intentional and correct.

---

## Current State (V1.5.3)

### Implemented

| Class                         | Package        | Description                                                             |
|-------------------------------|----------------|-------------------------------------------------------------------------|
| `Edge<V>`                     | `gryphon.java` | Immutable unweighted edge with `from`, `to`, `reverse`                  |
| `WeightedEdge<V,E>`           | `gryphon.java` | Extends `Edge<V>` with typed `attribute`                                |
| `Graph<V>`                    | `gryphon.java` | Mutable lazy-builder façade; directed/undirected                        |
| `WeightedGraph<V,E>`          | `gryphon.java` | Extends `Graph<V>`; typed edge attribute; `weightedEdges()`             |
| `GraphTraversal`              | `gryphon.java` | Package-private BFS/DFS; returns `Map<V,V>` parent trees                |
| `Connectivity<V>`             | `gryphon.java` | Mutable façade over `Connectivity` / `ConnectivityOptimized`            |
| `ShortestPaths`               | `gryphon.java` | Dijkstra (Option 1: `Double`; Option 3: custom combine/zero/comparator) |
| `MinimumSpanningTree`         | `gryphon.java` | Prim, Kruskal, and Borůvka (Option 1: `Double`; Option 3: custom comparator only) |
| `StronglyConnectedComponents` | `gryphon.java` | Kosaraju; `count()`; `components()`                                     |
| `ConnectedComponents`         | `gryphon.java` | Connected components (Option 1 only); `count`, `componentIds`, `components` |
| `TopologicalSort`             | `gryphon.java` | Topological sort of DAGs; `sort` returns `Optional<List<V>>`; `isDAG` predicate |
| `JavaFacadeBridge`            | `gryphon.java` | Internal Scala bridge: graph materialisation and algorithm delegation   |

### Test Coverage

| Test class                        | Framework | Status  |
|-----------------------------------|-----------|---------|
| `ConnectivityTest`                | JUnit 5   | ✅ Green |
| `GraphTest`                       | JUnit 5   | ✅ Green |
| `ShortestPathsTest`               | JUnit 5   | ✅ Green |
| `MinimumSpanningTreeTest`         | JUnit 5   | ✅ Green |
| `BoruvkaTest`                     | JUnit 5   | ✅ Green |
| `TunnelsTest`                     | JUnit 5   | ✅ Green |
| `TunnelsFromResourceTest`         | JUnit 5   | ✅ Green |
| `ConnectedComponentsTest`         | JUnit 5   | ✅ Green |
| `TopologicalSortTest`             | JUnit 5   | ✅ Green |
| `GraphReverseTest`                | JUnit 5   | ✅ Green |
| `WeightedGraphFromResourceTest`   | JUnit 5   | ✅ Green |
| `StronglyConnectedComponentsTest` | JUnit 5   | ✅ Green |

### Build

- JUnit 5 via `com.github.sbt:junit-interface:0.13.3`
- `javacOptions ++= Seq("--release", "21")`
- Java 21 (Oracle OpenJDK 21.0.1)
- sbt 1.12.7

### Real-world validation

The Java façade has been validated against the Northeastern University tunnel
network design (`Tunnels_Gryphon.java` in DSAIPG). Using
`WeightedGraph<Building, TunnelProperties>` with Prim's algorithm (ordered by
`TunnelProperties.cost`), the façade produces a minimum spanning tree of 79
tunnels connecting 80 campus buildings at a total cost of $6,648,954 — agreeing
exactly with independent Kruskal and Borůvka implementations, confirming correctness.

A self-contained integration test (`TunnelsTest`, `TunnelsFromResourceTest`) is
included in Gryphon's own test suite, using building codes as `String` vertices
and `Long` costs, loading from `tunnels.graph` via `WeightedGraph.undirectedFromResource`.

---

## Architecture

### `Graph<V>` — Lazy Builder

```
Graph<V>
  boolean                  directed        // fixed at construction
  Map<V, List<Edge<V>>>    adjacency       // full adjacency (both dirs for undirected)
  List<Edge<V>>            canonicalEdges  // one entry per addEdge call
  AbstractGraph            scalaGraph      // cache; null when invalidated
```

`addEdge` appends to `canonicalEdges`, updates `adjacency` (both directions for
undirected graphs), and sets `scalaGraph = null`.

Plain traversals (`bfs`, `dfs`) delegate to `GraphTraversal`, which operates on
the Java `adjacency` map directly and returns a `Map<V, V>` parent tree.

Weighted algorithms (`ShortestPaths`, `MinimumSpanningTree`,
`StronglyConnectedComponents`) call `getScalaGraph()` (package-private) to
materialise the Scala cache, then delegate to `JavaFacadeBridge`.

### `WeightedGraph<V,E>` — Typed Edge Subtype

Extends `Graph<V>` with:
- `addEdge(WeightedEdge<V,E>)` — primary mutation method; type-safe
- `addEdge(Edge<V>)` — overridden to throw `IllegalArgumentException` if the
  edge is not a `WeightedEdge`, enforcing the type invariant
- `weightedEdges()` — returns `List<WeightedEdge<V,E>>` for use by the bridge,
  eliminating all runtime casts
- Factory methods `directedWeighted()` and `undirectedWeighted()` — distinct
  names required due to Java type erasure (they would clash with `Graph<V>`'s
  `directed()` and `undirected()` after erasure)

### `GraphTraversal` — Package-Private

Iterative BFS (queue-based) and DFS (explicit stack) to avoid stack overflow on
large graphs. Both return `Map<V, V>` parent maps — the traversal tree rooted at
the start vertex. The start vertex maps to itself; unreachable vertices are absent.

### `JavaFacadeBridge` — Internal Scala Object

Package-private Scala `object` handling all Scala-side concerns:

- `materialise(edges, directed)` — builds `DirectedGraph[V, Unit]` or
  `UndirectedGraph[V, Unit]` from the Java canonical edge list.
- `materialiseWeighted(edges)` — builds `DirectedGraph[V, E]` from a typed
  `WeightedEdge` list; no casts needed.
- `materialiseWeightedUndirected(edges)` — builds `UndirectedGraph[V, E]`
  similarly.
- `dijkstraDouble` / `dijkstraCustom` — Dijkstra delegation. `dijkstraCustom`
  takes `combineFn`, `zero`, and `comparator` to construct `Monoid[E]` and
  `Ordering[E]`.
- `primDouble` / `primCustom` — Prim delegation. `primCustom` takes only
  `comparator`; `Monoid.combine` stub returns `identity` and is never called.
- `kruskalDouble` / `kruskalCustom` — Kruskal delegation. `kruskalCustom` takes
  only `comparator`.
- `kosaraju` — Kosaraju delegation.

### `ShortestPaths` — Static Façade

Returns SPT as `Map<V, WeightedEdge<V, E>>`. Option 3 signature:
`dijkstra(graph, start, combine, zero, comparator)`.

### `MinimumSpanningTree` — Static Façade

- **Prim** Option 3: `prim(graph, start, comparator)` — comparator only.
- **Kruskal** Option 3: `kruskal(graph, comparator)` — comparator only.
- **Borůvka** Option 3: `boruvka(graph, comparator)` — comparator only.

### `ConnectedComponents` — Static Façade

- `count(graph)` — number of connected components.
- `componentIds(graph)` — `Map<V, Integer>` vertex → component id.
- `components(graph)` — `Map<Integer, Set<V>>` grouped by component.

### `TopologicalSort` — Static Façade

- `sort(graph)` — `Optional<List<V>>` topological order; empty if cyclic.
- `isDAG(graph)` — `true` if graph is acyclic.

### `Graph.reverse()` — Instance Method

Returns a new directed `Graph<V>` with all edges flipped. Throws
`IllegalStateException` for undirected graphs.

### `StronglyConnectedComponents` — Static Façade

- `kosaraju(graph)` — `Map<V, Integer>` vertex → SCC id.
- `count(graph)` — number of SCCs.
- `components(graph)` — `Map<Integer, Set<V>>` grouped by SCC.

### `Connectivity<V>` — Mutable Wrapper

- `Connectivity.create(...)` — Weighted Quick Union, O(log n)
- `Connectivity.createOptimized(...)` — path compression, amortised near-O(1)

---

## Directionality Constraints

| Algorithm                              | Graph type required |
|----------------------------------------|---------------------|
| `Graph.bfs` / `Graph.dfs`              | either              |
| `ShortestPaths.dijkstra`               | directed            |
| `MinimumSpanningTree.prim`             | undirected          |
| `MinimumSpanningTree.kruskal`          | undirected          |
| `MinimumSpanningTree.boruvka`          | undirected          |
| `ConnectedComponents.count` / `componentIds` / `components` | undirected |
| `TopologicalSort.sort` / `isDAG`       | directed            |
| `StronglyConnectedComponents.kosaraju` | directed            |

---

## Option 3 API Summary

| Algorithm                     | Option 3 parameters             | Reason                                     |
|-------------------------------|---------------------------------|--------------------------------------------|
| `ShortestPaths.dijkstra`      | `combine`, `zero`, `comparator` | Accumulates path costs — needs full Monoid |
| `MinimumSpanningTree.prim`    | `comparator` only               | Compares edge weights; never combines      |
| `MinimumSpanningTree.kruskal` | `comparator` only               | Sorts edge weights; never combines         |
| `MinimumSpanningTree.boruvka` | `comparator` only               | Selects min crossing edge; never combines  |

---

## Known Issues

- ~~**`DirectedGraph.addEdge` does not ensure the destination vertex exists.**~~
  **Fixed in V1.5.2.** `DirectedGraph.addEdge` now delegates to `VertexMap.+[E]`
  like `UndirectedGraph.addEdge`, ensuring both endpoints exist. The `JavaFacadeBridge`
  workaround (folding over `VertexMap.+[E]` directly) remains in place and is
  still correct. See [Issue #16](https://github.com/rchillyard/Gryphon/issues/16#issue-4240244349)

- **`Monoid[E].combine` is a stub in `primCustom`.**
  Prim's algorithm only uses `Monoid[E].identity` (as the initial frontier cost)
  and `Ordering[E]` (to compare edge weights); it never accumulates costs. The
  `Monoid[E]` given in `primCustom` therefore supplies a stub `combine` that
  returns `x`. This is harmless at runtime. A cleaner fix would be to decouple
  the `Ordering[E]` and `Monoid[E]` context bounds in `WeightedTraversal`,
  making `Monoid` optional for Prim. That is a Gryphon/Visitor change. See [Issue #9](https://github.com/rchillyard/Visitor/issues/9#issue-4240254773)

- **CameFrom pointers in the Visitor engine.** Adding a `CameFromJournal[V]` to
  Visitor would allow `GraphTraversal.bfs` / `dfs` to delegate fully to the
  Scala engine. This is a Visitor library change, not a Gryphon change.
  See [Issue #10](https://github.com/rchillyard/Visitor/issues/10#issue-4240254800)

---

## Deferred Work

### Medium Priority

- **`Graph.reverse()`** — expose as a public Java method on `Graph<V>` returning
  a new `Graph<V>` with all edge directions flipped. Currently
  `DirectedGraph.reverse` is called internally by Kosaraju but not accessible
  from Java.

- **`Graph.fromEdgeList(List<Edge<V>> edges, boolean directed)`** — convenience
  factory for constructing a graph from an existing edge collection.

- **`ConnectedComponents` façade.**
  ```java
  List<Set<V>> ConnectedComponents.find(Graph<V> g);
  ```

- **`TopologicalSort` façade.**
  ```java
  List<V> TopologicalSort.sort(Graph<V> g);
  ```

- ~~**Graph loading from file.**~~ **Done in V1.5.1** — `WeightedGraph.undirectedFromResource`
  and `directedFromResource` (Option 1 and Option 3) load from `.graph` classpath
  resources via `GraphBuilder`. `asParseable` wraps Java `Function<String,T>` with
  appropriate token regex (`\w+` for vertices, double regex for weights).

### Low Priority / Aspirational

- **Visitor Java façade.** Functional interfaces map to typeclasses as follows:

  | Typeclass          | Java equivalent            |
  |--------------------|----------------------------|
  | `Neighbours[H, V]` | `Function<V, Iterable<V>>` |
  | `VisitedSet[V]`    | `Supplier<Set<V>>`         |
  | `Evaluable[V, R]`  | `Function<V, R>`           |
  | `Frontier[F[_]]`   | `Supplier<Deque<V>>`       |

- **`Graph.vertices()` returning `List<V>`** — currently returns `Set<V>`;
  a deterministic `List<V>` (insertion-order) may be more useful for student
  assignments where vertex ordering matters.

- **Graph loading from a standard interchange file.** A `GraphReader` parsing GraphML files
  and producing a `Graph<V>` or `WeightedGraph<V,E>`.

- **Scala `Graph` unification.** Collapsing `DirectedGraph[V, E]` and
  `UndirectedGraph[V, E]` into a single `Graph[V, E](directed: Boolean)` would
  simplify the Java façade's materialisation logic considerably.

---

## Open Questions

1. **Thread safety.** `Graph<V>` is not thread-safe — the `scalaGraph` cache
   is not protected. Acceptable for the student use case but should be documented
   in the Javadoc.

2. **Package naming.** All Java façade classes currently live in
   `com.phasmidsoftware.gryphon.java`. If the façade grows substantially, it may
   warrant sub-packages: `gryphon.java.graph`, `gryphon.java.algo`, etc.

4. **Isolated vertices are not preserved in Scala materialisation.**
   `JavaFacadeBridge.materialise` folds over `canonicalEdges` only — vertices
   added via `Graph.addVertex` with no edges are silently absent from the
   Scala `VertexMap`. Low priority for the student use case.

3. **Java type erasure forces distinct factory method names on `WeightedGraph`.**
   `WeightedGraph.directed()` and `WeightedGraph.undirected()` would clash with
   the inherited `Graph.directed()` and `Graph.undirected()` after erasure.
   Workaround: factory methods are named `directedWeighted()` and
   `undirectedWeighted()`.

---

## Version History

| Version | Changes                                                                                                                                                                                                                                            |
|---------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1.0.0   | Initial release                                                                                                                                                                                                                                    |
| 1.1.0   | First update post-release                                                                                                                                                                                                                          |
| 1.2.0   | Rename UnionFind→Connectivity; F-bounded DisjointSet; ConnectivityOptimized; WeightedUnion                                                                                                                                                         |
| 1.2.1   | Java façade: Edge, WeightedEdge, Graph, GraphTraversal, Connectivity; JUnit 5; Java 21                                                                                                                                                             |
| 1.2.2   | ShortestPaths (Dijkstra Option 1 and Option 3); JavaFacadeBridge; ShortestPathsTest                                                                                                                                                                |
| 1.2.3   | MinimumSpanningTree (Prim and Kruskal); StronglyConnectedComponents (Kosaraju); MST Scala entry point                                                                                                                                              |
| 1.3.0   | WeightedGraph<V,E> extending Graph<V>; typed edge attributes eliminate runtime casts; simplified Option 3 API for Prim/Kruskal (Comparator only); Dijkstra Option 3 retains combine+zero+comparator; validated against Northeastern tunnel network |
| 1.4.0   | All BFS/DFS delegate to Scala Visitor engine via CameFromJournal |
| 1.5.0   | MinimumSpanningTree.boruvka (Option 1 and Option 3) |
| 1.5.1   | WeightedGraph.fromResource (Option 1 and Option 3); Graph/WeightedGraph resource-load constructors; GraphBuilder Scala API |
| 1.5.2   | Borůvka deduplication bug fix; Boruvka returns Seq like Kruskal; DirectedGraph.addEdge fixed (Issue #16); tunnel integration tests |
| 1.5.3   | ParseableLong (optional L suffix); ConnectedComponents façade; TopologicalSort façade; Graph.reverse() |