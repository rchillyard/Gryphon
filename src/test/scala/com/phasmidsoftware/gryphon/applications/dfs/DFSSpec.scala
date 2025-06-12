/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.applications.dfs

import com.phasmidsoftware.gryphon.adjunct.DirectedGraph
import com.phasmidsoftware.gryphon.adjunct.DirectedGraph.triplesToTryGraph
import com.phasmidsoftware.gryphon.core.*
import com.phasmidsoftware.gryphon.parse.GraphParser
import com.phasmidsoftware.gryphon.util.TryUsing
import com.phasmidsoftware.gryphon.visit.Visitor
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

import scala.io.Source
import scala.util.*

class DFSSpec extends AnyFlatSpec with should.Matchers {

  behavior of "DFS"

  val dijkstraGraphPath = "dijkstra.graph"

  it should "fail on empty graph" in {
    val graph: Graph[Int] = DirectedGraph[Int, Unit]
    Using(Visitor.createPre[Int]) {
      visitor =>
        graph.dfs(visitor)(1)
    }
  }

  it should "dfs Dijkstra" in {
    val p = new GraphParser[Int, Double, EdgeType]
    val triedSource = Try(Source.fromResource(dijkstraGraphPath))
    val zsy: Try[Seq[Triplet[Int, Double, EdgeType]]] = TryUsing.tryIt(triedSource) {
      (source: Source) => p.parseSource[Triplet[Int, Double, EdgeType]](p.parseTriple)(source)
    }
    zsy match {
      case Success(triplets) =>
        triplesToTryGraph(triplets) match {
          case Success(graph: EdgeGraph[_, _]) =>
            println(graph.edges)
            graph.vertexMap.map.size shouldBe 8
            graph.edges.size shouldBe 16
            graph.dfs(Visitor.createPre[Int])(1) match {
              case visitor => println(visitor)
            }
          case Failure(x) =>
            fail("parse failed: ", x)
          case _ => fail("parse failed: Graph is not an EdgeGraph")
        }

      case Failure(x) =>
        fail("parse failed", x)
    }
  }

  //  it should "dfsTree" in {
  //    val uy = resource("/dfsu.graph")
  //    val graphBuilder = new GraphBuilder[Int, Unit, VertexPair[Int]]()
  //    val z: Try[Iterable[VertexPair[Int]]] = graphBuilder.createEdgeListPair(uy)(VertexPairCase.apply)
  //    val graph: Graph[Int, Unit, VertexPair[Int], VertexPair[Int]] = VertexPairGraph[Int, VertexPair[Int]]("DFSU")
  //    graphBuilder.createGraphFromEdges[VertexPair[Int]](graph)(z) match {
  //      case Success(g) =>
  //        val helper = new DFSHelper[Int, VertexPair[Int], DirectedEdge[Int, Unit]]
  //        val start: Int = 0
  //        val treeDFS: TreeDFS[Int, DirectedEdge[Int, Unit], Unit] = helper.dfsTree(g, start)(pair => DirectedEdgeCase(pair.vertices._1, pair.vertices._2, ()))
  //        val edges = treeDFS.tree.edges
  //        edges.size shouldBe 6
  //        edges foreach println
  //        val lastEdge: DirectedEdge[Int, Unit] = edges.last
  //        lastEdge.white
  //      case Failure(x) => throw x
  //    }
  //
  //  }
}
