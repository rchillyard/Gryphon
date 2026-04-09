package com.phasmidsoftware.gryphon.java

import com.phasmidsoftware.gryphon.adjunct.{AttributedDirectedEdge, DirectedGraph, UndirectedEdge, UndirectedGraph}
import com.phasmidsoftware.gryphon.core.{AbstractGraph, VertexMap}
import com.phasmidsoftware.gryphon.traverse.{MST, ShortestPaths}
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
      val vm = edges.asScala.foldLeft(VertexMap[V]) { (vm, e) =>
        vm + AttributedDirectedEdge(None, e.from, e.to)
      }
      DirectedGraph[V, Unit](vm)
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
  // Kosaraju — SCC
  // ---------------------------------------------------------------------------

  /**
   * Runs Kosaraju's algorithm on a directed graph, returning a Java
   * `Map<V, Integer>` mapping each vertex to its SCC id.
   *
   * Edge weights are irrelevant for SCC; the unweighted `DirectedGraph[V, Unit]`
   * already materialised in `scalaGraph` is used directly, cast to
   * `DirectedGraph[V, Unit]`.
   *
   * If `scalaGraph` is null or not a `DirectedGraph`, we rematerialise from
   * `edges`. In practice `getScalaGraph()` is always called first from Java.
   *
   * @param scalaGraph the cached Scala graph (a `DirectedGraph[V, Unit]`).
   * @param edges      the Java canonical edge list (used as fallback).
   * @tparam V the vertex type.
   * @return an unmodifiable Java `Map<V, Integer>` of vertex → SCC id.
   */
  def kosaraju[V](
                         scalaGraph: com.phasmidsoftware.gryphon.core.AbstractGraph[V],
                         edges: java.util.List[Edge[V]]
                 ): java.util.Map[V, java.lang.Integer] =
    given Random = Random(42)

    val directedGraph: com.phasmidsoftware.gryphon.adjunct.DirectedGraph[V, Unit] =
      scalaGraph match
        case dg: com.phasmidsoftware.gryphon.adjunct.DirectedGraph[V, Unit] @unchecked => dg
        case _ => materialise(edges, directed = true)
                .asInstanceOf[com.phasmidsoftware.gryphon.adjunct.DirectedGraph[V, Unit]]
    val result: com.phasmidsoftware.gryphon.traverse.SCCResult[V] =
      com.phasmidsoftware.gryphon.traverse.Kosaraju.stronglyConnectedComponents(directedGraph)
    val javaMap = new java.util.LinkedHashMap[V, java.lang.Integer]()
    result.foreach { case (v, id) => javaMap.put(v, id) }
    java.util.Collections.unmodifiableMap(javaMap)

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
  // Prim — Option 1: Double weights
  // ---------------------------------------------------------------------------

  /**
   * Runs Prim's algorithm on an undirected graph with `Double` edge weights.
   *
   * @param scalaGraph present for future use.
   * @param edges      the Java canonical edge list (must all be `WeightedEdge<V, Double>`).
   * @param start      the source vertex.
   * @tparam V the vertex type.
   * @return a Java `Map<V, WeightedEdge<V, Double>>` representing the MST.
   */
  def primDouble[V](
                           scalaGraph: AbstractGraph[V],
                           edges: java.util.List[Edge[V]],
                           start: V
                   ): java.util.Map[V, WeightedEdge[V, Double]] =
    given Random = Random(0)

    given Ordering[Double] = scala.math.Ordering.Double.TotalOrdering

    val weightedGraph = materialiseWeightedUndirected[V, Double](edges)
    val result = MST.prim[V, Double](weightedGraph, start)
    mstToJavaMap(result)

  // ---------------------------------------------------------------------------
  // Prim — Option 3: custom weight, zero, comparator
  // ---------------------------------------------------------------------------

  /**
   * Runs Prim's algorithm with a caller-supplied weight function and comparator.
   *
   * @param scalaGraph unused directly; present for symmetry.
   * @param edges      the Java canonical edge list.
   * @param start      the source vertex.
   * @param weightFn   extracts weight from a Java `Edge<V>`.
   * @param zero       the identity element for the weight type.
   * @param comparator orders weights.
   * @tparam V the vertex type.
   * @tparam E the weight type.
   * @return a Java `Map<V, WeightedEdge<V, E>>` representing the MST.
   */
  def primCustom[V, E](
                              scalaGraph: AbstractGraph[V],
                              edges: java.util.List[Edge[V]],
                              start: V,
                              weightFn: java.util.function.Function[Edge[V], E],
                              zero: E,
                              comparator: java.util.Comparator[E]
                      ): java.util.Map[V, WeightedEdge[V, E]] =
    given Random = Random(0)

    given Ordering[E] = Ordering.comparatorToOrdering(using comparator)

    given Monoid[E] with
      def identity: E = zero

      def combine(x: E, y: E): E = zero // Prim doesn't use combine; zero is sufficient
    val weightedGraph: com.phasmidsoftware.gryphon.adjunct.UndirectedGraph[V, E] =
      edges.asScala.foldLeft(com.phasmidsoftware.gryphon.adjunct.UndirectedGraph[V, E](VertexMap[V])) { (g, e) =>
        g.addEdge(com.phasmidsoftware.gryphon.adjunct.UndirectedEdge(weightFn.apply(e), e.from, e.to))
      }
    val result = MST.prim[V, E](weightedGraph, start)
    mstToJavaMap(result)

  // ---------------------------------------------------------------------------
  // Kruskal — Option 1: Double weights
  // ---------------------------------------------------------------------------

  /**
   * Runs Kruskal's algorithm on an undirected graph with `Double` edge weights.
   *
   * Materialises a `UndirectedGraph[V, Double]` from the Java edge list,
   * delegates to `Kruskal.mst`, and converts the result to a Java `List`.
   *
   * @param edges the Java canonical edge list (must all be `WeightedEdge<V, Double>`).
   * @tparam V the vertex type.
   * @return a Java `List<WeightedEdge<V, Double>>` in non-decreasing weight order.
   */
  def kruskalDouble[V](
                              edges: java.util.List[Edge[V]]
                      ): java.util.List[WeightedEdge[V, Double]] =
    given Ordering[Double] = scala.math.Ordering.Double.TotalOrdering

    val weightedGraph = materialiseWeightedUndirected[V, Double](edges)
    val result = com.phasmidsoftware.gryphon.traverse.Kruskal.mst(weightedGraph)
    mstSeqToJavaList(result)

  // ---------------------------------------------------------------------------
  // Kruskal — Option 3: custom weight and comparator
  // ---------------------------------------------------------------------------

  /**
   * Runs Kruskal's algorithm with a caller-supplied weight function and comparator.
   *
   * Wraps the `Comparator[E]` into both `Ordering[E]` and a minimal `Numeric[E]`
   * (only `compare` is used by Kruskal — the arithmetic operations are never called).
   *
   * @param edges      the Java canonical edge list.
   * @param weightFn   extracts weight from a Java `Edge<V>`.
   * @param comparator orders weights.
   * @tparam V the vertex type.
   * @tparam E the weight type.
   * @return a Java `List<WeightedEdge<V, E>>` in non-decreasing weight order.
   */

  def kruskalCustom[V, E](
                                 edges: java.util.List[Edge[V]],
                                 weightFn: java.util.function.Function[Edge[V], E],
                                 comparator: java.util.Comparator[E]
                         ): java.util.List[WeightedEdge[V, E]] =
    given Ordering[E] = Ordering.comparatorToOrdering(using comparator)

    val weightedGraph: com.phasmidsoftware.gryphon.adjunct.UndirectedGraph[V, E] =
      edges.asScala.foldLeft(com.phasmidsoftware.gryphon.adjunct.UndirectedGraph[V, E](VertexMap[V])) { (g, e) =>
        g.addEdge(com.phasmidsoftware.gryphon.adjunct.UndirectedEdge(weightFn.apply(e), e.from, e.to))
      }
    val result = com.phasmidsoftware.gryphon.traverse.Kruskal.mst(weightedGraph)
    mstSeqToJavaList(result)

  // ---------------------------------------------------------------------------
  // Kruskal result conversion
  // ---------------------------------------------------------------------------

  /**
   * Converts a Scala `Seq[Edge[V, E]]` (the MST produced by Kruskal) into a
   * Java `List<WeightedEdge<V, E>>`, preserving weight-ascending order.
   *
   * @param result the Scala MST edge sequence.
   * @tparam V the vertex type.
   * @tparam E the weight type.
   * @return an unmodifiable Java list of MST edges.
   */
  private def mstSeqToJavaList[V, E](
                                            result: Seq[com.phasmidsoftware.gryphon.core.Edge[V, E]]
                                    ): java.util.List[WeightedEdge[V, E]] =
    val javaList = new java.util.ArrayList[WeightedEdge[V, E]]()
    result.foreach { e =>
      javaList.add(new WeightedEdge[V, E](e.white, e.black, e.attribute))
    }
    java.util.Collections.unmodifiableList(javaList)

  // ---------------------------------------------------------------------------
  // Materialisation — weighted undirected
  // ---------------------------------------------------------------------------

  /**
   * Builds an immutable `UndirectedGraph[V, E]` from the Java canonical edge list.
   * Uses `VertexMap.+[E]` directly to ensure both endpoints are present.
   */
  private def materialiseWeightedUndirected[V, E](edges: java.util.List[Edge[V]]): com.phasmidsoftware.gryphon.adjunct.UndirectedGraph[V, E] =
    val vm = edges.asScala.foldLeft(VertexMap[V]) { (vm, e) =>
      val we = e.asInstanceOf[WeightedEdge[V, E]]
      vm + com.phasmidsoftware.gryphon.adjunct.UndirectedEdge(we.attribute, we.from, we.to)
    }
    com.phasmidsoftware.gryphon.adjunct.UndirectedGraph[V, E](vm)

  // ---------------------------------------------------------------------------
  // MST result conversion
  // ---------------------------------------------------------------------------

  /**
   * Converts a Scala `TraversalResult[V, Edge[V, E]]` (the MST produced by Prim)
   * into a Java `Map<V, WeightedEdge<V, E>>`.
   *
   * The start vertex (for which `vertexTraverse` returns `None`) is absent.
   * `Edge.white` and `Edge.black` give the two endpoints; `Edge.attribute` the weight.
   *
   * @param result the Scala MST result.
   * @tparam V the vertex type.
   * @tparam E the weight type.
   * @return an unmodifiable Java map from vertex to its MST edge.
   */
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