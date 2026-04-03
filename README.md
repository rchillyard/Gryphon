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

However, in version 1.0.0, there are no Java interface methods.

---

## Getting Started

Add Gryphon to your `build.sbt`:

```scala
libraryDependencies += "com.phasmidsoftware" %% "gryphon" % "1.0.0"
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
| 4.7 | Minimum spanning tree (Prim) | `GraphTraversal.PrimTraversal` |
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
- **Union-Find** — `UnionFind`, `WeightedUnionFind` (used internally by Kruskal)
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

## Usage Examples

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
given Monoid[Double] with
  def empty: Double = 0.0
  def combine(x: Double, y: Double): Double = x + y
given Ordering[Double] = scala.math.Ordering.Double.TotalOrdering

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
val mstEdges: Seq[Edge[Double, Int]] = Kruskal.mst(undirectedGraph)
val totalWeight = mstEdges.map(_.attribute).sum
```

### Strongly connected components (Kosaraju)

```scala
val sccResult: SCCResult[Int] = Kosaraju.components(directedGraph)
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

## Architecture

Gryphon is structured in four layers:

```
com.phasmidsoftware.gryphon
  .core       — Graph, VertexMap, Vertex, Edge, Adjacency, Connexion,
                Connected, Traversable, EdgeTraversable
  .adjunct    — DirectedGraph, UndirectedGraph, DirectedEdge,
                UndirectedEdge, DisjointSet, UnionFind, WeightedUnionFind
  .traverse   — ConnectedComponents, Kosaraju, TopologicalSort,
                ShortestPaths, AcyclicShortestPaths, BellmanFord,
                Kruskal, TraversalResult, Connexions
  .parse      — GraphParser, BaseParser, Parseable
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

---

## Testing

The library has 365 tests covering all algorithms, graph properties, and
edge cases including:
- Disconnected graphs and forests
- Negative edge weights (Bellman–Ford, AcyclicShortestPaths)
- Negative cycle detection
- Self-loops
- Agreement between Bellman–Ford and Dijkstra on non-negative graphs
- Agreement between Prim and Kruskal on MST weight

```bash
sbt test
```

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