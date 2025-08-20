package com.phasmidsoftware.gryphon.core

import com.phasmidsoftware.gryphon.adjunct.AttributedDirectedEdge
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class AdjacencySpec extends AnyFlatSpec with Matchers {

  behavior of "AdjacencyVertex"

  it should "vertex" in {
    val adjacency = AdjacencyVertex(Vertex.create(1))
    adjacency.vertex shouldBe 1
  }

  behavior of "AdjacencyEdge"

  it should "vertex" in {
    val vertex1 = Vertex.create(1)
    val vertex2 = Vertex.create(2)
    val adjacency1 = AdjacencyEdge(AttributedDirectedEdge[String, Int]("child", 1, 2))
    adjacency1.maybeEdge.map(_.attribute) shouldBe Some("child")
    adjacency1.vertex shouldBe 2
    val adjacency2 = AdjacencyEdge(AttributedDirectedEdge[String, Int]("child", 1, 2), flipped = true)
    adjacency2.maybeEdge.map(_.attribute) shouldBe Some("child")
    adjacency2.vertex shouldBe 1
  }

}
