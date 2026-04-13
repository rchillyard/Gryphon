package com.phasmidsoftware.gryphon.core

import com.phasmidsoftware.gryphon.adjunct.{AttributedDirectedEdge, DirectedEdge, UndirectedGraph}
import com.phasmidsoftware.gryphon.parse.GraphParser
import com.phasmidsoftware.gryphon.traverse.{Connexions, VertexTraversalResult}
import com.phasmidsoftware.gryphon.util.TryUsing
import com.phasmidsoftware.visitor.core.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import scala.io.Source
import scala.util.{Failure, Success, Try}

class TraversableSpec extends AnyFlatSpec with should.Matchers:

  behavior of "Traversable"

  private val edgeList = EdgeList[Int, String, EdgeType](
    Seq(
      com.phasmidsoftware.gryphon.adjunct.AttributedDirectedEdge("A", 1, 2),
      com.phasmidsoftware.gryphon.adjunct.AttributedDirectedEdge("B", 2, 3)
    )
  )
  private val target: VertexMap[Int] = VertexMap[Int].addEdges(edgeList)

  private given Evaluable[Int, Int] with
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

  it should "adjacentVertices" in {
    target.adjacentVertices(1).toSet shouldBe Set(2)
    target.adjacentVertices(2).toSet shouldBe Set(3)
    target.adjacentVertices(3).toSet shouldBe Set.empty
  }

  it should "filteredAdjacentVertices — keep all" in {
    target.filteredAdjacentVertices(_ => true)(1).toSet shouldBe Set(2)
  }

  it should "filteredAdjacentVertices — keep none" in {
    target.filteredAdjacentVertices(_ => false)(1).toSet shouldBe Set.empty
  }

  it should "filteredAdjacencies" in {
    val adjs = target.filteredAdjacencies(_ => true)(1).toList
    adjs.size shouldBe 1
    adjs.head.vertex shouldBe 2
  }

  it should "dfs visits all vertices in chain" in {
    val visitor = JournaledVisitor.withQueueJournal[Int, Int]
    val result = target.dfs(visitor)(1)
    result.result.map(_._1).toSet shouldBe Set(1, 2, 3)
  }

  it should "dfs visits in pre-order" in {
    val visitor = JournaledVisitor.withQueueJournal[Int, Int]
    val result = target.dfs(visitor)(1)
    result.result.iterator.next()._1 shouldBe 1
  }

  it should "bfs visits all vertices in chain" in {
    val visitor = JournaledVisitor.withQueueJournal[Int, Int]
    val result = target.bfs(visitor)(1)
    result.result.map(_._1).toSet shouldBe Set(1, 2, 3)
  }

  it should "bfs stops early at goal" in {
    val visitor = JournaledVisitor.withQueueJournal[Int, Int]
    val result = target.bfs(visitor)(1, goal = _ == 2)
    result.result.map(_._1).toSet should contain(2)
    result.result.map(_._1).toSet should not contain 3
  }

  it should "dfsAll visits all vertices in disconnected graph" in {
    val vm = VertexMap[Int]
            .+(com.phasmidsoftware.gryphon.adjunct.AttributedDirectedEdge("X", 1, 2))
            .+(com.phasmidsoftware.gryphon.adjunct.AttributedDirectedEdge("Y", 10, 11))
    val visitor = JournaledVisitor.withQueueJournal[Int, Int]
    val result = vm.dfsAll(visitor)
    result.result.map(_._1).toSet shouldBe Set(1, 2, 10, 11)
  }

  it should "vertexMappedTraversalDfs" in :
    withDfsuGraph { graph =>
      graph.vertexMappedTraversalDfs(v => v.toString)(0) match
        case Success(VertexTraversalResult(map)) =>
          map.size shouldBe 7
          map(0) shouldBe "0"
          map(6) shouldBe "6"
        case Success(x) => fail(s"unexpected result: $x")
        case Failure(exception) => fail(exception)
    }

  it should "ProtoConnexions.create" in :
    withDfsuGraph { graph =>
      val connexions: Connexions[Int, Unit] = Connexions.create[Int, Unit](graph)(0)
      connexions match
        case Connexions(map) =>
          map.size shouldBe 6
          val values: Seq[DirectedEdge[Int, Unit]] = map.values.toSeq
          values.contains(AttributedDirectedEdge(None, 0, 2)) shouldBe true
          values.contains(AttributedDirectedEdge(None, 1, 0)) shouldBe false
          values.contains(AttributedDirectedEdge(None, 0, 5)) shouldBe true
    }