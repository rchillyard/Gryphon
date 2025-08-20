package com.phasmidsoftware.gryphon.core

import com.phasmidsoftware.gryphon.adjunct.{AttributedDirectedEdge, DirectedEdge, UndirectedGraph}
import com.phasmidsoftware.gryphon.parse.GraphParser
import com.phasmidsoftware.gryphon.traverse.{Connexions, VertexTraversal}
import com.phasmidsoftware.gryphon.util.FP.sequence
import com.phasmidsoftware.gryphon.util.TryUsing
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import org.scalatest.matchers.should.Matchers.shouldBe

import scala.io.Source
import scala.util.{Failure, Success, Try}

class TraversableSpec extends AnyFlatSpec with should.Matchers {

  case class VertexRecord(from: Int)

  behavior of "Traversable"

  it should "filteredAdjacentVertices" in {
    // TODO implement me
  }

  it should "vertexMappedTraversalDfs" in {
    val p = new GraphParser[Int, Unit, EdgeType]
    val triedSource = Try(Source.fromResource("dfsu.graph"))
    val wsy: Try[Seq[String]] = TryUsing.trial(triedSource)(_.getLines().toSeq)
    wsy.isSuccess shouldBe true
    val ws = wsy.get filterNot (w => w.startsWith("//"))
    sequence(for (w <- ws) yield p.parseTriple(w)) match {
      case Success(triplets) =>
        UndirectedGraph.triplesToTryGraph(triplets) match {
          case Success(graph: Graph[_]) =>
            graph.vertexMappedTraversalDfs(v => v.toString)(0) match {
              case Success(VertexTraversal(map)) =>
                map.size shouldBe 7 // TODO CHECK this
                map(0) shouldBe "0"
                map(6) shouldBe "6"
              case Success(x) =>
                fail(s"vertexMappedTraversalDfs failed: $x")
              case Failure(exception) =>
                fail(exception)
            }
          case Failure(exception) => fail(exception)
        }
      case Failure(exception) => fail(exception)
    }
  }

  it should "Connexions.create" in {
    val p = new GraphParser[Int, Unit, EdgeType]
    val triedSource = Try(Source.fromResource("dfsu.graph"))
    val wsy: Try[Seq[String]] = TryUsing.trial(triedSource)(_.getLines().toSeq)
    wsy.isSuccess shouldBe true
    val ws = wsy.get filterNot (w => w.startsWith("//"))
    sequence(for (w <- ws) yield p.parseTriple(w)) match {
      case Success(triplets) =>
        UndirectedGraph.triplesToTryGraph(triplets) match {
          case Success(graph: Graph[_]) =>
            val connexions: Connexions[Int, Unit] = Connexions.create[Int, Unit](graph)(0)
            println(connexions)
            connexions match {
              case Connexions(map) =>
                map.size shouldBe 6
                val values: Seq[DirectedEdge[Unit, Int]] = map.values.toSeq
                values.contains(AttributedDirectedEdge(None, 0, 2)) shouldBe true
                values.contains(AttributedDirectedEdge(None, 1, 0)) shouldBe false
                values.contains(AttributedDirectedEdge(None, 0, 5)) shouldBe true // NOTE this might fail for some random seeds in VertexMap
            }
          case Failure(exception) => fail(exception)
        }
      case Failure(exception) => fail(exception)
    }
  }

  it should "dfsAll" in {
    // TODO implement me
  }

  it should "undiscoveredAdjacentVertices" in {
    // TODO implement me
  }

  it should "bfs" in {
    // TODO implement me
  }

  it should "dfs" in {
    // TODO implement me
  }

  it should "adjacencies" in {
    // TODO implement me
  }

}
