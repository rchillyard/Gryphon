/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.util

import scala.annotation.tailrec
import scala.collection.mutable
import scala.collection.mutable.PriorityQueue

/**
 * Case class LazyPriorityQueue which delegates to a mutable PriorityQueue.
 * CONSIDER implementing an indexed PriorityQueue
 *
 * @param pq the delegate priority queue.
 * @tparam X the underlying type of this priority queue.
 */
case class LazyPriorityQueue[X: Ordering](pq: mutable.PriorityQueue[X]) {
  def nonEmpty: Boolean = pq.nonEmpty

  def addOne(x: X): Unit = pq.addOne(x)

  def dequeueOpt: Option[X] = Option.when(pq.nonEmpty)(pq.dequeue())

  /**
   * This method is a quasi-eager version of dequeue.
   * If it finds an element that is not eligible, that element will be expunged and another tried.
   *
   * @param p a predicate.
   * @return an optional X.
   */
  def conditionalDequeue(p: X => Boolean): Option[X] = {
    @tailrec
    def inner: Option[X] = dequeueOpt match {
      case Some(x) => if (p(x)) Some(x) else inner
      case None => None
    }

    inner
  }

  def headOption: Option[X] = pq.headOption
}

object LazyPriorityQueue {
  def apply[X: Ordering]: LazyPriorityQueue[X] = apply(new PriorityQueue[X]())
}