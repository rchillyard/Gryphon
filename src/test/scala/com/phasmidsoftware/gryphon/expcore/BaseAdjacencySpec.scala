/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.expcore

import com.phasmidsoftware.gryphon.visit.{PostVisitor, Visitor}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import scala.collection.immutable.Queue

class BaseAdjacencySpec extends AnyFlatSpec with should.Matchers {

  behavior of "BaseAdjacency"

  it should "ordered adjacent" in {

    val emptyBaseAdjacency: OrderedBaseAdjacency[Int, Connexion[Int]] = OrderedBaseAdjacency.empty[Int, Connexion[Int]]
    val connexions: BaseConnexions[Int, DirectedConnexion[Int]] = BaseConnexionsCase[Int, DirectedConnexionCase[Int]](Bag.create(DirectedConnexionCase[Int](0, 1)))
    val tuple: (Int, Connexions[Int, DirectedConnexion[Int], Unit]) = 0 -> connexions
    val a: Adjacency[Int, Connexion[Int], Unit] = emptyBaseAdjacency + tuple
    a.adjacent(0).connexions.iterator.next() shouldBe DirectedConnexionCase(0, 1)
  }

  it should "unordered adjacent" in {
    val a: Adjacency[Int, Connexion[Int], Unit] = UnorderedAdjacency.empty[Int, Connexion[Int], Unit] + (0 -> BaseConnexionsCase[Int, DirectedConnexionCase[Int]](Bag.create(DirectedConnexionCase(0, 1))))
    a.adjacent(0).connexions.iterator.next() shouldBe DirectedConnexionCase(0, 1)
  }

  it should "+" in {
    val a: Adjacency[Int, Connexion[Int], Unit] = UnorderedAdjacency.empty[Int, Connexion[Int], Unit] + (0 -> BaseConnexionsCase[Int, DirectedConnexionCase[Int]](Bag.create(DirectedConnexionCase(0, 1))))
    a.adjacent(0).connexions.iterator.next() shouldBe DirectedConnexionCase(0, 1)
    val b = a + (0 -> BaseConnexionsCase[Int, DirectedConnexionCase[Int]](Bag.create(DirectedConnexionCase(0, 2))))
    b.adjacent(0).connexions.iterator.toList shouldBe List(DirectedConnexionCase(0, 2))
  }

  it should "connect" in {
    val a: Adjacency[Int, Connexion[Int], Unit] = UnorderedAdjacency.empty[Int, Connexion[Int], Unit] + (0 -> BaseConnexionsCase[Int, DirectedConnexionCase[Int]](Bag.create(DirectedConnexionCase(0, 1))))
    a.adjacent(0).connexions.iterator.next() shouldBe DirectedConnexionCase(0, 1)
    val b = a connect(0, DirectedConnexionCase(0, 2))
    val connexions = b.adjacent(0).connexions.iterator.toList
    connexions.contains(DirectedConnexionCase(0, 2)) shouldBe true
    connexions.contains(DirectedConnexionCase(0, 1)) shouldBe true
  }


  it should "dfs" in {
    val a: Adjacency[Int, Connexion[Int], Unit] = UnorderedAdjacency.empty[Int, Connexion[Int], Unit] + (0 -> BaseConnexionsCase[Int, DirectedConnexionCase[Int]](Bag.create(DirectedConnexionCase(0, 1))))
    a.adjacent(0).connexions.iterator.next() shouldBe DirectedConnexionCase(0, 1)
    val b = a connect(0, DirectedConnexionCase(0, 2))
    val connexions = b.adjacent(0).connexions.iterator.toList
    connexions.contains(DirectedConnexionCase(0, 2)) shouldBe true
    connexions.contains(DirectedConnexionCase(0, 1)) shouldBe true
    val visitor: PostVisitor[Int, Queue[Int]] = Visitor.createPost
    implicit val uIp: Properties[Int, Unit] = new com.phasmidsoftware.gryphon.expcore.Properties[Int, Unit] {
      def getProperties(v: Int): Unit = ()

      def setProperties(v: Int)(p: Unit): Unit = ()
    }
    val visited = b.dfs(visitor)(0)
    visited.journal.head shouldBe 0
  }

}
