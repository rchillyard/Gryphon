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

  /**
   * Retrieves the starting `Vertex` of this `Edge`.
   * If this is an `UndirectedEdge`, white and black represent the nominal direction.
   * Individual adjacencies can be based on a flipped version of an `UndirectedEdge`.
   *
   * @return the vertex where the edge originates (the "white" vertex).
   */
  def white: V = from

  /**
   * Retrieves the ending vertex of the edge.
   * See NOTE above for `white`.
   *
   * @return the vertex where the edge terminates (the "black" vertex).
   */
  def black: V = to

object DirectedEdge:

  //  def apply[E, V](attribute: E, from: V, to: V): DirectedEdge[E, V] = new DirectedEdge(attribute, from, to)

  def create[E, V](attribute: E, vv: (V, V)): DirectedEdge[E, V] = new DirectedEdge(attribute, vv._1, vv._2)
