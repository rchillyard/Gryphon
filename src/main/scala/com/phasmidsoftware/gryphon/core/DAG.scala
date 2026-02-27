/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.core

import com.phasmidsoftware.gryphon.adjunct.{DirectedEdge, DirectedGraph}

/**
 * Trait to model the behavior of directed acyclic graph.
 *
 * @tparam V the (key) vertex-attribute type.
 * @tparam E the edge-attribute type.
 */
trait DAG[V, E] extends DirectedGraph[V, E] {
  def isCyclic: Boolean = false // TODO we should be able to assert this

  def isBipartite: Boolean = false
}
