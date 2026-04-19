package com.phasmidsoftware.gryphon.traverse

import com.phasmidsoftware.gryphon.adjunct.AttributedDirectedEdge
import com.phasmidsoftware.gryphon.core
import com.phasmidsoftware.gryphon.core.{Edge, Traversable}
import com.phasmidsoftware.visitor.core.Monoid
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
   * @tparam E the edge-weight type; must be Monoid and Ordering.
   * @return a `TraversalResult[V, DirectedEdge[V, E]]` mapping each settled
   *         vertex to its cheapest incoming edge, or None for the start vertex.
   */
  def dijkstra[V, E: {Monoid, Ordering}](traversable: Traversable[V], start: V)(using random: Random = Random()): TraversalResult[V, AttributedDirectedEdge[V, E]] =
    DijkstraTraversal[V, E]().run(traversable)(start)

  /**
   * Returns the directed edges reachable from v.
   */
  def undiscoveredEdges[V, E](traversable: core.Traversable[V])(v: V)(using random: Random = Random()): Seq[Edge[V, E]] =
    traversable.filteredAdjacencies(_ => true)(v)
            .flatMap(_.maybeEdge[E])
            .toSeq

  /**
   * Returns the vertices reachable via attributed directed edges from v.
   */
  def undiscoveredVertices[V, E](traversable: core.Traversable[V])(v: V)(using random: Random = Random()): Seq[V] =
    undiscoveredEdges(traversable)(v)
            .collect { case e: AttributedDirectedEdge[V, E] @unchecked => e.black }