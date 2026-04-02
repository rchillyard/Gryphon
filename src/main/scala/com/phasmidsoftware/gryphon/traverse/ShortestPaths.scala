package com.phasmidsoftware.gryphon.traverse

import com.phasmidsoftware.gryphon.adjunct.{AttributedDirectedEdge, DirectedEdge}
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
   * @param traversable the weighted directed graph.
   * @param start       the source vertex.
   * @param random      controls adjacency ordering.
   * @tparam V the vertex type.
   * @tparam E the edge-weight type; must be Numeric and Ordering.
   * @return a `TraversalResult[V, DirectedEdge[E, V]]` mapping each settled
   *         vertex to its cheapest incoming edge, or None for the start vertex.
   */
  def dijkstra[V, E: {Numeric, Ordering}](traversable: Traversable[V], start: V)(using random: Random = Random()): TraversalResult[V, AttributedDirectedEdge[E, V]] =
    DijkstraTraversal[V, E]().run(traversable)(start)
    
  /**
   * Returns the directed edges reachable from v.
   */
  def undiscoveredEdges[V, E](traversable: core.Traversable[V])(v: V)(using random: Random = Random()): Seq[Edge[E, V]] =
    traversable.filteredAdjacencies(_ => true)(v)
            .flatMap(_.maybeEdge[E])
            .toSeq

  /**
   * Returns the vertices reachable via attributed directed edges from v.
   */
  def undiscoveredVertices[V, E](traversable: core.Traversable[V])(v: V)(using random: Random = Random()): Seq[V] =
    undiscoveredEdges(traversable)(v)
            .collect { case e: com.phasmidsoftware.gryphon.adjunct.AttributedDirectedEdge[E, V] => e.black }