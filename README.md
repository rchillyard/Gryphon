<img src="img/gryphon-logo.svg" width="500" alt="Gryphon logo"/>

[![Sonatype Central](https://maven-badges.sml.io/sonatype-central/com.phasmidsoftware/gryphon_3/badge.svg?color=blue)](https://central.sonatype.com/artifact/com.phasmidsoftware/gryphon_3)
[![API Docs](https://img.shields.io/badge/api-docs-blue)](https://rchillyard.github.io/Gryphon/latest/api/)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/6a6fa4419f41403287ef19c619fd79a8)](https://app.codacy.com/gh/rchillyard/Gryphon/dashboard)
[![Codacy Coverage](https://app.codacy.com/project/badge/Coverage/6a6fa4419f41403287ef19c619fd79a8)](https://app.codacy.com/gh/rchillyard/Gryphon/dashboard)
[![CircleCI](https://dl.circleci.com/status-badge/img/gh/rchillyard/Gryphon/tree/main.svg?style=shield)](https://dl.circleci.com/status-badge/redirect/gh/rchillyard/Gryphon/tree/main)
[![GitHub Top Languages](https://img.shields.io/github/languages/top/rchillyard/Gryphon)](https://github.com/rchillyard/Gryphon)
[![GitHub](https://img.shields.io/github/license/rchillyard/Gryphon)](https://github.com/rchillyard/Gryphon/blob/main/LICENSE)
[![GitHub last commit](https://img.shields.io/github/last-commit/rchillyard/Gryphon)](https://github.com/rchillyard/Gryphon/commits/main)
[![GitHub issues](https://img.shields.io/github/issues-raw/rchillyard/Gryphon)](https://github.com/rchillyard/Gryphon/issues)
[![GitHub issues by-label](https://img.shields.io/github/issues/rchillyard/Gryphon/bug)](https://github.com/rchillyard/Gryphon/issues?q=is%3Aissue+is%3Aopen+label%3Abug)

A Scala 3 graph algorithms library built on the
[Visitor](https://github.com/rchillyard/Visitor) typeclass traversal engine.
Additionally, an API allowing easy access to the library from Java.

Gryphon provides clean, purely functional implementations of the classic graph
algorithms from Sedgewick & Wayne, *Algorithms* (4th ed.), Chapter 4.
All algorithms are implemented using Scala 3 idioms — typeclasses, `given`/`using`,
and clean architectural separation between graph structure and traversal logic.

Gryphon is also intended as an artefact of the [DSAIPG](https://github.com/rchillyard/DSAIPG) (Java) repository
that supports the author's own textbook: _Data Structures, Algorithms, and Invariants - a Practical Guide_
(published by Cognella, 2025), hereinafter referred to as DSAIPG.

---

## Getting Started

Gryphon is published on Maven Central. Add it to your `build.sbt`,
where `x.y.z` is the latest version on Maven Central: it's shown on the [Sonatype Central](https://central.sonatype.com/artifact/com.phasmidsoftware/gryphon_3) badge at the top of this page.

```scala
libraryDependencies += "com.phasmidsoftware" %% "gryphon" % "x.y.z"
```

Gryphon 1.6.0 and later need Scala 3.9 or later; for Scala 3.7 or 3.8, use 1.5.1.

Full Scaladoc is at <https://rchillyard.github.io/Gryphon/latest/api/>, and the Java façade's
Javadoc is at <https://rchillyard.github.io/Gryphon/latest/javadoc/>.

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
| — | Minimum spanning tree (Borůvka) | `Boruvka` |
| 4.9 | Shortest paths (Dijkstra) | `ShortestPaths.dijkstra` |
| 4.10 | Shortest paths in DAGs | `AcyclicShortestPaths` |
| 4.11 | Shortest paths (Bellman–Ford) | `BellmanFord` |

Additionally, some algorithms and graph properties that are not directly covered in that chapter:

- **Cycle detection** — `Graph.isCyclic` (undirected and directed)
- **Bipartite checking** — `UndirectedGraph.isBipartite`
- **Connectivity** — `UndirectedGraph.isConnected`
- **Degree statistics** — `UndirectedGraph.degree`, `maxDegree`, `meanDegree`;
  `EdgeGraph.inDegree` and `outDegree`, which are defined for directed graphs too
- **Self-loop count** — `EdgeGraph.numberOfSelfLoops`
- **Union-Find** — `Connectivity` (used internally by Kruskal and Borůvka)
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
// Monoid[Double] comes from cats-kernel's implicit scope; no import is needed.

val result = ShortestPaths.dijkstra(graph, 0)   // graph must be a DirectedGraph
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

### Minimum spanning tree (Borůvka)

```scala
val mstEdges: Seq[Edge[Int, Double]] = Boruvka.mst(undirectedGraph)
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
graph.numberOfSelfLoops // Int
graph.inDegree(v)       // Int — for an undirected graph, the same as degree
graph.outDegree(v)      // Int

// Undirected only. These are not declared for a DirectedGraph, so calling one
// on a digraph is a compile error rather than an exception.
graph.isBipartite       // Boolean
graph.isConnected       // Boolean
graph.degree(v)         // Int
graph.maxDegree         // Int
graph.meanDegree        // Double — equals 2*M/N
```

For a directed graph, use `ConnectedComponents` for weak connectivity,
`StronglyConnectedComponents.kosaraju` for strong, and `inDegree`/`outDegree`
in place of `degree`.

---

## Java API

Gryphon provides a complete Java façade in the `com.phasmidsoftware.gryphon.java`
package, giving Java students access to all major graph algorithms without
exposure to Scala typeclasses, `given`/`using`, or companion object syntax.

Its API documentation is published separately from the Scaladoc, at
<https://rchillyard.github.io/Gryphon/latest/javadoc/>. The Scaladoc covers the Scala
library only — Scaladoc does not process Java sources — so the façade does not appear
there.

### Building with Maven

Insert the following into the `<dependencies>` block of your pom.xml:

````xml
<dependency>
  <groupId>com.phasmidsoftware</groupId>
  <artifactId>gryphon_3</artifactId>
  <version>x.y.z</version>
</dependency>
````

The Java API is the primary interface for readers of DSAIPG and/or INFO6205 students at Northeastern University.

### Core types

| Class | Description |
|---|---|
| `Edge<V>` | Immutable unweighted edge with `from()`, `to()`, `reverse()` |
| `WeightedEdge<V, E>` | Extends `Edge<V>` with a typed `attribute()` |
| `Graph<V>` | Mutable lazy-builder façade; directed or undirected |
| `Connectivity<V>` | Mutable Union-Find façade for disjoint-set operations |
| `ShortestPaths` | Dijkstra's algorithm |
| `MinimumSpanningTree` | Prim's, Kruskal's, and Borůvka's algorithms |
| `WeightedGraph.fromResource` | Loads a graph directly from a `.graph` resource file |
| `ConnectedComponents` | Connected components of undirected graphs |
| `TopologicalSort` | Topological sort of DAGs; `isDAG` predicate |
| `ShortestPaths.bellmanFord` | Shortest paths with negative weights; detects negative cycles |
| `ShortestPaths.acyclic` | Linear-time shortest paths in a DAG |
| `StronglyConnectedComponents` | Kosaraju's algorithm |

### Building a graph from a resource file

Java students can load a graph directly from a `.graph` classpath resource,
avoiding verbose `addEdge` calls in test code:

```java
// Option 1 — Integer vertices, Double weights
WeightedGraph<Integer, Double> g = WeightedGraph.undirectedFromResource("prim.graph");

// Option 3 — custom vertex and edge types
WeightedGraph<Building, TunnelProperties> tunnels = WeightedGraph.undirectedFromResource(
    "tunnels.graph", Building::parse, TunnelProperties::parse);
```

The `.graph` format uses `=` for undirected edges and `>` for directed edges;
`//` introduces a comment line.

### Building a graph

`Graph<V>` is mutable in the Java idiom. Create it with a factory method and
add edges one at a time:

```java
// Unweighted directed graph
Graph<String> g = Graph.directed();
g.addEdge("A", "B");
g.addEdge("B", "C");
g.addEdge("C", "A");

// Weighted undirected graph. Note WeightedGraph, not Graph: a plain Graph<V>
// accepts a WeightedEdge but keeps no weight, so the algorithms cannot use it.
WeightedGraph<Integer, Double> w = WeightedGraph.undirectedWeighted();
w.addEdge(new WeightedEdge<>(0, 1, 0.26));
w.addEdge(new WeightedEdge<>(0, 7, 0.16));
w.addEdge(new WeightedEdge<>(1, 7, 0.19));
```

### BFS and DFS

Both traversals return a came-from map — a `Map<V, V>` where each entry
`v → parent` records who discovered `v`. **The start vertex is absent**: it has no
predecessor. So are unreachable vertices. This is the same convention as the
Dijkstra SPT, the Prim MST and the Kosaraju SCC map.

```java
Graph<String> g = Graph.undirected();
g.addEdge("A", "B");
g.addEdge("B", "C");
g.addEdge("C", "D");

Map<String, String> bfsTree = g.bfs("A");
boolean reachable = bfsTree.containsKey("D");   // true
String parent     = bfsTree.get("C");           // "B"

// Reconstruct path from D back to A: walk until the key is absent.
List<String> path = new ArrayList<>();
String v = "D";
path.add(v);
while (bfsTree.containsKey(v)) {
    v = bfsTree.get(v);
    path.add(0, v);
}
// path == ["A", "B", "C", "D"]

Map<String, String> dfsTree = g.dfs("A");
```

`dfs` explores only the component containing its start vertex. To cover the
whole graph, use `dfsAll`, which restarts from each undiscovered vertex and
returns a depth-first *forest*. One root per component is absent from the map,
so the number of absent vertices is the number of components.

```java
Graph<Integer> g = Graph.undirected();
g.addEdge(0, 1);
g.addEdge(2, 3);          // a second, disjoint component

g.dfs(0).size();          // 1 — only vertex 1, in the first component
g.dfsAll().size();        // 2 — both components, less one root each
```

To stop as soon as you reach a known target, use `bfsUntil`. The came-from map
then covers only what was discovered before stopping.

```java
Map<Integer, Integer> toGoal = g.bfsUntil(0, v -> v == 3);
```

It is named `bfsUntil` rather than being an overload of `bfs`, because a lambda
could not then be resolved against the `Function<V, Iterable<V>>` overload.

### Shortest paths (Dijkstra)

`ShortestPaths.dijkstra` returns a shortest-path tree (SPT) as a
`Map<V, WeightedEdge<V, Double>>`. Each entry `v → edge` records the cheapest
incoming edge to `v`. The start vertex is absent.

```java
WeightedGraph<Integer, Double> g = WeightedGraph.directedWeighted();
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

// Option 3 — custom cost type: combine, zero, and comparator
Map<Integer, WeightedEdge<Integer, Double>> spt3 = ShortestPaths.dijkstra(
    g, 0,
    Double::sum,
    0.0,
    Comparator.naturalOrder());
```

### Shortest paths with negative weights (Bellman–Ford)

Dijkstra assumes no edge makes a path cheaper the further you go, so it is wrong
on negative weights. `ShortestPaths.bellmanFord` handles them. Where a negative
cycle is reachable from the source no shortest path exists — each lap round the
cycle is cheaper than the last — and the result is an empty `Optional`.

```java
WeightedGraph<Integer, Double> g = WeightedGraph.directedWeighted();
g.addEdge(new WeightedEdge<>(0, 1,  1.0));
g.addEdge(new WeightedEdge<>(1, 2, -2.0));
g.addEdge(new WeightedEdge<>(0, 2,  0.5));

Map<Integer, WeightedEdge<Integer, Double>> spt =
    ShortestPaths.bellmanFord(g, 0).orElseThrow();

spt.get(2).from();   // 1 — going via 1 costs −1.0, cheaper than the direct 0.5

// A negative cycle is reported rather than thrown:
g.addEdge(new WeightedEdge<>(2, 1, -1.0));
ShortestPaths.bellmanFord(g, 0);   // Optional.empty()
```

An Option 3 overload takes `combine`, `zero` and a `Comparator`, as Dijkstra's does.

### Shortest paths in a DAG

If the graph is acyclic, `ShortestPaths.acyclic` relaxes edges in topological
order, visiting each edge once. That is linear in the size of the graph — faster
than both Dijkstra and Bellman–Ford — and negative weights are safe, because a
DAG has no cycle for them to make cheaper.

```java
Map<Integer, WeightedEdge<Integer, Double>> spt = ShortestPaths.acyclic(dag, 0);
```

It throws `IllegalArgumentException` if the graph has a cycle; test first with
`TopologicalSort.isDAG(g)` if you are not sure.

### Graph properties and predicates

```java
Graph<Integer> g = Graph.undirected();
g.addEdge(0, 1);
g.addEdge(1, 2);
g.addEdge(2, 0);

g.order();              // 3 — the number of vertices, N
g.size();               // 3 — the number of edges, M
g.degree(0);            // 2
g.inDegree(0);          // 2 — same as degree, in an undirected graph
g.outDegree(0);         // 2
g.maxDegree();          // 2
g.meanDegree();         // 2.0, which is 2M/N
g.numberOfSelfLoops();  // 0

g.isCyclic();           // true
g.isConnected();        // true
g.isBipartite();        // false — a 3-cycle cannot be two-coloured
```

`order`, `size`, `numberOfSelfLoops` and `isCyclic` apply to both kinds of graph,
and `WeightedGraph` inherits all of them.

`inDegree` and `outDegree` apply to both as well. In an undirected graph every edge can be
traversed both ways, so the in-degree, the out-degree and the degree are the same number; in a
directed one they count arcs arriving and leaving, and a self-loop counts once towards each.

```java
Graph<Integer> d = Graph.directed();
d.addEdge(0, 1);
d.addEdge(0, 2);

d.outDegree(0);   // 2
d.inDegree(0);    // 0 — a source
d.inDegree(2);    // 1
```

Two groups do not apply to both:

- `degree`, `maxDegree` and `meanDegree` throw `GraphException` on a directed
  graph. A vertex of a digraph has an in-degree and an out-degree, not one
  degree — use those. `GraphException` is unchecked from 2.0.0, so Java callers
  can catch it; before that they could not, because the façade's methods do not
  declare it and javac rejects a `catch` for a checked exception the called
  method cannot throw.
- `isConnected` and `isBipartite` throw `GraphException` on a directed graph.
  Connectivity of a digraph has two meanings — weak (ignoring direction) and
  strong (every vertex reachable from every other) — and neither is assumed. Use
  `ConnectedComponents` for the first and `StronglyConnectedComponents.kosaraju`
  for the second. In Scala these are not declared on a directed graph at all, so
  the compiler rejects them; the façade checks at run time because Java cannot
  express that.

### Minimum spanning tree (Prim)

`MinimumSpanningTree.prim` returns the MST as a `Map<V, WeightedEdge<V, Double>>`
mapping each non-source vertex to its cheapest MST edge. The start vertex is absent.

```java
WeightedGraph<Integer, Double> g = WeightedGraph.undirectedWeighted();
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

// Option 3 — custom comparator
List<WeightedEdge<Integer, Double>> mst3 = MinimumSpanningTree.kruskal(
    g,
    Comparator.naturalOrder());
```
### Minimum spanning tree (Borůvka)

`MinimumSpanningTree.boruvka` returns the MST as a
`List<WeightedEdge<V, Double>>`. Unlike Kruskal, the list is not guaranteed
to be in weight-sorted order — edges are added round by round.

```java
// Option 1 — Double weights
List<WeightedEdge<Integer, Double>> mst = MinimumSpanningTree.boruvka(g);

double totalWeight = mst.stream()
    .mapToDouble(WeightedEdge::attribute)
    .sum(); // 1.81

// Option 3 — custom comparator
List<WeightedEdge<Integer, Double>> mst3 = MinimumSpanningTree.boruvka(
    g, Comparator.naturalOrder());
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
Connectivity<String> c = Connectivity.createLazy("A", "B", "C", "D");

// Weighted Quick Union with path compression — amortised near-O(1)
Connectivity<String> optimized = Connectivity.createOptimized("A", "B", "C", "D");

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

- **Option 3** — the caller supplies functional interfaces
  (`BinaryOperator`, `Comparator`) that control how costs are combined and
  compared. This mirrors the typeclass abstraction of the Scala engine
  and is suitable for non-`Double` weight types or custom orderings.
  Algorithms that only compare costs, such as Prim, Kruskal and Borůvka, take
  a `Comparator` alone; those that accumulate them, such as Dijkstra and
  Bellman–Ford, also take a combiner and its identity.

```java
// Option 1 — Dijkstra with Double weights
Map<Integer, WeightedEdge<Integer, Double>> spt =
    ShortestPaths.dijkstra(g, 0);

// Option 3 — Dijkstra with a custom combiner, identity and comparator
Map<Integer, WeightedEdge<Integer, Double>> spt3 =
    ShortestPaths.dijkstra(g, 0,
        Double::sum,
        0.0,
        Comparator.naturalOrder());
```

### Directionality constraints

| Algorithm | Graph type required |
|---|---|
| `Graph.bfs` / `Graph.dfs` / `Graph.dfsAll` / `Graph.bfsUntil` | either |
| `Graph.order` / `size` / `numberOfSelfLoops` | either |
| `Graph.inDegree` / `outDegree` | either |
| `Graph.isCyclic` | either |
| `Graph.isConnected` / `isBipartite` | undirected |
| `Graph.degree` / `maxDegree` / `meanDegree` | undirected |
| `ShortestPaths.dijkstra` | directed |
| `ShortestPaths.bellmanFord` | directed |
| `ShortestPaths.acyclic` | directed, and acyclic |
| `MinimumSpanningTree.prim` | undirected |
| `MinimumSpanningTree.kruskal` | undirected |
| `MinimumSpanningTree.boruvka` | undirected |
| `StronglyConnectedComponents.kosaraju` | directed |

Calling an algorithm with the wrong graph type throws `IllegalStateException`
with a descriptive message. The exception is the degree family, which throws
`GraphException`: it is a property of the graph rather than an algorithm over
it, and the check happens in the Scala layer. `ShortestPaths.acyclic` throws
`IllegalArgumentException` if the graph is directed but has a cycle.

---

## Architecture

Gryphon is structured in five layers:

```
com.phasmidsoftware.gryphon
  .core       — Graph, VertexMap, Vertex, Edge, Adjacency, Connexion,
                Connected, Traversable, EdgeTraversable
  .adjunct    — DirectedGraph, UndirectedGraph, DirectedEdge,
                UndirectedEdge, Connectivity,
  .builder    — GraphBuilder
  .traverse   — ConnectedComponents, Kosaraju, TopologicalSort,
                ShortestPaths, MST, AcyclicShortestPaths, BellmanFord,
                Kruskal, Boruvka, TraversalResult, Connexions
  .parse      — GraphParser, BaseParser, Parseable
  .java       — Graph, Edge, WeightedEdge, Connectivity,
                ShortestPaths, MinimumSpanningTree,
                StronglyConnectedComponents, ConnectedComponents,
                TopologicalSort, JavaFacadeBridge
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

The library has 583 tests covering all algorithms, graph properties, and
edge cases including:
- Disconnected graphs and forests
- Negative edge weights (Bellman–Ford, AcyclicShortestPaths)
- Negative cycle detection
- Self-loops
- Agreement between Bellman–Ford and Dijkstra on non-negative graphs
- Agreement between Prim, Kruskal, and Borůvka on MST weight
- Integration tests on the Northeastern University tunnel network (80 buildings, 79 edges, $6,648,954)
- Java API tests for all major algorithms

```bash
sbt test
```

---

## Versioning

| Version | Changes                                                                            |
|---|------------------------------------------------------------------------------------|
| 1.0.0 | Initial release                                                                    |
| 1.1.0 | First update post-release                                                          |
| 1.2.0 | Rename UnionFind→Connectivity; F-bounded DisjointSet; ConnectivityOptimized        |
| 1.2.1 | Java façade: Edge, WeightedEdge, Graph, GraphTraversal, Connectivity               |
| 1.2.2 | Java façade: ShortestPaths (Dijkstra), MinimumSpanningTree (Prim)                  |
| 1.2.3 | Java façade: MinimumSpanningTree (Kruskal), StronglyConnectedComponents (Kosaraju) |
| 1.3.0 | Added WeightedGraph                                                                |
| 1.4.0 | All BFS/DFS now use Visitor.traverse                                               |
| 1.5.0 | Borůvka MST; symmetric UndirectedEdge.equals/hashCode; Java façade: MinimumSpanningTree (Borůvka) |
| 1.5.1 | GraphBuilder (Scala); WeightedGraph.fromResource (Java); Graph/WeightedGraph resource-load constructors |
| 1.5.2 | Borůvka.mst returns Seq[Edge[V,E]] like Kruskal; Borůvka deduplication bug fix; tunnel integration tests |
| 1.5.3 | Refactoring: Connectivity is now a trait with factory methods; Java façade updates |
| 1.6.0 | Scala 3.9.0, Visitor 1.7.0, Flog 1.1.0; Borůvka refactoring. First published release since 1.5.1, so it also brings the 1.5.2 and 1.5.3 changes to Central |
| 2.0.0 | `GraphException` is now unchecked, so Java callers can catch it; `addEdge` accepts only the kind of edge its graph admits; `prim` and `dijkstra` require an undirected and a directed graph respectively; `isConnected` and `isBipartite` are undirected-only; `inDegree`/`outDegree` added; the Java façade's `Connectivity.create` is renamed `createLazy`; type classes moved to `cats.kernel.Monoid`, adding a `cats-kernel` dependency, and the unused `util.HasZero` is removed; Javadoc published for the Java façade. **Breaking** — see "Migrating from 1.x" below |
| 1.7.0 | Java API for the rest of the Scala library: Bellman–Ford, acyclic shortest paths, graph properties and predicates, `dfsAll` and `bfsUntil`. Two bug fixes: an isolated vertex added with `addVertex` no longer vanishes when the graph is materialised, and `dfsAll` no longer revisits vertices. One `GraphException`: `util.GraphException` is now a deprecated type alias for `core.GraphException`, and the unusable `core.DAG` trait is removed (both **binary-incompatible** — see the notes below). Build and release standardisation: `-Werror`, the PVP version scheme, MiMa binary-compatibility checking, and logback-classic moved to Test scope (it is no longer pulled in by Gryphon's users) |

### Migrating from 1.x to 2.0.0

Most code needs no change. Everything below is a compile error rather than a silent change of
behaviour, so the compiler will find each site for you.

| 1.x | 2.0.0 | Why |
|---|---|---|
| `com.phasmidsoftware.gryphon.util.GraphException` | `...core.GraphException` | The two identical classes are now one. The old name is a deprecated alias and still compiles; the class is gone from the jar, so recompile. |
| `GraphException` caught as a checked exception in Java | unchecked | It extends `RuntimeException`, so a Java caller can finally write `catch (GraphException e)` — previously javac refused. |
| `trait DAG[V, E]` | `AcyclicShortestPaths.shortestPaths`, or `ShortestPaths.acyclic` from Java | `DAG` extended the `DirectedGraph` case class, so nothing could implement it. Nothing did. |
| `trait HasZero[T]` | `cats.kernel.Monoid[T]` | Unused dead code; Cats is the answer its own comment pointed at. |
| `undirectedGraph.addEdge(someDirectedEdge)` | use an `UndirectedEdge` | It used to be accepted and silently mishandled: the adjacency was added, so `degree` counted it, while `edges` and `M` did not. |
| `MST.prim(traversable, start)` | `MST.prim(undirectedGraph, start)` | Prim needs an undirected graph; the signature now says so. |
| `ShortestPaths.dijkstra(traversable, start)` | `ShortestPaths.dijkstra(directedGraph, start)` | As above, for a directed graph. |
| `directedGraph.isConnected` / `.isBipartite` | `ConnectedComponents` (weak) or `Kosaraju` (strong) | They used to throw `UnsupportedOperationException`; now they are not declared for a digraph, so the compiler says so. From Java, they throw `GraphException`. |
| `visitor.core.Monoid` / `visitor.core.Zero` bounds | `cats.kernel.Monoid` | Same abstraction, with laws and standard instances. `identity` is called `empty`. A `Zero` bound becomes a `Monoid` bound. |
| Java: `Connectivity.create(…)` | `Connectivity.createLazy(…)` | The old name read as "the default one" when it is the *unoptimised* one. It now matches the Scala layer it delegates to, where the pair has always been `createLazy` and `createOptimized`. "Lazy" means without path compression, not lazily evaluated. |
| `import com.phasmidsoftware.visitor.core.given` for `Monoid[Double]` | delete the import | `cats-kernel` supplies it through the implicit scope. |

Unchanged, in case you expect otherwise: `Ordering` is still `scala.math.Ordering` rather than
`cats.kernel.Order`; `degree`, `maxDegree` and `meanDegree` are still undirected-only; and the Java
façade's class and method names are the same throughout, so Java code that compiled against 1.7.0
compiles against 2.0.0 unless it caught `UnsupportedOperationException` from the two predicates.

### Note on 1.7.0: `GraphException`

Up to 1.6.0 Gryphon had two identical `GraphException` case classes, `core.GraphException` and
`util.GraphException`. They were unrelated types, so catching one did not catch the other, and which one a
method threw was effectively arbitrary. From 1.7.0 `core.GraphException` is the only one, and
`util.GraphException` is a deprecated alias for it.

**Source compatible:** the old name still names the type, constructs the exception and works as a pattern,
so existing code compiles unchanged — with a deprecation warning. Change the import to
`com.phasmidsoftware.gryphon.core.GraphException` to clear it.

**Not binary compatible:** a type alias is erased, so the class `com.phasmidsoftware.gryphon.util.GraphException`
is no longer in the jar. Code compiled against 1.6.0 or earlier must be recompiled. This is why 1.7.0 is a
minor release rather than a patch.

### Note on 1.7.0: `core.DAG` removed

The trait `com.phasmidsoftware.gryphon.core.DAG` has been removed. It extended the `DirectedGraph` case
class, so nothing could implement it — a case class may not extend another case class — and nothing in the
library referenced it. No working code can have depended on it, but it was a public type, so its removal is
listed here with the other binary-incompatible change above. For shortest paths in a DAG, use
`AcyclicShortestPaths.shortestPaths` (Scala) or `ShortestPaths.acyclic` (Java).

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