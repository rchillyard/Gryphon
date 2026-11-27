package com.phasmidsoftware.gryphon.traverse

import cats.kernel.Monoid
import com.phasmidsoftware.gryphon.adjunct.{AttributedDirectedEdge, DirectedGraph}
import scala.util.Random

/**
 * Computes shortest paths in a weighted directed graph using Dijkstra's algorithm.
 * Delegates to `DijkstraTraversal` from the `GraphTraversal` family.
 */
object ShortestPaths:

  /**
   * Runs Dijkstra's algorithm from `start`, returning the shortest-path tree.
   *
   * @param graph  the weighted directed graph.
   * @param start  the source vertex.
   * @param random controls adjacency ordering.
   * @tparam V the vertex type.
   * @tparam E the edge-weight type; must be Monoid and Ordering.
   * @return a `TraversalResult[V, AttributedDirectedEdge[V, E]]` mapping each settled
   *         vertex to its cheapest incoming edge, or None for the start vertex.
   * @note Result: varies with adjacency order; pin a seed with `given Random` to fix it.
   *       The distances do not vary; which of several equal-cost paths is reported does.
   */
  def dijkstra[V, E: {Monoid, Ordering}](graph: DirectedGraph[V, E], start: V)(using random: Random = Random()): TraversalResult[V, AttributedDirectedEdge[V, E]] =
    DijkstraTraversal[V, E]().run(graph)(start)

// undiscoveredEdges and undiscoveredVertices were here, duplicating the methods of the same
// names on Graph with the receiver moved into a parameter. Nothing called them: not src/main,
// not the specs — ShortestPathsSpec exercises Graph's pair — and not the Java façade.
//
// They were published in 1.10.0 and are in the 1.11.0 jars, so removing them is a binary break
// against both: the release carrying this must be 1.12.0, not 1.11.1. The four filters are in
// src/main/mima-filters/1.10.0.backwards.excludes, in a section marked as not belonging to
// 1.11.0. See #106, which also asks whether Graph's surviving pair should be renamed, since
// neither has anything to do with what has been discovered: `filteredAdjacencies(_ => true)`
// filters nothing.