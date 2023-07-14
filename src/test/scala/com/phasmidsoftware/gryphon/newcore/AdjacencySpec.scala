/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.newcore

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class AdjacencySpec extends AnyFlatSpec with should.Matchers {

  behavior of "Adjacency"

  it should "adjacent" in {

    val a = new Adjacency[Int, Connection[Int]] {
      def adjacent(v: Int): Seq[Connection[Int]] = v match {
        case 0 => List(DirectedConnection(0, 1)) // The connection between 0 and 1 is directed.
        case _ => Nil
      }
    }
    a.adjacent(0) shouldBe List(DirectedConnection(0, 1))
  }

}
