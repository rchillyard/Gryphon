package littlegryphon.util

import scala.collection.{AbstractIterator, mutable}

/**
 * General functional programming utilities.
 *
 */
object FP {

  /**
   * Combines a sequence of optional values into a single optional sequence, preserving
   * the order of elements and excluding any `None` values.
   * If the input sequence contains only `None` values, the result will be `None`.
   * `Some(Nil)` will never be returned.
   *
   * @param tos the sequence of optional values to be combined.
   * @tparam T the type of the elements inside the `Option`.
   * @return an `Option` containing a sequence of all values extracted from the input sequence,
   *         or `None` if there are no such values present.
   */
  def sequence[T](tos: Seq[Option[T]]): Option[Seq[T]] =
    tos.foldLeft[Option[Seq[T]]](None) {
      (tso, to) =>
        (tso, to) match {
          case (Some(ts), Some(t)) =>
            Some(ts :+ t)
          case (_, None) =>
            tso
          case (None, Some(t)) =>
            Some(Seq(t))
        }
    }

  def mutableQueueIterator[T](queue: mutable.Queue[T]): Iterator[T] = new AbstractIterator[T] {
    def hasNext: Boolean = queue.nonEmpty

    def next(): T = queue.dequeue()
  }
}
