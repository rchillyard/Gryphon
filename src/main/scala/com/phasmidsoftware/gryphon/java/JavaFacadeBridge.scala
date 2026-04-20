package com.phasmidsoftware.gryphon.java

import com.phasmidsoftware.gryphon.adjunct.{AttributedDirectedEdge, DirectedGraph, UndirectedEdge, UndirectedGraph}
import com.phasmidsoftware.gryphon.builder.GraphBuilder
import com.phasmidsoftware.gryphon.core.{AbstractGraph, VertexMap}
import com.phasmidsoftware.gryphon.parse.Parseable
import com.phasmidsoftware.gryphon.traverse.{ConnectedComponents, Kruskal, MST, ShortestPaths, TopologicalSort as ScalaTopSort}
import com.phasmidsoftware.visitor.core.{*, given}
import java.util.List as JavaList
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
  def bfs[V](scalaGraph: AbstractGraph[V], start: V): java.util.Map[V, V] =
    given Random = Random(0)

    given Evaluable[V, V] with
      def evaluate(v: V): Option[V] = Some(v)

    given GraphNeighbours[V] = scalaGraph.graphNeighbours

    val visitor = JournaledVisitor.withQueueJournalAndCameFrom[V, V]
    val result = Traversal.bfs(start, visitor)
            .asInstanceOf[JournaledVisitor[V, V, ?]]
    cameFromToJavaMap(result)

  // ---------------------------------------------------------------------------
  // BFS — Option 3: custom neighbour function
  // ---------------------------------------------------------------------------

  /**
   * Runs BFS from `start` using a caller-supplied neighbour function,
   * wrapped into a Scala `Neighbours[V, V]` given instance.
   * The start vertex is absent from the returned came-from map.
   */
  def bfsWithNeighbours[V](
                                  scalaGraph: AbstractGraph[V],
                                  start: V,
                                  neighboursFn: java.util.function.Function[V, java.lang.Iterable[V]]
                          ): java.util.Map[V, V] =

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
  def dfs[V](scalaGraph: AbstractGraph[V], start: V): java.util.Map[V, V] =
    given Random = Random(0)

    given Evaluable[V, V] with
      def evaluate(v: V): Option[V] = Some(v)

    given GraphNeighbours[V] = scalaGraph.graphNeighbours

    val visitor = JournaledVisitor.withListJournalAndCameFrom[V, V]
    val result = Traversal.dfs(start, visitor)
            .asInstanceOf[JournaledVisitor[V, V, ?]]
    cameFromToJavaMap(result)

  // ---------------------------------------------------------------------------
  // DFS — Option 3: custom neighbour function
  // ---------------------------------------------------------------------------

  /**
   * Runs DFS from `start` using a caller-supplied neighbour function,
   * wrapped into a Scala `Neighbours[V, V]` given instance.
   * The start vertex is absent from the returned came-from map.
   */
  def dfsWithNeighbours[V](
                                  scalaGraph: AbstractGraph[V],
                                  start: V,
                                  neighboursFn: java.util.function.Function[V, java.lang.Iterable[V]]
                          ): java.util.Map[V, V] =
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
                                                        vertexParser: java.util.function.Function[String, V],
                                                        edgeParser: java.util.function.Function[String, E]
                                                ): (UndirectedGraph[V, E], JavaList[WeightedEdge[V, E]]) =
    given Parseable[V] = asParseable(vertexParser, """\w+""")

    given Parseable[E] = asParseable(edgeParser, """(\d+(\.\d*)?|\d*\.\d+)""")
    val g = GraphBuilder.undirected[V, E].fromResource(resourceName).get
    (g, extractWeightedEdges(g))

  def directedWeightedFromResourceCustom[V, E](
                                                      resourceName: String,
                                                      vertexParser: java.util.function.Function[String, V],
                                                      edgeParser: java.util.function.Function[String, E]
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
    java.util.Collections.unmodifiableList(list)

  /**
   * Wraps a Java `Function<String, T>` into a `Parseable[T]`.
   * The regex must match exactly the tokens that `GraphParser` will see —
   * use `\\w+` for string vertex keys and the double regex for numeric weights.
   * Passing `.*` or `\\S+` causes comment lines starting with `//` to be
   * parsed as vertex tokens rather than filtered out.
   */
  private def asParseable[T](f: java.util.function.Function[String, T],
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
   */
  def materialise[V](edges: JavaList[Edge[V]], directed: Boolean): AbstractGraph[V] =
    if directed then
      val vm = edges.asScala.foldLeft(VertexMap[V]) { (vm, e) =>
        vm + AttributedDirectedEdge(None, e.from, e.to)
      }
      DirectedGraph[V, Unit](vm)
    else
      edges.asScala.foldLeft(UndirectedGraph[V, Unit](VertexMap[V])) { (g, e) =>
        g.addEdge(UndirectedEdge((), e.from, e.to))
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
                               scalaGraph: AbstractGraph[V],
                               edges: JavaList[WeightedEdge[V, Double]],
                               start: V
                       ): java.util.Map[V, WeightedEdge[V, Double]] =
    given Random = Random(0)
    given Ordering[Double] = scala.math.Ordering.Double.TotalOrdering

    val weightedGraph = materialiseWeighted[V, Double](edges)
    val result = ShortestPaths.dijkstra[V, Double](weightedGraph, start)
    sptToJavaMap(result)

  // ---------------------------------------------------------------------------
  // Dijkstra — Option 3: custom type, combine + zero + comparator
  // ---------------------------------------------------------------------------

  /**
   * Dijkstra requires both a `Monoid[E]` (combine + identity) for path cost
   * accumulation and an `Ordering[E]` for priority comparison.
   * Unlike Prim/Kruskal, Dijkstra cannot work with Ordering alone.
   */
  def dijkstraCustom[V, E](
                                  scalaGraph: AbstractGraph[V],
                                  edges: JavaList[WeightedEdge[V, E]],
                                  start: V,
                                  combineFn: java.util.function.BinaryOperator[E],
                                  zero: E,
                                  comparator: java.util.Comparator[E]
                          ): java.util.Map[V, WeightedEdge[V, E]] =
    given Random = Random(0)

    given Ordering[E] = Ordering.comparatorToOrdering(using comparator)

    given Monoid[E] with
      def identity: E = zero

      def combine(x: E, y: E): E = combineFn.apply(x, y)
    val weightedGraph = materialiseWeighted[V, E](edges)
    val result = ShortestPaths.dijkstra[V, E](weightedGraph, start)
    sptToJavaMap(result)

  // ---------------------------------------------------------------------------
  // Prim — Option 1: Double weights
  // ---------------------------------------------------------------------------

  def primDouble[V](
                           scalaGraph: AbstractGraph[V],
                           edges: JavaList[WeightedEdge[V, Double]],
                           start: V
                   ): java.util.Map[V, WeightedEdge[V, Double]] =
    given Random = Random(0)

    given Ordering[Double] = scala.math.Ordering.Double.TotalOrdering
    given Zero[Double] = summon[Zero[Double]]
    val weightedGraph = materialiseWeightedUndirected[V, Double](edges)
    val result = MST.prim[V, Double](weightedGraph, start)
    mstToJavaMap(result)

  // ---------------------------------------------------------------------------
  // Prim — Option 3: custom type, Comparator only
  // ---------------------------------------------------------------------------

  /**
   * Prim uses edge weight for priority comparison only — costs are never
   * accumulated. Only `Ordering[E]` is needed; the `Monoid.combine` stub
   * returns `identity` and is never called.
   */
  def primCustom[V, E](
                              scalaGraph: AbstractGraph[V],
                              edges: JavaList[WeightedEdge[V, E]],
                              start: V,
                              comparator: java.util.Comparator[E]
                      ): java.util.Map[V, WeightedEdge[V, E]] =
    given Random = Random(0)
    given Ordering[E] = Ordering.comparatorToOrdering(using comparator)

    given Zero[E] with
      def identity: E = edges.get(0).attribute
    val weightedGraph = materialiseWeightedUndirected[V, E](edges)
    val result = MST.prim[V, E](weightedGraph, start)
    mstToJavaMap(result)

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
                                 comparator: java.util.Comparator[E]
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
    given Monoid[Double] with
      def identity: Double = 0.0
      def combine(x: Double, y: Double): Double = x + y
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
                                 comparator: java.util.Comparator[E]
                         ): JavaList[WeightedEdge[V, E]] =
    given Ordering[E] = Ordering.comparatorToOrdering(using comparator)
    given Monoid[E] with
      def identity: E = edges.get(0).attribute
      def combine(x: E, y: E): E = identity
    val weightedGraph = materialiseWeightedUndirected[V, E](edges)
    val result = com.phasmidsoftware.gryphon.traverse.Boruvka.mst(weightedGraph)
    mstSeqToJavaList(result)

  // ---------------------------------------------------------------------------
  // ConnectedComponents
  // ---------------------------------------------------------------------------

  def connectedComponents[V](scalaGraph: AbstractGraph[V]): java.util.Map[V, java.lang.Integer] =
    given scala.util.Random = scala.util.Random(0)

    val (_, componentMap) = ConnectedComponents.components[V, Unit](scalaGraph)
    val javaMap = new java.util.LinkedHashMap[V, java.lang.Integer]()
    componentMap.foreach { case (v, id) => javaMap.put(v, id) }
    java.util.Collections.unmodifiableMap(javaMap)

  // ---------------------------------------------------------------------------
  // TopologicalSort
  // ---------------------------------------------------------------------------

  def topologicalSort[V](scalaGraph: AbstractGraph[V]): java.util.Optional[JavaList[V]] =
    scalaGraph match
      case dg: com.phasmidsoftware.gryphon.adjunct.DirectedGraph[V, ?] @unchecked =>
        ScalaTopSort.sort(dg) match
          case Some(order) =>
            val javaList = new java.util.ArrayList[V]()
            order.foreach(javaList.add)
            java.util.Optional.of(java.util.Collections.unmodifiableList(javaList))
          case None =>
            java.util.Optional.empty()
      case _ =>
        throw com.phasmidsoftware.gryphon.util.GraphException("topologicalSort requires a DirectedGraph")

  // ---------------------------------------------------------------------------
  // Kosaraju
  // ---------------------------------------------------------------------------

  def kosaraju[V](
                         scalaGraph: AbstractGraph[V],
                         edges: JavaList[Edge[V]]
                 ): java.util.Map[V, java.lang.Integer] =
    given Random = Random(42)

    val directedGraph: DirectedGraph[V, Unit] =
      scalaGraph match
        case dg: DirectedGraph[V, Unit] @unchecked => dg
        case _ => materialise(edges, directed = true)
                .asInstanceOf[DirectedGraph[V, Unit]]
    val result = com.phasmidsoftware.gryphon.traverse.Kosaraju
            .stronglyConnectedComponents(directedGraph)
    val javaMap = new java.util.LinkedHashMap[V, java.lang.Integer]()
    result.foreach { case (v, id) => javaMap.put(v, id) }
    java.util.Collections.unmodifiableMap(javaMap)

  // ---------------------------------------------------------------------------
  // Result conversion — came-from map (BFS/DFS)
  // ---------------------------------------------------------------------------

  private def cameFromToJavaMap[V](
                                          result: JournaledVisitor[V, V, ?]
                                  ): java.util.Map[V, V] =
    val javaMap = new java.util.LinkedHashMap[V, V]()
    result.cameFrom.foreach { map =>
      map.foreach { case (v, from) => javaMap.put(v, from) }
    }
    java.util.Collections.unmodifiableMap(javaMap)

  // ---------------------------------------------------------------------------
  // Result conversion — SPT (Dijkstra)
  // ---------------------------------------------------------------------------

  private def sptToJavaMap[V, E](
                                        result: com.phasmidsoftware.gryphon.traverse.TraversalResult[V, AttributedDirectedEdge[V, E]]
                                ): java.util.Map[V, WeightedEdge[V, E]] =
    val javaMap = new java.util.LinkedHashMap[V, WeightedEdge[V, E]]()
    result.keySet.foreach { v =>
      result.vertexTraverse(v).foreach { ade =>
        javaMap.put(v, new WeightedEdge[V, E](ade.white, ade.black, ade.attribute))
      }
    }
    java.util.Collections.unmodifiableMap(javaMap)

  // ---------------------------------------------------------------------------
  // Result conversion — MST map (Prim)
  // ---------------------------------------------------------------------------

  private def mstToJavaMap[V, E](
                                        result: com.phasmidsoftware.gryphon.traverse.TraversalResult[V, com.phasmidsoftware.gryphon.core.Edge[V, E]]
                                ): java.util.Map[V, WeightedEdge[V, E]] =
    val javaMap = new java.util.LinkedHashMap[V, WeightedEdge[V, E]]()
    result.keySet.foreach { v =>
      result.vertexTraverse(v).foreach { e =>
        javaMap.put(v, new WeightedEdge[V, E](e.white, e.black, e.attribute))
      }
    }
    java.util.Collections.unmodifiableMap(javaMap)

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
    java.util.Collections.unmodifiableList(javaList)
