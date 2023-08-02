/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.newcore

/**
 * A trait to model the concept of a connexion.
 *
 * NOTE that I am a fan of the old-fashioned way to spell what is now more commonly referred to as a connection.
 * Furthermore, it makes more sense to use the parametric type X to stand for a connexion.
 *
 * @tparam V the type of elements to be connected.
 */
trait Connexion[V] {
  /**
   * Method to determine what V value, if any, is connected via this Connexion.
   *
   * @param v the start (from) value of this Connexion.
   * @return an optional V which is the element connected to <code>v</code> via this Connexion.
   */
  def connexion(v: V): Option[V]
}

abstract class AbstractConnexion[V](start: V, end: V, twoWay: Boolean) extends Connexion[V] {
  /**
   * Method to determine what V value, if any, is connected via this Connexion.
   *
   * @param v the start of this Connexion.
   * @return Some(end), providing that the given vertex <code>v</code> is <code>start</code>; or
   *         Some(start), providing that the given vertex <code>v</code> is <code>end</code> AND twoWay is true.
   */
  def connexion(v: V): Option[V] = v match {
    case `start` => Some(end)
    case `end` if twoWay => Some(start)
    case _ => None
  }
}

trait DirectedConnexion[V] extends Connexion[V]

trait UndirectedConnexion[V] extends Connexion[V]

/**
 * Case class to represent a Connexion from start to end.
 *
 * @param start the origin of the connexion.
 * @param end   the target of the connexion.
 * @tparam V the type of elements to be connected.
 */
case class DirectedConnexionCase[V](start: V, end: V) extends AbstractConnexion[V](start, end, false) with DirectedConnexion[V]

/**
 * Case class to represent a Connexion from start to end.
 *
 * @param start the origin of the connexion.
 * @param end   the target of the connexion.
 * @tparam V the type of elements to be connected.
 */
case class UndirectedConnexionCase[V](start: V, end: V) extends AbstractConnexion[V](start, end, true) with UndirectedConnexion[V]

