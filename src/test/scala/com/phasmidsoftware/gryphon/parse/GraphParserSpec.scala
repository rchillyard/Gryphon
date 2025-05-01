package com.phasmidsoftware.gryphon.parse

import com.phasmidsoftware.gryphon.adjunct.DirectedGraph
import com.phasmidsoftware.gryphon.adjunct.DirectedGraph.triplesToTryGraph
import com.phasmidsoftware.gryphon.core.EdgeGraph
import com.phasmidsoftware.gryphon.util.FP.sequence
import com.phasmidsoftware.gryphon.util.TryUsing
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.collection.immutable.Seq
import scala.io.Source
import scala.util.*

class GraphParserSpec extends AnyFlatSpec with Matchers {

  behavior of "GraphParser"

  val dijkstraGraphPath = "dijkstra.graph"

  it should "parsePair 1" in {
    val p = new GraphParser[Int, Double]
    p.parsePair("1 2") should matchPattern { case Some((1, 2)) => }
  }

  it should "parsePair 2" in {
    val p = new GraphParser[String, Double]
    p.parsePair("A B") should matchPattern { case Some(("A", "B")) => }
  }

  it should "parseTriple" in {
    val p = new GraphParser[Int, Double]
    p.parseTriple("1 2 3.14") should matchPattern { case Some((1, 2, 3.14)) => }
  }

  it should "parse Dijkstra" in {
    val p = new GraphParser[Int, Double]
    val triedSource = Try(Source.fromResource(dijkstraGraphPath))
    val wsy: Try[Seq[String]] = TryUsing.trial(triedSource)(_.getLines().toSeq)
    wsy.isSuccess shouldBe true
    val ws = wsy.get
    sequence(for (w <- ws) yield p.parseTriple(w)) match {
      case Some(triples) =>
        triplesToTryGraph(triples) match {
          case Success(graph: EdgeGraph[_, _]) =>
            println(graph.edges)
            graph.vertexMap.map.size shouldBe 8
            graph.edges.size shouldBe 16
          case Failure(x) =>
            fail("parse failed: ", x)
          case _ => fail("parse failed: Graph is not an EdgeGraph")
        }

      case None => fail("parse failed")
    }

  }
}
