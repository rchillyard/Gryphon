/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.newcore

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class AdjacencySpec extends AnyFlatSpec with should.Matchers {

  behavior of "Adjacency"

  it should "ordered adjacent" in {

    val a: Adjacency[Int, Connexion[Int]] = OrderedAdjacency.empty[Int, Connexion[Int]] + (0 -> Bag.create(DirectedConnexion(0, 1)))
    a.adjacent(0).iterator.next() shouldBe DirectedConnexion(0, 1)
  }

  it should "unordered adjacent" in {

    val a: Adjacency[Int, Connexion[Int]] = UnorderedAdjacency.empty[Int, Connexion[Int]] + (0 -> Bag.create(DirectedConnexion(0, 1)))
    a.adjacent(0).iterator.next() shouldBe DirectedConnexion(0, 1)
  }

  it should "+" in {
    val a: Adjacency[Int, Connexion[Int]] = UnorderedAdjacency.empty[Int, Connexion[Int]] + (0 -> Bag.create(DirectedConnexion(0, 1)))
    a.adjacent(0).iterator.next() shouldBe DirectedConnexion(0, 1)
    val b = a + (0 -> Bag.create(DirectedConnexion(0, 2)))
    b.adjacent(0).iterator.toList shouldBe List(DirectedConnexion(0, 2))
  }

  it should "connect" in {
    val a: Adjacency[Int, Connexion[Int]] = UnorderedAdjacency.empty[Int, Connexion[Int]] + (0 -> Bag.create(DirectedConnexion(0, 1)))
    a.adjacent(0).iterator.next() shouldBe DirectedConnexion(0, 1)
    val b = a connect(0, DirectedConnexion(0, 2))
    val connexions = b.adjacent(0).iterator.toList
    connexions.contains(DirectedConnexion(0, 2)) shouldBe true
    connexions.contains(DirectedConnexion(0, 1)) shouldBe true
  }

}
