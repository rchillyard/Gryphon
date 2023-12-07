/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.newcore

import scala.util.Random

/**
 * An unordered collection of Item objects which may include duplicates.
 *
 * @param xs     a sequence of Item.
 * @param random (implicit) Random source.
 * @tparam Item the underlying type of the Bag.
 */
case class Bag[+Item](xs: Seq[Item])(implicit random: Random) extends Iterable[Item] {
  /**
   * Iterator method, as required by Iterable.
   * NOTE: do NOT assume that each invocation will return the same sequence of items.
   *
   * @return an iterator including all items, but in no particular order.
   */
  def iterator: Iterator[Item] = random.shuffle(xs).iterator

  /**
   * Method to add an element to this Bag.
   * NOTE: there is only one such method because a Bag has no order.
   *
   * @param x the element to be added.
   * @tparam X the type of x (a super-type of Item).
   * @return a new Bag[X].
   */
  def +[X >: Item](x: X): Bag[X] = new Bag(xs :+ x)
}

object Bag {
  implicit val defaultRandom: Random = new Random()

  /**
   * Method to construct a new Bag with a (comma-separated) list of Items.
   *
   * @param xs a comma-separated list of Items.
   * @tparam Item the item type.
   * @return a Bag[Item]
   */
  def create[Item](xs: Item*): Bag[Item] = new Bag(xs)

  /**
   * Method to construct a new empty Bag.
   *
   * @tparam Item the item type.
   * @return an empty Bag[Item]
   */
  def empty[Item]: Bag[Item] = Bag(Nil)
}
