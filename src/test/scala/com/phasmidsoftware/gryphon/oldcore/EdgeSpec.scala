/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.oldcore

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class EdgeSpec extends AnyFlatSpec with should.Matchers {

  private val red: Color = Color("red")
  private val blue: Color = Color("blue")

  behavior of "VertexPairCase"

  it should "vertices" in {
    val target: VertexPairCase[Color] = VertexPairCase(red, blue)
    target.vertices shouldBe(red, blue)
  }

  it should "attribute" in {
    val target: VertexPairCase[Color] = VertexPairCase(red, blue)
    target.attribute shouldBe()
  }

  it should "toString" in {
    val target: VertexPairCase[Color] = VertexPairCase(red, blue)
    target.toString shouldBe "Color(red):Color(blue)"
  }

  behavior of "DirectedEdgeCase"

  it should "apply" in {
    val target: DirectedEdgeCase[Color, Color] = DirectedEdgeCase(red, blue, Color("green"))
    //noinspection RedundantNewCaseClass
    target shouldBe new DirectedEdgeCase(red, blue, Color("green")) // leave "new" intact
  }

  it should "from" in {
    val target: DirectedEdgeCase[Color, String] = DirectedEdgeCase(red, blue, "isa")
    target.from shouldBe red
  }

  it should "to" in {
    val target: DirectedEdgeCase[String, String] = DirectedEdgeCase("A", "B", "isa")
    target.to shouldBe "B"
  }

  it should "vertices" in {
    val target: DirectedEdgeCase[String, String] = DirectedEdgeCase("A", "B", "isa")
    target.vertices shouldBe("A", "B")
  }

  it should "attribute" in {
    val target: DirectedEdgeCase[String, String] = DirectedEdgeCase("A", "B", "isa")
    target.attribute shouldBe "isa"
  }

  it should "toString" in {
    val target: DirectedEdgeCase[String, String] = DirectedEdgeCase("A", "B", "isa")
    target.toString shouldBe "A--(isa)-->B"
  }

  behavior of "DirectedOrderedEdgeCase"

  it should "from" in {
    val target: DirectedOrderedEdgeCase[String, String] = DirectedOrderedEdgeCase("A", "B", "isa")
    target.from shouldBe "A"
  }

  it should "to" in {
    val target: DirectedOrderedEdgeCase[String, String] = DirectedOrderedEdgeCase("A", "B", "isa")
    target.to shouldBe "B"
  }

  it should "vertices" in {
    val target: DirectedOrderedEdgeCase[String, String] = DirectedOrderedEdgeCase("A", "B", "isa")
    target.vertices shouldBe("A", "B")
  }

  it should "attribute" in {
    val target: DirectedOrderedEdgeCase[String, String] = DirectedOrderedEdgeCase("A", "B", "isa")
    target.attribute shouldBe "isa"
  }

  it should "compare with other edge" in {
    val target: DirectedOrderedEdgeCase[String, Int] = DirectedOrderedEdgeCase("A", "B", 1)
    val comparand: DirectedOrderedEdgeCase[String, Int] = DirectedOrderedEdgeCase("A", "B", 2)
    target.compare(comparand) shouldBe -1
  }

  it should "toString" in {
    val target: DirectedOrderedEdgeCase[String, String] = DirectedOrderedEdgeCase("A", "B", "isa")
    target.toString shouldBe "A--(isa)-->B"
  }

  behavior of "UndirectedEdgeCase"

  it should "apply" in {
    val target: UndirectedEdgeCase[String, Color] = UndirectedEdgeCase("A", "B", Color("C"))
    //noinspection RedundantNewCaseClass
    target shouldBe new UndirectedEdgeCase("A", "B", Color("C")) // leave "new" intact
  }

  it should "v1" in {
    val target: UndirectedEdgeCase[String, String] = UndirectedEdgeCase("A", "B", "isa")
    target.v1 shouldBe "A"
  }

  it should "v2" in {
    val target: UndirectedEdgeCase[String, String] = UndirectedEdgeCase("A", "B", "isa")
    target.v2 shouldBe "B"
  }

  it should "vertices" in {
    UndirectedEdgeCase("A", "B", "isa").vertices shouldBe("A", "B")
    UndirectedEdgeCase("B", "A", "isa").vertices shouldBe("A", "B")
  }

  it should "vertex" in {
    UndirectedEdgeCase("A", "B", "isa").vertex shouldBe "A"
    UndirectedEdgeCase("B", "A", "isa").vertex shouldBe "B"
  }

  it should "other" in {
    val ab = UndirectedEdgeCase("A", "B", "isa")
    ab.other(ab.vertex) shouldBe Some("B")
    val ba = UndirectedEdgeCase("B", "A", "has")
    ba.other(ba.vertex) shouldBe Some("A")
  }

  it should "attribute" in {
    val target: UndirectedEdgeCase[String, String] = UndirectedEdgeCase("A", "B", "isa")
    target.attribute shouldBe "isa"
  }

  it should "toString" in {
    val target: UndirectedEdgeCase[String, String] = UndirectedEdgeCase("A", "B", "isa")
    target.toString shouldBe "A<--(isa)-->B"
  }

  behavior of "UndirectedOrderedEdgeCase"

  it should "v1" in {
    val target: UndirectedOrderedEdgeCase[String, String] = UndirectedOrderedEdgeCase("A", "B", "isa")
    target.v1 shouldBe "A"
  }

  it should "v2" in {
    val target: UndirectedOrderedEdgeCase[String, String] = UndirectedOrderedEdgeCase("A", "B", "isa")
    target.v2 shouldBe "B"
  }

  it should "vertices" in {
    val target: UndirectedOrderedEdgeCase[String, String] = UndirectedOrderedEdgeCase("A", "B", "isa")
    target.vertices shouldBe("A", "B")
  }

  it should "attribute" in {
    val target: UndirectedOrderedEdgeCase[String, String] = UndirectedOrderedEdgeCase("A", "B", "isa")
    target.attribute shouldBe "isa"
  }

  it should "vertex" in {
    UndirectedOrderedEdgeCase("A", "B", "isa").vertex shouldBe "A"
    UndirectedOrderedEdgeCase("B", "A", "isa").vertex shouldBe "B"
  }

  it should "other" in {
    val ab = UndirectedOrderedEdgeCase("A", "B", "isa")
    ab.other(ab.vertex) shouldBe Some("B")
    val ba = UndirectedOrderedEdgeCase("B", "A", "has")
    ba.other(ba.vertex) shouldBe Some("A")
    ba.other("C") shouldBe None
  }

  it should "otherVertex" in {
    val ab = UndirectedOrderedEdgeCase("A", "B", "isa")
    ab.otherVertex(ab.vertex) shouldBe "B"
    val ba = UndirectedOrderedEdgeCase("B", "A", "has")
    ba.otherVertex(ba.vertex) shouldBe "A"
    a[GraphException] should be thrownBy ba.otherVertex("C")
  }

  it should "compare with other edge" in {
    val target: UndirectedOrderedEdgeCase[String, Int] = UndirectedOrderedEdgeCase("A", "B", 1)
    val comparand: UndirectedOrderedEdgeCase[String, Int] = UndirectedOrderedEdgeCase("A", "B", 2)
    target.compare(comparand) shouldBe -1
  }

  it should "toString" in {
    val target: UndirectedOrderedEdgeCase[String, String] = UndirectedOrderedEdgeCase("A", "B", "isa")
    target.toString shouldBe "A<--(isa)-->B"
  }

}

/**
 * Case class for testing where we have an attribute that does not have an order.
 *
 * @param name the name of the color.
 */
case class Color(name: String)
