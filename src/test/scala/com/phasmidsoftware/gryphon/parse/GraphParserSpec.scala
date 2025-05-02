package com.phasmidsoftware.gryphon.parse

import com.phasmidsoftware.gryphon.adjunct.DirectedGraph
import com.phasmidsoftware.gryphon.adjunct.DirectedGraph.triplesToTryGraph
import com.phasmidsoftware.gryphon.core.{Directed, EdgeGraph, EdgeType, Undirected}
import com.phasmidsoftware.gryphon.util.FP.sequence
import com.phasmidsoftware.gryphon.util.{FP, TryUsing}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.collection.immutable.Seq
import scala.io.Source
import scala.util.*

class GraphParserSpec extends AnyFlatSpec with Matchers {

  behavior of "GraphParser"

  val dijkstraGraphPath = "dijkstra.graph"

  it should "parsePair 1" in {
    val p = new GraphParser[Int, Double, EdgeType]
    p.parsePair("1 2") should matchPattern { case Success((1, 2, None)) => }
  }

  it should "parsePair 2" in {
    val p = new GraphParser[String, Double, EdgeType]
    p.parsePair("A B") should matchPattern { case Success(("A", "B", None)) => }
  }

  it should "parseTriple Undirected" in {
    val p = new GraphParser[Int, Double, EdgeType]
    p.parseTriple("1 = 2 3.14") should matchPattern { case Success(Some((1, 2, 3.14, Undirected))) => }
  }

  it should "parseTriple Directed" in {
    val p = new GraphParser[Int, Double, EdgeType]
    val triedMaybeTuple = p.parseTriple("1 2 3.14")
    triedMaybeTuple should matchPattern { case Success(Some((1, 2, 3.14, Directed))) => }
  }

  it should "parse Dijkstra" in {
    val p = new GraphParser[Int, Double, EdgeType]
    val triedSource = Try(Source.fromResource(dijkstraGraphPath))
    val wsy: Try[Seq[String]] = TryUsing.trial(triedSource)(_.getLines().toSeq)
    wsy.isSuccess shouldBe true
    val ws = wsy.get
    sequence(for (w <- ws) yield p.parseTriple(w)) match {
      case Success(maybeTuples) =>
        FP.sequence(maybeTuples) match {
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
          case None => fail("parse failed: no triples")
        }

      case Failure(x) =>
        fail("parse failed", x)
    }

  }
}
