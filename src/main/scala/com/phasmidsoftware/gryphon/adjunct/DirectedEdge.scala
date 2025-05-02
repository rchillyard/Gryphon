package com.phasmidsoftware.gryphon.adjunct

import com.phasmidsoftware.gryphon.core.{Directed, Edge, EdgeType}

/**
 * Represents a directed edge in a graph structure, connecting a starting vertex (`from`)
 * to an ending vertex (`to`) while carrying an associated attribute.
 *
 * @tparam E the type of the attribute associated with the edge.
 * @tparam V the type of the vertex attributes connected by the edge.
 * @param attribute a value representing additional information or weight associated with the edge.
 * @param from      the vertex where the directed edge originates.
 * @param to        the vertex where the directed edge terminates.
 */
case class DirectedEdge[E, V](attribute: E, from: V, to: V) extends Edge[E, V]:

  /**
   * Returns the type of the edge represented by this object.
   *
   * @return the edge type, which is always `Directed` for this implementation.
   */
  def edgeType: EdgeType = Directed
  
