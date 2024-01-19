/*
 * Copyright (c) 2024. Phasmid Software
 */

package com.phasmidsoftware.gryphon.core

/**
 * Trait to define the behavior of a Bag (or MultiSet).
 *
 * NOTE: there are only two operations on Bag: iteration and insertion (+).
 *
 * @tparam X the underlying type of this Bag (covariant).
 */
trait Bag[+X] extends Iterable[X] {
  /**
   * Method to add an element to this Bag.
   *
   * @param y the element to add.
   * @tparam Y the underlying type of the result and of <code>y</code> (must be a super-type of X).
   * @return a Bag[Y].
   */
  def +[Y >: X](y: Y): Bag[Y]

}

/**
 * Companion object to Bag.
 */
object Bag {
  /**
   * Constant value: empty: Bag[Nothing].
   */
  val empty: Bag[Nothing] = ListBag.apply
}

/**
 * Concrete case class which implements Bag that behaves rather more like a Set.
 *
 * @param xs a sequence of X. The order of this sequence is immaterial.
 * @tparam X the underlying type of this Bag (covariant).
 */
case class ListBag[+X](xs: Seq[X]) extends Bag[X] {

  /**
   * Method to insert a new value of type Y (a super-type of X) into this ListBag.
   * If <code>y</code> is essentially the same as an existing member of this Bag, then that member will be replaced by <code>y</code>.
   * The consequence of this, in the context of a Vertex (which has a Bag of Connexions) is that a simple Pair cannot
   * be duplicated in the bag.
   * If you want to have two edges between the same vertices, then you would need to give them a (different) Edge attribute.
   *
   * @param y the element to add.
   * @tparam Y the underlying type of the result and of <code>y</code> (must be a super-type of X).
   * @return a Bag[Y].
   */
  def +[Y >: X](y: Y): Bag[Y] = ListBag[Y]((xs filterNot (_ == y)) :+ y)

  override def toString(): String = s"ListBag($xs)"

  override def equals(obj: Any): Boolean = obj match {
    case bag: ListBag[X] => xs.size == bag.xs.size && (xs diff bag.xs).isEmpty
    case _ => false
  }

  /**
   * Method to yield an Iterator[X] on the values of this Bag.
   * CONSIDER shuffling the order of the values.
   *
   * @return an iterator on xs.
   */
  def iterator: Iterator[X] = xs.iterator
}

/**
 * Companion object to ListBag.
 */
object ListBag {
  def create[X](xs: X*): ListBag[X] =
    new ListBag[X](xs)

  def apply: ListBag[Nothing] = apply(Nil)
}