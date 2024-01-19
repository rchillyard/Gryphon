/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.expcore


/**
 * Trait which models a set (bag, to be precise) of Connexion objects.
 * A Connexions object is associated with a vertex (node) of a graph.
 * It represents all of the connections adjacent to the associated vertex.
 *
 * @tparam V the underlying key (attribute) type of the element(s) to be connected, i.e. a vertex (node).
 * @tparam X the underlying type of the connexion which connects two vertices. Required to be a sub-type of Connexion[V].
 * @tparam P the property type of this Traversable object.
 *           Theoretically, this property can be anything at all.
 *           However, the dfs and bfs mechanisms use a field of type P in a node (vertex) to aid in traversal (graph navigation).
 */
trait Connexions[V, +X <: Connexion[V], P] {

  /**
   * Method to return the connexions.
   *
   * @return a Bag[X].
   */
  def connexions: Bag[X]

  def property: P

  def unit[Y >: X <: Connexion[V]](connexions: Bag[Y], property: P): Connexions[V, Y, P]

  def +[Y >: X <: Connexion[V]](x: Y): Connexions[V, Y, P] = unit(connexions + x, property)
}

/**
 * Trait to model the connexions around a vertex/node.
 *
 * @tparam V the underlying key (attribute) type of the nodes (vertices) of this BaseConnexions.
 * @tparam X the underlying type of the connexion which connects two vertices. Required to be a sub-type of Connexion[V].
 */
trait BaseConnexions[V, +X <: Connexion[V]] extends Connexions[V, X, Unit] {
  def toConnexions[P](p: Unit => P): Connexions[V, X, P] = ConnexionsCase(connexions, p(property))

  def unit[Y >: X <: Connexion[V]](connexions: Bag[Y]): BaseConnexions[V, Y]

//  override def +(x: X): BaseConnexions[V, X] = unit(connexions + x)
}

/**
 * Abstract class to represent a Connexions[V, X, P].
 *
 * @param _bag     the bag of Connexion objects which make up this Connexions object.
 * @param property the P object which pertains to this Connexions object.
 * @tparam V the underlying key (attribute) type of the element(s) to be connected, i.e. a vertex (node).
 * @tparam X the underlying type of the connexion which connects two vertices. Required to be a sub-type of Connexion[V].
 * @tparam P the property type of this Traversable object.
 *           Theoretically, this property can be anything at all.
 *           However, the dfs and bfs mechanisms use a field of type P in a node (vertex) to aid in traversal (graph navigation).
 */
abstract class AbstractConnexions[V, X <: Connexion[V], P](_bag: Bag[X], property: P) extends Connexions[V, X, P] {
  /**
   * Method to return the connexions.
   *
   * @return a Bag[X].
   */
  def connexions: Bag[X] = _bag
}

/**
 * Abstract class to represent a BaseConnexions object.
 *
 * @param bag the bag of connexions for this Connexions object.
 * @tparam V the underlying key (attribute) type of the element(s) to be connected, i.e. a vertex (node).
 * @tparam X the underlying type of the connexion which connects two vertices. Required to be a sub-type of Connexion[V].
 */
abstract class AbstractBaseConnexions[V, X <: Connexion[V]](bag: Bag[X]) extends AbstractConnexions[V, X, Unit](bag, ()) with BaseConnexions[V, X] {
  val property: Unit = ()
}


object BaseConnexions {
  def empty[V, X <: Connexion[V]]: BaseConnexions[V, X] = BaseConnexionsCase[V, X](Bag.empty)
}

object Connexions {
  def empty[V, X <: Connexion[V], P](property: P): Connexions[V, X, P] = ConnexionsCase[V, X, P](Bag.empty, property)

}

/**
 * Concrete case class to represent a BaseConnexions.
 *
 * @param bag the bag of connexions for this Connexions object.
 * @tparam V the underlying key (attribute) type of the element(s) to be connected, i.e. a vertex (node).
 * @tparam X the underlying type of the connexion which connects two vertices. Required to be a sub-type of Connexion[V].
 */
case class BaseConnexionsCase[V, X <: Connexion[V]](bag: Bag[X]) extends AbstractBaseConnexions[V, X](bag) with BaseConnexions[V, X] {

  def unit[Y >: X <: Connexion[V]](connexions: Bag[Y]): BaseConnexions[V, Y] = BaseConnexionsCase(connexions)

  def unit[Y >: X <: Connexion[V]](connexions: Bag[Y], property: Unit): Connexions[V, Y, Unit] = BaseConnexionsCase(connexions)
}

/**
 * Concrete case class which extends Connexions.
 *
 * @param _bag the bag of Connexion objects which make up this Connexions object.
 * @param property the P object which pertains to this Connexions object.
 * @tparam V the underlying key (attribute) type of the element(s) to be connected, i.e. a vertex (node).
 * @tparam X the underlying type of the connexion which connects two vertices. Required to be a sub-type of Connexion[V].
 * @tparam P the property type of this Traversable object.
 *           Theoretically, this property can be anything at all.
 *           However, the dfs and bfs mechanisms use a field of type P in a node (vertex) to aid in traversal (graph navigation).
 */
case class ConnexionsCase[V, X <: Connexion[V], P](_bag: Bag[X], property: P) extends AbstractConnexions[V, X, P](_bag, property) with Connexions[V, X, P] {
//  def unit(connexions: Bag[X]): BaseConnexions[V, X] = ConnexionsCase[V, X, Unit](connexions, ())

  def unit[Y >: X <: Connexion[V]](connexions: Bag[Y], property: P): Connexions[V, Y, P] = ConnexionsCase(connexions, property)

}

/**
 * A case class which implements AbstractConnexions.
 *
 * @param v        the vertex identifier.
 * @param bag      the bag of Xs.
 * @param property the P object which pertains to this Connexions object.
 * @tparam V the underlying key (attribute) type of the nodes (vertices) of this Vertex.
 * @tparam X the underlying type of the connexion which connects two vertices. Required to be a sub-type of Connexion[V].
 * @tparam P the property type of this Traversable object.
 *           Theoretically, this property can be anything at all.
 *           However, the dfs and bfs mechanisms use a field of type P in a node (vertex) to aid in traversal (graph navigation).
 */
case class Vertex[V, X <: Connexion[V], P: Initializable : Discoverable](v: V, bag: Bag[X], property: P) extends AbstractConnexions[V, X, P](bag, property) {

  def unit[Y >: X <: Connexion[V]](connexions: Bag[Y], property: P): Vertex[V, Y, P] = Vertex(v, connexions, property)
//
//  def unit(connexions: Bag[X]): BaseConnexions[V, X] = ???
//
//  def unit(connexions: Bag[X], property: P): Connexions[V, X, P] = ???
}
