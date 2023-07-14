/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.newcore

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class AdjacencySpec extends AnyFlatSpec with should.Matchers {

  behavior of "Adjacency"

  it should "ordered adjacent" in {

    val a: Adjacency[Int, Connection[Int]] = OrderedAdjacency.empty[Int, Connection[Int]] + (0 -> Bag.create(DirectedConnection(0, 1)))

    a.adjacent(0).iterator.next() shouldBe DirectedConnection(0, 1)
  }

  it should "unordered adjacent" in {

    val a: Adjacency[Int, Connection[Int]] = UnorderedAdjacency.empty[Int, Connection[Int]] + (0 -> Bag.create(DirectedConnection(0, 1)))

    a.adjacent(0).iterator.next() shouldBe DirectedConnection(0, 1)
  }

}
