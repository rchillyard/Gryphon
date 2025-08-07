/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.traverse

import com.phasmidsoftware.gryphon.adjunct.DirectedGraph
import com.phasmidsoftware.gryphon.adjunct.DirectedGraph.triplesToTryGraph
import com.phasmidsoftware.gryphon.core.{EdgeTraversable, EdgeType, Graph, Triplet}
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
    val p: GraphParser[Int, Double, EdgeType] = new GraphParser[Int, Double, EdgeType]
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
            graph match {
              case g: EdgeTraversable[Int, Double] =>
                val traversal = Traversal.edgeTraversal[Int, Double, String](edge => s"${edge.white} -> ${edge.black}")(g)
                println(traversal)
              case _ =>
              // Do nothing
            }
            val maybeTopologicalOrder = TopologicalSort.sort(graph)
            maybeTopologicalOrder.isDefined shouldBe true
            val topologicalOrder = maybeTopologicalOrder.get
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

  it should "topological sort successfully" in {
    val p = new GraphParser[Int, Double, EdgeType]
    val triedSource = Try(Source.fromResource("dag.graph"))
    val zsy: Try[Seq[Triplet[Int, Double, EdgeType]]] = TryUsing.tryIt(triedSource) {
      (source: Source) => p.parseSource[Triplet[Int, Double, EdgeType]](p.parseTriple)(source)
    }
    zsy match {
      case Success(triplets) =>
        triplesToTryGraph[Int, Double](triplets) match {
          case Success(graph: DirectedGraph[_, _]) =>
            TopologicalSort.traversal(graph) match {
              case Some(t@TopologicalSort(map)) =>
                map.size shouldBe 7
                map(6) shouldBe 1
                map(3) shouldBe 0
                map(0) shouldBe 2
              case None =>
                fail("TopologicalSort.traversal failed (cyclic graph?)")
            }
          case Failure(x) =>
            fail("parse failed: ", x)
          case _ => fail("parse failed: Graph is not an EdgeGraph")
        }
      case Failure(x) =>
        fail("parse failed", x)
    }
  }
  it should "topological sort failing" in {
    val p = new GraphParser[Int, Double, EdgeType]
    val triedSource = Try(Source.fromResource("directed.graph"))
    val zsy: Try[Seq[Triplet[Int, Double, EdgeType]]] = TryUsing.tryIt(triedSource) {
      (source: Source) => p.parseSource[Triplet[Int, Double, EdgeType]](p.parseTriple)(source)
    }
    zsy match {
      case Success(triplets) =>
        triplesToTryGraph[Int, Double](triplets) match {
          case Success(graph: DirectedGraph[_, _]) =>
            TopologicalSort.traversal(graph) match {
              case Some(t@TopologicalSort(map)) =>
                fail("TopologicalSort.traversal succeeded when it shouldn't (acyclic graph?)")
              case None =>
                succeed
            }
          case Failure(x) =>
            fail("parse failed: ", x)
          case _ => fail("parse failed: Graph is not an EdgeGraph")
        }
      case Failure(x) =>
        fail("parse failed", x)
    }
  }
}
