package com.phasmidsoftware.gryphon.traverse

import com.phasmidsoftware.gryphon.adjunct.UndirectedGraph
import com.phasmidsoftware.gryphon.core.*
import com.phasmidsoftware.gryphon.edgeFunc
import com.phasmidsoftware.gryphon.parse.GraphParser
import com.phasmidsoftware.gryphon.util.FP.sequence
import com.phasmidsoftware.gryphon.util.TryUsing
import com.phasmidsoftware.visitor.core.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scala.io.Source
import scala.util.{Failure, Success, Try}

class TraversalResultSpec extends AnyFlatSpec with Matchers:

  behavior of "TraversalResult"

  it should "vertexMappedTraversalDfs on small undirected graph" in {
    val tripletsUndirected: Seq[Triplet[Int, Unit, EdgeType]] =
      Seq(Triplet(1, 2, None, Undirected), Triplet(2, 3, None, Undirected))
    val vm = VertexMap[Int].addTriplets[Unit, EdgeType](Vertex.createWithSet, edgeFunc)(tripletsUndirected)
    val graph = UndirectedGraph[Int, Unit](vm)
    graph.vertexMappedTraversalDfs(v => v * 10)(1) match
      case Success(VertexTraversalResult(map)) =>
        map.size shouldBe 3
        map(1) shouldBe 10
        map(2) shouldBe 20
        map(3) shouldBe 30
      case Success(x) => fail(s"unexpected result: $x")
      case Failure(exception) => fail(exception)
  }

  it should "vertexMappedTraversalDfs on dfsu.graph" in {
    val p = new GraphParser[Int, Unit, EdgeType]
    val triedSource = Try(Source.fromResource("dfsu.graph"))
    val wsy: Try[Seq[String]] = TryUsing.trial(triedSource)(_.getLines().toSeq)
    wsy.isSuccess shouldBe true
    val ws = wsy.get filterNot (_.startsWith("//"))
    sequence(for w <- ws yield p.parseTriple(w)) match
      case Success(triplets) =>
        UndirectedGraph.triplesToTryGraph(triplets) match
          case Success(graph: Graph[_]) =>
            graph.vertexMappedTraversalDfs(v => v.toString)(0) match
              case Success(VertexTraversalResult(map)) =>
                map.size shouldBe 7
                map(0) shouldBe "0"
              case Success(x) => fail(s"unexpected result: $x")
              case Failure(exception) => fail(exception)
          case Failure(exception) => fail(exception)
      case Failure(exception) => fail(exception)
  }

  it should "vertexMappedTraversalBfs on small undirected graph" in {
    val tripletsUndirected: Seq[Triplet[Int, Unit, EdgeType]] =
      Seq(Triplet(1, 2, None, Undirected), Triplet(2, 3, None, Undirected))
    val vm = VertexMap[Int].addTriplets[Unit, EdgeType](Vertex.createWithSet, edgeFunc)(tripletsUndirected)
    val graph = UndirectedGraph[Int, Unit](vm)
    graph.vertexMappedTraversalBfs(v => v * 10)(1) match
      case Success(VertexTraversalResult(map)) =>
        map.size shouldBe 3
        map(1) shouldBe 10
        map(2) shouldBe 20
        map(3) shouldBe 30
      case Success(x) => fail(s"unexpected result: $x")
      case Failure(exception) => fail(exception)
  }