package com.phasmidsoftware.gryphon.core

import com.phasmidsoftware.gryphon.adjunct.{AttributedDirectedEdge, UndirectedEdge}
import com.phasmidsoftware.gryphon.util.TryUsing
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.net.URL
import scala.io.Source
import scala.util.{Failure, Success, Try}

class SerializableGraphSpec extends AnyFlatSpec with Matchers:

  behavior of "SerializableGraph"

  val dijkstraGraphPath = "dijkstra.graph"

  it should "createFromEdges" in {
    val edges: Seq[Edge[Double, Int]] = Seq(
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
      case Failure(e) => Failure(e)
    triedStrings.isSuccess shouldBe true
    triedStrings.get.size shouldBe 17
  }