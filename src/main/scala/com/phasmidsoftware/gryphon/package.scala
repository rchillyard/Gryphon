/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware

package object gryphon {

  implicit class PipedIterator[X](xs: Iterator[X]) {
    def pipe(f: Seq[X] => Unit): Iterator[X] = {
      val seq = xs.toSeq
      f(seq)
      seq.iterator
    }
  }

}
