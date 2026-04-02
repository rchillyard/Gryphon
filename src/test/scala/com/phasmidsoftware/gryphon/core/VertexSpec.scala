package com.phasmidsoftware.gryphon.core

import com.phasmidsoftware.visitor.core.given
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class VertexSpec extends AnyFlatSpec with should.Matchers:

  behavior of "Vertex"

  // DiscoverableVertex is a type alias for SimpleVertex — both names are valid
  val target: DiscoverableVertex[Int] = Vertex.create(1)

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
    val result = vertex2 + AdjacencyVertex(2)
    result.attribute shouldBe 2
    val adjacencies: Unordered_Bag[Adjacency[Int]] = result.adjacencies.asInstanceOf[Unordered_Bag[Adjacency[Int]]]
    adjacencies should matchPattern { case Unordered_Bag(_) => }
    adjacencies.size shouldBe 1
    adjacencies.elements.contains(AdjacencyVertex(2)) shouldBe true
  }

  it should "$plus 2" in {
    val vertex2 = Vertex.createWithSet(2)
    val result = vertex2 + AdjacencyVertex(1) + AdjacencyVertex(3) + AdjacencyVertex(4)
    result.attribute shouldBe 2
    val adjacencies: Unordered_Set[Adjacency[Int]] = result.adjacencies.asInstanceOf[Unordered_Set[Adjacency[Int]]]
    adjacencies should matchPattern { case Unordered_Set(_) => }
    adjacencies.size shouldBe 3
    adjacencies.elements.contains(AdjacencyVertex(target)) shouldBe true
    adjacencies.iterator.size
  }

  it should "render SimpleVertex" in {
    Vertex.createWithSet(42).render shouldBe "v42"
  }

  behavior of "RelaxableVertex"

  it should "have no cost initially" in {
    val v = Vertex.createRelaxableWithSet[Int, Double](5)
    v.attribute shouldBe 5
    v.maybeR shouldBe None
  }

  it should "relax to a lower cost" in {
    val v = Vertex.createRelaxableWithSet[Int, Double](1)
    v.relax(3.5)
    v.maybeR shouldBe Some(3.5)
  }

  it should "not relax to a higher cost" in {
    val v = Vertex.createRelaxableWithSet[Int, Double](1)
    v.relax(2.0)
    v.relax(5.0)
    v.maybeR shouldBe Some(2.0)
  }

  it should "compare by cost" in {
    val v1 = Vertex.createRelaxableWithSet[Int, Double](1)
    val v2 = Vertex.createRelaxableWithSet[Int, Double](2)
    v1.relax(1.0)
    v2.relax(3.0)
    v1.compare(v2) should be < 0
  }

  it should "render RelaxableVertex" in {
    val v = Vertex.createRelaxableWithSet[Int, Double](7)
    v.render shouldBe "v7(maybeR=None)"
    v.relax(2.5)
    v.render shouldBe "v7(maybeR=Some(2.5))"
  }

  it should "preserve adjacencies through unit" in {
    val v: RelaxableVertex[Int, Double] = Vertex.createRelaxableWithSet[Int, Double](3)
    val withAdj: RelaxableVertex[Int, Double] = v.unit(v.adjacencies + AdjacencyVertex(99))
    withAdj.relax(1.0)
    withAdj.adjacencies.size shouldBe 1
    withAdj.maybeR shouldBe Some(1.0)
  }