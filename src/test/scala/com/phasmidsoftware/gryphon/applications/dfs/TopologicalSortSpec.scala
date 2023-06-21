/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.applications.dfs

import com.phasmidsoftware.gryphon.core.{DirectedEdge, DirectedGraph, Graph}
import com.phasmidsoftware.gryphon.util.GraphBuilder
import com.phasmidsoftware.util.FP.resource
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import scala.util._

class TopologicalSortSpec extends AnyFlatSpec with should.Matchers {

    behavior of "TopologicalSort"

    it should "sort" in {
        val uy = resource("/dag.graph")
        val graphBuilder: GraphBuilder[Int, Unit, Unit] = new GraphBuilder[Int, Unit, Unit]()
        val z: Try[Iterable[DirectedEdge[Int, Unit]]] = graphBuilder.createEdgeListPair(uy)(DirectedEdge.apply[Int])
        val graph: Graph[Int, Unit, DirectedEdge[Int, Unit], Unit] = DirectedGraph[Int, Unit, DirectedEdge[Int, Unit], Unit]("DAG")
        val gy: Try[Graph[Int, Unit, DirectedEdge[Int, Unit], Unit]] = graphBuilder.createGraphFromEdges[DirectedEdge[Int, Unit]](graph)(z)
        gy match {
            case Success(g) =>
                val sorted = TopologicalSort.sort(g)
                sorted shouldBe List(3, 6, 0, 5, 2, 1, 4)
                TopologicalSort.acyclic(graph.edges, sorted) shouldBe true
            case Failure(x) => throw x
        }
    }

    it should "handle a directed graph" in {
        val uy = resource("/directed.graph")
        val graphBuilder = new GraphBuilder[Int, Unit, Unit]()
        val z = graphBuilder.createEdgeListPair(uy)(DirectedEdge.apply[Int])
        val graph = DirectedGraph[Int, Unit, DirectedEdge[Int, Unit], Unit]("DAG")
        val gy = graphBuilder.createGraphFromEdges[DirectedEdge[Int, Unit]](graph)(z)
        gy match {
            case Success(g) =>
                val sorted = TopologicalSort.sort(g)
                TopologicalSort.acyclic(g.edges, sorted) shouldBe false
            case Failure(x) => throw x
        }
    }
}
