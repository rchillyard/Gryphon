package com.phasmidsoftware.gryphon.core

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class VertexSpec extends AnyFlatSpec with should.Matchers {

  behavior of "Vertex"
  val target: Vertex[Int] = Vertex.create(1)

  it should "discovered" in {
    target.discovered shouldBe false
    target.discovered = true
    target.discovered shouldBe true
  }

  it should "adjacencies" in {
    target.adjacencies shouldBe Unordered_Bag(Bag.empty)
  }

  it should "attribute" in {
    target.attribute shouldBe 1
  }

  it should "createWithBag" in {
    val iv: Vertex[Int] = Vertex.createWithBag(2)
    iv.attribute shouldBe 2
    iv.adjacencies shouldBe Unordered_Bag(Bag.empty)
  }

  it should "createWithSet" in {
    val iv: Vertex[Int] = Vertex.createWithSet(2)
    iv.attribute shouldBe 2
    iv.adjacencies shouldBe Unordered_Set(Set.empty)
  }

  it should "$plus 1" in {
    val vertex2 = Vertex.create(2)
    val result = vertex2 + AdjacencyVertex(target)
    result.attribute shouldBe 2
    val adjacencies: Unordered_Bag[Adjacency[Int]] = result.adjacencies.asInstanceOf[Unordered_Bag[Adjacency[Int]]]
    adjacencies should matchPattern { case Unordered_Bag(_) => }
    adjacencies.size shouldBe 1
    adjacencies.elements.contains(AdjacencyVertex(target)) shouldBe true
  }

  it should "$plus 2" in {
    val vertex2 = Vertex.createWithSet(2)
    val vertex3 = Vertex.createWithSet(3)
    val vertex4 = Vertex.createWithSet(4)
    val result = vertex2 + AdjacencyVertex(target) + AdjacencyVertex(vertex3) + AdjacencyVertex(vertex4)
    result.attribute shouldBe 2
    val adjacencies: Unordered_Set[Adjacency[Int]] = result.adjacencies.asInstanceOf[Unordered_Set[Adjacency[Int]]]
    adjacencies should matchPattern { case Unordered_Set(_) => }
    adjacencies.size shouldBe 3
    adjacencies.elements.contains(AdjacencyVertex(target)) shouldBe true
    adjacencies.iterator.size
  }
}
