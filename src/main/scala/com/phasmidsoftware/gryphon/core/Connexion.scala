/*
 * Copyright (c) 2024. Phasmid Software
 */

package com.phasmidsoftware.gryphon.core

/**
 * Hierarchical trait defining the behavior of a connexion between two nodes (vertices) of a graph.
 *
 * And yes, I do know how to spell Connexion.
 * Admittedly, it's an archaic form of spelling but I like it.
 *
 * @tparam V the underlying attribute type.
 * @tparam X the container type for the target of this Connexion.
 */
trait Connexion[V, X[_]] {
  /**
   * The attribute type for the start of this Connexion.
   *
   * @return an instance of V.
   */
  def v1: V

  def v2: X[V]
}

/**
 * Typeclass trait to define behavior of having Connexions.
 *
 * @tparam T underlying attribute type.
 * @tparam X container type.
 */
trait HasConnexions[T, X[_]] {
  /**
   * Method to yield the connexions of an object of type X[T].
   *
   * @param tx the object with the connexions.
   * @return an Iterable of Connexion[T, X]
   */
  def connexions(tx: X[T]): Iterable[Connexion[T, X]]
}

/**
 * Concrete Connexion type that simply connects two nodes (directed) without any additional attributes.
 *
 * NOTE when comparing vertices (v2) for equality for example, we do not consider connexions nor discovered.
 *
 * @param v1 the attribute of the starting node.
 * @param v2 the container (Node) of the ending node.
 * @tparam V the underlying node attribute type.
 */
case class Pair[V](v1: V, v2: Node[V]) extends Connexion[V, Node]

/**
 * Concrete Connexion type that connects two nodes (directed) with an additional attributes.
 *
 * NOTE when comparing vertices (v2) for equality for example, we do not consider connexions nor discovered.
 *
 * @param v1        the attribute of the starting node.
 * @param v2        the container (Node) of the ending node.
 * @param attribute the edge attribute, for example, a weight or cost of traversal.
 * @tparam V the underlying node attribute type.
 */
abstract class Edge[V, E](v1: V, v2: Tuple1[V], attribute: E) extends Connexion[V, Tuple1] with Attribute[E]

case class DirectedEdge[V, E](v1: V, v2: Tuple1[V], attribute: E) extends Edge[V, E](v1, v2, attribute)

object DirectedEdge {
  def apply[V, E](from: V, to: V, e: E): DirectedEdge[V, E] = new DirectedEdge(from, Tuple1(to), e)
}
