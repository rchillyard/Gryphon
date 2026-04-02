package com.phasmidsoftware.gryphon.parse

import com.phasmidsoftware.gryphon.adjunct.DirectedGraph.triplesToTryGraph
import com.phasmidsoftware.gryphon.core.*
import com.phasmidsoftware.gryphon.util.TryUsing
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scala.io.{BufferedSource, Source}
import scala.util.{Failure, Success, Try}

class GraphParserSpec extends AnyFlatSpec with Matchers {

  behavior of "GraphParser"

  /**
   * dijkstra.graph: the file path to the graph definition file used for Dijkstra's algorithm parsing.
   */
  val dijkstraGraphPath = "dijkstra.graph"

  it should "parsePair 1" in {
    val p = new GraphParser[Int, Double, EdgeType]
    p.parsePair("1 2") should matchPattern { case Success((1, 2, None)) => }
  }

  it should "parsePair 2" in {
    val p = new GraphParser[String, Double, EdgeType]
    p.parsePair("A B") should matchPattern { case Success(("A", "B", None)) => }
  }

  it should "parseAll" in {
    val p = new GraphParser[String, Double, EdgeType]
    p.parseAll(p.pair, "A B") should matchPattern { case p.Success(("A", "B", None), _) => }
  }

  it should "parseTriple Undirected" in {
    val p = new GraphParser[Int, Double, EdgeType]
    p.parseTriple("1 = 2 3.14") should matchPattern { case Success(Triplet(1, 2, Some(3.14), Undirected)) => }
  }

  it should "parseTriple Directed" in {
    val p = new GraphParser[Int, Double, EdgeType]
    val triedMaybeTuple = p.parseTriple("1 2 3.14")
    triedMaybeTuple should matchPattern { case Success(Triplet(1, 2, Some(3.14), Directed)) => }
  }

  it should "parse Dijkstra" in {
    val p = new GraphParser[Int, Double, EdgeType]
    val triedSource: Try[BufferedSource] = Try(Source.fromResource(dijkstraGraphPath))
    val zsy = TryUsing.tryIt(triedSource) {
      (source: Source) => p.parseSource[Triplet[Int, Double, EdgeType]](p.parseTriple)(source)
    }
    zsy match {
      case Success(triplets) =>
        triplesToTryGraph[Int, Double](Vertex.createWithSet)(triplets) match {
          case Success(graph: EdgeGraph[_, _]) =>
            graph.vertexMap.map.size shouldBe 8
            graph.edges.size shouldBe 16
          case Failure(x) =>
            fail("parse failed: ", x)
          case _ => fail("parse failed: Graph is not an EdgeGraph")
        }

      case Failure(x) =>
        fail("parse failed", x)
    }

  }
}
