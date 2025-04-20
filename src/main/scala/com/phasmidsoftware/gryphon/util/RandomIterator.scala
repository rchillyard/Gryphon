package com.phasmidsoftware.gryphon.util

import scala.collection.mutable
import scala.util.Random

/**
 * A class providing an iterator that iterates through elements of a collection in a random order.
 *
 * This iterator removes elements from the underlying collection as it iterates over them.
 * The randomness in the iteration order is determined by the implicit `Random` instance provided.
 *
 * @tparam T the type of elements contained in the iterator.
 * @param iterable the collection of elements to be iterated in random order.
 * @param random   an implicit `Random` instance used for randomization.
 */
case class RandomIterator[T](private val iterable: Iterable[T])(implicit random: Random) extends Iterator[T] {

  /**
   * Determines whether the iterator has more elements.
   *
   * @return true if there are more elements to iterate through, false otherwise.
   */
  def hasNext: Boolean = list.nonEmpty

  /**
   * Returns the next randomly selected element from the underlying collection and removes it.
   *
   * The element is chosen randomly using the provided implicit `Random` instance.
   *
   * @return the next randomly selected element of type `T`.
   * @throws NoSuchElementException if the collection is empty.
   */
  def next(): T = list.remove(random.nextInt(list.size))

  random.nextLong() // XXX burn the first value

  private val list: mutable.ListBuffer[T] = mutable.ListBuffer(iterable.toSeq: _*)
}

/**
 * An object providing utility methods for creating and working with `RandomIterator`.
 *
 * The `RandomIterator` is used to iterate over a collection of elements in a random order.
 */
object RandomIterator:

  /**
   * Creates a `RandomIterator` that iterates over the elements of the provided `Iterator` in random order.
   *
   * @param iterator the `Iterator` containing the elements to be iterated in random order.
   * @param random   an implicit `Random` instance used to determine the random order of iteration.
   * @return a `RandomIterator` containing the elements of the input `Iterator` shuffled randomly.
   */
  def apply[T](iterator: Iterator[T])(implicit random: Random): RandomIterator[T] =
    new RandomIterator(iterator.toSeq)
