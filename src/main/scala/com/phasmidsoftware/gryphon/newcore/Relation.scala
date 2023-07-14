/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.newcore

/**
 * A trait to model the concept of a relationship.
 *
 * @tparam V the type of elements to be related.
 */
trait Relation[V] {
  /**
   * Method to determine what V value, if any, is related via this Relation.
   *
   * @param v the start (from) value of this Relation.
   * @return an optional V which is the element related to v via this Relation.
   */
  def relation(v: V): Option[V]
}

/**
 * Case class to represent a Relation from start to end.
 *
 * @param start the origin of the relationship.
 * @param end   the target of the relationship.
 * @tparam V the type of elements to be related.
 */
case class Connection[V](start: V, end: V) extends Relation[V] {
  /**
   * Method to determine what V value, if any, is connected via this Relation.
   *
   * @param v the start of this Relation.
   * @return a Some(end), providing that the given vertex <code>v</code> is <code>start</code>.
   */
  def relation(v: V): Option[V] = v match {
    case `start` => Some(end)
    case _ => None
  }
}

