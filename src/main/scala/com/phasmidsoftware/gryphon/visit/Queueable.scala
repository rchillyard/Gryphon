/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.visit

import scala.collection.immutable.Queue
import scala.collection.mutable
import scala.collection.mutable.PriorityQueue

/**
 * Type class trait to define the behavior of an immutable queue-like object.
 *
 * @tparam Q the queue type.
 * @tparam V the underlying type of the journal.
 */
trait Queueable[Q, V] extends Journal[Q, V] {

  /**
   * Take the oldest element from a queue-like object.
   *
   * @param q the queue-like object to take from.
   * @return a tuple of a V and a Q (the latter represents what's left in the queue-like object).
   */
  def take(q: Q): Option[(V, Q)]

  /**
   * Enqueue an entire sequence of Vs to q (given).
   *
   * @param q  a queue-like object to receive the V values.
   * @param vs a sequence of V values.
   * @return a new queue-like object (Q).
   */
  def appendAll(q: Q, vs: Seq[V]): Q =
    vs.foldLeft(q)((b, v) => append(b, v))
}

/**
 * Companion object for the `Queueable` type class that provides default
 * implementations and utilities for working with queue-like data structures,
 * specifically focusing on immutable `Queue` types.
 */
object Queueable {
  /**
   * Trait that extends `Queueable` specifically for Scala's immutable `Queue` type,
   * allowing handling of `Queue` as a queue-like data structure with specific operations.
   *
   * @tparam V the type of elements contained in the `Queue`.
   */
  trait QueueableQueue[V] extends Queueable[Queue[V], V] {
    /**
     * Removes and retrieves the first element from the given queue if it is not empty.
     *
     * @param vq the queue from which the first element will be taken
     * @return an `Option` containing a tuple of the first element and the remaining queue if the queue is not empty,
     *         or `None` if the queue is empty
     */
    def take(vq: Queue[V]): Option[(V, Queue[V])] = {
      vq match {
        case v +: q =>
          Some(v -> q)
        case _ =>
          None
      }
    }

    /**
     * Represents an empty `Queue` of type `V`.
     *
     * @return an empty `Queue` instance.
     */
    def empty: Queue[V] =
      Queue.empty

    /**
     * Appends an element to the end of the given queue.
     *
     * @param vq the queue to which the element will be appended
     * @param v  the element to append to the queue
     * @return a new queue with the element appended
     */
    def append(vq: Queue[V], v: V): Queue[V] =
      vq.appended(v)
  }

  /**
   * An implicit implementation of `QueueableQueue` for `String` type.
   *
   * This provides the ability to handle `Queue[String]` with standard
   * operations like taking an element from the front of the queue,
   * appending a new element to the end, or creating an empty queue.
   */
  implicit object QueueableStringQueue extends QueueableQueue[String]

  /**
   * Provides an implicit instance of `QueueableQueue` for queues with 
   * elements of type `Int`. This object defines how integer values can 
   * be processed in the context of a `Queue` by implementing the 
   * operations defined in the `QueueableQueue` trait.
   *
   * Specifically, it supports operations such as taking an element from 
   * the head of the queue, returning an empty queue, and appending integer 
   * elements to the queue.
   */
  implicit object QueueableIntQueue extends QueueableQueue[Int]
}

/**
 * Type class trait to define the behavior of an immutable queue-like object.
 *
 * @tparam Q the queue type.
 * @tparam J the underlying type of the journal.
 */
trait MutableQueueable[Q, J] {

  /**
   * Take the oldest element from a mutable queue-like object.
   *
   * @param q the mutable queue-like object to take from.
   * @return a tuple of a V and a Q (the latter represents what's left in the queue-like object).
   */
  def take(q: Q): Option[J]

  /**
   * Returns an empty instance of the queue-like object.
   *
   * @return an empty queue-like object of type Q.
   */
  def empty: Q

  /**
   * Appends a value to a mutable queue-like object.
   *
   * @param vq the mutable queue-like object to which the value will be appended
   * @param v  the value to append to the queue-like object
   * @return Unit, the operation modifies the queue-like object in place
   */
  def append(vq: Q, v: J): Unit

  /**
   * Enqueue an entire sequence of Vs to q (given).
   *
   * @param q  a queue-like object to receive the V values.
   * @param vs a sequence of V values.
   * @return a new queue-like object (Q).
   */
  def appendAll(q: Q, vs: Seq[J]): Unit =
    vs foreach (v => append(q, v))
}

/**
 * Object containing implementations and instances for the `MutableQueueable` type class.
 *
 * Provides a default implementation of `MutableQueueable` for `mutable.Queue`
 * to handle queue-like operations on mutable collections.
 */
object MutableQueueable {
  /**
   * A specialized implementation of the `MutableQueueable` type class for `mutable.Queue`.
   *
   * This trait provides queue-like operations specifically for `scala.collection.mutable.Queue`.
   *
   * @tparam X the type of elements stored in the queue.
   */
  trait MutableQueueableQueue[X] extends MutableQueueable[mutable.Queue[X], X] {
    /**
     * Removes and retrieves an element from the front of the specified mutable queue, 
     * if the queue is not empty.
     *
     * @param q the mutable queue from which the element is to be dequeued
     * @return an Option containing the dequeued element if the queue is non-empty, 
     *         or None if the queue is empty
     */
    def take(q: mutable.Queue[X]): Option[X] =
      Option.when(q.nonEmpty)(q.dequeue())

    /**
     * Returns an empty mutable queue of type `X`.
     *
     * This method provides a way to create a new, empty instance of a `mutable.Queue[X]`.
     *
     * @return an empty `mutable.Queue[X]`.
     */
    def empty: mutable.Queue[X] =
      mutable.Queue.empty

    /**
     * Appends an element to the specified mutable queue.
     *
     * @param vq the mutable queue to which the element will be appended
     * @param v  the element to append to the queue
     * @return Unit, as this method modifies the given mutable queue in place
     */
    def append(vq: mutable.Queue[X], v: X): Unit =
      vq.enqueue(v)
  }

  /**
   * Implicit object providing a concrete implementation of `MutableQueueableQueue` for `String` type.
   *
   * This object allows for queue-like operations to be performed specifically
   * on instances of `mutable.Queue[String]`, such as appending elements and
   * retrieving or removing elements while maintaining mutability.
   *
   * It leverages the functionality defined in the `MutableQueueableQueue` trait,
   * which provides methods for common queue operations.
   */
  implicit object MutableQueueableStringQueue extends MutableQueueableQueue[String]
}

/**
 * Type class trait to define the behavior of an immutable queue-like object.
 *
 * @tparam Q the queue type.
 * @tparam V the underlying type of the journal.
 */
trait PriorityQueueable[Q, V] extends MutableQueueable[Q, V] with Ordering[V]

/**
 * Companion object for the PriorityQueueable type class, which provides specific 
 * implementations for priority queues of various types.
 */
object PriorityQueueable {
  /**
   * A trait that defines operations for priority queues using Scala's mutable.PriorityQueue.
   *
   * This trait extends the `PriorityQueueable` type class, providing a concrete implementation
   * for working with mutable priority queues. It facilitates operations such as appending elements,
   * retrieving the highest-priority element, and initializing an empty priority queue.
   *
   * @tparam V the type of elements stored in the priority queue.
   */
  trait QueueablePriorityQueue[V] extends PriorityQueueable[mutable.PriorityQueue[V], V] {

    /**
     * Removes and returns the highest-priority element from the given priority queue, if it is not empty.
     *
     * @param q the priority queue from which to take the highest-priority element
     * @return an option containing the highest-priority element if the queue is non-empty, or None otherwise
     */
    def take(q: mutable.PriorityQueue[V]): Option[V] =
      Option.when(q.nonEmpty)(q.dequeue())

    /**
     * Creates and returns an empty mutable priority queue.
     *
     * @return an empty instance of `mutable.PriorityQueue[V]`
     */
    def empty: mutable.PriorityQueue[V] =
      mutable.PriorityQueue.empty(this)

    /**
     * Appends an element to the specified mutable priority queue.
     *
     * @param vq the mutable priority queue to which the element will be added
     * @param v  the element to be appended to the priority queue
     * @return Unit, indicating the operation completes by appending the element
     */
    def append(vq: mutable.PriorityQueue[V], v: V): Unit =
      vq.enqueue(v)
  }

  /**
   * An implicit object providing a `QueueablePriorityQueue` implementation for `Double` values.
   *
   * This object allows working with `Double` values in a mutable priority queue by defining
   * a comparison function for ordering elements.
   *
   * It extends the `QueueablePriorityQueue` trait and specifies the ordering logic
   * for queue operations using `Double.compareTo`.
   */
  implicit object QueueableDoublePriorityQueue extends QueueablePriorityQueue[Double] {
    /**
     * Compares two `Double` values by their natural ordering.
     *
     * @param x the first `Double` value to compare
     * @param y the second `Double` value to compare
     * @return an integer representing the comparison result:
     *         - a negative integer if `x` is less than `y`
     *         - zero if `x` is equal to `y`
     *         - a positive integer if `x` is greater than `y`
     */
    def compare(x: Double, y: Double): Int =
      x.compareTo(y)
  }

  /**
   * An implicit object that provides a `QueueablePriorityQueue` implementation for Strings.
   *
   * This object allows Strings to be stored in and managed by a mutable priority queue
   * using their natural ordering (lexicographical order). The comparison between two strings
   * is performed using the `compareTo` method.
   */
  implicit object QueueableStringPriorityQueue extends QueueablePriorityQueue[String] {
    /**
     * Compares two strings lexicographically.
     * The comparison is based on the Unicode value of each character in the strings.
     *
     * @param x the first string to be compared
     * @param y the second string to be compared
     * @return a negative integer, zero, or a positive integer as the first string is 
     *         less than, equal to, or greater than the second string
     */
    def compare(x: String, y: String): Int =
      x.compareTo(y)
  }
}
