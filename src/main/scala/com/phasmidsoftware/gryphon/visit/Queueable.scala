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
  def appendAll(q: Q, vs: Seq[V]): Q = vs.foldLeft(q)((b, v) => append(b, v))
}

object Queueable {
  trait QueueableQueue[V] extends Queueable[Queue[V], V] {
    def take(vq: Queue[V]): Option[(V, Queue[V])] = {
      vq match {
        case v +: q => Some(v -> q)
        case _ => None
      }
    }

    def empty: Queue[V] = Queue.empty

    def append(vq: Queue[V], v: V): Queue[V] = vq.appended(v)
  }

  implicit object QueueableStringQueue extends QueueableQueue[String]

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

  def empty: Q

  def append(vq: Q, v: J): Unit

  /**
   * Enqueue an entire sequence of Vs to q (given).
   *
   * @param q  a queue-like object to receive the V values.
   * @param vs a sequence of V values.
   * @return a new queue-like object (Q).
   */
  def appendAll(q: Q, vs: Seq[J]): Unit = vs foreach (v => append(q, v))
}

object MutableQueueable {
  trait MutableQueueableQueue[X] extends MutableQueueable[mutable.Queue[X], X] {
    def take(q: mutable.Queue[X]): Option[X] = Option.when(q.nonEmpty)(q.dequeue())

    def empty: mutable.Queue[X] = mutable.Queue.empty

    def append(vq: mutable.Queue[X], v: X): Unit = vq.enqueue(v)
  }

  implicit object MutableQueueableStringQueue extends MutableQueueableQueue[String]
}

/**
 * Type class trait to define the behavior of an immutable queue-like object.
 *
 * @tparam Q the queue type.
 * @tparam V the underlying type of the journal.
 */
trait PriorityQueueable[Q, V] extends MutableQueueable[Q, V] with Ordering[V]

object PriorityQueueable {
  trait QueueablePriorityQueue[V] extends PriorityQueueable[mutable.PriorityQueue[V], V] {

    def take(q: mutable.PriorityQueue[V]): Option[V] = Option.when(q.nonEmpty)(q.dequeue())

    def empty: mutable.PriorityQueue[V] = mutable.PriorityQueue.empty(this)

    def append(vq: mutable.PriorityQueue[V], v: V): Unit = vq.enqueue(v)
  }

  implicit object QueueableDoublePriorityQueue extends QueueablePriorityQueue[Double] {
    def compare(x: Double, y: Double): Int = x.compareTo(y)
  }

  implicit object QueueableStringPriorityQueue extends QueueablePriorityQueue[String] {
    def compare(x: String, y: String): Int = x.compareTo(y)
  }
}
