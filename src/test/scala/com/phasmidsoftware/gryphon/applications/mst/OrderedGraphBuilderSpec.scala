/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.applications.mst

import com.phasmidsoftware.gryphon.core.UndirectedOrderedEdgeCase
import com.phasmidsoftware.gryphon.util.{EdgeDataParser, OrderedGraphBuilder}
import com.phasmidsoftware.util.FP.resource
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import scala.util.{Failure, Success}

class OrderedGraphBuilderSpec extends AnyFlatSpec with should.Matchers {

    behavior of "OrderedGraphBuilder"

    it should "createUndirectedOrderedEdgeList" in {
        val primGraph = "/prim.graph"
        val uy = resource(primGraph)
        val graphBuilder = new OrderedGraphBuilder[Int, Double, Unit]()
        val esy = graphBuilder.createUndirectedOrderedEdgeList(uy)(UndirectedOrderedEdgeCase(_, _, _))
        graphBuilder.createGraphFromUndirectedOrderedEdges(esy) match {
            case Success(g) =>
                val edges = g.edges
                edges.size shouldBe 16
            case Failure(x) => throw x
        }
    }

    it should "parseUndirectedEdgeList" in {
        val parser = new EdgeDataParser[Int, Double]()
        val primGraph = "/prim.graph"
        val graphBuilder = new OrderedGraphBuilder[Int, Double, Unit]()
        graphBuilder.createGraphFromUndirectedOrderedEdges(parser.parseEdgesFromCsv(primGraph)) match {
            case Success(g) =>
                val edges = g.edges
                edges.size shouldBe 16
            case Failure(x) => throw x
        }
    }
}
