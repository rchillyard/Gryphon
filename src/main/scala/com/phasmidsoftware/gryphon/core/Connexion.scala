package com.phasmidsoftware.gryphon.core

/**
 * A trait representing a connection between two vertices in a graph structure.
 * This trait is designed to generalize the concept of a connexion between exactly two vertices
 * in a graph.
 * The vertices are called `white` and `black` in order to avoid any suggestion of an order.
 * NOTE: if you're curious why I use the archaic spelling "Connexion",
 * see https://en.wikipedia.org/wiki/Connexion_(graph_theory)
 *
 * @tparam V the type representing the vertices connected by this edge.
 *           It could be any user-defined type that models a vertex.
 */
trait Connexion[V] {
  /**
   * Retrieves the `white` `Vertex` of this `Connexion`.
   * If this is a `DirectedEdge`, this vertex represents the start.
   * If this is an `UndirectedEdge`, white to black represents the nominal direction.
   * Individual adjacencies can be based on a flipped version of an `UndirectedEdge`.
   *
   * @return the "white" vertex.
   */
  def white: V

  /**
   * Retrieves the `black` `Vertex` of this `Connexion`.
   * If this is a `DirectedEdge`, this vertex represents the end.
   * If this is an `UndirectedEdge`, see the note above for `white`.
   *
    * @return the "black" vertex.
   */
  def black: V
}

/**
 * A case class representing a specific instance of a `Connexion` between two vertices in a graph.
 *
 * This class extends the `Connexion` trait, providing a concrete representation of a connexion
 * between two vertices, identified as `white` and `black`. These terms are used to distinguish
 * the two vertices in an abstract way, without implying any ordering or directional meaning.
 *
 * @tparam V the type of the vertices forming the connection. This parameter allows flexibility
 *           to support any user-defined type representing a vertex.
 * @param white the first vertex in the connection, referred to as `white`.
 * @param black the second vertex in the connection, referred to as `black`.
 */
case class VertexPair[V](white: V, black: V) extends Connexion[V]
