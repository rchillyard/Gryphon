/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.applications.bfs

import com.phasmidsoftware.gryphon.adjunct.DirectedGraph
import com.phasmidsoftware.gryphon.core.{EdgeType, Graph, Triplet}
import com.phasmidsoftware.gryphon.parse.GraphParser
import com.phasmidsoftware.gryphon.util.TryUsing
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

import scala.collection.immutable.Queue
import scala.io.Source
import scala.util.*

class DijkstraSpec extends AnyFlatSpec with should.Matchers {

  behavior of "Dijkstra"

  it should "oldvisit all vertices in dijkstra" in {
    //    val p = new GraphParser[Int, Double, EdgeType]
    //    val triedSource = Try(Source.fromResource("dijkstra.graph"))
    //    val zsy: Try[Seq[Triplet[Int, Double, EdgeType]]] = TryUsing.tryIt(triedSource) {
    //      (source: Source) => p.parseSource[Triplet[Int, Double, EdgeType]](p.parseTriple)(source)
    //    }
    //    zsy match {
    //      case Success(triplets) =>
    //        DirectedGraph.triplesToTryGraph(triplets) match {
    //          case Success(graph: Graph[_]) =>
    //            val result: Visitor[Int] = graph.bfs(SimpleVisitor.createPre[Int])(0)(_ => false)
    //            val queue = result.journals.head
    //            queue.size shouldBe 8
    //          case Failure(exception) => fail(exception)
    //        }
    //      case Failure(exception) => fail(exception)
    //    }
  }
}
