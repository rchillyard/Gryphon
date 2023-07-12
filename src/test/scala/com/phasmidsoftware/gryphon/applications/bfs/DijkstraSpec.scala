/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.applications.bfs

import com.phasmidsoftware.gryphon.core.{DirectedGraph, DirectedOrderedEdge, DirectedOrderedEdgeCase}
import com.phasmidsoftware.gryphon.util.GraphBuilder
import com.phasmidsoftware.util.FP.resource
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import scala.util._

class DijkstraSpec extends AnyFlatSpec with should.Matchers {

  behavior of "Dijkstra"

  it should "isReachable" in {
    val uy = resource("/dijkstra.graph")
    val graphBuilder = new GraphBuilder[Int, Double, Double]()
    val z = graphBuilder.createEdgeListTriple(uy)(DirectedOrderedEdgeCase.apply[Int, Double])
    val graph = DirectedGraph[Int, Double, DirectedOrderedEdge[Int, Double], Double]("DAG")
    val gy = graphBuilder.createGraphFromEdges[DirectedOrderedEdge[Int, Double]](graph)(z)
    gy match {
      case Success(g) =>
        val dijkstra: SP[Int, Double, DirectedOrderedEdge[Int, Double]] = Dijkstra(g)(0)
        dijkstra.isReachable(0) shouldBe true
      case Failure(x) => throw x
    }

  }

  it should "shortestPath" in {

  }

}
