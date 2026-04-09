package com.phasmidsoftware.gryphon.java

import com.phasmidsoftware.gryphon.adjunct.{AttributedDirectedEdge, DirectedGraph, UndirectedEdge, UndirectedGraph}
import com.phasmidsoftware.gryphon.core.{AbstractGraph, VertexMap}
import com.phasmidsoftware.gryphon.traverse.ShortestPaths
import com.phasmidsoftware.visitor.core.{Monoid, given}
import scala.jdk.CollectionConverters.*
import scala.util.Random

/**
 * Internal Scala bridge used by the Java façade.
 *
 * All members are accessible from Java as static methods on `JavaFacadeBridge$`.
 * This object is package-private to `com.phasmidsoftware.gryphon.java`.
 *
 * Responsibilities:
 *  - Materialise a Scala `AbstractGraph` from the Java canonical edge list.
 *  - Delegate weighted algorithms (Dijkstra, Prim) to the Scala engine with
 *    the appropriate typeclass instances supplied.
 *  - Convert Scala result types back to Java collections.
 */
private[java] object JavaFacadeBridge:

  // ---------------------------------------------------------------------------
  // Materialisation — unweighted (Unit edge attribute)
  // ---------------------------------------------------------------------------

  /**
   * Builds an immutable Scala graph from the Java canonical edge list,
   * using `Unit` as the edge attribute type (for BFS/DFS where weight is irrelevant).
   *
   * @param edges    the canonical edge list from `Graph<V>`.
   * @param directed whether to build a `DirectedGraph` or `UndirectedGraph`.
   * @tparam V the vertex type.
   * @return the materialised Scala graph.
   */
  def materialise[V](edges: java.util.List[Edge[V]], directed: Boolean): AbstractGraph[V] =
    if directed then
      edges.asScala.foldLeft(DirectedGraph[V, Unit](VertexMap[V])) { (g, e) =>
        g.addEdge(AttributedDirectedEdge(None, e.from, e.to))
      }
    else
      edges.asScala.foldLeft(UndirectedGraph[V, Unit](VertexMap[V])) { (g, e) =>
        g.addEdge(UndirectedEdge((), e.from, e.to))
      }

  // ---------------------------------------------------------------------------
  // Materialisation — weighted
  // ---------------------------------------------------------------------------

  /**
   * Builds an immutable `DirectedGraph[V, E]` from the Java canonical edge list,
   * casting each `Edge<V>` to `WeightedEdge<V, E>` to extract the attribute.
   *
   * Called only from the Dijkstra bridge methods, where the caller has already
   * asserted that all edges are `WeightedEdge` instances.
   *
   * @param edges the canonical edge list; all entries must be `WeightedEdge<V, E>`.
   * @tparam V the vertex type.
   * @tparam E the edge-attribute (weight) type.
   * @return the materialised weighted directed graph.
   */
  private def materialiseWeighted[V, E](edges: java.util.List[Edge[V]]): DirectedGraph[V, E] =
    materialiseWeighted(edges, e => e.asInstanceOf[WeightedEdge[V, E]].attribute)

  private def materialiseWeighted[V, E](
                                               edges: java.util.List[Edge[V]],
                                               weightFn: Edge[V] => E
                                       ): DirectedGraph[V, E] =
    val vm = edges.asScala.foldLeft(VertexMap[V]) { (vm, e) =>
      vm + AttributedDirectedEdge(weightFn(e), e.from, e.to)
    }
    DirectedGraph[V, E](vm)

  // ---------------------------------------------------------------------------
  // Dijkstra — Option 1: Double weights
  // ---------------------------------------------------------------------------

  /**
   * Runs Dijkstra's algorithm on a graph with `Double` edge weights.
   *
   * Supplies `given_Monoid_Double` from Visitor and `Ordering.Double.TotalOrdering`
   * from the Scala standard library.
   *
   * @param scalaGraph present for future use (e.g. vertex-set validation).
   * @param edges      the Java canonical edge list (must all be `WeightedEdge<V, Double>`).
   * @param start      the source vertex.
   * @tparam V the vertex type.
   * @return a Java `Map<V, WeightedEdge<V, Double>>` representing the SPT.
   */
  def dijkstraDouble[V](
                               scalaGraph: AbstractGraph[V],
                               edges: java.util.List[Edge[V]],
                               start: V
                       ): java.util.Map[V, WeightedEdge[V, Double]] =
    given Random = Random(0)

    given Ordering[Double] = scala.math.Ordering.Double.TotalOrdering

    val weightedGraph: DirectedGraph[V, Double] = materialiseWeighted[V, Double](edges)
    val result = ShortestPaths.dijkstra[V, Double](weightedGraph, start)
    sptToJavaMap(result)

  // ---------------------------------------------------------------------------
  // Dijkstra — Option 3: custom weight, combiner, zero
  // ---------------------------------------------------------------------------

  /**
   * Runs Dijkstra's algorithm with a caller-supplied weight function, combiner,
   * and identity element — corresponding to a `Monoid[E]` in Scala.
   *
   * @param scalaGraph unused directly; present for symmetry.
   * @param edges      the Java canonical edge list.
   * @param start      the source vertex.
   * @param weightFn   extracts weight from a Java `Edge<V>`.
   * @param combineFn  combines two weights (must be associative with `zero`).
   * @param zero       the identity element for `combineFn`.
   * @tparam V the vertex type.
   * @tparam E the weight type; must have an `Ordering`.
   * @return a Java `Map<V, WeightedEdge<V, E>>` representing the SPT.
   */
  def dijkstraCustom[V, E](
                                  scalaGraph: AbstractGraph[V],
                                  edges: java.util.List[Edge[V]],
                                  start: V,
                                  weightFn: java.util.function.Function[Edge[V], E],
                                  combineFn: java.util.function.BinaryOperator[E],
                                  zero: E,
                                  comparator: java.util.Comparator[E]   // ← added
                          ): java.util.Map[V, WeightedEdge[V, E]] =
    given Ordering[E] = Ordering.comparatorToOrdering(using comparator)

    given Random = Random(0)

    given Monoid[E] with
      def identity: E = zero

      def combine(x: E, y: E): E = combineFn.apply(x, y)
    val weightedGraph: DirectedGraph[V, E] =
      materialiseWeighted(edges, e => weightFn.apply(e))
    val result = ShortestPaths.dijkstra[V, E](weightedGraph, start)
    sptToJavaMap(result)

  // ---------------------------------------------------------------------------
  // Result conversion
  // ---------------------------------------------------------------------------

  /**
   * Converts a Scala `TraversalResult[V, AttributedDirectedEdge[V, E]]` (the SPT
   * produced by Dijkstra) into a Java `Map<V, WeightedEdge<V, E>>`.
   *
   * The start vertex (for which `vertexTraverse` returns `None`) is absent
   * from the returned map.
   *
   * `AttributedDirectedEdge.white` is the from-vertex, `.black` is the to-vertex,
   * `.attribute` is the edge weight (an `Option[E]` in the Scala type; we unwrap
   * it here since Dijkstra always produces attributed edges).
   *
   * @param result the Scala SPT.
   * @tparam V the vertex type.
   * @tparam E the weight type.
   * @return an unmodifiable Java map from vertex to its cheapest incoming SPT edge.
   */
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