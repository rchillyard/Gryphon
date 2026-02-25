package com.phasmidsoftware.gryphon.util

import com.phasmidsoftware.gryphon.util.FP.tee

/**
 * @param f  the logging function.
 * @param xs the incoming Iterator.
 * @tparam X the underlying type.
 */
class LoggingIterator[X](f: X => Unit)(xs: Iterator[X]) extends Iterator[X] {
  def hasNext: Boolean = xs.hasNext

  def next(): X = tee(f)(xs.next())
}

/**
 * A wrapper class around a sequence that provides an iterator whose elements can be viewed in the debugger.
 *
 * @tparam X the type of elements this iterator will traverse.
 * @constructor Initializes the iterator with the given sequence.
 * @param xs the sequence of elements to be traversed by the iterator.
 */
case class ViewableIterator[X](xs: Seq[X]) extends Iterator[X] {
  private val it = xs.iterator

  def hasNext: Boolean = it.hasNext

  def next(): X = it.next()
}

object ViewableIterator {
  def apply[X](xs: Iterator[X]): ViewableIterator[X] =
    new ViewableIterator(xs to Seq)
}