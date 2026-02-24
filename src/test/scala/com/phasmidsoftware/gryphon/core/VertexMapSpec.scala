package com.phasmidsoftware.gryphon.core

import com.phasmidsoftware.gryphon.adjunct.{AttributedDirectedEdge, UndirectedEdge}
import com.phasmidsoftware.gryphon.edgeFunc
import com.phasmidsoftware.visitor.core.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class VertexMapSpec extends AnyFlatSpec with Matchers:

  behavior of "VertexMap"

  private val edgeList: EdgeList[Int, String, EdgeType] = EdgeList(Seq(AttributedDirectedEdge("A", 1, 2), AttributedDirectedEdge("B", 2, 3)))
  private val tripletsDirected: Seq[Triplet[Int, Unit, EdgeType]] = Seq(Triplet(1, 2, None, Directed), Triplet(2, 3, None, Directed))
  private val tripletsUndirected: Seq[Triplet[Int, Unit, EdgeType]] = Seq(Triplet(1, 2, None, Undirected), Triplet(2, 3, None, Undirected))
  private val vertexPairListDirected: VertexPairList[Int] = VertexPairList(Seq((1, 2, Directed), (2, 3, Directed)))
  private val vertexPairListUndirected: VertexPairList[Int] = VertexPairList(Seq((1, 2, Undirected), (2, 3, Undirected)))
  private val defaultVertex: Vertex[Int] = Vertex.createWithBag(0)

  // Shared Evaluable instance for traversal tests
  private given Evaluable[Int, Int] with
    def evaluate(v: Int): Option[Int] = Some(v)

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
    val updated = target + AttributedDirectedEdge("C", 4, 2)
    updated.contains(4) shouldBe true
    updated.apply(4).attribute shouldBe 4
    // SimpleVertex is aliased as DiscoverableVertex for backward compatibility
    updated.get(2) should matchPattern { case Some(SimpleVertex(2, Unordered_Set(_))) => }
    updated(2).adjacencies.iterator.hasNext shouldBe false
    updated(4).adjacencies.iterator.to(List) shouldBe List(AdjacencyEdge(AttributedDirectedEdge("C", 4, 2)))
  }

  it should "empty $plus UndirectedEdge" in {
    val target: VertexMap[Int] = VertexMap[Int]
    val updated = target + UndirectedEdge("C", 4, 2)
    updated.contains(4) shouldBe true
    updated.apply(4).attribute shouldBe 4
    updated.get(2) should matchPattern { case Some(SimpleVertex(2, Unordered_Set(_))) => }
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
    target.get(1) should matchPattern { case Some(SimpleVertex(1, _)) => }
  }

  it should "getOrElse" in {
    val target = VertexMap[Int].addEdges(edgeList)
    target.getOrElse(1, defaultVertex) should matchPattern { case SimpleVertex(1, _) => }
    target.getOrElse(4, defaultVertex) shouldBe defaultVertex
  }

  it should "keySet" in {
    val target = VertexMap[Int].addEdges(edgeList)
    target.keySet shouldBe Set(1, 2, 3)
  }

  it should "dfs" in {
    val target = VertexMap[Int].addEdges(edgeList)
    val visitor = JournaledVisitor.withQueueJournal[Int, Int]
    val result = target.dfs(visitor)(1)
    // QueueJournal preserves visit order; directed 1->2->3 so expect all 3 vertices
    val journal = result.result
    journal.size shouldBe 3
    journal.iterator.next()._1 shouldBe 1
    journal.iterator.toList.last._1 shouldBe 3
  }

  it should "simple bfs" in {
    val target = VertexMap[Int].addEdges(edgeList)
    val goal: Int => Boolean = _ == 3
    val visitor = JournaledVisitor.withQueueJournal[Int, Int]
    val result = target.bfs(visitor)(1, goal)
    val journal = result.result
    journal.size shouldBe 3
    journal.iterator.next()._1 shouldBe 1
    journal.iterator.toList.last._1 shouldBe 3
  }