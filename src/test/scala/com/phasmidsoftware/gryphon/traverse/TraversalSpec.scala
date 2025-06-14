package com.phasmidsoftware.gryphon.traverse

import com.phasmidsoftware.gryphon.adjunct.UndirectedGraph
import com.phasmidsoftware.gryphon.core.*
import com.phasmidsoftware.gryphon.edgeFunc
import com.phasmidsoftware.gryphon.parse.GraphParser
import com.phasmidsoftware.gryphon.util.FP.sequence
import com.phasmidsoftware.gryphon.util.TryUsing
import com.phasmidsoftware.gryphon.visit.MappedJournalMap
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.io.Source
import scala.util.{Failure, Success, Try}

class TraversalSpec extends AnyFlatSpec with Matchers {

  behavior of "Traversal"

  case class VertexRecord(from: Int)

  it should "vertexMappedTraversalDfs 1" in {
    val tripletsUndirected: Seq[Triplet[Int, Unit, EdgeType]] = Seq(Triplet(1, 2, None, Undirected), Triplet(2, 3, None, Undirected))
    val vm: VertexMap[Int] = VertexMap[Int].addTriplets[Unit, EdgeType](Vertex.createWithSet, edgeFunc)(tripletsUndirected)
    val graph = UndirectedGraph[Int, Unit](vm)
    implicit object MappedJournalVT extends MappedJournalMap[Int, VertexRecord] {
      def fulfill(i: Int): VertexRecord = VertexRecord(i)
    }
    val traversal = graph.vertexMappedTraversalDfs(1)
    traversal match {
      case VertexTraversal(map) => map.size shouldBe 3
      case _ => fail("vertexMappedTraversalDfs failed")
    }
  }

  it should "vertexMappedTraversalDfs 2" in {
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
}
