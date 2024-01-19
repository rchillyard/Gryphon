/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.oldcore

import com.phasmidsoftware.gryphon.visit.{IterableVisitor, Visitor}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class GraphSpec extends AnyFlatSpec with should.Matchers {

  private val red: Color = Color("red")
  private val vertexA = "A"
  private val vertexB = "B"

  behavior of "UndirectedGraphCase"

  it should "create an empty graph" in {
    val target: UndirectedGraph[String, String, UndirectedEdge[String, String], Unit] = UndirectedGraph.createUnordered("test")
    target.edges shouldBe Nil
    target.vertices shouldBe Set.empty
  }

  it should "create a graph with one empty vertex" in {
    val graph: UndirectedGraph[String, String, UndirectedEdge[String, String], Unit] = UndirectedGraph.createUnordered("test")
    val target = graph.addVertex(vertexA)
    target.vertices shouldBe Set(vertexA)
  }

  it should "create a graph with one edge" in {
    val graph: UndirectedGraph[String, Color, UndirectedEdge[String, Color], Unit] = UndirectedGraph.createUnordered("test")
    val edge: UndirectedEdgeCase[String, Color] = UndirectedEdgeCase(vertexA, vertexB, red)
    val target = graph.addEdge(edge)
    target.vertices.size shouldBe 2
  }

  it should "create an ordered graph with one edge" in {
    val graph: UndirectedGraph[String, Color, UndirectedEdge[String, Color], Unit] = UndirectedGraph.createOrdered("test")
    val edge: UndirectedEdgeCase[String, Color] = UndirectedEdgeCase(vertexA, vertexB, red)
    val target = graph.addEdge(edge)
    target.vertices shouldBe Set(vertexA, vertexB)
    target.vertices.size shouldBe 2
    target.edgeAttributes.headOption shouldBe Some(red)
  }

  it should "dfs" in {
    import com.phasmidsoftware.gryphon.visit.Journal._
    val graph: UndirectedGraph[String, Int, UndirectedEdge[String, Int], Unit] = UndirectedGraph.createUnordered("test")
    val target = graph.addEdge(UndirectedEdgeCase("A", "B", 1)).addEdge(UndirectedEdgeCase("B", "C", 2))
    val visitor = Visitor.createPostQueue[String]
    target.dfs(visitor)("A") match {
      case v: IterableVisitor[String, _] => v.iterator.toSeq shouldBe Seq("C", "B", "A")
    }
  }

//    it should "bfs" in {
//        import com.phasmidsoftware.gryphon.visit.Journal._
//        val graph: UndirectedGraph[String, Int, UndirectedEdge[String, Int], Unit] = UndirectedGraph.createUnordered("test")
//        val target = graph.addEdge(UndirectedEdgeCase("A", "B", 1)).addEdge(UndirectedEdgeCase("A", "D", 3)).addEdge(UndirectedEdgeCase("A", "C", 2))
//        val visitor = Visitor.createPreQueue[String]
//        val result = target.bfs(visitor)("A")(_ => false)
//        result match {
//            case v: IterableVisitor[String, _] => v.iterator.toSeq shouldBe Seq("A", "C", "D", "B")
//        }
//    }


  behavior of "DirectedGraphCase"

  it should "create an empty graph" in {
    val target: DirectedGraph[String, String, DirectedEdge[String, String], Unit] = DirectedGraph("test")
    target.edges shouldBe Nil
    target.vertices shouldBe Set.empty
  }

  it should "create a graph with one empty vertex" in {
    val graph: DirectedGraph[String, String, DirectedEdge[String, String], Unit] = DirectedGraph("test")
    val target = graph.addVertex(vertexA)
    target.vertices shouldBe Set(vertexA)
  }

  it should "dfs pre-order" in {
    import com.phasmidsoftware.gryphon.visit.Journal._
    val graph: DirectedGraph[String, Int, DirectedEdge[String, Int], Unit] = DirectedGraph("test")
    val target = graph.addEdge(DirectedEdgeCase("A", "B", 1)).addEdge(DirectedEdgeCase("B", "C", 2))
    val visitor = Visitor.createPreQueue[String]
    target.dfs(visitor)("A") match {
      case v: IterableVisitor[String, _] => v.iterator.toSeq shouldBe Seq("A", "B", "C")
    }
  }

  it should "dfs reverse post-order" in {
    import com.phasmidsoftware.gryphon.visit.Journal._
    val graph: DirectedGraph[String, Int, DirectedEdge[String, Int], Unit] = DirectedGraph("test")
    val target = graph.addEdge(DirectedEdgeCase("A", "B", 1)).addEdge(DirectedEdgeCase("B", "C", 2))
    val visitor = Visitor.reversePostList[String]
    target.dfs(visitor)("A") match {
      case v: IterableVisitor[String, _] => v.iterator.toSeq shouldBe Seq("A", "B", "C")
    }
  }

//    it should "bfs" in {
//        import com.phasmidsoftware.gryphon.visit.Journal._
//        val graph: DirectedGraph[String, Int, DirectedEdge[String, Int], Unit] = DirectedGraph("test")
//        val target = graph.addEdge(DirectedEdgeCase("A", "B", 1)).addEdge(DirectedEdgeCase("B", "D", 3)).addEdge(DirectedEdgeCase("A", "C", 2))
//        val visitor = Visitor.createPreQueue[String]
//        val result = target.bfs(visitor)("A")(_ => false)
//        result match {
//            case v: IterableVisitor[String, _] => v.iterator.toSeq shouldBe Seq("A", "C", "B", "D")
//        }
//    }

  it should "bfs with PriorityQueue" in {
    import com.phasmidsoftware.gryphon.visit.Journal._
    val graph: DirectedGraph[String, Int, DirectedEdge[String, Int], Unit] = DirectedGraph("test")
    val target = graph.addEdge(DirectedEdgeCase("A", "B", 1)).addEdge(DirectedEdgeCase("B", "D", 3)).addEdge(DirectedEdgeCase("A", "C", 2))
    val visitor = Visitor.createPreQueue[String]
    import com.phasmidsoftware.gryphon.visit.PriorityQueueable._
    val result = target.bfsMutable(visitor)("A")(v => false)
    result match {
      case v: IterableVisitor[String, _] => v.iterator.toSeq shouldBe Seq("A", "C", "B", "D")
    }
  }

  it should "path" in {
    val vertexMap: VertexMap[String, DirectedEdgeCase[String, Int], Unit] = OrderedVertexMap.empty[String, DirectedEdgeCase[String, Int], Unit].addEdge("A", DirectedEdgeCase("A", "B", 1)).addEdge("B", DirectedEdgeCase("B", "C", 2)).addVertex("C")
    val target = DirectedGraphCase[String, Int, DirectedEdgeCase[String, Int], Unit]("test", vertexMap)
    target.path("A", "B") shouldBe List("A", "B")
    target.path("B", "A") shouldBe Nil
    target.path("A", "C") shouldBe List("A", "B", "C")
    target.path("C", "A") shouldBe Nil
  }

  it should "isPath" in {
    val vertexMap: VertexMap[String, DirectedEdgeCase[String, Int], Unit] = OrderedVertexMap.empty[String, DirectedEdgeCase[String, Int], Unit].addEdge("A", DirectedEdgeCase("A", "B", 1)).addEdge("B", DirectedEdgeCase("B", "C", 2)).addVertex("C")
    val target = DirectedGraphCase[String, Int, DirectedEdgeCase[String, Int], Unit]("test", vertexMap)
    target.isPath("A", "B") shouldBe true
    target.isPath("B", "A") shouldBe false
    target.isPath("A", "C") shouldBe true
    target.isPath("C", "A") shouldBe false
  }

}
