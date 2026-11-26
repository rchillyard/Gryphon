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
        }

      case Failure(x) =>
        fail("parse failed", x)
    }

  }

  // -------------------------------------------------------------------------------------
  // Issue #80: parseStrings' two arms. `ok` is true when there were no failures at all, or
  // when every failure was an EmptyStringException — blank lines are not an error. Anything
  // else makes the whole parse a Failure. Only the success arm was ever exercised, so the
  // Stryker4s trial found the `else` branch unasserted: nothing distinguished "tolerate the
  // blank lines" from "tolerate everything".

  behavior of "GraphParser.parseStrings"

  it should "fail, naming the input, when a line cannot be parsed" in {
    val p = new GraphParser[Int, Double, EdgeType]
    p.parseStrings(p.parseTriple)(Iterator("0 1 2.0", "this is not a triple")) match {
      case Failure(x: ParseException) =>
        x.getMessage should startWith("Bad input:")
      case other =>
        fail(s"expected a ParseException naming the bad input, got: $other")
    }
  }

  it should "tolerate blank lines, which fail with EmptyStringException" in {
    val p = new GraphParser[Int, Double, EdgeType]
    p.parseStrings(p.parseTriple)(Iterator("0 1 2.0", "", "1 2 3.0")) match {
      case Success(ts) =>
        ts.size shouldBe 2
      case other =>
        fail(s"expected the blank line to be tolerated, got: $other")
    }
  }

  it should "fail when a bad line accompanies blank lines" in {
    // The bad line must make the whole parse fail even though the blanks on their own would not:
    // `forall` over a mixed `bad` is false, which is the case that distinguishes the two arms.
    val p = new GraphParser[Int, Double, EdgeType]
    p.parseStrings(p.parseTriple)(Iterator("0 1 2.0", "", "not a triple")) should matchPattern {
      case Failure(_: ParseException) =>
    }
  }
}
