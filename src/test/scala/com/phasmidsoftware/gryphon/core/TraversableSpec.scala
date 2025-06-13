package com.phasmidsoftware.gryphon.core

import com.phasmidsoftware.gryphon.adjunct.UndirectedGraph
import com.phasmidsoftware.gryphon.parse.GraphParser
import com.phasmidsoftware.gryphon.traverse.VertexTraversal
import com.phasmidsoftware.gryphon.util.FP.sequence
import com.phasmidsoftware.gryphon.util.TryUsing
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

import scala.io.Source
import scala.util.{Failure, Success, Try}

class TraversableSpec extends AnyFlatSpec with should.Matchers {

  case class VertexRecord(from: Int)

  behavior of "TraversableSpec"

  it should "filteredAdjacencies" in {

  }

  it should "vertexTraversalDfs" in {
    val p = new GraphParser[Int, Unit, EdgeType]
    val triedSource = Try(Source.fromResource("dfsu.graph"))
    val wsy: Try[Seq[String]] = TryUsing.trial(triedSource)(_.getLines().toSeq)
    wsy.isSuccess shouldBe true
    val ws = wsy.get filterNot (w => w.startsWith("//"))
    sequence(for (w <- ws) yield p.parseTriple(w)) match {
      case Success(triplets) =>
        UndirectedGraph.triplesToTryGraph(triplets) match {
          case Success(graph: Graph[_]) =>
            val vertexFunction: Int => VertexRecord = i => VertexRecord(i)
            val traversal = graph.vertexTraversalDfs(vertexFunction)(0)
            traversal match {
              case VertexTraversal(map) => map.size shouldBe 7 // TODO CHECK this
              case _ => fail("vertexTraversalDfs failed")
            }
          case Failure(exception) => fail(exception)
        }
      case Failure(exception) => fail(exception)
    }
  }

  it should "dfsAll" in {

  }

  it should "undiscoveredAdjacencies" in {

  }

  it should "bfs" in {

  }

  it should "dfs" in {

  }

  it should "adjacencies" in {

  }

}
