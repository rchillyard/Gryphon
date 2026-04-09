# Gryphon

![Sonatype Central](https://maven-badges.sml.io/sonatype-central/com.phasmidsoftware/gryphon_3/badge.svg?color=blue)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/bb7de1b3ea4e4256997e6b1fac66281b)](https://app.codacy.com/gh/rchillyard/Gryphon?utm_source=github.com&utm_medium=referral&utm_content=rchillyard/Gryphon&utm_campaign=Badge_Grade)
[![CircleCI](https://dl.circleci.com/status-badge/img/gh/rchillyard/Gryphon/tree/main.svg?style=shield)](https://dl.circleci.com/status-badge/redirect/gh/rchillyard/Gryphon/tree/main)
![GitHub Top Languages](https://img.shields.io/github/languages/top/rchillyard/Gryphon)
![GitHub](https://img.shields.io/github/license/rchillyard/Gryphon)
![GitHub last commit](https://img.shields.io/github/last-commit/rchillyard/Gryphon)
![GitHub issues](https://img.shields.io/github/issues-raw/rchillyard/Gryphon)
![GitHub issues by-label](https://img.shields.io/github/issues/rchillyard/Gryphon/bug)

A Scala 3 graph algorithms library built on the
[Visitor](https://github.com/rchillyard/Visitor) typeclass traversal engine.

Gryphon provides clean, purely functional implementations of the classic graph
algorithms from Sedgewick & Wayne, *Algorithms* (4th ed.), Chapter 4.
All algorithms are implemented using Scala 3 idioms — typeclasses, `given`/`using`,
and clean architectural separation between graph structure and traversal logic.

Gryphon is intended as an artefact of the [DSAIPG](https://github.com/rchillyard/DSAIPG) repository
that supports the author's own textbook Data Structures, Algorithms, and Invariants - a Practical Guide
(published by Cognella).

---

## Getting Started

Add Gryphon to your `build.sbt`:

```scala
libraryDependencies += "com.phasmidsoftware" %% "gryphon" % "1.2.2"
```

Gryphon requires **Scala 3** and depends on
[Visitor](https://github.com/rchillyard/Visitor) (`com.phasmidsoftware %% visitor`),
which is pulled in automatically.

---

## Algorithms

Gryphon implements all eleven graph algorithms from Chapter 4 of
Sedgewick & Wayne, *Algorithms* (4th ed.):

| # | Algorithm | Class / Object |
|---|-----------|----------------|
| 4.1 | Depth-first search | `Graph.dfs` / `Graph.dfsAll` |
| 4.2 | Breadth-first search | `Graph.bfs` |
| 4.3 | Connected components | `ConnectedComponents` |
| 4.4 | Reachability | `Graph.dfs` on `DirectedGraph` |
| 4.5 | Topological sort | `TopologicalSort` |
| 4.6 | Strongly connected components (Kosaraju–Sharir) | `Kosaraju` |
| 4.7 | Minimum spanning tree (Prim) | `MST.prim` |
| 4.8 | Minimum spanning tree (Kruskal) | `Kruskal` |
| 4.9 | Shortest paths (Dijkstra) | `ShortestPaths.dijkstra` |
| 4.10 | Shortest paths in DAGs | `AcyclicShortestPaths` |
| 4.11 | Shortest paths (Bellman–Ford) | `BellmanFord` |

Additionally, some algorithms and graph properties that are not directly covered in that chapter:

- **Cycle detection** — `Graph.isCyclic` (undirected and directed)
- **Bipartite checking** — `UndirectedGraph.isBipartite`
- **Connectivity** — `UndirectedGraph.isConnected`
- **Degree statistics** — `UndirectedGraph.degree`, `maxDegree`, `meanDegree`
- **Self-loop count** — `EdgeGraph.numberOfSelfLoops`
- **Union-Find** — `Connectivity`, `ConnectivityOptimized` (used internally by Kruskal)
- **Graph reversal** — `DirectedGraph.reverse` (used internally by Kosaraju)

---

## Graph Types

Gryphon supports two concrete graph types, both backed by a `VertexMap`:

```scala
// Directed weighted graph
val g: DirectedGraph[Int, Double] = DirectedGraph.triplesToTryGraph(...).get

// Undirected weighted graph
val g: UndirectedGraph[Int, Double] = UndirectedGraph.triplesToTryGraph(...).get
```

Graphs are constructed from sequences of `Triplet[V, E, EdgeType]`, typically
parsed from a graph file using `GraphParser`:

```scala
val p = new GraphParser[Int, Double, EdgeType]
val triplets = TryUsing.tryIt(Try(Source.fromResource("mygraph.graph"))) { source =>
  p.parseSource[Triplet[Int, Double, EdgeType]](p.parseTriple)(source)
}.get
val graph = DirectedGraph.triplesToTryGraph[Int, Double](Vertex.createWithSet)(triplets).get
```

### Graph file format

Graph files use a simple line-based format. Lines beginning with `//` are comments.

```
// Directed weighted graph
0 > 1 0.5
1 > 2 1.2
2 > 0 0.8
```

```
// Undirected weighted graph
0 = 1 0.16
1 = 2 0.19
```

The separator `>` means directed; `=` means undirected.

---

## Scala Usage Examples

### Depth-first and breadth-first search

```scala
given Evaluable[Int, Int] with
  def evaluate(v: Int): Option[Int] = Some(v)

val visitor = JournaledVisitor.withQueueJournal[Int, Int]

// DFS from vertex 0
val dfsResult = graph.dfs(visitor)(0)
val visited = dfsResult.result.map(_._1).toSet

// BFS from vertex 0
val bfsResult = graph.bfs(visitor)(0)

// DFS over all components
val allVisited = graph.dfsAll(visitor).result.map(_._1).toSet
```

### Topological sort

```scala
TopologicalSort.sort(dag) match
  case Some(order) => println(s"Topological order: $order")
  case None        => println("Graph contains a cycle")
```

### Shortest paths (Dijkstra)

```scala
given Ordering[Double] = scala.math.Ordering.Double.TotalOrdering
import com.phasmidsoftware.visitor.core.given // provides Monoid[Double]

val result = ShortestPaths.dijkstra(graph, 0)
result.vertexTraverse(5).foreach(e => println(s"Shortest edge into 5: $e"))
```

### Shortest paths in a DAG (handles negative weights)

```scala
val result = AcyclicShortestPaths.shortestPaths(dag, 0)
result.vertexTraverse(3).foreach(e => println(s"Shortest edge into 3: $e"))
```

### Bellman–Ford (handles negative weights and detects negative cycles)

```scala
BellmanFord.shortestPaths(graph, 0) match
  case Some(result) => result.vertexTraverse(3).foreach(println)
  case None         => println("Negative cycle detected")
```

### Minimum spanning tree (Kruskal)

```scala
val mstEdges: Seq[Edge[Int, Double]] = Kruskal.mst(undirectedGraph)
val totalWeight = mstEdges.map(_.attribute).sum
```

### Strongly connected components (Kosaraju)

```scala
val sccResult: SCCResult[Int] = Kosaraju.stronglyConnectedComponents(directedGraph)
// sccResult maps each vertex to its SCC id
val numSCCs = sccResult.values.toSet.size
```

### Graph properties

```scala
graph.isCyclic          // Boolean
graph.isBipartite       // Boolean (undirected only)
graph.isConnected       // Boolean (undirected only)
graph.degree(v)         // Int (undirected only)
graph.maxDegree         // Int (undirected only)
graph.meanDegree        // Double (undirected only) — equals 2*M/N
graph.numberOfSelfLoops // Int
```

---

## Java API

Gryphon provides a complete Java façade in the `com.phasmidsoftware.gryphon.java`
package, giving Java students access to all major graph algorithms without
exposure to Scala typeclasses, `given`/`using`, or companion object syntax.

### Building with Maven

Insert the following into the `<dependencies>` block of your pom.xml:

````xml
<dependency>
    <groupId>com.phasmidsoftware</groupId>
    <artifactId>gryphon_3</artifactId>
    <version>1.2.3</version>
</dependency>
````

The Java API is the primary interface for students in INFO6205 at Northeastern University.

### Core types

| Class | Description |
|---|---|
| `Edge<V>` | Immutable unweighted edge with `from()`, `to()`, `reverse()` |
| `WeightedEdge<V, E>` | Extends `Edge<V>` with a typed `attribute()` |
| `Graph<V>` | Mutable lazy-builder façade; directed or undirected |
| `Connectivity<V>` | Mutable Union-Find façade for disjoint-set operations |
| `ShortestPaths` | Dijkstra's algorithm |
| `MinimumSpanningTree` | Prim's and Kruskal's algorithms |
| `StronglyConnectedComponents` | Kosaraju's algorithm |

### Building a graph

`Graph<V>` is mutable in the Java idiom. Create it with a factory method and
add edges one at a time:

```java
// Unweighted directed graph
Graph<String> g = Graph.directed();
g.addEdge("A", "B");
g.addEdge("B", "C");
g.addEdge("C", "A");

// Weighted undirected graph
Graph<Integer> g = Graph.undirected();
g.addEdge(new WeightedEdge<>(0, 1, 0.26));
g.addEdge(new WeightedEdge<>(0, 7, 0.16));
g.addEdge(new WeightedEdge<>(1, 7, 0.19));
```

### BFS and DFS

Both traversals return a parent map — a `Map<V, V>` where each entry `v → parent`
records who discovered `v`. The start vertex maps to itself. Unreachable vertices
are absent.

```java
Graph<String> g = Graph.undirected();
g.addEdge("A", "B");
g.addEdge("B", "C");
g.addEdge("C", "D");

Map<String, String> bfsTree = g.bfs("A");
boolean reachable = bfsTree.containsKey("D");   // true
String parent     = bfsTree.get("C");           // "B"

// Reconstruct path from D back to A
List<String> path = new ArrayList<>();
String v = "D";
while (!v.equals(bfsTree.get(v))) {
    path.add(0, v);
    v = bfsTree.get(v);
}
path.add(0, v); // add start
// path == ["A", "B", "C", "D"]

Map<String, String> dfsTree = g.dfs("A");
```

### Shortest paths (Dijkstra)

`ShortestPaths.dijkstra` returns a shortest-path tree (SPT) as a
`Map<V, WeightedEdge<V, Double>>`. Each entry `v → edge` records the cheapest
incoming edge to `v`. The start vertex is absent.

```java
Graph<Integer> g = Graph.directed();
g.addEdge(new WeightedEdge<>(0, 1,  5.0));
g.addEdge(new WeightedEdge<>(0, 4,  9.0));
g.addEdge(new WeightedEdge<>(0, 7,  8.0));
g.addEdge(new WeightedEdge<>(4, 5,  4.0));
g.addEdge(new WeightedEdge<>(5, 2,  1.0));
// ... add remaining edges

// Option 1 — Double weights, no configuration needed
Map<Integer, WeightedEdge<Integer, Double>> spt = ShortestPaths.dijkstra(g, 0);

WeightedEdge<Integer, Double> edgeTo2 = spt.get(2);
System.out.println(edgeTo2.from());      // 5  (vertex 2 reached via vertex 5)
System.out.println(edgeTo2.attribute()); // 1.0 (the 5→2 edge weight)

// Reconstruct full path to vertex 2
List<Integer> path = new ArrayList<>();
int v = 2;
while (spt.containsKey(v)) {
    path.add(0, v);
    v = spt.get(v).from();
}
path.add(0, v); // add start vertex
// path == [0, 4, 5, 2]

// Option 3 — custom weight extractor, combiner, and comparator
Map<Integer, WeightedEdge<Integer, Double>> spt3 = ShortestPaths.dijkstra(
    g, 0,
    e -> ((WeightedEdge<Integer, Double>) e).attribute(),
    Double::sum,
    0.0,
    Comparator.naturalOrder());
```

### Minimum spanning tree (Prim)

`MinimumSpanningTree.prim` returns the MST as a `Map<V, WeightedEdge<V, Double>>`
mapping each non-source vertex to its cheapest MST edge. The start vertex is absent.

```java
Graph<Integer> g = Graph.undirected();
g.addEdge(new WeightedEdge<>(0, 7, 0.16));
g.addEdge(new WeightedEdge<>(2, 3, 0.17));
// ... add remaining edges

// Option 1 — Double weights
Map<Integer, WeightedEdge<Integer, Double>> mst = MinimumSpanningTree.prim(g, 0);

double totalWeight = mst.values().stream()
    .mapToDouble(WeightedEdge::attribute)
    .sum(); // 1.81

List<WeightedEdge<Integer, Double>> mstEdges = new ArrayList<>(mst.values());
```

### Minimum spanning tree (Kruskal)

`MinimumSpanningTree.kruskal` returns the MST as a
`List<WeightedEdge<V, Double>>` in non-decreasing weight order.

```java
// Option 1 — Double weights
List<WeightedEdge<Integer, Double>> mst = MinimumSpanningTree.kruskal(g);

double totalWeight = mst.stream()
    .mapToDouble(WeightedEdge::attribute)
    .sum(); // 1.81

// Prim and Kruskal agree on total weight (graph has unique edge weights)
Map<Integer, WeightedEdge<Integer, Double>> primMst = MinimumSpanningTree.prim(g, 0);
double primTotal   = primMst.values().stream().mapToDouble(WeightedEdge::attribute).sum();
double kruskalTotal = mst.stream().mapToDouble(WeightedEdge::attribute).sum();
// primTotal == kruskalTotal == 1.81

// Option 3 — custom weight and comparator
List<WeightedEdge<Integer, Double>> mst3 = MinimumSpanningTree.kruskal(
    g,
    e -> ((WeightedEdge<Integer, Double>) e).attribute(),
    Comparator.naturalOrder());
```

### Strongly connected components (Kosaraju)

`StronglyConnectedComponents.kosaraju` returns a `Map<V, Integer>` mapping
each vertex to an integer SCC id. Vertices sharing the same id are in the same SCC.

```java
Graph<Integer> g = Graph.directed();
g.addEdge(0, 5); g.addEdge(0, 2); g.addEdge(0, 1);
g.addEdge(5, 2); g.addEdge(2, 6); g.addEdge(6, 0); // cycle: 0→5→2→6→0
g.addEdge(3, 6); g.addEdge(3, 5); g.addEdge(3, 4);
g.addEdge(6, 4); g.addEdge(1, 4); g.addEdge(3, 2);

// Raw result — vertex to SCC id
Map<Integer, Integer> sccs = StronglyConnectedComponents.kosaraju(g);
boolean sameComponent = sccs.get(0).equals(sccs.get(2)); // true (both in {0,2,5,6})

// Count SCCs
int numSccs = StronglyConnectedComponents.count(g); // 4

// Group vertices by SCC
Map<Integer, Set<Integer>> groups = StronglyConnectedComponents.components(g);
// One group will be {0, 2, 5, 6}; the others are {1}, {3}, {4}
```

### Connectivity (Union-Find)

`Connectivity<V>` wraps Gryphon's disjoint-set implementation in a mutable
Java-idiomatic API. Two implementations are available:

```java
// Weighted Quick Union — O(log n) per operation
Connectivity<String> c = Connectivity.create("A", "B", "C", "D");

// Weighted Quick Union with path compression — amortised near-O(1)
Connectivity<String> c = Connectivity.createOptimized("A", "B", "C", "D");

c.connect("A", "B");
c.connect("C", "D");

c.isConnected("A", "B"); // true
c.isConnected("A", "C"); // false
c.size();                // 2 (two components)

// Add a new singleton
c.put("E");
c.size();                // 3
```

### Two-tier API

Every algorithm method in the Java façade is available in two forms:

- **Option 1** — sensible defaults, works out of the box. Edge weights are
  read from `WeightedEdge.attribute()` and treated as `Double`. No
  configuration required.

- **Option 3** — the caller supplies functional interfaces (`Function`,
  `BinaryOperator`, `Comparator`) that control weight extraction and
  comparison. This mirrors the typeclass abstraction of the Scala engine
  and is suitable for non-`Double` weight types or custom orderings.

```java
// Option 1 — Dijkstra with Double weights
Map<Integer, WeightedEdge<Integer, Double>> spt =
    ShortestPaths.dijkstra(g, 0);

// Option 3 — Dijkstra with custom weight, combiner, and comparator
Map<Integer, WeightedEdge<Integer, Double>> spt =
    ShortestPaths.dijkstra(g, 0,
        e -> ((WeightedEdge<Integer, Double>) e).attribute(),
        Double::sum,
        0.0,
        Comparator.naturalOrder());
```

### Directionality constraints

| Algorithm | Graph type required |
|---|---|
| `Graph.bfs` / `Graph.dfs` | either |
| `ShortestPaths.dijkstra` | directed |
| `MinimumSpanningTree.prim` | undirected |
| `MinimumSpanningTree.kruskal` | undirected |
| `StronglyConnectedComponents.kosaraju` | directed |

Calling an algorithm with the wrong graph type throws `IllegalStateException`
with a descriptive message.

---

## Architecture

Gryphon is structured in five layers:

```
com.phasmidsoftware.gryphon
  .core       — Graph, VertexMap, Vertex, Edge, Adjacency, Connexion,
                Connected, Traversable, EdgeTraversable
  .adjunct    — DirectedGraph, UndirectedGraph, DirectedEdge,
                UndirectedEdge, Connectivity, ConnectivityOptimized
  .traverse   — ConnectedComponents, Kosaraju, TopologicalSort,
                ShortestPaths, MST, AcyclicShortestPaths, BellmanFord,
                Kruskal, TraversalResult, Connexions
  .parse      — GraphParser, BaseParser, Parseable
  .java       — Graph, Edge, WeightedEdge, Connectivity,
                ShortestPaths, MinimumSpanningTree,
                StronglyConnectedComponents, JavaFacadeBridge
```

The traversal engine lives in the separate
[Visitor](https://github.com/rchillyard/Visitor) library, which provides
the five orthogonal typeclasses that drive all traversals:
`Evaluable[V, R]`, `Neighbours[H, V]`, `VisitedSet[V]`,
`Frontier[F[_]]`, and `Visitor[V, R, J]`.

---

## Design Principles

- **Scala 3 throughout** — typeclasses with `given`/`using`, `enum`, type aliases,
  context bounds, and clean separation of concerns.
- **Purely functional results** — all algorithms return immutable result types;
  any internal mutable state is strictly local to the algorithm.
- **Consistent type parameter ordering** — `V` (vertex type) always precedes
  `E` (edge attribute type) throughout the API.
- **`E: {Monoid, Ordering}`** — edge weight constraints use Visitor's `Monoid`
  rather than `Numeric`, keeping the constraint as light as possible.
- **No default implementations** — abstract methods on `Graph[V]` are always
  abstract, forcing each graph type to provide a correct concrete implementation.
- **Java API hides all Scala-isms** — no `Option`, typeclasses, `given`/`using`,
  or companion object syntax visible to Java callers.

---

## Testing

The library has 365+ tests covering all algorithms, graph properties, and
edge cases including:
- Disconnected graphs and forests
- Negative edge weights (Bellman–Ford, AcyclicShortestPaths)
- Negative cycle detection
- Self-loops
- Agreement between Bellman–Ford and Dijkstra on non-negative graphs
- Agreement between Prim and Kruskal on MST weight
- Java API tests for all major algorithms

```bash
sbt test
```

---

## Versioning

| Version | Changes |
|---|---|
| 1.0.0 | Initial release |
| 1.1.0 | First update post-release |
| 1.2.0 | Rename UnionFind→Connectivity; F-bounded DisjointSet; ConnectivityOptimized |
| 1.2.1 | Java façade: Edge, WeightedEdge, Graph, GraphTraversal, Connectivity |
| 1.2.2 | Java façade: ShortestPaths (Dijkstra), MinimumSpanningTree (Prim) |
| 1.2.3 | Java façade: MinimumSpanningTree (Kruskal), StronglyConnectedComponents (Kosaraju) |

---

## License

This project is licensed under the MIT License — see the
[LICENSE](LICENSE) file for details.

## Related Projects

- [Visitor](https://github.com/rchillyard/Visitor) — the typeclass-based
  graph traversal engine that Gryphon builds on
- [Number](https://github.com/rchillyard/Number) — complex number support
  (planned for a future quantum computing library)
- [TableParser](https://github.com/rchillyard/TableParser) — tabular data
  parsing used in Northeastern University coursework