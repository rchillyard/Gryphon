/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware

package object gryphon {

  /**
   * Implicit class to enable easy piping of an Iterator.
   *
   * @param xs an Iterator[X].
   * @tparam X the underlying type of xs.
   */
  implicit class PipedIterator[X](xs: Iterator[X]) {

    /**
     * Method to pipe the Iterator (xs) to a function and return a new copy of the iterator.
     *
     * Note that if we just want to log an Iterator, we can use Flog for that.
     * However, Flog's iterator function tends to drop most of the elements of an iterator.
     *
     * @param f a side-effectful-function which takes a Seq[X] (not an Iterator[X]) and yields Unit.
     * @return a new copy of xs (having been through a sequence).
     */
    def pipeS(f: Seq[X] => Unit): Iterator[X] = {
      val seq = xs.toSeq
      f(seq)
      seq.iterator
    }

    /**
     * Method to pipe the Iterator (xs) to a function and return a new copy of the iterator.
     *
     * Note that if we just want to log an Iterator, we can use Flog for that.
     * However, Flog's iterator function tends to drop most of the elements of an iterator.
     *
     * @param f a side-effectful-function which takes an X and yields Unit.
     * @return a new copy of xs.
     */
    def pipe(f: X => Unit): Iterator[X] = new Iterator[X] {

      def hasNext: Boolean = xs.hasNext

      def next(): X = {
        val x = xs.next()
        f(x)
        x
      }
    }
  }

}
