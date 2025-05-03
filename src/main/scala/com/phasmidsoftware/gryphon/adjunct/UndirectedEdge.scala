package com.phasmidsoftware.gryphon.adjunct

import com.phasmidsoftware.gryphon.core.{Edge, EdgeType, Undirected}

/**
 * Represents an undirected edge in a graph structure, connecting two vertices (`v1` and `v2`) equally,
 * without a defined direction, and carrying an associated attribute.
 *
 * @tparam E the type of the attribute associated with the edge.
 * @tparam V the type of the vertex attributes connected by the edge.
 * @param attribute a value representing additional information or weight associated with the edge.
 * @param v1        one of the vertices connected by the undirected edge.
 * @param v2        the other vertex connected by the undirected edge.
 */
case class UndirectedEdge[E, V](attribute: E, v1: V, v2: V) extends Edge[E, V]:

  /**
   * Returns the type of the edge as an instance of `EdgeType`.
   *
   * @return the edge type, which is `Undirected` for this implementation.
   */
  def edgeType: EdgeType = Undirected
