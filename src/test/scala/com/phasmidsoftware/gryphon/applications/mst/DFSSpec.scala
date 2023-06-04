/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.applications.mst

import com.phasmidsoftware.gryphon.core.Parseable.ParseableUnit
import com.phasmidsoftware.gryphon.core._
import com.phasmidsoftware.gryphon.util.UndirectedGraphBuilder
import com.phasmidsoftware.util.FP.resource
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import scala.util._

class DFSSpec extends AnyFlatSpec with should.Matchers {

    behavior of "DFS"

    it should "dfsTree" in {
        val uy = resource("/dfsu.graph")
        val graphBuilder = new UndirectedGraphBuilder[Int, Unit, VertexPair[Int]]()
        val z: Try[Iterable[VertexPair[Int]]] = graphBuilder.createEdgeListPair(uy)(VertexPairCase.apply)
        val graph: Graph[Int, Unit, VertexPair[Int], VertexPair[Int]] = VertexPairGraph[Int, VertexPair[Int]]("DFSU")
        val gy: Try[Graph[Int, Unit, VertexPair[Int], VertexPair[Int]]] = graphBuilder.createGraphFromEdges[VertexPair[Int]](graph)(z)
        gy match {
            case Success(g) =>
                val helper = new DFSHelper[Int, VertexPair[Int], DirectedEdge[Int, Unit]]
                val start: Int = 0
                val tree = helper.dfsTree(g, start)
                println(tree)
            case Failure(x) => throw x
        }

    }

}
