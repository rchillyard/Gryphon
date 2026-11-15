package com.phasmidsoftware.gryphon.java

import com.phasmidsoftware.gryphon.adjunct.{AttributedDirectedEdge, DirectedGraph, UndirectedEdge, UndirectedGraph}
import com.phasmidsoftware.gryphon.builder.GraphBuilder
// Deliberately not a wildcard, for the same reason as the traverse import below: package
// `...gryphon.java` has its own `Edge` and `Graph`, and Scala ranks a wildcard import ABOVE
// package members that come from a different file. So `core.*` rebinds `Edge` to `core.Edge[V, E]`
// and `materialise`'s `JavaList[Edge[V]]` stops compiling — which is exactly what happened.
import com.phasmidsoftware.gryphon.core.{AbstractGraph, EdgeGraph, EvaluableGraphNeighboursTraversal, GraphException, Vertex, VertexMap}
import com.phasmidsoftware.gryphon.parse.Parseable
import scala.annotation.unused
// Kept to six selectors or fewer, and deliberately not a wildcard: this file is in package
// `...gryphon.java`, which has its own ShortestPaths, ConnectedComponents and TopologicalSort.
// A wildcard import has lower precedence than same-package definitions, so `ShortestPaths` would
// rebind to the Java façade class. BellmanFord, AcyclicShortestPaths, Boruvka and Kosaraju are
// written out in full at their use sites instead.
import com.phasmidsoftware.gryphon.traverse.{ConnectedComponents, Kruskal, MST, ShortestPaths, TopologicalSort as ScalaTopSort}
// Visitor's own Monoid and Zero are hidden: Gryphon's algorithms take cats.kernel.Monoid now,
// and the wildcard would otherwise make the two ambiguous.
import cats.kernel.Monoid
import com.phasmidsoftware.visitor.core.{Monoid as _, *, given}
import java.lang.{Integer as JavaInteger, Iterable as JavaIterable}
import java.util.function.{BinaryOperator as JavaBinaryOperator, Function as JavaFunction}
// Exactly six selectors, in one clause, because Codacy enforces two rules that leave no other
// option: more than six in a clause asks for a wildcard, and splitting the package across two
// clauses is itself a finding. A wildcard is not available, since every name here is renamed to
// stop java.util's List, Map and Set shadowing Scala's own. The two least-used names,
// ArrayList and Set, are therefore written out in full at their use sites.
import java.util.{Collections as JavaCollections, Comparator as JavaComparator, LinkedHashMap as JavaLinkedHashMap, List as JavaList, Map as JavaMap, Optional as JavaOptional}
import scala.jdk.CollectionConverters.*
import scala.util.{Random, Try}

/**
 * Internal Scala bridge used by the Java façade.
 *
 * All members are accessible from Java as static methods on `JavaFacadeBridge$`.
 * This object is package-private to `com.phasmidsoftware.gryphon.java`.
 *
 * All BFS and DFS traversals — both Option 1 and Option 3 — delegate to the
 * Scala Visitor engine using `CameFromJournal`. The start vertex is absent from
 * the came-from map in all cases, consistent with all other algorithm façades.
 *
 * Option 3 wraps the Java `Function<V, Iterable<V>>` into a Scala
 * `Neighbours[V, V]` given instance, then delegates to `Traversal.bfs`/`dfs`.
 * The overhead of the `asScala` iterator conversion is O(1) per neighbour visit
 * and negligible for all realistic graph sizes.
 */
private[java] object JavaFacadeBridge:

  // ---------------------------------------------------------------------------
  // BFS — Option 1: graph's own adjacency
  // ---------------------------------------------------------------------------

  /**
   * Runs BFS from `start` on the materialised Scala graph.
   * The start vertex is absent from the returned came-from map.
   */
  def bfs[V](scalaGraph: AbstractGraph[V], start: V): JavaMap[V, V] =
    given Random = Random(0)

    new EvaluableGraphNeighboursTraversal[V, V, JavaMap[V, V]](identity)(scalaGraph) {
      def traversal: JavaMap[V, V] =
        val visitor = JournaledVisitor.withQueueJournalAndCameFrom[V, V]
        // The seven casts to JournaledVisitor in this file are all the same thing, and none is
        // removable here: Visitor's `Traversal.bfs`/`dfs` are declared to return `Visitor[V, R, J]`,
        // the supertype, while the journal — the whole point of the call — is only reachable on the
        // JournaledVisitor subtype. The fix belongs in Visitor's signatures, not in this file.
        val result = Traversal.bfs(start, visitor).asInstanceOf[JournaledVisitor[V, V, ?]]
        cameFromToJavaMap(result)
    }.traversal

  // ---------------------------------------------------------------------------
  // BFS — Option 3: custom neighbour function
  // ---------------------------------------------------------------------------

  /**
   * Runs BFS from `start` using a caller-supplied neighbour function,
   * wrapped into a Scala `Neighbours[V, V]` given instance.
   * The start vertex is absent from the returned came-from map.
   */
  def bfsWithNeighbours[V](
                                  @unused scalaGraph: AbstractGraph[V],
                                  start: V,
                                  neighboursFn: JavaFunction[V, JavaIterable[V]]
                          ): JavaMap[V, V] =

    given Evaluable[V, V] with
      def evaluate(v: V): Option[V] = Some(v)
    given Neighbours[V, V] with
      def neighbours(v: V): Iterator[V] =
        neighboursFn.apply(v).iterator().asScala
    val visitor = JournaledVisitor.withQueueJournalAndCameFrom[V, V]
    val result = Traversal.bfs(start, visitor)
            .asInstanceOf[JournaledVisitor[V, V, ?]]
    cameFromToJavaMap(result)

  // ---------------------------------------------------------------------------
  // DFS — Option 1: graph's own adjacency
  // ---------------------------------------------------------------------------

  /**
   * Runs DFS from `start` on the materialised Scala graph, returning a Java
   * came-from map. The start vertex is absent — it has no predecessor.
   *
   * @param scalaGraph the materialised Scala graph.
   * @param start      the source vertex.
   * @tparam V the vertex type.
   * @return an unmodifiable Java `Map<V, V>` came-from map.
   */
  def dfs[V](scalaGraph: AbstractGraph[V], start: V): JavaMap[V, V] =
    given Random = Random(0)

    new EvaluableGraphNeighboursTraversal[V, V, JavaMap[V, V]](identity)(scalaGraph) {
      def traversal: JavaMap[V, V] =
        val visitor = JournaledVisitor.withListJournalAndCameFrom[V, V]
        val result = Traversal.dfs(start, visitor)
                .asInstanceOf[JournaledVisitor[V, V, ?]]
        cameFromToJavaMap(result)
    }.traversal

  // ---------------------------------------------------------------------------
  // DFS — Option 3: custom neighbour function
  // ---------------------------------------------------------------------------

  /**
   * Runs DFS from `start` using a caller-supplied neighbour function,
   * wrapped into a Scala `Neighbours[V, V]` given instance.
   * The start vertex is absent from the returned came-from map.
   */
  def dfsWithNeighbours[V](
                                  @unused scalaGraph: AbstractGraph[V],
                                  start: V,
                                  neighboursFn: JavaFunction[V, JavaIterable[V]]
                          ): JavaMap[V, V] =
    given Evaluable[V, V] with
      def evaluate(v: V): Option[V] = Some(v)
    given Neighbours[V, V] with
      def neighbours(v: V): Iterator[V] =
        neighboursFn.apply(v).iterator().asScala
    val visitor = JournaledVisitor.withListJournalAndCameFrom[V, V]
    val result = Traversal.dfs(start, visitor)
            .asInstanceOf[JournaledVisitor[V, V, ?]]
    cameFromToJavaMap(result)

  // ---------------------------------------------------------------------------
  // Graph file reader
  // ---------------------------------------------------------------------------

  /**
   * Loads an undirected weighted graph from a resource and returns both the
   * Scala graph and a Java edge list extracted from it.
   * The edge list is used to populate WeightedGraph.weightedEdges so that
   * algorithm bridge methods that require it work correctly.
   */
  def undirectedWeightedFromResource(resourceName: String
                                    ): (UndirectedGraph[Int, Double], JavaList[WeightedEdge[Int, Double]]) =
    val g = GraphBuilder.undirected[Int, Double].fromResource(resourceName).get
    (g, extractWeightedEdges(g))

  def directedWeightedFromResource(resourceName: String
                                  ): (DirectedGraph[Int, Double], JavaList[WeightedEdge[Int, Double]]) =
    val g = GraphBuilder.directed[Int, Double].fromResource(resourceName).get
    (g, extractWeightedEdges(g))

  def undirectedWeightedFromResourceCustom[V, E](
                                                        resourceName: String,
                                                        vertexParser: JavaFunction[String, V],
                                                        edgeParser: JavaFunction[String, E]
                                                ): (UndirectedGraph[V, E], JavaList[WeightedEdge[V, E]]) =
    given Parseable[V] = asParseable(vertexParser, """\w+""")

    given Parseable[E] = asParseable(edgeParser, """(\d+(\.\d*)?|\d*\.\d+)""")

    val g = GraphBuilder.undirected[V, E].fromResource(resourceName).get
    (g, extractWeightedEdges(g))

  def directedWeightedFromResourceCustom[V, E](
                                                      resourceName: String,
                                                      vertexParser: JavaFunction[String, V],
                                                      edgeParser: JavaFunction[String, E]
                                              ): (DirectedGraph[V, E], JavaList[WeightedEdge[V, E]]) =
    given Parseable[V] = asParseable(vertexParser, """\w+""")

    given Parseable[E] = asParseable(edgeParser, """(\d+(\.\d*)?|\d*\.\d+)""")

    val g = GraphBuilder.directed[V, E].fromResource(resourceName).get
    (g, extractWeightedEdges(g))

  /**
   * Extracts the edges from a Scala EdgeGraph as a Java List of WeightedEdge.
   * Only flipped=false adjacencies are included to avoid duplicates.
   */
  private def extractWeightedEdges[V, E](g: com.phasmidsoftware.gryphon.core.EdgeGraph[V, E]
                                        ): JavaList[WeightedEdge[V, E]] =
    val list = new java.util.ArrayList[WeightedEdge[V, E]]()
    g.edges.foreach { e =>
      list.add(new WeightedEdge[V, E](e.white, e.black, e.attribute))
    }
    JavaCollections.unmodifiableList(list)

  /**
   * Wraps a Java `Function<String, T>` into a `Parseable[T]`.
   * The regex must match exactly the tokens that `GraphParser` will see —
   * use `\\w+` for string vertex keys and the double regex for numeric weights.
   * Passing `.*` or `\\S+` causes comment lines starting with `//` to be
   * parsed as vertex tokens rather than filtered out.
   */
  private def asParseable[T](f: JavaFunction[String, T],
                             regexStr: String): Parseable[T] =
    new Parseable[T]:
      def parse(s: String): Try[T] = Try(f.apply(s))

      def regex: scala.util.matching.Regex = regexStr.r

      def message: String = "custom"

      def none: T = throw new UnsupportedOperationException("none not supported for custom Parseable")

  // ---------------------------------------------------------------------------
  // Materialisation — unweighted (Unit edge attribute)
  // ---------------------------------------------------------------------------

  /**
   * Builds an immutable Scala graph from the Java canonical edge list,
   * using `Unit` as the edge attribute type (for BFS/DFS).
   * Folds over `VertexMap.+[E]` directly to ensure both endpoints exist.
   *
   * `vertices` carries the façade's whole vertex set, so that a vertex added
   * with `addVertex` and never joined by an edge still exists in the Scala
   * graph. Without it such a vertex was silently dropped, and every algorithm
   * — the component count in particular — behaved as if it were not there.
   */
  def materialise[V](edges: JavaList[Edge[V]], vertices: java.util.Set[V], directed: Boolean): AbstractGraph[V] =
    if directed then
      val vm = edges.asScala.foldLeft(VertexMap[V]) { (vm, e) =>
        vm + AttributedDirectedEdge(None, e.from, e.to)
      }
      DirectedGraph[V, Unit](withIsolatedVertices(vm, vertices))
    else
      val g = edges.asScala.foldLeft(UndirectedGraph[V, Unit](VertexMap[V])) { (g, e) =>
        g.addEdge(UndirectedEdge((), e.from, e.to))
      }
      UndirectedGraph[V, Unit](withIsolatedVertices(g.vertexMap, vertices))

  /**
   * Adds any vertex of `vertices` that the edges did not already introduce.
   * Vertices already present are left alone, so their adjacencies survive.
   */
  private def withIsolatedVertices[V](vm: VertexMap[V], vertices: java.util.Set[V]): VertexMap[V] =
    vertices.asScala.foldLeft(vm) { (acc, v) =>
      if acc.get(v).isDefined then acc else acc + Vertex.createWithBag(v)
    }

  // ---------------------------------------------------------------------------
  // Materialisation — typed weighted directed
  // ---------------------------------------------------------------------------

  private def materialiseWeighted[V, E](edges: JavaList[WeightedEdge[V, E]]): DirectedGraph[V, E] =
    val vm = edges.asScala.foldLeft(VertexMap[V]) { (vm, e) =>
      vm + AttributedDirectedEdge(e.attribute, e.from, e.to)
    }
    DirectedGraph[V, E](vm)

  // ---------------------------------------------------------------------------
  // Materialisation — typed weighted undirected
  // ---------------------------------------------------------------------------

  private def materialiseWeightedUndirected[V, E](edges: JavaList[WeightedEdge[V, E]]): UndirectedGraph[V, E] =
    val vm = edges.asScala.foldLeft(VertexMap[V]) { (vm, e) =>
      vm + UndirectedEdge(e.attribute, e.from, e.to)
    }
    UndirectedGraph[V, E](vm)

  // ---------------------------------------------------------------------------
  // Dijkstra — Option 1: Double weights
  // ---------------------------------------------------------------------------

  def dijkstraDouble[V](
                               @unused scalaGraph: AbstractGraph[V],
                               edges: JavaList[WeightedEdge[V, Double]],
                               start: V
                       ): JavaMap[V, WeightedEdge[V, Double]] =
    given Random = Random(0)

    given Ordering[Double] = scala.math.Ordering.Double.TotalOrdering

    val weightedGraph = materialiseWeighted[V, Double](edges)
    val result = ShortestPaths.dijkstra[V, Double](weightedGraph, start)
    edgeResultToJavaMap(result)

  // ---------------------------------------------------------------------------
  // Dijkstra — Option 3: custom type, combine + zero + comparator
  // ---------------------------------------------------------------------------

  /**
   * Dijkstra requires both a `Monoid[E]` (combine + identity) for path cost
   * accumulation and an `Ordering[E]` for priority comparison.
   * Unlike Prim/Kruskal, Dijkstra cannot work with Ordering alone.
   */
  def dijkstraCustom[V, E](
                                  @unused scalaGraph: AbstractGraph[V],
                                  edges: JavaList[WeightedEdge[V, E]],
                                  start: V,
                                  combineFn: JavaBinaryOperator[E],
                                  zero: E,
                                  comparator: JavaComparator[E]
                          ): JavaMap[V, WeightedEdge[V, E]] =
    given Random = Random(0)

    given Ordering[E] = Ordering.comparatorToOrdering(using comparator)

    given Monoid[E] with
      def empty: E = zero

      def combine(x: E, y: E): E = combineFn.apply(x, y)
    val weightedGraph = materialiseWeighted[V, E](edges)
    val result = ShortestPaths.dijkstra[V, E](weightedGraph, start)
    edgeResultToJavaMap(result)

  // ---------------------------------------------------------------------------
  // Prim — Option 1: Double weights
  // ---------------------------------------------------------------------------

  def primDouble[V](
                           @unused scalaGraph: AbstractGraph[V],
                           edges: JavaList[WeightedEdge[V, Double]],
                           start: V
                   ): JavaMap[V, WeightedEdge[V, Double]] =
    given Random = Random(0)

    given Ordering[Double] = scala.math.Ordering.Double.TotalOrdering

    // Prim compares edge weights and never accumulates them, so only `empty` is ever used.
    // Cats has no identity-only type class — a lone identity satisfies no law — so this is a
    // Monoid whose combine is the honest one for Double rather than a stub.
    val weightedGraph = materialiseWeightedUndirected[V, Double](edges)
    val result = MST.prim[V, Double](weightedGraph, start)
    edgeResultToJavaMap(result)

  // ---------------------------------------------------------------------------
  // Prim — Option 3: custom type, Comparator only
  // ---------------------------------------------------------------------------

  /**
   * Prim uses edge weight for priority comparison only — costs are never
   * accumulated. Only `Ordering[E]` is needed; the `Monoid.combine` stub
   * returns `identity` and is never called.
   */
  def primCustom[V, E](
                              @unused scalaGraph: AbstractGraph[V],
                              edges: JavaList[WeightedEdge[V, E]],
                              start: V,
                              comparator: JavaComparator[E]
                      ): JavaMap[V, WeightedEdge[V, E]] =
    given Random = Random(0)

    given Ordering[E] = Ordering.comparatorToOrdering(using comparator)

    // As above, Prim never accumulates, so combine is never called; with a caller-supplied
    // attribute type there is nothing sensible to combine with, so it returns empty.
    given Monoid[E] with
      def empty: E = edges.get(0).attribute

      def combine(x: E, y: E): E = empty
    val weightedGraph = materialiseWeightedUndirected[V, E](edges)
    val result = MST.prim[V, E](weightedGraph, start)
    edgeResultToJavaMap(result)

  // ---------------------------------------------------------------------------
  // Kruskal — Option 1: Double weights
  // ---------------------------------------------------------------------------

  def kruskalDouble[V](
                              edges: JavaList[WeightedEdge[V, Double]]
                      ): JavaList[WeightedEdge[V, Double]] =
    given Ordering[Double] = scala.math.Ordering.Double.TotalOrdering

    val weightedGraph = materialiseWeightedUndirected[V, Double](edges)
    val result = Kruskal.mst(weightedGraph)
    mstSeqToJavaList(result)

  // ---------------------------------------------------------------------------
  // Kruskal — Option 3: custom type, Comparator only
  // ---------------------------------------------------------------------------

  /**
   * Kruskal sorts edges by weight for comparison only — costs are never
   * accumulated. Only `Ordering[E]` is needed.
   */
  def kruskalCustom[V, E](
                                 edges: JavaList[WeightedEdge[V, E]],
                                 comparator: JavaComparator[E]
                         ): JavaList[WeightedEdge[V, E]] =
    given Ordering[E] = Ordering.comparatorToOrdering(using comparator)

    val weightedGraph = materialiseWeightedUndirected[V, E](edges)
    val result = Kruskal.mst(weightedGraph)
    mstSeqToJavaList(result)


  // ---------------------------------------------------------------------------
  // Boruvka — Option 1: Double weights
  // ---------------------------------------------------------------------------

  def boruvkaDouble[V](
                              edges: JavaList[WeightedEdge[V, Double]]
                      ): JavaList[WeightedEdge[V, Double]] =
    given Ordering[Double] = scala.math.Ordering.Double.TotalOrdering

    val weightedGraph = materialiseWeightedUndirected[V, Double](edges)
    val result = com.phasmidsoftware.gryphon.traverse.Boruvka.mst(weightedGraph)
    mstSeqToJavaList(result)

  // ---------------------------------------------------------------------------
  // Boruvka — Option 3: custom type, Comparator only
  // ---------------------------------------------------------------------------

  /**
   * Borůvka selects minimum-weight crossing edges — costs are never accumulated.
   * Only `Ordering[E]` is needed; the `Monoid.combine` stub is never called.
   */
  def boruvkaCustom[V, E](
                                 edges: JavaList[WeightedEdge[V, E]],
                                 comparator: JavaComparator[E]
                         ): JavaList[WeightedEdge[V, E]] =
    given Ordering[E] = Ordering.comparatorToOrdering(using comparator)

    given Monoid[E] with
      def empty: E = edges.get(0).attribute

      def combine(x: E, y: E): E = empty
    val weightedGraph = materialiseWeightedUndirected[V, E](edges)
    val result = com.phasmidsoftware.gryphon.traverse.Boruvka.mst(weightedGraph)
    mstSeqToJavaList(result)

  // ---------------------------------------------------------------------------
  // Bellman-Ford — Option 1: Double weights
  // ---------------------------------------------------------------------------

  /**
   * Shortest paths allowing negative edge weights.
   * Empty if a negative cycle is reachable from `start`.
   */
  def bellmanFordDouble[V](
                                  edges: JavaList[WeightedEdge[V, Double]],
                                  start: V
                          ): JavaOptional[JavaMap[V, WeightedEdge[V, Double]]] =
    given Ordering[Double] = scala.math.Ordering.Double.TotalOrdering

    optionalEdgeResult(com.phasmidsoftware.gryphon.traverse.BellmanFord.shortestPaths[V, Double](materialiseWeighted(edges), start))

  // ---------------------------------------------------------------------------
  // Bellman-Ford — Option 3: custom type, combine + zero + comparator
  // ---------------------------------------------------------------------------

  /**
   * As `bellmanFordDouble`, with a caller-supplied cost type. Like Dijkstra and
   * unlike Prim/Kruskal/Borůvka, Bellman-Ford accumulates costs, so it needs a
   * `Monoid` as well as an `Ordering`.
   */
  def bellmanFordCustom[V, E](
                                     edges: JavaList[WeightedEdge[V, E]],
                                     start: V,
                                     combineFn: JavaBinaryOperator[E],
                                     zero: E,
                                     comparator: JavaComparator[E]
                             ): JavaOptional[JavaMap[V, WeightedEdge[V, E]]] =
    given Ordering[E] = Ordering.comparatorToOrdering(using comparator)

    given Monoid[E] with
      def empty: E = zero

      def combine(x: E, y: E): E = combineFn.apply(x, y)
    optionalEdgeResult(com.phasmidsoftware.gryphon.traverse.BellmanFord.shortestPaths[V, E](materialiseWeighted(edges), start))

  // ---------------------------------------------------------------------------
  // Acyclic shortest paths — Option 1: Double weights
  // ---------------------------------------------------------------------------

  /**
   * Shortest paths in a DAG, in topological order: linear time, and negative
   * weights are allowed because a DAG has no cycles.
   */
  def acyclicShortestPathsDouble[V](
                                           edges: JavaList[WeightedEdge[V, Double]],
                                           start: V
                                   ): JavaMap[V, WeightedEdge[V, Double]] =
    given Ordering[Double] = scala.math.Ordering.Double.TotalOrdering

    edgeResultToJavaMap(com.phasmidsoftware.gryphon.traverse.AcyclicShortestPaths.shortestPaths[V, Double](materialiseWeighted(edges), start))

  // ---------------------------------------------------------------------------
  // Acyclic shortest paths — Option 3: custom type
  // ---------------------------------------------------------------------------

  def acyclicShortestPathsCustom[V, E](
                                              edges: JavaList[WeightedEdge[V, E]],
                                              start: V,
                                              combineFn: JavaBinaryOperator[E],
                                              zero: E,
                                              comparator: JavaComparator[E]
                                      ): JavaMap[V, WeightedEdge[V, E]] =
    given Ordering[E] = Ordering.comparatorToOrdering(using comparator)

    given Monoid[E] with
      def empty: E = zero

      def combine(x: E, y: E): E = combineFn.apply(x, y)
    edgeResultToJavaMap(com.phasmidsoftware.gryphon.traverse.AcyclicShortestPaths.shortestPaths[V, E](materialiseWeighted(edges), start))

  // ---------------------------------------------------------------------------
  // Graph properties
  // ---------------------------------------------------------------------------

  /** The number of vertices, N in Sedgewick's notation. */
  def order[V](scalaGraph: AbstractGraph[V]): Int = scalaGraph.order

  /**
   * The number of edges, M in Sedgewick's notation.
   *
   * Needs an edge graph: from 1.9.0 the count lives on `EdgeTraversable`, because counting edges
   * requires knowing what an edge is. In Scala that is a compile-time matter; here, as everywhere
   * in the façade, it is a `GraphException`.
   */
  def size[V](scalaGraph: AbstractGraph[V]): Int =
    asEdgeGraph(scalaGraph, "size").size

  def degree[V](scalaGraph: AbstractGraph[V], v: V): Int =
    asUndirected(scalaGraph, "degree").degree(v)

  def maxDegree[V](scalaGraph: AbstractGraph[V]): Int =
    asUndirected(scalaGraph, "maxDegree").maxDegree

  def meanDegree[V](scalaGraph: AbstractGraph[V]): Double =
    asUndirected(scalaGraph, "meanDegree").meanDegree

  def numberOfSelfLoops[V](scalaGraph: AbstractGraph[V]): Int =
    asEdgeGraph(scalaGraph, "numberOfSelfLoops").numberOfSelfLoops

  /** Defined for both kinds of graph, unlike `degree`. */
  def inDegree[V](scalaGraph: AbstractGraph[V], v: V): Int =
    asEdgeGraph(scalaGraph, "inDegree").inDegree(v)

  /** Defined for both kinds of graph, unlike `degree`. */
  def outDegree[V](scalaGraph: AbstractGraph[V], v: V): Int =
    asEdgeGraph(scalaGraph, "outDegree").outDegree(v)

  private def asEdgeGraph[V](scalaGraph: AbstractGraph[V], method: String): EdgeGraph[V, ?] =
    scalaGraph match
      case eg: EdgeGraph[V, ?] @unchecked => eg
      case _ => throw GraphException(s"$method requires a graph with edges")

  def isCyclic[V](scalaGraph: AbstractGraph[V]): Boolean = scalaGraph.isCyclic

  /**
   * Undirected only. A Scala caller gets a compile error on a digraph; Java has no way to express
   * that, so the façade checks and throws, as it does for the degree family.
   */
  def isConnected[V](scalaGraph: AbstractGraph[V]): Boolean =
    asUndirected(scalaGraph, "isConnected").isConnected

  /** Undirected only; see `isConnected`. */
  def isBipartite[V](scalaGraph: AbstractGraph[V]): Boolean =
    asUndirected(scalaGraph, "isBipartite").isBipartite

  /**
   * Degree is defined for undirected graphs only: a vertex of a directed graph
   * has an in-degree and an out-degree, not one degree.
   */
  private def asUndirected[V](scalaGraph: AbstractGraph[V], method: String): UndirectedGraph[V, ?] =
    scalaGraph match
      case ug: UndirectedGraph[V, ?] @unchecked => ug
      case _ => throw GraphException(s"$method requires an undirected graph")

  // ---------------------------------------------------------------------------
  // DFS over every component
  // ---------------------------------------------------------------------------

  /**
   * Runs DFS from every vertex not already discovered, so the came-from map
   * covers the whole graph rather than one component. Each component's root is
   * absent from the map, as the start vertex is for a single DFS.
   */
  def dfsAll[V](scalaGraph: AbstractGraph[V]): JavaMap[V, V] =
    given Random = Random(0)

    given Evaluable[V, V] with
      def evaluate(v: V): Option[V] = Some(v)
    val visitor = JournaledVisitor.withListJournalAndCameFrom[V, V]
    cameFromToJavaMap(scalaGraph.dfsAll(visitor).asInstanceOf[JournaledVisitor[V, V, ?]])

  // ---------------------------------------------------------------------------
  // BFS stopping at a goal
  // ---------------------------------------------------------------------------

  /**
   * Runs BFS from `start`, stopping as soon as `goal` holds for a vertex.
   * The came-from map then covers only what was discovered before stopping.
   */
  def bfsUntil[V](
                         scalaGraph: AbstractGraph[V],
                         start: V,
                         goal: JavaFunction[V, java.lang.Boolean]
                 ): JavaMap[V, V] =
    given Random = Random(0)

    given Evaluable[V, V] with
      def evaluate(v: V): Option[V] = Some(v)
    val visitor = JournaledVisitor.withQueueJournalAndCameFrom[V, V]
    val result = scalaGraph.bfs(visitor)(start, v => goal.apply(v).booleanValue())
    cameFromToJavaMap(result.asInstanceOf[JournaledVisitor[V, V, ?]])

  // ---------------------------------------------------------------------------
  // ConnectedComponents
  // ---------------------------------------------------------------------------

  def connectedComponents[V](scalaGraph: AbstractGraph[V]): JavaMap[V, JavaInteger] =
    given scala.util.Random = scala.util.Random(0)

    val (_, componentMap) = ConnectedComponents.components[V, Unit](scalaGraph)
    val javaMap = new JavaLinkedHashMap[V, JavaInteger]()
    componentMap.foreach { case (v, id) => javaMap.put(v, id) }
    JavaCollections.unmodifiableMap(javaMap)

  // ---------------------------------------------------------------------------
  // TopologicalSort
  // ---------------------------------------------------------------------------

  def topologicalSort[V](scalaGraph: AbstractGraph[V]): JavaOptional[JavaList[V]] =
    scalaGraph match
      case dg: com.phasmidsoftware.gryphon.adjunct.DirectedGraph[V, ?] @unchecked =>
        ScalaTopSort.sort(dg) match
          case Some(order) =>
            val javaList = new java.util.ArrayList[V]()
            order.foreach(javaList.add)
            JavaOptional.of(JavaCollections.unmodifiableList(javaList))
          case None =>
            JavaOptional.empty()
      case _ =>
        throw com.phasmidsoftware.gryphon.core.GraphException("topologicalSort requires a DirectedGraph")

  // ---------------------------------------------------------------------------
  // Kosaraju
  // ---------------------------------------------------------------------------

  def kosaraju[V](
                         @unused scalaGraph: AbstractGraph[V],
                         edges: JavaList[Edge[V]]
                 ): JavaMap[V, JavaInteger] =
    given Random = Random(42)

    val directedGraph: DirectedGraph[V, Unit] =
      scalaGraph match
        case dg: DirectedGraph[V, Unit] @unchecked => dg
        case _ => materialise(edges, JavaCollections.emptySet[V](), directed = true)
                .asInstanceOf[DirectedGraph[V, Unit]]
    val result = com.phasmidsoftware.gryphon.traverse.Kosaraju
            .stronglyConnectedComponents(directedGraph)
    val javaMap = new JavaLinkedHashMap[V, JavaInteger]()
    result.foreach { case (v, id) => javaMap.put(v, id) }
    JavaCollections.unmodifiableMap(javaMap)

  // ---------------------------------------------------------------------------
  // Result conversion — came-from map (BFS/DFS)
  // ---------------------------------------------------------------------------

  private def cameFromToJavaMap[V](
                                          result: JournaledVisitor[V, V, ?]
                                  ): JavaMap[V, V] =
    val javaMap = new JavaLinkedHashMap[V, V]()
    result.cameFrom.foreach { map =>
      map.foreach { case (v, from) => javaMap.put(v, from) }
    }
    JavaCollections.unmodifiableMap(javaMap)

  // ---------------------------------------------------------------------------
  // Result conversion — any vertex-keyed map of edges
  // ---------------------------------------------------------------------------

  /**
   * Converts a vertex-keyed traversal result into a Java map of `WeightedEdge`.
   * Serves the SPT (Dijkstra, Bellman-Ford, acyclic) and the MST map (Prim).
   * `TraversalResult` is invariant in its second parameter, hence the type
   * parameter `X` rather than plain `Edge[V, E]`.
   */
  private def edgeResultToJavaMap[V, E, X <: com.phasmidsoftware.gryphon.core.Edge[V, E]](
                                                                                                 result: com.phasmidsoftware.gryphon.traverse.TraversalResult[V, X]
                                                                                         ): JavaMap[V, WeightedEdge[V, E]] =
    val javaMap = new JavaLinkedHashMap[V, WeightedEdge[V, E]]()
    result.keySet.foreach { v =>
      result.vertexTraverse(v).foreach { e =>
        javaMap.put(v, new WeightedEdge[V, E](e.white, e.black, e.attribute))
      }
    }
    JavaCollections.unmodifiableMap(javaMap)

  /** As `edgeResultToJavaMap`, for an algorithm that may report failure. */
  private def optionalEdgeResult[V, E, X <: com.phasmidsoftware.gryphon.core.Edge[V, E]](
                                                                                                result: Option[com.phasmidsoftware.gryphon.traverse.TraversalResult[V, X]]
                                                                                        ): JavaOptional[JavaMap[V, WeightedEdge[V, E]]] =
    result match
      case Some(r) => JavaOptional.of(edgeResultToJavaMap(r))
      case None => JavaOptional.empty()

  // ---------------------------------------------------------------------------
  // Result conversion — MST list (Kruskal)
  // ---------------------------------------------------------------------------

  private def mstSeqToJavaList[V, E](
                                            result: Seq[com.phasmidsoftware.gryphon.core.Edge[V, E]]
                                    ): JavaList[WeightedEdge[V, E]] =
    val javaList = new java.util.ArrayList[WeightedEdge[V, E]]()
    result.foreach { e =>
      javaList.add(new WeightedEdge[V, E](e.white, e.black, e.attribute))
    }
    JavaCollections.unmodifiableList(javaList)
