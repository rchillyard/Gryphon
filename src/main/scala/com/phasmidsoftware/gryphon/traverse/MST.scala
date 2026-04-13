package com.phasmidsoftware.gryphon.traverse

import com.phasmidsoftware.gryphon.core.{Edge, Traversable}
import com.phasmidsoftware.visitor.core.Zero
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
   * @param traversable the weighted undirected graph.
   * @param start       the source vertex.
   * @param random      controls adjacency ordering.
   * @tparam V the vertex type.
   * @tparam E the edge-weight type; must be Monoid and Ordering.
   * @return a `TraversalResult[V, Edge[V, E]]` mapping each MST vertex to its
   *         cheapest incoming edge.
   */
  def prim[V, E: {Zero, Ordering}](traversable: Traversable[V], start: V)(using random: Random = Random()): TraversalResult[V, Edge[V, E]] =
    PrimTraversal[V, E]().run(traversable)(start)