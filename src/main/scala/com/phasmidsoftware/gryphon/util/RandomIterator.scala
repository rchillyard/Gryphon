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
  def hasNext: Boolean =
    list.nonEmpty

  /**
   * Returns the next randomly selected element from the underlying collection and removes it.
   *
   * The element is chosen randomly using the provided implicit `Random` instance.
   *
   * @return the next randomly selected element of type `T`.
   * @throws NoSuchElementException if the collection is empty.
   */
  def next(): T =
    list.remove(random.nextInt(list.size))

  // Burn the first value, to avoid an initial bias in the sequence.
  //
  // Not because the first value is the seed — it is not; java.util.Random scrambles the seed on
  // construction. The real problem is that the first output is very nearly a linear function of
  // the seed, which is the usual weakness of a linear congruential generator. Measured on Java 21,
  // `new Random(s).nextInt(100000)` for s = 0..9 gives
  //
  //   41360 48985 26108 33734 71862 79487 56611 64236 2364 9989
  //
  // which steps by +7625 repeatedly, wrapping. The *second* value shows no such structure. So one
  // discarded draw is enough, and it matters here because nearby seeds are a normal thing to use.
  random.nextLong(): Unit

  private val list: mutable.ListBuffer[T] =
    mutable.ListBuffer(iterable.toSeq *)
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
