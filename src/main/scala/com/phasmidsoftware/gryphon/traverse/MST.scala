package com.phasmidsoftware.gryphon.traverse

import cats.kernel.Monoid
import com.phasmidsoftware.gryphon.adjunct.UndirectedGraph
import com.phasmidsoftware.gryphon.core.Edge
import scala.util.Random

/**
 * Computes minimum spanning trees using Prim's algorithm.
 * Delegates to `PrimTraversal` from the `GraphTraversal` family.
 */
object MST:

  /**
   * Runs Prim's algorithm from `start`, returning the MST as a traversal result.
   *
   * Each entry `v → edge` in the result records the cheapest edge connecting
   * `v` to the growing MST. The start vertex is absent (it has no predecessor).
   *
   * @param graph  the weighted undirected graph.
   * @param start  the source vertex.
   * @param random controls adjacency ordering (implicit).
   * @tparam V the vertex type.
   * @tparam E the edge-weight type; must be Monoid and Ordering.
   * @return a `TraversalResult[V, Edge[V, E]]` mapping each MST vertex to its
   *         cheapest incoming edge.
   * @note Result: varies with adjacency order; pin a seed with `given Random` to fix it.
   *       The total weight does not vary -- a minimum spanning tree is minimum -- but which of
   *       several equal-weight trees comes back does.
   */
  def prim[V, E: {Monoid, Ordering}](graph: UndirectedGraph[V, E], start: V)(using random: Random = Random()): TraversalResult[V, Edge[V, E]] =
    PrimTraversal[V, E]().run(graph)(start)