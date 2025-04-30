package com.phasmidsoftware.gryphon.core

/**
 * Defines an abstraction for an edge in a graph structure that connects two vertices.
 * An `Edge` may be directed or undirected, and it carries an attribute of type `E`.
 *
 * The connection is established between two vertices of type `V`, referred to as `from` and `to`.
 * The directionality of the edge, as well as whether it allows traversal in one or both directions,
 * is represented by the `oneWay` method.
 *
 * @tparam E the type of the attribute associated with the edge.
 * @tparam V the type of the vertices connected by the edge.
 */
trait Edge[E, V] extends Attribute[E] {
  /**
   * Retrieves the starting `Vertex` of this `Edge`.
   * If this is an `UndirectedEdge`, from and to represent the nominal direction.
   * Individual adjacencies can be based on a flipped version of an `UndirectedEdge`.
   *
   * @return the vertex where the edge originates (the "from" vertex).
   */
  def from: V

  /**
   * Retrieves the ending vertex of the edge.
   * See NOTE above for `from`.
   *
   * @return the vertex where the edge terminates (the "to" vertex).
   */
  def to: V

  /**
   * Determines if this edge can only be traversed in one direction.
   *
   * @return true if the edge is one-way, meaning it can only be traversed from the `from` vertex to the `to` vertex;
   *         false if the edge can be traversed in both directions.
   */
  def oneWay: Boolean
}
