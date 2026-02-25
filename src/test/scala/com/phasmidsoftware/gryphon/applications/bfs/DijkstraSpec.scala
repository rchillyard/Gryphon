package com.phasmidsoftware.gryphon.applications.bfs

import com.phasmidsoftware.gryphon.adjunct.DirectedGraph
import com.phasmidsoftware.gryphon.adjunct.DirectedGraph.triplesToTryGraph
import com.phasmidsoftware.gryphon.core.{EdgeType, Graph, Triplet, Vertex}
import com.phasmidsoftware.gryphon.parse.GraphParser
import com.phasmidsoftware.gryphon.util.TryUsing
import com.phasmidsoftware.visitor.core.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

import scala.io.Source
import scala.util.*

class DijkstraSpec extends AnyFlatSpec with should.Matchers:

  behavior of "Dijkstra"

  it should "visit all vertices via bfs on dijkstra graph" in {
    val p = new GraphParser[Int, Double, EdgeType]
    val triedSource = Try(Source.fromResource("dijkstra.graph"))
    val zsy: Try[Seq[Triplet[Int, Double, EdgeType]]] = TryUsing.tryIt(triedSource) {
      source => p.parseSource[Triplet[Int, Double, EdgeType]](p.parseTriple)(source)
    }
    zsy match
      case Success(triplets) =>
        given Numeric[Double] = scala.math.Numeric.DoubleIsFractional

        given Evaluable[Int, Int] with
          def evaluate(v: Int): Option[Int] = Some(v)
        triplesToTryGraph[Int, Double](Vertex.createWithSet)(triplets) match
          case Success(graph: DirectedGraph[Int, Double]) =>
            val visitor = JournaledVisitor.withQueueJournal[Int, Int]
            val result = graph.bfs(visitor)(0)
            result.result.map(_._1).toSet.size shouldBe 8
          case Failure(x) => fail("parse failed", x)
          case _ => fail("not a DirectedGraph")
      case Failure(x) => fail("parse failed", x)
  }

  it should "visit all vertices via dfs on dijkstra graph" in {
    val p = new GraphParser[Int, Double, EdgeType]
    val triedSource = Try(Source.fromResource("dijkstra.graph"))
    val zsy: Try[Seq[Triplet[Int, Double, EdgeType]]] = TryUsing.tryIt(triedSource) {
      source => p.parseSource[Triplet[Int, Double, EdgeType]](p.parseTriple)(source)
    }
    zsy match
      case Success(triplets) =>
        given Numeric[Double] = scala.math.Numeric.DoubleIsFractional

        given Evaluable[Int, Int] with
          def evaluate(v: Int): Option[Int] = Some(v)
        triplesToTryGraph[Int, Double](Vertex.createWithSet)(triplets) match
          case Success(graph: DirectedGraph[Int, Double]) =>
            val visitor = JournaledVisitor.withQueueJournal[Int, Int]
            // DFS from 0 on a connected graph should visit all 8 vertices
            val result = graph.dfsAll(visitor)
            result.result.map(_._1).toSet.size shouldBe 8
          case Failure(x) => fail("parse failed", x)
          case _ => fail("not a DirectedGraph")
      case Failure(x) => fail("parse failed", x)
  }