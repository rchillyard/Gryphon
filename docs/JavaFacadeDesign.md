# Gryphon Java Façade — Design Document

## Overview

The Gryphon Java façade provides a Java-idiomatic API over Gryphon's purely
functional Scala graph library. Its primary audience is students in INFO6205
(Data Structures, Algorithms, and Intelligent Programming with Graphs) at
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
   collections and builds the underlying Scala graph on demand when a traversal
   is requested. The Scala graph is an implementation detail — a cache that is
   invalidated on every mutation.

4. **Two-tier traversal API.** Every traversal method is provided in two forms:
    - **Option 1** — sensible defaults, works out of the box.
    - **Option 3** — caller supplies a functional interface (neighbour function,
      weight extractor, combiner, etc.), matching the typeclass abstraction of
      the Scala API.

   Option 2 (builder pattern) was considered and rejected as adding complexity
   without pedagogical value.

5. **Single `Graph<V>` class.** Directed and undirected graphs are distinguished
   by a `boolean directed` flag set at construction time via factory methods
   `Graph.directed()` and `Graph.undirected()`. There is no separate
   `DirectedGraph<V>` / `UndirectedGraph<V>` hierarchy — this mirrors the
   direction of travel in the Scala codebase, where `DirectedGraph` and
   `UndirectedGraph` are considered architectural dinosaurs.

6. **No `Void` avoidance.** `WeightedEdge<V, Void>` (with `null` attribute) is
   a legitimate type for contexts that require the `WeightedEdge` type but carry
   no actual weight. Students are not shielded from `Void`; it is more honest
   than defaulting to weight `1.0`.

---

## Current State (V1.2.1)

### Implemented

| Class | Package | Description |
|---|---|---|
| `Edge<V>` | `gryphon.java` | Immutable unweighted edge with `from`, `to`, `reverse` |
| `WeightedEdge<V,E>` | `gryphon.java` | Extends `Edge<V>` with typed `attribute` |
| `Graph<V>` | `gryphon.java` | Mutable lazy-builder façade; directed/undirected |
| `GraphTraversal` | `gryphon.java` | Package-private BFS/DFS; returns `Map<V,V>` parent trees |
| `Connectivity<V>` | `gryphon.java` | Mutable façade over `Connectivity` / `ConnectivityOptimized` |

### Test Coverage

| Test class | Framework | Status |
|---|---|---|
| `ConnectivityTest` | JUnit 5 | ✅ Green |
| `GraphTest` | JUnit 5 | ✅ Green |

### Build

- JUnit 5 via `com.github.sbt:junit-interface:0.13.3`
- `javacOptions ++= Seq("--release", "21")`
- Java 21 (Oracle OpenJDK 21.0.1)
- sbt 1.12.7

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

Traversals call `GraphTraversal.bfs` / `GraphTraversal.dfs`, which currently
operate on the Java `adjacency` map directly. When Scala delegation is wired up,
they will instead call `getScalaGraph()` to materialise the cache and delegate
to Gryphon's traversal engine.

### `GraphTraversal` — Package-Private

Iterative BFS (queue-based) and DFS (explicit stack) to avoid stack overflow on
large graphs. Both return `Map<V, V>` parent maps — the traversal tree rooted at
the start vertex. The start vertex maps to itself; unreachable vertices are absent.

The parent map supports:
- **Connectivity query:** `map.containsKey(v)` — O(1)
- **Path reconstruction:** walk up the parent chain from any vertex to start

### `Connectivity<V>` — Mutable Wrapper

Holds an `AbstractDisjointSet` delegate. Mutating operations (`connect`, `put`)
replace the delegate with the return value of the immutable Scala operation.
Factory methods:

- `Connectivity.create(...)` — Weighted Quick Union, O(log n)
- `Connectivity.createOptimized(...)` — Weighted Quick Union + path compression,
  amortised near-O(1)

---

## Deferred Work

### High Priority

- **Wire Scala traversal engine into `GraphTraversal`.**
  Currently BFS/DFS are implemented directly in Java. The `scalaGraph` cache in
  `Graph<V>` should be materialised from `canonicalEdges` and the traversal
  delegated to `Traversal.bfs` / `Traversal.dfs` in Gryphon's Scala engine.
  This requires deciding how to supply the required typeclass instances
  (`Neighbours`, `VisitedSet`, `Frontier`, `Evaluable`) from Java — likely via
  hardwired defaults for Option 1, and functional interfaces for Option 3.

- **`ShortestPaths` façade (Dijkstra).**
  ```java
  // Option 1
  Map<V, E> ShortestPaths.dijkstra(Graph<V> g, V start);
  // Option 3
  Map<V, E> ShortestPaths.dijkstra(Graph<V> g, V start,
      Function<Edge<V>, E> weight,
      BinaryOperator<E> combine);
  ```
  Requires `WeightedEdge<V, E>` edges. The `combine` parameter maps directly to
  the `Monoid[E]` typeclass in the Scala engine.

- **`MinimumSpanningTree` façade (Prim, Kruskal).**
  ```java
  // Prim
  List<Edge<V>> MinimumSpanningTree.prim(Graph<V> g, V start,
      Function<Edge<V>, E> weight,
      Comparator<E> comparator);
  // Kruskal — uses Connectivity internally
  List<Edge<V>> MinimumSpanningTree.kruskal(Graph<V> g,
      Function<Edge<V>, E> weight,
      Comparator<E> comparator);
  ```
  Both require undirected graph — should throw `IllegalStateException` if called
  on a directed graph, with a clear message.

### Medium Priority

- **`Graph.reverse()`** — returns a new `Graph<V>` with all edge directions
  flipped. Required for Kosaraju's SCC algorithm.

- **`StronglyConnectedComponents` façade (Kosaraju).**
  ```java
  List<Set<V>> StronglyConnectedComponents.kosaraju(Graph<V> g);
  ```
  Requires directed graph — should throw `IllegalStateException` if called on
  undirected. Internally delegates to `KosarajuTraversal` in Gryphon.

- **`Graph.fromEdgeList(List<Edge<V>> edges, boolean directed)`** — convenience
  factory for constructing a graph from an existing edge collection, e.g. when
  loading from a file.

- **`Graph.vertices()` returning `List<V>`** — currently returns `Set<V>`;
  a deterministic `List<V>` (insertion-order) may be more useful for student
  assignments where vertex ordering matters.

### Low Priority / Aspirational

- **`ConnectedComponents` façade.**
  ```java
  List<Set<V>> ConnectedComponents.find(Graph<V> g);
  ```
  Undirected graphs only.

- **`TopologicalSort` façade.**
  ```java
  List<V> TopologicalSort.sort(Graph<V> g);
  ```
  Directed acyclic graphs only — should throw if a cycle is detected.

- **Graph loading from file.** A `GraphReader` that parses the `.graph` resource
  format used in Gryphon's test suite and produces a `Graph<V>`.

- **Visitor Java façade.** The Visitor typeclass engine is significantly harder
  to expose in Java than the graph types. Functional interfaces map to
  typeclasses as follows:

  | Typeclass | Java equivalent |
    |---|---|
  | `Neighbours[H, V]` | `Function<V, Iterable<V>>` |
  | `VisitedSet[V]` | `Supplier<Set<V>>` |
  | `Evaluable[V, R]` | `Function<V, R>` |
  | `Frontier[F[_]]` | `Supplier<Deque<V>>` |

  This mapping should drive the Option 3 API for all traversal methods once
  Scala delegation is wired in.

- **Scala `Graph` unification.** The Scala codebase currently has separate
  `DirectedGraph[V, E]` and `UndirectedGraph[V, E]` classes. These are
  architectural dinosaurs — the directed/undirected distinction is entirely
  captured by a boolean in the `VertexMap` construction logic. A unified
  `Graph[V, E](directed: Boolean)` Scala class would simplify the Java façade's
  materialisation logic and is a natural refactor to pursue.

---

## Open Questions

1. **Scala traversal delegation.** When materialising the Scala graph from the
   Java edge list, what is the cleanest way to supply typeclass instances from
   Java? The leading candidate is hardwiring sensible defaults (HashSet for
   visited, Stack/Queue for frontier) in an internal Scala helper object, with
   the functional interface parameters for Option 3 wrapped in anonymous
   typeclass instances.

2. **Thread safety.** `Graph<V>` is not thread-safe — the `scalaGraph` cache
   is not protected. This is acceptable for the student use case (single-threaded
   assignments) but should be documented explicitly.

3. **`Graph<V>` vs `Graph<V, E>`.** The current design has no `E` type parameter
   on `Graph` — weight is a property of edges, not the graph. This is clean but
   means `ShortestPaths.dijkstra` must cast edges to `WeightedEdge<V, E>` at
   runtime. An alternative is `Graph<V, E>` where `E` defaults to `Void` for
   unweighted graphs. Decision deferred pending student feedback.

4. **Package naming.** All Java façade classes currently live in
   `com.phasmidsoftware.gryphon.java`. If the façade grows substantially, it may
   warrant sub-packages: `gryphon.java.graph`, `gryphon.java.algo`, etc.

---

## Version History

| Version | Changes |
|---|---|
| 1.0.0 | Initial release |
| 1.1.0 | First update post-release |
| 1.2.0 | Rename UnionFind→Connectivity; F-bounded DisjointSet; ConnectivityOptimized; WeightedUnion |
| 1.2.1 | Java façade: Edge, WeightedEdge, Graph, GraphTraversal, Connectivity; JUnit 5; Java 21 |