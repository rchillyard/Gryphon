/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.core

import com.phasmidsoftware.gryphon.adjunct.{DirectedEdge, DirectedGraph}
import com.phasmidsoftware.gryphon.traverse.{AcyclicShortestPaths, VertexTraversalResult}
import com.phasmidsoftware.visitor.core.Monoid

/**
 * Trait to model the behavior of directed acyclic graph.
 *
 * @tparam V the (key) vertex-attribute type.
 * @tparam E the edge-attribute type.
 */
trait DAG[V, E: {Monoid, Ordering}] extends DirectedGraph[V, E] {
  def shortestPaths(start: V): VertexTraversalResult[V, DirectedEdge[E, V]] =
    AcyclicShortestPaths.shortestPaths(this, start)
}
