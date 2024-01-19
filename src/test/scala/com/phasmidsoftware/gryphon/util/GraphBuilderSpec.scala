/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.util

import com.phasmidsoftware.gryphon.oldcore.{UndirectedGraph, UndirectedOrderedEdge, UndirectedOrderedEdgeCase}
import com.phasmidsoftware.util.FP.resource
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import scala.util.{Failure, Success}

class GraphBuilderSpec extends AnyFlatSpec with should.Matchers {

  behavior of "GraphBuilder"

  it should "createEdgeListTriple" in {
    val primGraph = "/prim.graph"
    val uy = resource(primGraph)
    val graphBuilder = new GraphBuilder[Int, Double, Unit]()
    val esy = graphBuilder.createEdgeListTriple(uy)(UndirectedOrderedEdgeCase(_, _, _))
    graphBuilder.createGraphFromEdges(UndirectedGraph.createUnordered[Int, Double, UndirectedOrderedEdge[Int, Double], Unit]("no title"))(esy) match {
      case Success(g) =>
        val edges = g.edges
        edges.size shouldBe 16
      case Failure(x) => throw x
    }
  }

  it should "parseUndirectedEdgeList" in {
    val parser = new EdgeDataParser[Int, Double]()
    val primGraph = "/prim.graph"
    val graphBuilder = new GraphBuilder[Int, Double, Unit]()
    graphBuilder.createGraphFromEdges(UndirectedGraph.createUnordered[Int, Double, UndirectedOrderedEdge[Int, Double], Unit]("no title"))(parser.parseEdgesFromCsv(primGraph)) match {
      case Success(g) =>
        val edges = g.edges
        edges.size shouldBe 16
      case Failure(x) => throw x
    }
  }
}
