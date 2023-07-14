/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.newcore

/**
 * A trait to model the concept of a connection.
 *
 * @tparam V the type of elements to be related.
 */
trait Connection[V] {
  /**
   * Method to determine what V value, if any, is related via this Connection.
   *
   * @param v the start (from) value of this Connection.
   * @return an optional V which is the element related to v via this Connection.
   */
  def connection(v: V): Option[V]
}

abstract class AbstractConnection[V](start: V, end: V, twoWay: Boolean) extends Connection[V] {
  /**
   * Method to determine what V value, if any, is connected via this Connection.
   *
   * @param v the start of this Connection.
   * @return Some(end), providing that the given vertex <code>v</code> is <code>start</code>; or
   *         Some(start), providing that the given vertex <code>v</code> is <code>end</code> AND twoWay is true.
   */
  def connection(v: V): Option[V] = v match {
    case `start` => Some(end)
    case `end` if twoWay => Some(start)
    case _ => None
  }
}

/**
 * Case class to represent a Connection from start to end.
 *
 * @param start the origin of the relationship.
 * @param end   the target of the relationship.
 * @tparam V the type of elements to be related.
 */
case class DirectedConnection[V](start: V, end: V) extends AbstractConnection[V](start, end, false)

/**
 * Case class to represent a Connection from start to end.
 *
 * @param start the origin of the relationship.
 * @param end   the target of the relationship.
 * @tparam V the type of elements to be related.
 */
case class UndirectedConnection[V](start: V, end: V) extends AbstractConnection[V](start, end, true)

