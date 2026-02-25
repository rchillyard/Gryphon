/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.applications.dfs

import com.phasmidsoftware.gryphon.adjunct.DirectedGraph.triplesToTryGraph
import com.phasmidsoftware.gryphon.adjunct.{DirectedGraph, UndirectedGraph}
import com.phasmidsoftware.gryphon.core.*
import com.phasmidsoftware.gryphon.parse.GraphParser
import com.phasmidsoftware.gryphon.util.FP.sequence
import com.phasmidsoftware.gryphon.util.TryUsing
import com.phasmidsoftware.visitor.core.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

import scala.io.Source
import scala.util.*

class DFSSpec extends AnyFlatSpec with should.Matchers:

  behavior of "DFS"

  it should "fail on empty graph" in {
    val graph: Graph[Int] = DirectedGraph[Int, Unit]
    given Evaluable[Int, Int] with
      def evaluate(v: Int): Option[Int] = Some(v)
    val visitor = JournaledVisitor.withQueueJournal[Int, Int]
    an[Exception] should be thrownBy graph.dfs(visitor)(1)
  }

  it should "dfs Dijkstra graph visits all 8 vertices" in {
    val p = new GraphParser[Int, Double, EdgeType]
    val triedSource = Try(Source.fromResource("dijkstra.graph"))
    val zsy: Try[Seq[Triplet[Int, Double, EdgeType]]] = TryUsing.tryIt(triedSource) {
      source => p.parseSource[Triplet[Int, Double, EdgeType]](p.parseTriple)(source)
    }
    zsy match
      case Success(triplets) =>
        given Evaluable[Int, Int] with
          def evaluate(v: Int): Option[Int] = Some(v)
        triplesToTryGraph[Int, Double](Vertex.createWithSet)(triplets) match
          case Success(graph: EdgeGraph[Int, Double] @unchecked) =>
            graph.vertexMap.map.size shouldBe 8
            graph.edges.size shouldBe 16
            val visitor = JournaledVisitor.withQueueJournal[Int, Int]
            val result = graph.dfsAll(visitor)
            result.result.map(_._1).toSet.size shouldBe 8
          case Failure(x) => fail("parse failed: ", x)
          case _ => fail("Graph is not an EdgeGraph")
      case Failure(x) => fail("parse failed", x)
  }

  it should "dfs on dfsu.graph visits all 13 vertices" in {
    val p = new GraphParser[Int, Unit, EdgeType]
    val triedSource = Try(Source.fromResource("dfsu.graph"))
    val wsy: Try[Seq[String]] = TryUsing.trial(triedSource)(_.getLines().toSeq)
    wsy.isSuccess shouldBe true
    val ws = wsy.get filterNot (_.startsWith("//"))
    sequence(for w <- ws yield p.parseTriple(w)) match
      case Success(triplets) =>
        UndirectedGraph.triplesToTryGraph(triplets) match
          case Success(graph: Graph[Int]) =>
            given Evaluable[Int, Int] with
              def evaluate(v: Int): Option[Int] = Some(v)
            val visitor = JournaledVisitor.withQueueJournal[Int, Int]
            val result = graph.dfsAll(visitor)
            result.result.map(_._1).toSet.size shouldBe 13
          case Failure(x) => fail(x)
      case Failure(x) => fail(x)
  }

  it should "bfs on dfsu.graph from vertex 0 visits connected component" in {
    val p = new GraphParser[Int, Unit, EdgeType]
    val triedSource = Try(Source.fromResource("dfsu.graph"))
    val wsy: Try[Seq[String]] = TryUsing.trial(triedSource)(_.getLines().toSeq)
    wsy.isSuccess shouldBe true
    val ws = wsy.get filterNot (_.startsWith("//"))
    sequence(for w <- ws yield p.parseTriple(w)) match
      case Success(triplets) =>
        UndirectedGraph.triplesToTryGraph(triplets) match
          case Success(graph: Graph[Int]) =>
            given Evaluable[Int, Int] with
              def evaluate(v: Int): Option[Int] = Some(v)
            val visitor = JournaledVisitor.withQueueJournal[Int, Int]
            val result = graph.bfs(visitor)(0)
            result.result.map(_._1).toSet should not be empty
          case Failure(x) => fail(x)
      case Failure(x) => fail(x)
  }