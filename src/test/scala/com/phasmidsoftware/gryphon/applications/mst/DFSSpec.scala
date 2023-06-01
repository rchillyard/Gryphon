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

    // FIXME
    ignore should "dfsTree" in {
        val uy = resource("/dfsu.graph")
        val graphBuilder = new UndirectedGraphBuilder[Int, Unit, Unit]()
        val z: Try[Iterable[VertexPair[Int]]] = graphBuilder.createEdgeListPair(uy)(VertexPairCase.apply)
        val graph: Graph[Int, Unit, VertexPair[Int], Unit] = VertexPairGraph[Int, Unit]("DFSU")
        val gy: Try[Graph[Int, Unit, VertexPair[Int], Unit]] = graphBuilder.createGraphFromEdges[VertexPair[Int]](graph)(z)
        gy match {
            case Success(g) =>
                val helper = new DFSHelper[Int, UndirectedEdge[Int, Unit], Unit]
                val start: Int = 0
                helper.dfsTree[VertexPair[Int]](g, start)((w, m) => UndirectedTreeCase[Int, Unit, UndirectedEdge[Int, Unit], Unit](w, m))
            case Failure(x) => throw x
        }

    }

}
