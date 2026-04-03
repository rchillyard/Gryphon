package com.phasmidsoftware.gryphon.adjunct

import com.phasmidsoftware.gryphon.core.{Edge, EdgeType, Undirected}

/**
 * Represents an undirected edge in a graph structure, connecting two vertices (`white` and `black`) equally,
 * without a defined direction, and carrying an associated attribute.
 *
 * @tparam E the type of the attribute associated with the edge.
 * @tparam V the type of the vertex attributes connected by the edge.
 * @param attribute a value representing additional information or weight associated with the edge.
 * @param white     one of the vertices connected by the undirected edge.
 * @param black     the other vertex connected by the undirected edge.
 */
case class UndirectedEdge[V, E](attribute: E, white: V, black: V) extends Edge[V, E]:

  /**
   * Returns the type of the edge as an instance of `EdgeType`.
   *
   * @return the edge type, which is `Undirected` for this implementation.
   */
  def edgeType: EdgeType = Undirected

  /**
   * Retrieves one of the vertices connected by the undirected edge.
   *
   * @return the vertex `white`, one of the two vertices connected by this undirected edge.
   */
  def either: V = white // CONSIDER using a random Boolean to determine which vertex to return.

  /**
   * Retrieves the vertex at the opposite end of the edge relative to the given vertex.
   *
   * @param v the vertex for which the opposite vertex is to be determined.
   * @return the vertex at the other end of the edge. If the given vertex is `white`, returns `black`; otherwise, returns `white`.
   */
  def other(v: V): V =
    if (v == white) black else white
