/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.traverse

import com.phasmidsoftware.gryphon.adjunct.DirectedGraph
import com.phasmidsoftware.gryphon.adjunct.DirectedGraph.triplesToTryGraph
import com.phasmidsoftware.gryphon.core.{EdgeType, Graph, Triplet}
import com.phasmidsoftware.gryphon.parse.GraphParser
import com.phasmidsoftware.gryphon.util.TryUsing
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
          case Success(graph: DirectedGraph[_, _]) =>
            graph.vertexMap.map.size shouldBe 7
            graph.edges.size shouldBe 11
            graph.vertexMap.map.keys.toSeq.sorted shouldBe Seq(0, 1, 2, 3, 4, 5, 6)
            val traversal = Traversal.edgeTraversal[Int, Int, String](edge => s"${edge.white} -> ${edge.black}")(graph)
            println(traversal)
            val topologicalOrder = TopologicalSort.sort(graph)
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
    //          TopologicalSort.acyclic(graph.edges, sorted) shouldBe true
  }

  it should "traversal" in {
    val p = new GraphParser[Int, Double, EdgeType]
    val triedSource = Try(Source.fromResource("dag.graph"))
    val zsy: Try[Seq[Triplet[Int, Double, EdgeType]]] = TryUsing.tryIt(triedSource) {
      (source: Source) => p.parseSource[Triplet[Int, Double, EdgeType]](p.parseTriple)(source)
    }
    zsy match {
      case Success(triplets) =>
        triplesToTryGraph[Int, Double](triplets) match {
          case Success(graph: DirectedGraph[_, _]) =>
            val traversal: VertexTraversal[Int, Int] = TopologicalSort.traversal(graph).asInstanceOf[VertexTraversal[Int, Int]]
            println(traversal)
            val map = traversal.map
            map.size shouldBe 7
            map(6) shouldBe 1
            map(3) shouldBe 0
            map(0) shouldBe 2
          case Failure(x) =>
            fail("parse failed: ", x)
          case _ => fail("parse failed: Graph is not an EdgeGraph")
        }

      case Failure(x) =>
        fail("parse failed", x)
    }
    //          TopologicalSort.acyclic(graph.edges, sorted) shouldBe true
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
