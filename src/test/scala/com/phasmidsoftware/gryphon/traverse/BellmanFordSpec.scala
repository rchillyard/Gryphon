/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.traverse

import com.phasmidsoftware.gryphon.adjunct.DirectedGraph.triplesToTryGraph
import com.phasmidsoftware.gryphon.adjunct.{AttributedDirectedEdge, DirectedGraph}
import com.phasmidsoftware.gryphon.core.{*, given}
import com.phasmidsoftware.gryphon.parse.GraphParser
import com.phasmidsoftware.gryphon.util.TryUsing
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import scala.io.Source
import scala.util.{Failure, Success, Try}

/**
 * Tests for BellmanFord shortest paths.
 *
 * Test graph 1 — negative weight, no negative cycle:
 * 0 --1.0--> 1 --(-2.0)--> 2
 * 0 --4.0--> 2
 * 1 --3.0--> 3
 * 2 --1.0--> 3
 *
 * Shortest paths from 0:
 * dist(1) = 1.0   via 0->1
 * dist(2) = -1.0  via 0->1->2  (not 4.0 via 0->2)
 * dist(3) = 0.0   via 0->1->2->3
 *
 * Test graph 2 — negative cycle:
 * 0 --1.0--> 1 --(-3.0)--> 2 --1.0--> 1  (cycle 1->2->1 sums to -2.0)
 */
class BellmanFordSpec extends AnyFlatSpec with should.Matchers:

  given Ordering[Double] = scala.math.Ordering.Double.TotalOrdering

  // -------------------------------------------------------------------------
  // Shared fixtures
  // -------------------------------------------------------------------------

  private val negativeTriplets: Seq[Triplet[Int, Double, EdgeType]] = Seq(
    Triplet(0, 1, Some(1.0), Directed),
    Triplet(0, 2, Some(4.0), Directed),
    Triplet(1, 2, Some(-2.0), Directed),
    Triplet(1, 3, Some(3.0), Directed),
    Triplet(2, 3, Some(1.0), Directed)
  )

  private val negativeCycleTriplets: Seq[Triplet[Int, Double, EdgeType]] = Seq(
    Triplet(0, 1, Some(1.0), Directed),
    Triplet(1, 2, Some(-3.0), Directed),
    Triplet(2, 1, Some(1.0), Directed)   // cycle 1->2->1 sums to -2.0
  )

  private def withNegativeGraph[A](f: DirectedGraph[Int, Double] => A): A =
    triplesToTryGraph[Int, Double](Vertex.createWithSet)(negativeTriplets) match
      case Success(g: DirectedGraph[Int, Double] @unchecked) => f(g)
      case Failure(x) => fail("graph construction failed", x)
      case other => fail(s"unexpected: $other")

  private def withNegativeCycleGraph[A](f: DirectedGraph[Int, Double] => A): A =
    triplesToTryGraph[Int, Double](Vertex.createWithSet)(negativeCycleTriplets) match
      case Success(g: DirectedGraph[Int, Double] @unchecked) => f(g)
      case Failure(x) => fail("graph construction failed", x)
      case other => fail(s"unexpected: $other")

  private def withDijkstraGraph[A](f: DirectedGraph[Int, Double] => A): A =
    val p = new GraphParser[Int, Double, EdgeType]
    val triedSource = Try(Source.fromResource("dijkstra.graph"))
    TryUsing.tryIt(triedSource) { source =>
      p.parseSource[Triplet[Int, Double, EdgeType]](p.parseTriple)(source)
    } match
      case Success(triplets) =>
        triplesToTryGraph[Int, Double](Vertex.createWithSet)(triplets) match
          case Success(g: DirectedGraph[Int, Double] @unchecked) => f(g)
          case Failure(x) => fail("graph construction failed", x)
          case other => fail(s"unexpected: $other")
      case Failure(x) => fail("parse failed", x)

  // -------------------------------------------------------------------------
  // Result structure
  // -------------------------------------------------------------------------

  behavior of "BellmanFord — result structure"

  import com.phasmidsoftware.visitor.core.given_Monoid_Double

  it should "return Some for a graph with no negative cycle" in :
    withNegativeGraph { graph =>
      BellmanFord.shortestPaths(graph, 0) shouldBe defined
    }

  it should "return None for a graph with a negative cycle" in :
    withNegativeCycleGraph { graph =>
      BellmanFord.shortestPaths(graph, 0) shouldBe None
    }

  it should "not include the source vertex in the result" in :
    withNegativeGraph { graph =>
      val result = BellmanFord.shortestPaths(graph, 0).get
      result.vertexTraverse(0) shouldBe None
    }

  it should "include all reachable non-source vertices" in :
    withNegativeGraph { graph =>
      val result = BellmanFord.shortestPaths(graph, 0).get
      result.map.keySet shouldBe Set(1, 2, 3)
    }

  // -------------------------------------------------------------------------
  // Shortest path distances (via edge attributes)
  // -------------------------------------------------------------------------

  behavior of "BellmanFord — path distances"

  it should "find shortest path to vertex 1 via edge weight 1.0" in :
    withNegativeGraph { graph =>
      val result = BellmanFord.shortestPaths(graph, 0).get
      result.vertexTraverse(1).map(_.attribute) shouldBe Some(1.0)
    }

  it should "find shortest path to vertex 2 via 0->1->2 (edge weight -2.0, not 0->2 weight 4.0)" in :
    withNegativeGraph { graph =>
      val result = BellmanFord.shortestPaths(graph, 0).get
      result.vertexTraverse(2).map(_.attribute) shouldBe Some(-2.0)
    }

  it should "find shortest path to vertex 3 via 0->1->2->3 (edge weight 1.0)" in :
    withNegativeGraph { graph =>
      val result = BellmanFord.shortestPaths(graph, 0).get
      result.vertexTraverse(3).map(_.attribute) shouldBe Some(1.0)
    }

  // -------------------------------------------------------------------------
  // Predecessor edges
  // -------------------------------------------------------------------------

  behavior of "BellmanFord — predecessor edges"

  it should "give vertex 1 predecessor edge from 0" in :
    withNegativeGraph { graph =>
      val result = BellmanFord.shortestPaths(graph, 0).get
      result.vertexTraverse(1) match
        case Some(e: AttributedDirectedEdge[Double, Int] @unchecked) =>
          e.white shouldBe 0
          e.black shouldBe 1
        case other => fail(s"unexpected: $other")
    }

  it should "give vertex 2 predecessor edge from 1 (not from 0)" in :
    withNegativeGraph { graph =>
      val result = BellmanFord.shortestPaths(graph, 0).get
      result.vertexTraverse(2) match
        case Some(e: AttributedDirectedEdge[Double, Int] @unchecked) =>
          e.white shouldBe 1
          e.black shouldBe 2
        case other => fail(s"unexpected: $other")
    }

  it should "give vertex 3 predecessor edge from 2" in :
    withNegativeGraph { graph =>
      val result = BellmanFord.shortestPaths(graph, 0).get
      result.vertexTraverse(3) match
        case Some(e: AttributedDirectedEdge[Double, Int] @unchecked) =>
          e.white shouldBe 2
          e.black shouldBe 3
        case other => fail(s"unexpected: $other")
    }

  // -------------------------------------------------------------------------
  // Negative cycle detection
  // -------------------------------------------------------------------------

  behavior of "BellmanFord — negative cycle detection"

  it should "return None when source can reach a negative cycle" in :
    withNegativeCycleGraph { graph =>
      BellmanFord.shortestPaths(graph, 0) shouldBe None
    }

  it should "return Some when source cannot reach the negative cycle" in :
    // Negative cycle 1->2->1 exists but vertex 0 is disconnected from it
    val triplets: Seq[Triplet[Int, Double, EdgeType]] = Seq(
      Triplet(0, 3, Some(1.0), Directed),    // 0 only reaches 3
      Triplet(1, 2, Some(-3.0), Directed),    // negative cycle unreachable from 0
      Triplet(2, 1, Some(1.0), Directed)
    )
    triplesToTryGraph[Int, Double](Vertex.createWithSet)(triplets) match
      case Success(g: DirectedGraph[Int, Double] @unchecked) =>
        BellmanFord.shortestPaths(g, 0) shouldBe defined
      case other => fail(s"unexpected: $other")

  // -------------------------------------------------------------------------
  // Agreement with Dijkstra on non-negative graphs
  // -------------------------------------------------------------------------

  behavior of "BellmanFord — agreement with Dijkstra"

  it should "produce the same predecessor edges as Dijkstra on dijkstra.graph" in :
    withDijkstraGraph { graph =>
      val bf = BellmanFord.shortestPaths(graph, 0).get
      val dj = ShortestPaths.dijkstra(graph, 0)
      // Both should agree on the incoming edge for every vertex
      graph.vertexMap.keySet.foreach { v =>
        val bfEdge = bf.vertexTraverse(v).map(e => (e.white, e.black, e.attribute))
        val djEdge = dj.vertexTraverse(v).map(e => (e.white, e.black, e.attribute))
        bfEdge shouldBe djEdge
      }
    }

  it should "return Some for dijkstra.graph (no negative weights)" in :
    withDijkstraGraph { graph =>
      BellmanFord.shortestPaths(graph, 0) shouldBe defined
    }

  // -------------------------------------------------------------------------
  // Unreachable vertices
  // -------------------------------------------------------------------------

  behavior of "BellmanFord — unreachable vertices"

  it should "not include vertices unreachable from start" in :
    val triplets: Seq[Triplet[Int, Double, EdgeType]] = Seq(
      Triplet(0, 1, Some(1.0), Directed),
      Triplet(2, 3, Some(1.0), Directed)   // disconnected component
    )
    triplesToTryGraph[Int, Double](Vertex.createWithSet)(triplets) match
      case Success(g: DirectedGraph[Int, Double] @unchecked) =>
        val result = BellmanFord.shortestPaths(g, 0).get
        result.map.keySet shouldBe Set(1)
        result.vertexTraverse(2) shouldBe None
        result.vertexTraverse(3) shouldBe None
      case other => fail(s"unexpected: $other")