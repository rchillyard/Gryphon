/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.traverse

import com.phasmidsoftware.gryphon.adjunct.DirectedGraph.triplesToTryGraph
import com.phasmidsoftware.gryphon.adjunct.{AttributedDirectedEdge, DirectedGraph}
import com.phasmidsoftware.gryphon.core.*
import com.phasmidsoftware.visitor.core.given_Monoid_Double
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import scala.util.{Failure, Success}

/**
 * Tests for AcyclicShortestPaths.
 *
 * Test DAG (inline, Integer vertices, Double weights):
 *
 * 0 --1.0--> 1 --2.0--> 2
 * 0 --4.0--> 2
 * 1 --1.0--> 3
 * 2 --1.0--> 3
 *
 * Topological order: 0, 1, 2, 3
 * Shortest paths from 0:
 * dist(1) = 1.0  via 0->1
 * dist(2) = 3.0  via 0->1->2  (not 4.0 via 0->2)
 * dist(3) = 2.0  via 0->1->3  (not 4.0 via 0->1->2->3)
 *
 * Negative weight test DAG:
 * 0 --1.0--> 1 --(-2.0)--> 2
 * dist(1) = 1.0, dist(2) = -1.0  (Dijkstra would fail here)
 */
class AcyclicShortestPathsSpec extends AnyFlatSpec with should.Matchers:

//  given Monoid[Double] with
//    def empty: Double = 0.0
//    def combine(x: Double, y: Double): Double = x + y

  given Ordering[Double] = scala.math.Ordering.Double.TotalOrdering

  // -------------------------------------------------------------------------
  // Shared fixture — small weighted DAG
  // -------------------------------------------------------------------------

  private val dagTriplets: Seq[Triplet[Int, Double, EdgeType]] = Seq(
    Triplet(0, 1, Some(1.0), Directed),
    Triplet(0, 2, Some(4.0), Directed),
    Triplet(1, 2, Some(2.0), Directed),
    Triplet(1, 3, Some(1.0), Directed),
    Triplet(2, 3, Some(1.0), Directed)
  )

  private def withDag[A](f: DirectedGraph[Int, Double] => A): A =
    triplesToTryGraph[Int, Double](Vertex.createWithSet)(dagTriplets) match
      case Success(g: DirectedGraph[Int, Double] @unchecked) => f(g)
      case Failure(x) => fail("graph construction failed", x)
      case other => fail(s"unexpected: $other")

  // -------------------------------------------------------------------------
  // Result structure
  // -------------------------------------------------------------------------

  behavior of "AcyclicShortestPaths — result structure"

  it should "label all reachable non-source vertices" in :
    withDag { graph =>
      val result = AcyclicShortestPaths.shortestPaths(graph, 0)
      result.map.size shouldBe 3  // vertices 1, 2, 3 — not 0 (source)
    }

  it should "not include the source vertex in the result" in :
    withDag { graph =>
      val result = AcyclicShortestPaths.shortestPaths(graph, 0)
      result.vertexTraverse(0) shouldBe None
    }

  it should "include all reachable vertices" in :
    withDag { graph =>
      val result = AcyclicShortestPaths.shortestPaths(graph, 0)
      result.map.keySet shouldBe Set(1, 2, 3)
    }

  // -------------------------------------------------------------------------
  // Shortest path distances
  // -------------------------------------------------------------------------

  behavior of "AcyclicShortestPaths — path distances"

  it should "find shortest path to vertex 1 with distance 1.0" in :
    withDag { graph =>
      val result = AcyclicShortestPaths.shortestPaths(graph, 0)
      result.vertexTraverse(1).map(_.attribute) shouldBe Some(1.0)
    }

  it should "find shortest path to vertex 2 with distance 3.0 (via 0->1->2, not 0->2)" in :
    withDag { graph =>
      val result = AcyclicShortestPaths.shortestPaths(graph, 0)
      // Direct edge 0->2 costs 4.0; via 1 costs 1.0+2.0=3.0
      result.vertexTraverse(2).map(_.attribute) shouldBe Some(2.0)
    }

  it should "find shortest path to vertex 3 with distance 2.0 (via 0->1->3)" in :
    withDag { graph =>
      val result = AcyclicShortestPaths.shortestPaths(graph, 0)
      // Via 1->3: 1.0+1.0=2.0; via 2->3: 3.0+1.0=4.0
      result.vertexTraverse(3).map(_.attribute) shouldBe Some(1.0)
    }

  // -------------------------------------------------------------------------
  // Predecessor edges
  // -------------------------------------------------------------------------

  behavior of "AcyclicShortestPaths — predecessor edges"

  it should "give vertex 1 predecessor edge from 0" in :
    withDag { graph =>
      val result = AcyclicShortestPaths.shortestPaths(graph, 0)
      result.vertexTraverse(1) match
        case Some(e: AttributedDirectedEdge[Double, Int] @unchecked) =>
          e.white shouldBe 0
          e.black shouldBe 1
        case other => fail(s"unexpected: $other")
    }

  it should "give vertex 2 predecessor edge from 1 (not from 0)" in :
    withDag { graph =>
      val result = AcyclicShortestPaths.shortestPaths(graph, 0)
      result.vertexTraverse(2) match
        case Some(e: AttributedDirectedEdge[Double, Int] @unchecked) =>
          e.white shouldBe 1
          e.black shouldBe 2
        case other => fail(s"unexpected: $other")
    }

  it should "give vertex 3 predecessor edge from 1 (not from 2)" in :
    withDag { graph =>
      val result = AcyclicShortestPaths.shortestPaths(graph, 0)
      result.vertexTraverse(3) match
        case Some(e: AttributedDirectedEdge[Double, Int] @unchecked) =>
          e.white shouldBe 1
          e.black shouldBe 3
        case other => fail(s"unexpected: $other")
    }

  // -------------------------------------------------------------------------
  // Negative weights
  // -------------------------------------------------------------------------

  behavior of "AcyclicShortestPaths — negative weights"

  it should "correctly handle negative edge weights (unlike Dijkstra)" in :
    val triplets: Seq[Triplet[Int, Double, EdgeType]] = Seq(
      Triplet(0, 1, Some(1.0), Directed),
      Triplet(1, 2, Some(-2.0), Directed)
    )
    triplesToTryGraph[Int, Double](Vertex.createWithSet)(triplets) match
      case Success(g: DirectedGraph[Int, Double] @unchecked) =>
        val result = AcyclicShortestPaths.shortestPaths(g, 0)
        result.vertexTraverse(1).map(_.attribute) shouldBe Some(1.0)
        result.vertexTraverse(2).map(_.attribute) shouldBe Some(-2.0)
      case other => fail(s"unexpected: $other")

  // -------------------------------------------------------------------------
  // Unreachable vertices
  // -------------------------------------------------------------------------

  behavior of "AcyclicShortestPaths — unreachable vertices"

  it should "not include vertices unreachable from start" in :
    // 0->1->2 and isolated vertex 3->4
    val triplets: Seq[Triplet[Int, Double, EdgeType]] = Seq(
      Triplet(0, 1, Some(1.0), Directed),
      Triplet(1, 2, Some(1.0), Directed),
      Triplet(3, 4, Some(1.0), Directed)
    )
    triplesToTryGraph[Int, Double](Vertex.createWithSet)(triplets) match
      case Success(g: DirectedGraph[Int, Double] @unchecked) =>
        val result = AcyclicShortestPaths.shortestPaths(g, 0)
        result.map.keySet shouldBe Set(1, 2)
        result.vertexTraverse(3) shouldBe None
        result.vertexTraverse(4) shouldBe None
      case other => fail(s"unexpected: $other")

  // -------------------------------------------------------------------------
  // Error case — cyclic graph
  // -------------------------------------------------------------------------

  behavior of "AcyclicShortestPaths — error handling"

  it should "throw IllegalArgumentException for a cyclic graph" in :
    val triplets: Seq[Triplet[Int, Double, EdgeType]] = Seq(
      Triplet(0, 1, Some(1.0), Directed),
      Triplet(1, 2, Some(1.0), Directed),
      Triplet(2, 0, Some(1.0), Directed)
    )
    triplesToTryGraph[Int, Double](Vertex.createWithSet)(triplets) match
      case Success(g: DirectedGraph[Int, Double] @unchecked) =>
        an[IllegalArgumentException] should be thrownBy AcyclicShortestPaths.shortestPaths(g, 0)
      case other => fail(s"unexpected: $other")