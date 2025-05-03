package com.phasmidsoftware.gryphon.core

/**
 * A trait representing a connection between two vertices in a graph structure.
 * This trait is designed to generalize the concept of an edge in a graph, where
 * each edge connects exactly two vertices - a starting vertex (`v1`) and
 * an ending vertex (`v2`).
 * NOTE: if you're curious why I use the archaic spelling "Connexion",
 * see https://en.wikipedia.org/wiki/Connexion_(graph_theory)        
 *
 * @tparam V the type representing the vertices connected by this edge. 
 *           It could be any user-defined type that models a vertex.
 */
trait Connexion[V] {
  /**
   * Retrieves the starting `Vertex` of this `Edge`.
   * If this is an `UndirectedEdge`, v1 and v2 represent the nominal direction.
   * Individual adjacencies can be based on a flipped version of an `UndirectedEdge`.
   *
   * @return the vertex where the edge originates (the "v1" vertex).
   */
  def v1: V

  /**
   * Retrieves the ending vertex of the edge.
   * See NOTE above for `v1`.
   *
   * @return the vertex where the edge terminates (the "v2" vertex).
   */
  def v2: V
}

case class VertexPair[V](v1: V, v2: V) extends Connexion[V]
