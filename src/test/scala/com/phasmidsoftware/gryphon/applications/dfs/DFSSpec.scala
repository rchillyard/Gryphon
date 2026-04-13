/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.applications.dfs

import com.phasmidsoftware.gryphon.adjunct.DirectedGraph.triplesToTryGraph
import com.phasmidsoftware.gryphon.adjunct.{DirectedGraph, UndirectedGraph}
import com.phasmidsoftware.gryphon.core.*
import com.phasmidsoftware.gryphon.parse.GraphParser
import com.phasmidsoftware.gryphon.util.TryUsing
import com.phasmidsoftware.visitor.core.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import scala.io.Source
import scala.util.*

class DFSSpec extends AnyFlatSpec with should.Matchers:

  // dfsu.graph has 3 components:
  //   Component A: {0, 1, 2, 3, 4, 5, 6}  (7 vertices)
  //   Component B: {7, 8}                  (2 vertices)
  //   Component C: {9, 10, 11, 12}         (4 vertices)

  given Evaluable[Int, Int] with
    def evaluate(v: Int): Option[Int] = Some(v)

  private def withDfsuGraph[A](f: Graph[Int] => A): A =
    val p = new GraphParser[Int, Unit, EdgeType]
    val triedSource = Try(Source.fromResource("dfsu.graph"))
    TryUsing.tryIt(triedSource) { source =>
      p.parseSource[Triplet[Int, Unit, EdgeType]](p.parseTriple)(source)
    } match
      case Success(triplets) =>
        UndirectedGraph.triplesToTryGraph[Int, Unit](Vertex.createWithSet)(triplets) match
          case Success(graph: Graph[Int]) => f(graph)
          case Failure(x) => fail(x)
      case Failure(x) => fail(x)

  behavior of "DFS"

  it should "fail on empty graph" in {
    val graph: Graph[Int] = DirectedGraph[Int, Unit]
    val visitor = JournaledVisitor.withQueueJournal[Int, Int]
    an[Exception] should be thrownBy graph.dfs(visitor)(1)
  }

  it should "dfs Dijkstra graph visits all 8 vertices" in {
    val p = new GraphParser[Int, Double, EdgeType]
    val triedSource = Try(Source.fromResource("dijkstra.graph"))
    TryUsing.tryIt(triedSource) { source =>
      p.parseSource[Triplet[Int, Double, EdgeType]](p.parseTriple)(source)
    } match
      case Success(triplets) =>
        triplesToTryGraph[Int, Double](Vertex.createWithSet)(triplets) match
          case Success(graph: EdgeGraph[Int, Double] @unchecked) =>
            graph.vertexMap.map.size shouldBe 8
            graph.edges.size shouldBe 16
            val visitor = JournaledVisitor.withQueueJournal[Int, Int]
            val result = graph.dfsAll(visitor)
            result.result.map(_._1).toSet.size shouldBe 8
          case Failure(x) => fail("parse failed: ", x)
          case _ => fail("Graph is not an EdgeGraph")
      case Failure(x) => fail("parse failed", x)
  }

  it should "dfs on dfsu.graph visits all 13 vertices" in :
    withDfsuGraph { graph =>
      val visitor = JournaledVisitor.withQueueJournal[Int, Int]
      val result = graph.dfsAll(visitor)
      result.result.map(_._1).toSet.size shouldBe 13
    }

  it should "bfs on dfsu.graph from vertex 0 visits connected component" in :
    withDfsuGraph { graph =>
      val visitor = JournaledVisitor.withQueueJournal[Int, Int]
      val result = graph.bfs(visitor)(0)
      result.result.map(_._1).toSet should not be empty
    }

  it should "dfs from vertex 0 on dfsu.graph visits exactly component A" in :
    withDfsuGraph { graph =>
      val visitor = JournaledVisitor.withQueueJournal[Int, Int]
      val result = graph.dfs(visitor)(0)
      val visited = result.result.map(_._1).toSet
      visited shouldBe Set(0, 1, 2, 3, 4, 5, 6)
      visited should contain noneOf(7, 8, 9, 10, 11, 12)
    }

  it should "dfs from vertex 7 on dfsu.graph visits exactly component B" in :
    withDfsuGraph { graph =>
      val visitor = JournaledVisitor.withQueueJournal[Int, Int]
      val result = graph.dfs(visitor)(7)
      result.result.map(_._1).toSet shouldBe Set(7, 8)
    }

  it should "dfs from vertex 9 on dfsu.graph visits exactly component C" in :
    withDfsuGraph { graph =>
      val visitor = JournaledVisitor.withQueueJournal[Int, Int]
      val result = graph.dfs(visitor)(9)
      result.result.map(_._1).toSet shouldBe Set(9, 10, 11, 12)
    }

  it should "dfs post-order on dag.graph yields children before parents (QueueJournal)" in {
    val p = new GraphParser[Int, Double, EdgeType]
    val triedSource = Try(Source.fromResource("dag.graph"))
    TryUsing.tryIt(triedSource) { source =>
      p.parseSource[Triplet[Int, Double, EdgeType]](p.parseTriple)(source)
    } match
      case Success(triplets) =>
        triplesToTryGraph[Int, Double](Vertex.createWithSet)(triplets) match
          case Success(graph: Graph[Int]) =>
            val visitor = JournaledVisitor.withQueueJournal[Int, Int]
            val result = graph.dfs(visitor, DfsOrder.Post)(3)
            val postOrder = result.result.map(_._1).toList
            postOrder.last shouldBe 3
            postOrder.indexOf(2) should be < postOrder.indexOf(5)
            postOrder.indexOf(5) should be < postOrder.indexOf(3)
            postOrder.indexOf(0) should be < postOrder.indexOf(6)
            postOrder.indexOf(6) should be < postOrder.indexOf(3)
          case Failure(x) => fail("parse failed: ", x)
      case Failure(x) => fail("parse failed", x)
  }

  it should "dfs post-order on dag.graph yields children before parents (ListJournal)" in {
    val p = new GraphParser[Int, Double, EdgeType]
    val triedSource = Try(Source.fromResource("dag.graph"))
    TryUsing.tryIt(triedSource) { source =>
      p.parseSource[Triplet[Int, Double, EdgeType]](p.parseTriple)(source)
    } match
      case Success(triplets) =>
        triplesToTryGraph[Int, Double](Vertex.createWithSet)(triplets) match
          case Success(graph: Graph[Int]) =>
            val visitor = JournaledVisitor.withListJournal[Int, Int]
            val result = graph.dfs(visitor, DfsOrder.Post)(3)
            val postOrder = result.result.map(_._1).toList.reverse
            postOrder.last shouldBe 3
            postOrder.indexOf(2) should be < postOrder.indexOf(5)
            postOrder.indexOf(5) should be < postOrder.indexOf(3)
            postOrder.indexOf(0) should be < postOrder.indexOf(6)
            postOrder.indexOf(6) should be < postOrder.indexOf(3)
          case Failure(x) => fail("parse failed: ", x)
      case Failure(x) => fail("parse failed", x)
  }

  it should "dfsAll on dag.graph visits all 7 vertices" in {
    val p = new GraphParser[Int, Double, EdgeType]
    val triedSource = Try(Source.fromResource("dag.graph"))
    TryUsing.tryIt(triedSource) { source =>
      p.parseSource[Triplet[Int, Double, EdgeType]](p.parseTriple)(source)
    } match
      case Success(triplets) =>
        triplesToTryGraph[Int, Double](Vertex.createWithSet)(triplets) match
          case Success(graph: Graph[Int]) =>
            val visitor = JournaledVisitor.withQueueJournal[Int, Int]
            val result = graph.dfsAll(visitor)
            result.result.map(_._1).toSet shouldBe Set(0, 1, 2, 3, 4, 5, 6)
          case Failure(x) => fail("parse failed: ", x)
      case Failure(x) => fail("parse failed", x)
  }

  it should "dfs on single-vertex graph visits just that vertex" in {
    val vm = VertexMap[Int] + Vertex.createWithSet[Int](42)
    val graph = DirectedGraph[Int, Unit](vm)
    val visitor = JournaledVisitor.withQueueJournal[Int, Int]
    val result = graph.dfs(visitor)(42)
    result.result.map(_._1).toSet shouldBe Set(42)
  }