/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.applications.dfs

import com.phasmidsoftware.gryphon.adjunct.DirectedGraph.triplesToTryGraph
import com.phasmidsoftware.gryphon.core.{EdgeGraph, EdgeType, Graph, Triplet}
import com.phasmidsoftware.gryphon.parse.GraphParser
import com.phasmidsoftware.gryphon.traverse.Traversal
import com.phasmidsoftware.gryphon.util.TryUsing
import com.phasmidsoftware.gryphon.visit.Visitor
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import org.scalatest.matchers.should.Matchers.shouldBe

import scala.io.Source
import scala.util.*

class TopologicalSortSpec extends AnyFlatSpec with should.Matchers {

  behavior of "TopologicalSort"

  it should "sort" in {
    val p = new GraphParser[Int, Double, EdgeType]
    val triedSource = Try(Source.fromResource("dag.graph"))
    val zsy: Try[Seq[Triplet[Int, Double, EdgeType]]] = TryUsing.tryIt(triedSource) {
      (source: Source) => p.parseSource[Triplet[Int, Double, EdgeType]](p.parseTriple)(source)
    }
    zsy match {
      case Success(triplets) =>
        triplesToTryGraph[Int, Double](triplets) match {
          case Success(graph: EdgeGraph[_, _]) =>
            println(graph.edges)
            graph.vertexMap.map.size shouldBe 7
            graph.edges.size shouldBe 11
            graph.vertexMap.map.keys.toSeq.sorted shouldBe Seq(0, 1, 2, 3, 4, 5, 6)
            val traversal = Traversal.edgeTraversal[Int, Int, String](edge => s"${edge.white} -> ${edge.black}")(graph)
            println(traversal)
            val visitor = Visitor.reversePost[Int]
            val result = graph.dfsAll(visitor)
            val topologicalOrder = result.journal
            println(topologicalOrder)
            val possibleOrders = List(
              List(3, 6, 0, 1, 4, 5, 2),
              List(3, 6, 0, 5, 2, 1, 4),
              List(3, 6, 0, 5, 1, 4, 2)
            )
            possibleOrders.contains(topologicalOrder) shouldBe true
          case Failure(x) =>
            fail("parse failed: ", x)
          case _ => fail("parse failed: Graph is not an EdgeGraph")
        }

      case Failure(x) =>
        fail("parse failed", x)
    }

    //      val graphBuilder: GraphBuilder[Int, Unit, Unit] = new GraphBuilder[Int, Unit, Unit]()
    //      val z: Try[Iterable[DirectedEdge[Int, Unit]]] = graphBuilder.createEdgeListPair(uy)(DirectedEdge.apply[Int])
    //      val graph: Graph[Int, Unit, DirectedEdge[Int, Unit], Unit] = DirectedGraph[Int, Unit, DirectedEdge[Int, Unit], Unit]("DAG")
    //      val gy: Try[Graph[Int, Unit, DirectedEdge[Int, Unit], Unit]] = graphBuilder.createGraphFromEdges[DirectedEdge[Int, Unit]](graph)(z)
    //      gy match {
    //        case Success(g) =>
    //          val sorted = TopologicalSort.sort(g)
    //          sorted shouldBe List(3, 6, 0, 5, 2, 1, 4)
    //          TopologicalSort.acyclic(graph.edges, sorted) shouldBe true
    //        case Failure(x) => throw x
    //      }
  }

  //    it should "handle a directed graph" in {
  //      val uy = resource("/directed.graph")
  //      val graphBuilder = new GraphBuilder[Int, Unit, Unit]()
  //      val z = graphBuilder.createEdgeListPair(uy)(DirectedEdge.apply[Int])
  //      val graph = DirectedGraph[Int, Unit, DirectedEdge[Int, Unit], Unit]("DAG")
  //      val gy = graphBuilder.createGraphFromEdges[DirectedEdge[Int, Unit]](graph)(z)
  //      gy match {
  //        case Success(g) =>
  //          val sorted = TopologicalSort.sort(g)
  //          TopologicalSort.acyclic(g.edges, sorted) shouldBe false
  //        case Failure(x) => throw x
  //      }
  //    }
}
