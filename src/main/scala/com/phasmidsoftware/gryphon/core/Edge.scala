package com.phasmidsoftware.gryphon.core

trait Edge[E, V] extends Attribute[E] {
  /**
   * Retrieves the starting `Vertex` of this `Edge`.
   * If this is an `UndirectedEdge`, from and to represent the nominal direction.
   * Individual adjacencies can be based on a flipped version of an `UndirectedEdge`.
   *
   * @return the vertex where the edge originates (the "from" vertex).
   */
  def from: Vertex[V]

  /**
   * Retrieves the ending vertex of the edge.
   * See NOTE above for `from`.
   *
   * @return the vertex where the edge terminates (the "to" vertex).
   */
  def to: Vertex[V]
}
