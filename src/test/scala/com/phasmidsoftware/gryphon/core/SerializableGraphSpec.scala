package com.phasmidsoftware.gryphon.core

import com.phasmidsoftware.gryphon.adjunct.{AttributedDirectedEdge, DirectedGraph, UndirectedGraph}
import com.phasmidsoftware.gryphon.parse.GraphParser
import com.phasmidsoftware.gryphon.util.TryUsing
import java.net.URL
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scala.io.Source
import scala.util.{Failure, Success, Try}

class SerializableGraphSpec extends AnyFlatSpec with Matchers:

  behavior of "SerializableGraph"

  val dijkstraGraphPath = "dijkstra.graph"

  it should "createFromEdges" in {
    val edges: Seq[Edge[Int, Double]] = Seq(
      AttributedDirectedEdge(1.5, 0, 1),
      AttributedDirectedEdge(2.5, 1, 2)
    )
    val edgeList = EdgeList[Int, Double, EdgeType](edges)
    val sg: SerializableGraph[Int, Double, EdgeType] = edgeList
    sg.triplets.size shouldBe 2
    sg.triplets.head.from shouldBe 0
    sg.triplets.head.to shouldBe 1
    sg.triplets.head.maybeAttribute shouldBe Some(1.5)
  }

  it should "createFromVertexPairs" in {
    val pairs: Seq[(Int, Int, EdgeType)] = Seq((1, 2, Directed), (2, 3, Undirected))
    val vpl = VertexPairList[Int](pairs)
    vpl.triplets.size shouldBe 2
    vpl.triplets.head.from shouldBe 1
    vpl.triplets.head.to shouldBe 2
    vpl.triplets.head.edgeType shouldBe Directed
    vpl.triplets.last.edgeType shouldBe Undirected
  }

  it should "SerializableGraph.createFromTriplets round-trips" in {
    val triplets: Seq[Triplet[Int, Double, EdgeType]] = Seq(
      Triplet(0, 1, Some(3.14), Directed),
      Triplet(1, 2, Some(2.72), Undirected)
    )
    val sg = SerializableGraph.createFromTriplets(triplets)
    sg.triplets.size shouldBe 2
  }

  it should "serialize to a non-empty string" in {
    val triplets: Seq[Triplet[Int, Double, EdgeType]] = Seq(
      Triplet(0, 1, Some(1.0), Directed)
    )
    val sg = SerializableGraph.createFromTriplets(triplets)
    sg.serialize should not be empty
  }

  it should "Triplets.parse — read dijkstra.graph lines" in {
    val triedSource = Try(Source.fromResource(dijkstraGraphPath))
    val wsy: Try[Seq[String]] = TryUsing.trial(triedSource)(_.getLines().toSeq)
    wsy.isSuccess shouldBe true
    val ws = wsy.get
    ws.size shouldBe 17 // including first line as comment
  }

  it should "Triplets.parse 3 — read via URL" in {
    val url: URL = Thread.currentThread().getContextClassLoader.getResource(dijkstraGraphPath)
    val triedSource = Try(Source.fromURL(url, "UTF-8"))
    val triedStrings = triedSource match
      case Success(source) =>
        val result: Seq[String] = source.getLines().toSeq
        source.close()
        Success(result)
      case Failure(e) =>
        Failure(e)
    triedStrings.isSuccess shouldBe true
    triedStrings.get.size shouldBe 17
  }
  // =====================================================================================
  // Issue #135: the round trip.
  //
  // This is the test that settles the serialization format by construction -- whatever makes
  // parse ∘ serialize the identity IS the correct output -- and it is the test whose absence let
  // four defects sit undetected. What stood here before was
  //
  //     sg.serialize should not be empty
  //
  // which asserts that something was produced, not that it was right. serialize wrote
  // `0,1,Some(5.0)`: comma-separated where the parser wants whitespace, Scala's Option.toString
  // where it wants a bare number, and NO EDGE TYPE AT ALL, so directedness was lost. Nothing
  // could read back what the library wrote.
  //
  // The undirected case below is not redundant with the directed one. It is the one that pins
  // the edge type surviving: with `=` dropped, prim.graph comes back as eight Undefined-typed
  // triplets and the assertion fails, where the directed case could still pass by accident if
  // `>` happened to be the parser's default.

  private val parser = new GraphParser[Int, Double, EdgeType]

  private def tripletsOf(resource: String): Seq[Triplet[Int, Double, EdgeType]] =
    TryUsing.tryIt(Try(Source.fromResource(resource)))(s =>
      parser.parseSource[Triplet[Int, Double, EdgeType]](parser.parseTriple)(s)).get

  behavior of "serialize, as the inverse of the parser"

  it should "round-trip a directed graph" in {
    val original = tripletsOf("dijkstra.graph")
    DirectedGraph.triplesToTryGraph[Int, Double](Vertex.createWithSet)(original) match
      case Success(g: DirectedGraph[Int, Double] @unchecked) =>
        val unbuilt = g.unbuild[EdgeType]
        val reparsed = parser.parseStrings(parser.parseTriple)(unbuilt.serialize.linesIterator).get
        reparsed.toSet shouldBe unbuilt.triplets.toSet
      case other => fail(s"graph construction failed: $other")
  }

  it should "round-trip an undirected graph, keeping the edge type" in {
    val original = tripletsOf("prim.graph")
    UndirectedGraph.triplesToTryGraph[Int, Double](Vertex.createWithSet)(original) match
      case Success(g: UndirectedGraph[Int, Double] @unchecked) =>
        val unbuilt = g.unbuild[EdgeType]
        val reparsed = parser.parseStrings(parser.parseTriple)(unbuilt.serialize.linesIterator).get
        reparsed.toSet shouldBe unbuilt.triplets.toSet
        every(reparsed.map(_.edgeType)) shouldBe Undirected
      case other => fail(s"graph construction failed: $other")
  }

  it should "write the parser's own format" in {
    // Pinned explicitly as well as through the round trip, so a change to the format is visible
    // as a change to this line rather than only as a distant failure.
    val edges: Seq[Edge[Int, Double]] = Seq(AttributedDirectedEdge(5.0, 0, 1))
    EdgeList[Int, Double, EdgeType](edges).serialize shouldBe "0 > 1 5.0"
  }

  it should "omit the attribute entirely when there is none" in {
    // `maybeEdge` in the grammar is optional, so an absent attribute is absent from the line --
    // not the "None" the old implementation wrote.
    VertexPairList(Seq((0, 1, Undirected))).serialize shouldBe "0 = 1"
  }
