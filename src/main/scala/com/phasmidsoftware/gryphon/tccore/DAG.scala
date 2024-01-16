/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.tccore

/**
 * Trait to model the behavior of directed acyclic graph.
 *
 * @tparam V the (key) vertex-attribute type.
 * @tparam E the edge-attribute type.
 * @tparam X the type of edge which connects two vertices. A sub-type of DirectedEdge[V,E].
 * @tparam P the property type (a mutable property currently only supported by the Vertex type).
 */
//trait DAG[V, E, X <: DirectedEdge[V, E], P] extends DirectedGraph[V, E, X, P] {
//  override def isCyclic: Boolean = false // TODO we should be able to assert this
//
//  override def isBipartite: Boolean = false
//}
