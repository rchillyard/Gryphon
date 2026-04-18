package com.phasmidsoftware.gryphon.core

import com.phasmidsoftware.gryphon.adjunct.{AttributedDirectedEdge, UndirectedEdge}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class EdgeSpec extends AnyFlatSpec with Matchers:

  behavior of "EdgeType"

  it should "Directed.oneWay is true" in {
    Directed.oneWay shouldBe true
  }

  it should "Undirected.oneWay is false" in {
    Undirected.oneWay shouldBe false
  }

  it should "Undefined.oneWay is false" in {
    Undefined.oneWay shouldBe false
  }

  behavior of "AttributedDirectedEdge"

  it should "have correct white, black and attribute" in {
    val e = AttributedDirectedEdge(3.14, 1, 2)
    e.white shouldBe 1
    e.black shouldBe 2
    e.attribute shouldBe 3.14
  }

  it should "have Directed edgeType" in {
    AttributedDirectedEdge("w", 0, 1).edgeType shouldBe Directed
  }

  behavior of "UndirectedEdge"

  it should "have correct white, black and attribute" in {
    val e = UndirectedEdge(99, "A", "B")
    e.white shouldBe "A"
    e.black shouldBe "B"
    e.attribute shouldBe 99
  }

  it should "have Undirected edgeType" in {
    UndirectedEdge(0.0, 1, 2).edgeType shouldBe Undirected
  }

  it should "equals" in {
    val e1 = UndirectedEdge(99, "A", "B")
    val e2 = UndirectedEdge(99, "B", "A")
    val e3 = UndirectedEdge(100, "A", "B")
    e1 shouldBe e2
    e1 should not be e3
  }
  it should "equals Set" in {
    val e1 = UndirectedEdge(99, "A", "B")
    val e2 = UndirectedEdge(99, "B", "A")
    val e3 = UndirectedEdge(100, "A", "B")
    Set(e1, e2) shouldBe Set(e2, e1)
    Set(e1, e2) should not be Set(e3, e1)
  }
  it should "hashCode" in {
    val e1 = UndirectedEdge(99, "A", "B")
    val e2 = UndirectedEdge(99, "B", "A")
    val e3 = UndirectedEdge(100, "A", "B")
    e1.hashCode shouldBe e2.hashCode
    e1.hashCode should not be e3.hashCode
  }

  it should "hashCode Set" in {
    val e1 = UndirectedEdge(99, "A", "B")
    val e2 = UndirectedEdge(99, "B", "A")
    val e3 = UndirectedEdge(100, "A", "B")
    Set(e1, e2).hashCode() shouldBe Set(e2, e1).hashCode()
    Set(e1, e2).hashCode() should not be Set(e2, e3).hashCode()
  }

  behavior of "EdgeType.ParseableEdgeType"

  it should "parse '>' as Directed" in {
    EdgeType.ParseableEdgeType.parse(">").get shouldBe Directed
  }

  it should "parse '=' as Undirected" in {
    EdgeType.ParseableEdgeType.parse("=").get shouldBe Undirected
  }

  it should "parse unknown string as Undefined" in {
    EdgeType.ParseableEdgeType.parse("?").get shouldBe Undefined
  }