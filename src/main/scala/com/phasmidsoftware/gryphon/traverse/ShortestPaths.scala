package com.phasmidsoftware.gryphon.traverse

import cats.kernel.Monoid
import com.phasmidsoftware.gryphon.adjunct.{AttributedDirectedEdge, DirectedGraph}
import com.phasmidsoftware.gryphon.core
import com.phasmidsoftware.gryphon.core.{Edge, Traversable}
import scala.util.Random

/**
 * Computes shortest paths in a weighted directed graph using Dijkstra's algorithm.
 * Delegates to `DijkstraTraversal` from the `GraphTraversal` family.
 */
object ShortestPaths:

  /**
   * Runs Dijkstra's algorithm from `start`, returning the shortest-path tree.
   *
   * @param graph       the weighted directed graph.
   * @param start       the source vertex.
   * @param random      controls adjacency ordering.
   * @tparam V the vertex type.
   * @tparam E the edge-weight type; must be Monoid and Ordering.
   * @return a `TraversalResult[V, AttributedDirectedEdge[V, E]]` mapping each settled
   *         vertex to its cheapest incoming edge, or None for the start vertex.
   */
  def dijkstra[V, E: {Monoid, Ordering}](graph: DirectedGraph[V, E], start: V)(using random: Random = Random()): TraversalResult[V, AttributedDirectedEdge[V, E]] =
    DijkstraTraversal[V, E]().run(graph)(start)

  /**
   * Returns all outgoing edges discoverable from the specified vertex.
   *
   * @param traversable the graph traversal source used to obtain adjacencies.
   * @param v           the vertex whose adjacent edges are returned.
   * @param random      controls adjacency ordering when the traversal source uses randomized ordering.
   * @tparam V the vertex type.
   * @tparam E the edge attribute or weight type.
   * @return a sequence of edges associated with the adjacencies of the specified vertex.
   */
  def undiscoveredEdges[V, E](traversable: core.Traversable[V])(v: V)(using random: Random = Random()): Seq[Edge[V, E]] =
    traversable.filteredAdjacencies(_ => true)(v)
            .flatMap(_.maybeEdge[E])
            .toSeq

  /**
   * Returns the vertices reachable by outgoing attributed directed edges from the specified vertex.
   *
   * @param traversable the graph traversal source used to obtain adjacencies.
   * @param v           the vertex whose outgoing adjacent vertices are returned.
   * @param random      controls adjacency ordering when the traversal source uses randomized ordering.
   * @tparam V the vertex type.
   * @tparam E the edge attribute or weight type.
   * @return a sequence containing the target vertices of undiscovered attributed directed edges from the specified vertex.
   */
  def undiscoveredVertices[V, E](traversable: core.Traversable[V])(v: V)(using random: Random = Random()): Seq[V] =
    undiscoveredEdges(traversable)(v)
            .collect { case e: AttributedDirectedEdge[V, E] @unchecked => e.black }