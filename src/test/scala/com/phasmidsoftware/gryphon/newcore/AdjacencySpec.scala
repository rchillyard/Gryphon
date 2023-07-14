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

  it should "+" in {
    val a: Adjacency[Int, Connection[Int]] = UnorderedAdjacency.empty[Int, Connection[Int]] + (0 -> Bag.create(DirectedConnection(0, 1)))
    a.adjacent(0).iterator.next() shouldBe DirectedConnection(0, 1)
    val b = a + (0 -> Bag.create(DirectedConnection(0, 2)))
    b.adjacent(0).iterator.toList shouldBe List(DirectedConnection(0, 2))
  }

  it should "connect" in {
    val a: Adjacency[Int, Connection[Int]] = UnorderedAdjacency.empty[Int, Connection[Int]] + (0 -> Bag.create(DirectedConnection(0, 1)))
    a.adjacent(0).iterator.next() shouldBe DirectedConnection(0, 1)
    val b = a connect(0, DirectedConnection(0, 2))
    val connexions = b.adjacent(0).iterator.toList
    connexions.contains(DirectedConnection(0, 2)) shouldBe true
    connexions.contains(DirectedConnection(0, 1)) shouldBe true
  }

}
