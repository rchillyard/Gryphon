package com.phasmidsoftware.gryphon.core

import com.phasmidsoftware.gryphon.adjunct.UndirectedGraph
import com.phasmidsoftware.gryphon.parse.GraphParser
import com.phasmidsoftware.gryphon.traverse.VertexTraversal
import com.phasmidsoftware.gryphon.util.FP.sequence
import com.phasmidsoftware.gryphon.util.TryUsing
import com.phasmidsoftware.gryphon.visit.{IterableJournalQueue, MappedJournalMap}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import org.scalatest.matchers.should.Matchers.shouldBe

import scala.io.Source
import scala.util.{Failure, Success, Try}

class TraversableSpec extends AnyFlatSpec with should.Matchers {

  case class VertexRecord(from: Int)

  behavior of "TraversableSpec"

  it should "filteredAdjacencies" in {

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
            implicit object MappedJournalVT extends MappedJournalMap[Int, VertexRecord] {
              def fulfill(k: Int): VertexRecord = VertexRecord(k)
            }
            val traversal = graph.vertexMappedTraversalDfs(0)
            traversal match {
              case VertexTraversal(map) => map.size shouldBe 7 // TODO CHECK this
              case _ => fail("vertexMappedTraversalDfs failed")
            }
          case Failure(exception) => fail(exception)
        }
      case Failure(exception) => fail(exception)
    }
  }

  // FIXME 
  ignore should "vertexVertexIterableTraversalDfs" in {
    val p = new GraphParser[Int, Unit, EdgeType]
    val triedSource = Try(Source.fromResource("dfsu.graph"))
    val wsy: Try[Seq[String]] = TryUsing.trial(triedSource)(_.getLines().toSeq)
    wsy.isSuccess shouldBe true
    val ws = wsy.get filterNot (w => w.startsWith("//"))
    sequence(for (w <- ws) yield p.parseTriple(w)) match {
      case Success(triplets) =>
        UndirectedGraph.triplesToTryGraph(triplets) match {
          case Success(graph: Graph[_]) =>
            implicit object IterableJournalQueue extends IterableJournalQueue[(Int, Int)]
            val traversal = graph.vertexVertexIterableTraversalDfs(0)
            println(traversal)
            traversal match {
              case VertexTraversal(map) =>
                map.size shouldBe 6 // TODO CHECK this
                val values: Seq[(Int, Int)] = map.values.toSeq
                values.contains(0 -> 2) shouldBe true
                values.contains(1 -> 0) shouldBe false
                (values.contains(4 -> 6) || values.contains(6 -> 4)) shouldBe true
              case _ =>
                fail("vertexMappedTraversalDfs failed")
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
