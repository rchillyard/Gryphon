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

7. **BFS/DFS stay in Java.** Plain BFS and DFS are implemented directly in Java
   (`GraphTraversal`) and do not delegate to the Scala engine. The Scala engine
   is used for weighted algorithms (Dijkstra, Prim, Kruskal, Kosaraju) that have
   no natural Java equivalent. The reason: the Scala traversal engine journals
   visited vertices in order but does not record parent pointers — the
   `Map<V, V>` parent-tree result needed by the Java façade is not producible
   from the Scala journal without a Visitor library change (see Deferred Work).

---

## Current State (V1.2.2)

### Implemented

| Class | Package | Description |
|---|---|---|
| `Edge<V>` | `gryphon.java` | Immutable unweighted edge with `from`, `to`, `reverse` |
| `WeightedEdge<V,E>` | `gryphon.java` | Extends `Edge<V>` with typed `attribute` |
| `Graph<V>` | `gryphon.java` | Mutable lazy-builder façade; directed/undirected |
| `GraphTraversal` | `gryphon.java` | Package-private BFS/DFS; returns `Map<V,V>` parent trees |
| `Connectivity<V>` | `gryphon.java` | Mutable façade over `Connectivity` / `ConnectivityOptimized` |
| `ShortestPaths` | `gryphon.java` | Dijkstra (Option 1: `Double`; Option 3: custom weight/combiner/comparator) |
| `JavaFacadeBridge` | `gryphon.java` | Internal Scala bridge: graph materialisation and algorithm delegation |

### Test Coverage

| Test class | Framework | Status |
|---|---|---|
| `ConnectivityTest` | JUnit 5 | ✅ Green |
| `GraphTest` | JUnit 5 | ✅ Green |
| `ShortestPathsTest` | JUnit 5 | ✅ Green |

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

Plain traversals (`bfs`, `dfs`) delegate to `GraphTraversal`, which operates on
the Java `adjacency` map directly and returns a `Map<V, V>` parent tree.

Weighted algorithms (`ShortestPaths`, `MinimumSpanningTree`) call
`getScalaGraph()` (package-private) to materialise the Scala cache, then
delegate to `JavaFacadeBridge` which invokes the Scala engine.

### `GraphTraversal` — Package-Private

Iterative BFS (queue-based) and DFS (explicit stack) to avoid stack overflow on
large graphs. Both return `Map<V, V>` parent maps — the traversal tree rooted at
the start vertex. The start vertex maps to itself; unreachable vertices are absent.

The parent map supports:
- **Connectivity query:** `map.containsKey(v)` — O(1)
- **Path reconstruction:** walk up the parent chain from any vertex to start

### `JavaFacadeBridge` — Internal Scala Object

Package-private Scala `object` that handles all Scala-side concerns:

- `materialise(edges, directed)` — builds `DirectedGraph[V, Unit]` or
  `UndirectedGraph[V, Unit]` from the Java canonical edge list (for BFS/DFS
  cache; currently unused by traversal but available for future use).
- `materialiseWeighted(edges, weightFn)` — builds `DirectedGraph[V, E]` by
  folding directly over `VertexMap.+[E]`, ensuring both endpoints are present
  (see Known Issues).
- `dijkstraDouble` / `dijkstraCustom` — run Dijkstra on the materialised
  weighted graph with appropriate typeclass instances supplied, and convert
  the Scala `TraversalResult` back to `Map<V, WeightedEdge<V, E>>`.

### `ShortestPaths` — Static Façade

Returns a shortest-path tree (SPT) as `Map<V, WeightedEdge<V, E>>`. Each entry
`v → edge` records the cheapest incoming edge to `v`. The start vertex is absent.
From the SPT, students can read the immediate edge weight (`edge.attribute()`),
find the predecessor (`edge.from()`), or walk the tree for full path cost.

### `Connectivity<V>` — Mutable Wrapper

Holds an `AbstractDisjointSet` delegate. Mutating operations (`connect`, `put`)
replace the delegate with the return value of the immutable Scala operation.
Factory methods:

- `Connectivity.create(...)` — Weighted Quick Union, O(log n)
- `Connectivity.createOptimized(...)` — Weighted Quick Union + path compression,
  amortised near-O(1)

---

## Known Issues

- **`DirectedGraph.addEdge` does not ensure the destination vertex exists.**
  `DirectedGraph.addEdge` calls `vertexMap.modifyVertex` which only touches the
  `from` vertex. If called on an empty (or incomplete) `VertexMap`,
  destination-only vertices are silently absent, causing `key not found` errors
  when any traversal attempts to expand them. `UndirectedGraph.addEdge` is
  correct — it delegates to `VertexMap.+[E]` which calls `ensure` for both
  endpoints. `DirectedGraph.addEdge` should do the same.
  **Workaround:** `JavaFacadeBridge.materialiseWeighted` folds directly over
  `VertexMap.+[E]` rather than through `DirectedGraph.addEdge`.

---

## Deferred Work

### High Priority

- **`MinimumSpanningTree` façade (Prim, Kruskal).**
  ```java
  // Prim — Option 1 (Double weights)
  List<WeightedEdge<V, Double>> MinimumSpanningTree.prim(Graph<V> g, V start);
  // Prim — Option 3
  List<WeightedEdge<V, E>> MinimumSpanningTree.prim(Graph<V> g, V start,
      Function<Edge<V>, E> weight,
      Comparator<E> comparator);
  // Kruskal — uses Connectivity internally
  List<WeightedEdge<V, E>> MinimumSpanningTree.kruskal(Graph<V> g,
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

- **Parent pointers in the Visitor engine.** The Scala traversal engine journals
  visited vertices in order but does not record who discovered whom. Adding a
  `ParentJournal[V]` to Visitor (recording `(child, parent)` pairs) would allow
  `GraphTraversal.bfs` / `dfs` to delegate fully to the Scala engine rather than
  being reimplemented in Java. This is a Visitor library change, not a Gryphon
  change, and should keep the dependency direction clean.

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
  parent-pointer journalling is in place.

- **Scala `Graph` unification.** The Scala codebase currently has separate
  `DirectedGraph[V, E]` and `UndirectedGraph[V, E]` classes. These are
  architectural dinosaurs — the directed/undirected distinction is entirely
  captured by a boolean in the `VertexMap` construction logic. A unified
  `Graph[V, E](directed: Boolean)` Scala class would simplify the Java façade's
  materialisation logic and is a natural refactor to pursue.

- **Fix `DirectedGraph.addEdge` to ensure both endpoints exist.** See Known
  Issues. Requires making `VertexMap.ensure` at least `private[core]`, adding a
  public `ensureVertex` method, or delegating to `VertexMap.+[E]` as
  `UndirectedGraph` already does. Once fixed, the `materialiseWeighted` workaround
  in `JavaFacadeBridge` can be simplified.

---

## Open Questions

1. **Thread safety.** `Graph<V>` is not thread-safe — the `scalaGraph` cache
   is not protected. This is acceptable for the student use case (single-threaded
   assignments) but should be documented explicitly in the Javadoc.

2. **`Graph<V>` vs `Graph<V, E>`.** The current design has no `E` type parameter
   on `Graph` — weight is a property of edges, not the graph. This is clean but
   means `ShortestPaths.dijkstra` must cast edges to `WeightedEdge<V, E>` at
   runtime. An alternative is `Graph<V, E>` where `E` defaults to `Void` for
   unweighted graphs. Decision deferred pending student feedback.

3. **Package naming.** All Java façade classes currently live in
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
| 1.2.2 | ShortestPaths (Dijkstra Option 1 and Option 3); JavaFacadeBridge; ShortestPathsTest |