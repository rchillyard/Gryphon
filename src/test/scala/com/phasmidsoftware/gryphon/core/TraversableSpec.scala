package com.phasmidsoftware.gryphon.core

import com.phasmidsoftware.gryphon.adjunct.UndirectedGraph
import com.phasmidsoftware.gryphon.parse.GraphParser
import com.phasmidsoftware.gryphon.traverse.{Connexions, Traversal, VertexTraversal}
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
            val connexions: Connexions[Int] = Connexions.create(graph)(0)
            println(connexions)
            connexions match {
              case Connexions(map) =>
                map.size shouldBe 6
                val values: Seq[(Int, Int)] = map.values.toSeq
                values.contains(0 -> 2) shouldBe true
                values.contains(1 -> 0) shouldBe false
                values.contains(0 -> 5) shouldBe true // NOTE this might fail for some random seeds in VertexMap
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
