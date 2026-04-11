package com.phasmidsoftware.gryphon.java

import com.phasmidsoftware.gryphon.adjunct.{AttributedDirectedEdge, DirectedGraph, UndirectedEdge, UndirectedGraph}
import com.phasmidsoftware.gryphon.core.{AbstractGraph, VertexMap}
import com.phasmidsoftware.gryphon.traverse.{Kruskal, MST, ShortestPaths}
import com.phasmidsoftware.visitor.core.{*, given}
import scala.jdk.CollectionConverters.*
import scala.util.Random

/**
 * Internal Scala bridge used by the Java façade.
 *
 * All members are accessible from Java as static methods on `JavaFacadeBridge$`.
 * This object is package-private to `com.phasmidsoftware.gryphon.java`.
 *
 * V1.4.0 changes:
 *  - `bfs` and `dfs` bridge methods added; Java façade BFS/DFS now delegate
 *    to the Scala Visitor engine using `CameFromJournal`.
 *  - Start vertex is absent from the came-from map — consistent with all other
 *    algorithm façades (Dijkstra SPT, Prim/Kruskal MST, Kosaraju SCC).
 */
private[java] object JavaFacadeBridge:

  // ---------------------------------------------------------------------------
  // BFS — delegates to Scala Traversal engine with CameFromJournal
  // ---------------------------------------------------------------------------

  /**
   * Runs BFS from `start` on the materialised Scala graph, returning a Java
   * came-from map. The start vertex is absent — it has no predecessor.
   *
   * @param scalaGraph the materialised Scala graph.
   * @param start      the source vertex.
   * @tparam V the vertex type.
   * @return an unmodifiable Java `Map<V, V>` came-from map.
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
  // DFS — delegates to Scala Traversal engine with CameFromJournal
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
  // Materialisation — unweighted (Unit edge attribute)
  // ---------------------------------------------------------------------------

  /**
   * Builds an immutable Scala graph from the Java canonical edge list,
   * using `Unit` as the edge attribute type (for BFS/DFS).
   * Folds over `VertexMap.+[E]` directly to ensure both endpoints exist.
   */
  def materialise[V](edges: java.util.List[Edge[V]], directed: Boolean): AbstractGraph[V] =
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

  private def materialiseWeighted[V, E](edges: java.util.List[WeightedEdge[V, E]]): DirectedGraph[V, E] =
    val vm = edges.asScala.foldLeft(VertexMap[V]) { (vm, e) =>
      vm + AttributedDirectedEdge(e.attribute, e.from, e.to)
    }
    DirectedGraph[V, E](vm)

  // ---------------------------------------------------------------------------
  // Materialisation — typed weighted undirected
  // ---------------------------------------------------------------------------

  private def materialiseWeightedUndirected[V, E](edges: java.util.List[WeightedEdge[V, E]]): UndirectedGraph[V, E] =
    val vm = edges.asScala.foldLeft(VertexMap[V]) { (vm, e) =>
      vm + UndirectedEdge(e.attribute, e.from, e.to)
    }
    UndirectedGraph[V, E](vm)

  // ---------------------------------------------------------------------------
  // Dijkstra — Option 1: Double weights
  // ---------------------------------------------------------------------------

  def dijkstraDouble[V](
                               scalaGraph: AbstractGraph[V],
                               edges: java.util.List[WeightedEdge[V, Double]],
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
                                  edges: java.util.List[WeightedEdge[V, E]],
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
                           edges: java.util.List[WeightedEdge[V, Double]],
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
                              edges: java.util.List[WeightedEdge[V, E]],
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
                              edges: java.util.List[WeightedEdge[V, Double]]
                      ): java.util.List[WeightedEdge[V, Double]] =
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
                                 edges: java.util.List[WeightedEdge[V, E]],
                                 comparator: java.util.Comparator[E]
                         ): java.util.List[WeightedEdge[V, E]] =
    given Ordering[E] = Ordering.comparatorToOrdering(using comparator)

    val weightedGraph = materialiseWeightedUndirected[V, E](edges)
    val result = Kruskal.mst(weightedGraph)
    mstSeqToJavaList(result)

  // ---------------------------------------------------------------------------
  // Kosaraju
  // ---------------------------------------------------------------------------

  def kosaraju[V](
                         scalaGraph: AbstractGraph[V],
                         edges: java.util.List[Edge[V]]
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
                                    ): java.util.List[WeightedEdge[V, E]] =
    val javaList = new java.util.ArrayList[WeightedEdge[V, E]]()
    result.foreach { e =>
      javaList.add(new WeightedEdge[V, E](e.white, e.black, e.attribute))
    }
    java.util.Collections.unmodifiableList(javaList)