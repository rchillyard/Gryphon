/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.traverse

import com.phasmidsoftware.gryphon.adjunct.DirectedGraph
import com.phasmidsoftware.gryphon.adjunct.DirectedGraph.triplesToTryGraph
import com.phasmidsoftware.gryphon.core.*
import com.phasmidsoftware.gryphon.parse.GraphParser
import com.phasmidsoftware.gryphon.util.TryUsing
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import scala.io.Source
import scala.util.*

class TopologicalSortSpec extends AnyFlatSpec with should.Matchers {

  behavior of "TopologicalSort"

  // --- Existing tests (kept as-is) ---

  it should "sort" in {
    val p: GraphParser[Int, Double, EdgeType] = new GraphParser[Int, Double, EdgeType]
    val triedSource = Try(Source.fromResource("dag.graph"))
    val zsy: Try[Seq[Triplet[Int, Double, EdgeType]]] = TryUsing.tryIt(triedSource) {
      (source: Source) => p.parseSource[Triplet[Int, Double, EdgeType]](p.parseTriple)(source)
    }
    zsy match {
      case Success(triplets) =>
        triplesToTryGraph[Int, Double](Vertex.createWithSet)(triplets) match {
          case Success(graph: DirectedGraph[_, _]) =>
            graph.vertexMap.map.size shouldBe 7
            graph.edges.size shouldBe 11
            graph match {
              case g: EdgeTraversable[Int, Double] =>
                val traversal = TraversalResult.edgeTraversal[Int, Double, String](edge => s"${edge.white} -> ${edge.black}")(g)
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
  }

  it should "topological sort successfully" in {
    val p = new GraphParser[Int, Double, EdgeType]
    val triedSource = Try(Source.fromResource("dag.graph"))
    val zsy: Try[Seq[Triplet[Int, Double, EdgeType]]] = TryUsing.tryIt(triedSource) {
      (source: Source) => p.parseSource[Triplet[Int, Double, EdgeType]](p.parseTriple)(source)
    }
    zsy match {
      case Success(triplets) =>
        triplesToTryGraph[Int, Double](Vertex.createWithSet)(triplets) match {
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
        triplesToTryGraph[Int, Double](Vertex.createWithSet)(triplets) match {
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

  // --- New tests ---

  it should "topological sort result satisfies edge ordering constraints" in {
    // For every edge u -> v in the DAG, u must appear before v in the sort order.
    // dag.graph edges: 0->5, 0->2, 0->1, 3->6, 3->5, 3->4, 5->2, 6->4, 6->0, 3->2, 1->4
    val p = new GraphParser[Int, Double, EdgeType]
    val triedSource = Try(Source.fromResource("dag.graph"))
    val zsy: Try[Seq[Triplet[Int, Double, EdgeType]]] = TryUsing.tryIt(triedSource) {
      (source: Source) => p.parseSource[Triplet[Int, Double, EdgeType]](p.parseTriple)(source)
    }
    zsy match {
      case Success(triplets) =>
        triplesToTryGraph[Int, Double](Vertex.createWithSet)(triplets) match {
          case Success(graph: DirectedGraph[_, _]) =>
            val maybeOrder = TopologicalSort.sort(graph)
            maybeOrder shouldBe defined
            val order = maybeOrder.get
            val pos = order.zipWithIndex.toMap
            // Check all edges: source must come before target in the ordering
            val edges = List(0 -> 5, 0 -> 2, 0 -> 1, 3 -> 6, 3 -> 5, 3 -> 4, 5 -> 2, 6 -> 4, 6 -> 0, 3 -> 2, 1 -> 4)
            edges.foreach { case (u, v) =>
              withClue(s"Edge $u -> $v violated: $u at pos ${pos(u)}, $v at pos ${pos(v)}") {
                pos(u) should be < pos(v)
              }
            }
          case Failure(x) => fail("parse failed: ", x)
          case _ => fail("parse failed")
        }
      case Failure(x) => fail("parse failed", x)
    }
  }

  it should "traversal rank ordering: earlier in topological order means lower rank" in {
    // TopologicalSort.traversal returns a map of vertex -> rank.
    // For every edge u->v in the DAG, rank(u) < rank(v).
    val p = new GraphParser[Int, Double, EdgeType]
    val triedSource = Try(Source.fromResource("dag.graph"))
    val zsy: Try[Seq[Triplet[Int, Double, EdgeType]]] = TryUsing.tryIt(triedSource) {
      (source: Source) => p.parseSource[Triplet[Int, Double, EdgeType]](p.parseTriple)(source)
    }
    zsy match {
      case Success(triplets) =>
        triplesToTryGraph[Int, Double](Vertex.createWithSet)(triplets) match {
          case Success(graph: DirectedGraph[_, _]) =>
            TopologicalSort.traversal(graph) match {
              case Some(TopologicalSort(map)) =>
                val edges = List(0 -> 5, 0 -> 2, 0 -> 1, 3 -> 6, 3 -> 5, 3 -> 4, 5 -> 2, 6 -> 4, 6 -> 0, 3 -> 2, 1 -> 4)
                edges.foreach { case (u, v) =>
                  withClue(s"Edge $u -> $v: rank($u)=${map(u)} should be < rank($v)=${map(v)}") {
                    map(u) should be < map(v)
                  }
                }
              case None => fail("TopologicalSort.traversal failed")
            }
          case Failure(x) => fail("parse failed: ", x)
          case _ => fail("parse failed")
        }
      case Failure(x) => fail("parse failed", x)
    }
  }

  it should "topological sort on a two-vertex graph with one edge" in {
    // Simple: 10 -> 20
    val triplets: Seq[Triplet[Int, Double, EdgeType]] =
      Seq(Triplet(10, 20, Some(1.0), Directed))
    triplesToTryGraph[Int, Double](Vertex.createWithSet)(triplets) match {
      case Success(graph: DirectedGraph[_, _]) =>
        val maybeOrder = TopologicalSort.sort(graph)
        maybeOrder shouldBe defined
        val order = maybeOrder.get
        order.indexOf(10) should be < order.indexOf(20)
      case Failure(x) => fail("graph construction failed", x)
      case _ => fail("not a DirectedGraph")
    }
  }

  it should "topological sort on a linear chain respects full ordering" in {
    // 0 -> 1 -> 2 -> 3 -> 4
    val triplets: Seq[Triplet[Int, Double, EdgeType]] = Seq(
      Triplet(0, 1, Some(1.0), Directed),
      Triplet(1, 2, Some(1.0), Directed),
      Triplet(2, 3, Some(1.0), Directed),
      Triplet(3, 4, Some(1.0), Directed)
    )
    triplesToTryGraph[Int, Double](Vertex.createWithSet)(triplets) match {
      case Success(graph: DirectedGraph[_, _]) =>
        val maybeOrder = TopologicalSort.sort(graph)
        maybeOrder shouldBe defined
        maybeOrder.get shouldBe List(0, 1, 2, 3, 4)
      case Failure(x) => fail("graph construction failed", x)
      case _ => fail("not a DirectedGraph")
    }
  }

  it should "detect cycle in two-vertex cyclic graph" in {
    val triplets: Seq[Triplet[Int, Double, EdgeType]] = Seq(
      Triplet(0, 1, Some(1.0), Directed),
      Triplet(1, 0, Some(1.0), Directed)
    )
    triplesToTryGraph[Int, Double](Vertex.createWithSet)(triplets) match {
      case Success(graph: DirectedGraph[_, _]) =>
        TopologicalSort.traversal(graph) shouldBe None
      case Failure(x) => fail("graph construction failed", x)
      case _ => fail("not a DirectedGraph")
    }
  }
}
