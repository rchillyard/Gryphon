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

object Edge {

  /**
   * Creates vertices from a given triplet representation of a graph relationship and returns updated vertices.
   * NOTE this particular implementation is for directed graphs.
   *
   * @param f       a function that transforms a vertex attribute of type `V` into a `Vertex[V]`.
   * @param f1      a function that creates an adjacency relationship of type `Adjacency[V]`
   *                from two vertices and an edge attribute of type `E`.
   * @param f2      a function that optionally creates an adjacency relationship of type `Adjacency[V]`
   *                from two vertices and an edge attribute of type `E`.
   * @param triplet a triplet representing a relationship in the graph, consisting of two vertex attributes
   *                of type `V` and an edge attribute of type `E`.
   * @return a pair of updated vertices `(Vertex[V], Vertex[V])` after applying the adjacency relationships.
   */
  def createVerticesFromTriplet[V, E](f: V => Vertex[V])(f1: (Vertex[V], Vertex[V], E) => Adjacency[V])(f2: (Vertex[V], Vertex[V], E) => Option[Adjacency[V]])(triplet: Triplet[V, E]): (Vertex[V], Vertex[V]) = {
    val vv1: Vertex[V] = f(triplet._1)
    val vv2: Vertex[V] = f(triplet._2)
    val va1: Adjacency[V] = f1(vv1, vv2, triplet._3)
    val va2: Option[Adjacency[V]] = None // XXX directed edge
    (vv1 + va1, va2.fold(vv2)(vv2 + _))
  }

}