package com.phasmidsoftware.gryphon.core

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class VertexSpec extends AnyFlatSpec with should.Matchers {

    behavior of "Vertex"

    it should "attribute" in {
        Vertex.empty[String, Nothing, Unit]("A").attribute shouldBe "A"
    }

    it should "degree" in {
        Vertex.empty[String, Nothing, Unit]("A").degree shouldBe 0
        Vertex.empty[String, Nothing, Unit]("A").addEdge(DirectedEdgeCase("A", "B", "ab")).degree shouldBe 1
    }

    it should "adjacent" in {
        Vertex.empty[String, Nothing, Unit]("A").adjacent shouldBe AdjacencyList.empty
        Vertex.empty[String, Nothing, Unit]("A").addEdge(DirectedEdgeCase("A", "B", "ab")).adjacent shouldBe AdjacencyList(Seq(DirectedEdgeCase("A", "B", "ab")))
    }

    it should "addEdge" in {
        val a = Vertex.empty[String, DirectedEdgeCase[String, String], Unit]("A")
        //noinspection RedundantNewCaseClass
        a.addEdge(DirectedEdgeCase("A", "B", "ab")) shouldBe new VertexCase[String, DirectedEdge[String, String], Unit]("A", AdjacencyList(Seq(DirectedEdgeCase("A", "B", "ab")))) // leave "new" intact.
    }
}
