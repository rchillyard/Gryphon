/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.newcore


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
 * @tparam V the vertex/node identifier.
 * @tparam X the underlying connexion type.
 */
trait BaseConnexions[V, +X <: Connexion[V]] extends Connexions[V, X, Unit] {
  def toConnexions[P](p: Unit => P): Connexions[V, X, P] = ConnexionsCase(connexions, p(property))

  def unit[Y >: X <: Connexion[V]](connexions: Bag[Y]): BaseConnexions[V, Y]

//  override def +(x: X): BaseConnexions[V, X] = unit(connexions + x)
}

abstract class AbstractConnexions[V, X <: Connexion[V], P](_bag: Bag[X], val property: P) extends Connexions[V, X, P] {
  /**
   * Method to return the connexions.
   *
   * @return a Bag[X].
   */
  def connexions: Bag[X] = _bag
}

abstract class AbstractBaseConnexions[V, X <: Connexion[V]](val bag: Bag[X]) extends AbstractConnexions[V, X, Unit](bag, ()) with BaseConnexions[V, X]

object BaseConnexions {
  def empty[V, X <: Connexion[V]]: BaseConnexions[V, X] = BaseConnexionsCase[V, X](Bag.empty)
}

object Connexions {
  def empty[V, X <: Connexion[V], P](property: P): Connexions[V, X, P] = ConnexionsCase[V, X, P](Bag.empty, property)

}

case class BaseConnexionsCase[V, X <: Connexion[V]](_bag: Bag[X]) extends AbstractBaseConnexions[V, X](_bag) with BaseConnexions[V, X] {

  def unit[Y >: X <: Connexion[V]](connexions: Bag[Y]): BaseConnexions[V, Y] = BaseConnexionsCase(connexions)

  def unit[Y >: X <: Connexion[V]](connexions: Bag[Y], property: Unit): Connexions[V, Y, Unit] = BaseConnexionsCase(connexions)
}

case class ConnexionsCase[V, X <: Connexion[V], P](_bag: Bag[X], _property: P) extends AbstractConnexions[V, X, P](_bag, _property) with Connexions[V, X, P] {
//  def unit(connexions: Bag[X]): BaseConnexions[V, X] = ConnexionsCase[V, X, Unit](connexions, ())

  def unit[Y >: X <: Connexion[V]](connexions: Bag[Y], property: P): Connexions[V, Y, P] = ConnexionsCase(connexions, property)

}

/**
 * A case class which implements BaseConnexions.
 *
 * @param v         the vertex identifier.
 * @param _bag      the bag of Xs.
 * @param _property the property.
 * @tparam V the vertex/node identifier.
 * @tparam X the underlying connexion type.
 * @tparam P a property type.
 */
case class Vertex[V, X <: Connexion[V], P: Initializable : Discoverable](v: V, _bag: Bag[X], _property: P) extends AbstractConnexions[V, X, P](_bag, _property) {

  def unit[Y >: X <: Connexion[V]](connexions: Bag[Y], property: P): Vertex[V, Y, P] = Vertex(v, connexions, property)
//
//  def unit(connexions: Bag[X]): BaseConnexions[V, X] = ???
//
//  def unit(connexions: Bag[X], property: P): Connexions[V, X, P] = ???
}
