package littlegryphon.core

import littlegryphon.adjunct.DirectedEdge
import littlegryphon.visit.{PreVisitor, Visitor}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.matchers.should.Matchers.shouldBe

import scala.collection.immutable.Queue

class VertexMapSpec extends AnyFlatSpec with Matchers {

  behavior of "VertexMap"

  private val v1: Vertex[Int] = Vertex.createByVertex(1)
  private val v2: Vertex[Int] = Vertex.createByVertex(2)
  private val v3: Vertex[Int] = Vertex.createByVertex(3)
  private val edgeList: EdgeList[Int, String] = EdgeList(Seq(DirectedEdge("A", v1, v2), DirectedEdge("B", v2, v3)))
  private val defaultVertex: Vertex[Int] = Vertex.createByVertex(0)

  it should "implement contains, apply, and vertices" in {
    val target = VertexMap.create(edgeList)
    target.vertices.size shouldBe 3
    target.contains(1) shouldBe true
    target.contains(2) shouldBe true
    target.contains(3) shouldBe true
    target.apply(1).attribute shouldBe 1
    target.apply(2).attribute shouldBe 2
    target.apply(3).attribute shouldBe 3
  }

  it should "$plus" in {
    val target = VertexMap.create(edgeList)
    val updated = target + Vertex.createByVertex(4)
    updated.contains(4) shouldBe true
    updated.apply(4).attribute shouldBe 4
  }

  it should "applyOrElse" in {
    val target = VertexMap.create(edgeList)
    val vertex1: Vertex[Int] = target.applyOrElse(1, _ => defaultVertex)
    vertex1.attribute shouldBe 1
    val vertex0: Vertex[Int] = target.applyOrElse(4, _ => defaultVertex)
    vertex0.attribute shouldBe 0
  }
  it should "get" in {
    val target = VertexMap.create(edgeList)
    target.get(1) shouldBe Some(v1)
  }

  it should "getOrElse" in {
    val target = VertexMap.create(edgeList)
    target.getOrElse(1, defaultVertex) shouldBe v1
    target.getOrElse(4, defaultVertex) shouldBe defaultVertex
  }

  it should "keySet" in {
    val target = VertexMap.create(edgeList)
    target.keySet shouldBe Set(1, 2, 3)
  }

  it should "dfs" in {
    val visitor: PreVisitor[Int, Queue[Int]] = Visitor.createPre[Int]
    val target = VertexMap.create(edgeList)
    val result: Visitor[Int, Queue[Int]] = target.dfs(visitor)(1)
    println(result)
    //    result.journal.size shouldBe 3
    //    result.journal.head shouldBe 1
  }


}
