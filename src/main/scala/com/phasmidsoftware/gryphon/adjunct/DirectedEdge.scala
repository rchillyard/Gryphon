package com.phasmidsoftware.gryphon.adjunct

import com.phasmidsoftware.gryphon.core.{Edge, Vertex}

/**
 * Represents a directed edge in a graph structure, connecting a starting vertex (`from`)
 * to an ending vertex (`to`) while carrying an associated attribute.
 *
 * CONSIDER do we really need this class since it is exactly like Edge.
 *
 * @tparam E the type of the attribute associated with the edge.
 * @tparam V the type of the vertex attributes connected by the edge.
 * @param attribute a value representing additional information or weight associated with the edge.
 * @param from      the vertex where the directed edge originates.
 * @param to        the vertex where the directed edge terminates.
 */
case class DirectedEdge[E, V](attribute: E, from: Vertex[V], to: Vertex[V]) extends Edge[E, V]