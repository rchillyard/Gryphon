package com.phasmidsoftware.gryphon.util

import scala.collection.immutable.Queue
import scala.collection.mutable

/**
 * Represents a priority queue data structure that can store elements of type `T` with priorities determined
 * by the implicit `Ordering[T]` type class.
 * NOTE that this case class is immutable but it depends on a mutable PriorityQueue.
 * 
 * NOTE not used. Use MinPQ or MaxPQ instead.
 *
 * @tparam T the type of elements stored in the priority queue
 *           (must have an implicit ordering defined for comparison)
 */
case class PriorityQueueImmutable[T: Ordering](private val pq: mutable.PriorityQueue[T]) {

  /**
   * Checks if the priority queue is empty.
   *
   * @return true if the priority queue has no elements, false otherwise
   */
  def isEmpty: Boolean = pq.isEmpty

  /**
   * Removes and returns the highest-priority element from the priority queue if it is not empty.
   * If the queue is empty, it returns None,`` and the unchanged current priority queue.
   * If the queue is not empty, returns the highest-priority element wrapped in `Some`
   * and a new priority queue instance without that element.
   *
   * @return A tuple containing the highest-priority element wrapped in `Option` and the updated priority queue instance.
   */
  def take: (Option[T], PriorityQueueImmutable[T]) = pq match {
    case q if q.isEmpty =>
      (None, this)
    case q =>
      val t = q.dequeue
      (Some(t), copy(pq = q))
  }

  /**
   * Inserts a sequence of elements into the priority queue and returns a new PriorityQueue with the elements added.
   *
   * @param ts the sequence of elements to be inserted into the priority queue
   * @return a new PriorityQueue instance with the specified elements added
   */
  def insert(ts: Seq[T]): PriorityQueueImmutable[T] = copy(pq = {
    pq ++ ts; pq
  })

  /**
   * Inserts a single element into the priority queue.
   *
   * @param t the element to be inserted into the priority queue
   * @return a new PriorityQueue instance with the element inserted
   */
  def insert(t: T): PriorityQueueImmutable[T] = insert(Seq(t))
}
