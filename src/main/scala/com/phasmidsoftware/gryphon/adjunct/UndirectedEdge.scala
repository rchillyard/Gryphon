package com.phasmidsoftware.gryphon.adjunct

import com.phasmidsoftware.gryphon.core.Edge

/**
 * Represents an undirected edge in a graph structure, connecting two vertices (`from` and `to`) equally,
 * without a defined direction, and carrying an associated attribute.
 *
 * @tparam E the type of the attribute associated with the edge.
 * @tparam V the type of the vertex attributes connected by the edge.
 * @param attribute a value representing additional information or weight associated with the edge.
 * @param from      one of the vertices connected by the undirected edge.
 * @param to        the other vertex connected by the undirected edge.
 */
case class UndirectedEdge[E, V](attribute: E, from: V, to: V) extends Edge[E, V]:
  /**
   * Determines if this edge can only be traversed in one direction.
   *
   * @return false.
   */
  def oneWay: Boolean = false
