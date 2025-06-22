package com.phasmidsoftware.gryphon.core

import com.phasmidsoftware.gryphon.adjunct.{DirectedEdge, UndirectedEdge}
import com.phasmidsoftware.gryphon.edgeFunc
import com.phasmidsoftware.gryphon.visit.Visitor
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.matchers.should.Matchers.shouldBe

import scala.collection.immutable.Queue
import scala.util.Using

class VertexMapSpec extends AnyFlatSpec with Matchers {

  behavior of "VertexMap"

  private val v1: Vertex[Int] = Vertex.createWithBag(1)
  private val v2: Vertex[Int] = Vertex.createWithBag(2)
  private val v3: Vertex[Int] = Vertex.createWithBag(3)
  private val edgeList: EdgeList[Int, String, EdgeType] = EdgeList(Seq(DirectedEdge("A", 1, 2), DirectedEdge("B", 2, 3)))
  private val tripletsDirected: Seq[Triplet[Int, Unit, EdgeType]] = Seq(Triplet(1, 2, None, Directed), Triplet(2, 3, None, Directed))
  private val tripletsUndirected: Seq[Triplet[Int, Unit, EdgeType]] = Seq(Triplet(1, 2, None, Undirected), Triplet(2, 3, None, Undirected))
  private val vertexPairListDirected: VertexPairList[Int] = VertexPairList(Seq((1, 2, Directed), (2, 3, Directed)))
  private val vertexPairListUndirected: VertexPairList[Int] = VertexPairList(Seq((1, 2, Undirected), (2, 3, Undirected)))
  private val defaultVertex: Vertex[Int] = Vertex.createWithBag(0)

  it should "implement createVerticesFromTriplet undirected" in {
    val target: VertexMap[Int] = VertexMap[Int].addTriplets[Unit, EdgeType](Vertex.createWithSet, edgeFunc)(tripletsUndirected)
    target.vertices.size shouldBe 3
    target.contains(1) shouldBe true
    target.contains(2) shouldBe true
    target.contains(3) shouldBe true
    println(target)
    val v1 = target.apply(1)
    val v2 = target.apply(2)
    val v3 = target.apply(3)
    v1.attribute shouldBe 1
    v2.attribute shouldBe 2
    v3.attribute shouldBe 3
    v2.adjacencies.iterator.size shouldBe 2
  }

  it should "implement createVerticesFromTriplet directed" in {
    val target: VertexMap[Int] = VertexMap[Int].addTriplets[Unit, EdgeType](Vertex.createWithSet, edgeFunc)(tripletsDirected)
    target.vertices.size shouldBe 3
    target.contains(1) shouldBe true
    target.contains(2) shouldBe true
    target.contains(3) shouldBe true
    println(target)
    val v1 = target.apply(1)
    val v2 = target.apply(2)
    val v3 = target.apply(3)
    v1.attribute shouldBe 1
    v2.attribute shouldBe 2
    v3.attribute shouldBe 3
    v2.adjacencies.iterator.size shouldBe 1
  }

  it should "implement createFromVertexPairList, contains, apply, and vertices 1" in {
    val target = VertexMap.createFromVertexPairList(vertexPairListDirected) // 1 -> 2, 2 -> 3
    target.vertices.size shouldBe 3
    target.contains(1) shouldBe true
    target.contains(2) shouldBe true
    target.contains(3) shouldBe true
    println(target)
    val v1 = target.apply(1)
    val v2 = target.apply(2)
    val v3 = target.apply(3)
    v1.attribute shouldBe 1
    v2.attribute shouldBe 2
    v3.attribute shouldBe 3
    v2.adjacencies.iterator.size shouldBe 1
  }

  it should "implement createFromVertexPairList, contains, apply, and vertices 2" in {
    val target = VertexMap.createFromVertexPairList(vertexPairListUndirected) // 1 -> 2, 2 -> 3
    target.vertices.size shouldBe 3
    target.contains(1) shouldBe true
    target.contains(2) shouldBe true
    target.contains(3) shouldBe true
    println(target)
    val v1 = target.apply(1)
    val v2 = target.apply(2)
    val v3 = target.apply(3)
    v1.attribute shouldBe 1
    v2.attribute shouldBe 2
    v3.attribute shouldBe 3
    v2.adjacencies.iterator.size shouldBe 2
  }

  it should "empty $plus Vertex" in {
    val target: VertexMap[Int] = VertexMap[Int]
    val updated = target + Vertex.createWithBag(4)
    updated.contains(4) shouldBe true
    updated.apply(4).attribute shouldBe 4
  }

  it should "$plus Vertex" in {
    val target = VertexMap[Int].addEdges(edgeList)
    val updated = target + Vertex.createWithBag(4)
    updated.contains(4) shouldBe true
    updated.apply(4).attribute shouldBe 4
  }

  it should "empty $plus DirectedEdge" in {
    val target: VertexMap[Int] = VertexMap[Int]
    val updated = target + DirectedEdge("C", 4, 2)
    updated.contains(4) shouldBe true
    updated.apply(4).attribute shouldBe 4
    updated.get(2) should matchPattern { case Some(Vertex(2, Unordered_Set(_))) => }
    updated(2).adjacencies.iterator.hasNext shouldBe false
    updated(4).adjacencies.iterator.to(List) shouldBe List(AdjacencyEdge(DirectedEdge("C", 4, 2)))
  }

  it should "empty $plus UndirectedEdge" in {
    val target: VertexMap[Int] = VertexMap[Int]
    val updated = target + UndirectedEdge("C", 4, 2)
    updated.contains(4) shouldBe true
    updated.apply(4).attribute shouldBe 4
    updated.get(2) should matchPattern { case Some(Vertex(2, Unordered_Set(_))) => }
    updated(2).adjacencies.iterator.to(List) shouldBe List(AdjacencyEdge(UndirectedEdge("C", 4, 2), true))
    updated(4).adjacencies.iterator.to(List) shouldBe List(AdjacencyEdge(UndirectedEdge("C", 4, 2)))
  }

  it should "$plus Edge" in {
    val target = VertexMap[Int].addEdges(edgeList)
    val updated = target + Vertex.createWithBag(4)
    updated.contains(4) shouldBe true
    updated.apply(4).attribute shouldBe 4
  }

  it should "applyOrElse" in {
    val target = VertexMap[Int].addEdges(edgeList)
    val vertex1: Vertex[Int] = target.applyOrElse(1, _ => defaultVertex)
    vertex1.attribute shouldBe 1
    val vertex0: Vertex[Int] = target.applyOrElse(4, _ => defaultVertex)
    vertex0.attribute shouldBe 0
  }
  it should "get" in {
    val target = VertexMap[Int].addEdges(edgeList)
    target.get(1) should matchPattern { case Some(Vertex(1, _)) => }
  }

  it should "getOrElse" in {
    val target = VertexMap[Int].addEdges(edgeList)
    target.getOrElse(1, defaultVertex) should matchPattern { case Vertex(1, _) => }
    target.getOrElse(4, defaultVertex) shouldBe defaultVertex
  }

  it should "keySet" in {
    val target = VertexMap[Int].addEdges(edgeList)
    target.keySet shouldBe Set(1, 2, 3)
  }

  it should "dfs" in {
    Using(Visitor.createPre[Int]) {
      visitor =>
        val target = VertexMap[Int].addEdges(edgeList)
        val result: Visitor[Int, Queue[Int]] = target.dfs(visitor)(1)
        val journal = result.journals.head
        journal.size shouldBe 3
        journal.head shouldBe 1
        journal.last shouldBe 3
    }
  }

  it should "simple bfs" in {
    Using(Visitor.createPre[Int]) {
      visitor =>
        val target = VertexMap[Int].addEdges(edgeList)
        val result: Visitor[Int, Queue[Int]] = target.bfs(visitor)(1)(x => x == 3)
        val journal = result.journals.head
        journal.size shouldBe 3
        journal.head shouldBe 1
        journal.last shouldBe 3
    }
  }

  it should "complex bfs" in {
    Using(Visitor.createPre[Int]) {
      visitor =>
        val target = VertexMap[Int].addEdges(edgeList)
        val result: Visitor[Int, Queue[Int]] = target.bfs(visitor)(1)(x => x == 3)
        val journal = result.journals.head
        journal.size shouldBe 3
        journal.head shouldBe 1
        journal.last shouldBe 3
    }
  }
}
