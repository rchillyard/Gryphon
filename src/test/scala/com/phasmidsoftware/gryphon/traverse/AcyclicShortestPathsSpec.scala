/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.traverse

import com.phasmidsoftware.gryphon.adjunct.DirectedGraph.triplesToTryGraph
import com.phasmidsoftware.gryphon.adjunct.{AttributedDirectedEdge, DirectedGraph}
import com.phasmidsoftware.gryphon.core.*
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

  // -------------------------------------------------------------------------
  // Result structure
  // -------------------------------------------------------------------------

  behavior of "AcyclicShortestPaths — result structure"

  it should "label all reachable non-source vertices" in :
    withDag { graph =>
      val result = ShortestPaths.acyclic(graph, 0)
      result.map.size shouldBe 3  // vertices 1, 2, 3 — not 0 (source)
    }

  it should "not include the source vertex in the result" in :
    withDag { graph =>
      val result = ShortestPaths.acyclic(graph, 0)
      result.vertexTraverse(0) shouldBe None
    }

  it should "include all reachable vertices" in :
    withDag { graph =>
      val result = ShortestPaths.acyclic(graph, 0)
      result.map.keySet shouldBe Set(1, 2, 3)
    }

  // -------------------------------------------------------------------------
  // Shortest path distances
  // -------------------------------------------------------------------------

  behavior of "AcyclicShortestPaths — path distances"

  it should "find shortest path to vertex 1 with distance 1.0" in :
    withDag { graph =>
      val result = ShortestPaths.acyclic(graph, 0)
      result.vertexTraverse(1).map(_.attribute) shouldBe Some(1.0)
    }

  it should "find shortest path to vertex 2 with distance 3.0 (via 0->1->2, not 0->2)" in :
    withDag { graph =>
      val result = ShortestPaths.acyclic(graph, 0)
      // Direct edge 0->2 costs 4.0; via 1 costs 1.0+2.0=3.0
      result.vertexTraverse(2).map(_.attribute) shouldBe Some(2.0)
    }

  it should "find shortest path to vertex 3 with distance 2.0 (via 0->1->3)" in :
    withDag { graph =>
      val result = ShortestPaths.acyclic(graph, 0)
      // Via 1->3: 1.0+1.0=2.0; via 2->3: 3.0+1.0=4.0
      result.vertexTraverse(3).map(_.attribute) shouldBe Some(1.0)
    }

  // -------------------------------------------------------------------------
  // Predecessor edges
  // -------------------------------------------------------------------------

  behavior of "AcyclicShortestPaths — predecessor edges"

  it should "give vertex 1 predecessor edge from 0" in :
    withDag { graph =>
      val result = ShortestPaths.acyclic(graph, 0)
      result.vertexTraverse(1) match
        case Some(e: AttributedDirectedEdge[Double, Int] @unchecked) =>
          e.white shouldBe 0
          e.black shouldBe 1
        case other =>
          fail(s"unexpected: $other")
    }

  it should "give vertex 2 predecessor edge from 1 (not from 0)" in :
    withDag { graph =>
      val result = ShortestPaths.acyclic(graph, 0)
      result.vertexTraverse(2) match
        case Some(e: AttributedDirectedEdge[Double, Int] @unchecked) =>
          e.white shouldBe 1
          e.black shouldBe 2
        case other =>
          fail(s"unexpected: $other")
    }

  it should "give vertex 3 predecessor edge from 1 (not from 2)" in :
    withDag { graph =>
      val result = ShortestPaths.acyclic(graph, 0)
      result.vertexTraverse(3) match
        case Some(e: AttributedDirectedEdge[Double, Int] @unchecked) =>
          e.white shouldBe 1
          e.black shouldBe 3
        case other =>
          fail(s"unexpected: $other")
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
        val result = ShortestPaths.acyclic(g, 0)
        result.vertexTraverse(1).map(_.attribute) shouldBe Some(1.0)
        result.vertexTraverse(2).map(_.attribute) shouldBe Some(-2.0)
      case other =>
        fail(s"unexpected: $other")

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
        val result = ShortestPaths.acyclic(g, 0)
        result.map.keySet shouldBe Set(1, 2)
        result.vertexTraverse(3) shouldBe None
        result.vertexTraverse(4) shouldBe None
      case other =>
        fail(s"unexpected: $other")

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
        an[IllegalArgumentException] should be thrownBy ShortestPaths.acyclic(g, 0)
      case other =>
        fail(s"unexpected: $other")
  // =====================================================================================
  // Issue #114: longest paths in a DAG.
  //
  // The book derives this by negating the weights, which cannot be done here -- E carries only
  // Monoid and Ordering, and neither offers negation. It is also unnecessary: `relax` uses Monoid
  // only to accumulate and Ordering only to decide whether a candidate beats the incumbent, so
  // reversing the Ordering turns "keep the smaller" into "keep the larger" and changes nothing
  // else.
  //
  // The shared DAG distinguishes the two at BOTH branching vertices, which is why it is reused:
  //
  //   to 2:  shortest 3.0 via 0->1->2   longest 4.0 via 0->2
  //   to 3:  shortest 2.0 via 0->1->3   longest 5.0 via 0->2->3
  //
  // so a test that confused them would have to be wrong twice in the same direction to pass.

  behavior of "ShortestPaths.longestAcyclic"

  it should "label the same vertices as shortestPaths" in :
    withDag { graph =>
      ShortestPaths.longestAcyclic(graph, 0).map.keySet shouldBe Set(1, 2, 3)
    }

  it should "reach 2 directly, where the shortest path goes via 1" in :
    withDag { graph =>
      ShortestPaths.longestAcyclic(graph, 0).vertexTraverse(2).map(_.white) shouldBe Some(0)
      ShortestPaths.acyclic(graph, 0).vertexTraverse(2).map(_.white) shouldBe Some(1)
    }

  it should "reach 3 via 2, where the shortest path goes via 1" in :
    withDag { graph =>
      ShortestPaths.longestAcyclic(graph, 0).vertexTraverse(3).map(_.white) shouldBe Some(2)
      ShortestPaths.acyclic(graph, 0).vertexTraverse(3).map(_.white) shouldBe Some(1)
    }

  it should "agree with shortestPaths where only one path exists" in :
    // Vertex 1 has a single incoming edge, so there is nothing to choose between and the two
    // must agree. Without this, reversing the Ordering somewhere it should not be would be
    // invisible on a graph where every vertex has a choice.
    withDag { graph =>
      ShortestPaths.longestAcyclic(graph, 0).vertexTraverse(1) shouldBe
              ShortestPaths.acyclic(graph, 0).vertexTraverse(1)
    }

  it should "refuse a cyclic graph, as shortestPaths does" in :
    val cyclic: Seq[Triplet[Int, Double, EdgeType]] = Seq(
      Triplet(0, 1, Some(1.0), Directed),
      Triplet(1, 0, Some(1.0), Directed))
    triplesToTryGraph[Int, Double](Vertex.createWithSet)(cyclic) match
      case Success(g: DirectedGraph[Int, Double] @unchecked) =>
        an[IllegalArgumentException] should be thrownBy ShortestPaths.longestAcyclic(g, 0)
      case Failure(x) => fail("graph construction failed", x)

  // =====================================================================================
  // Issue #112: walking the parent map back into a path.
  //
  // These results are parent maps -- each reached vertex against the edge it was reached by --
  // which is the edgeTo[] array of Sedgewick & Wayne's DepthFirstPaths. The structure was here
  // and the walk was not, so every caller wanting a path had to write it. One implementation on
  // TraversalResult serves Connexions, Dijkstra's result and this one, because they share the
  // shape rather than the type.

  behavior of "TraversalResult.pathTo"

  it should "give the shortest path to 3 as 0, 1, 3" in :
    withDag { graph =>
      val r = ShortestPaths.acyclic(graph, 0)
      TraversalResult.pathTo(r)(3) shouldBe Seq(0, 1, 3)
    }

  it should "give the longest path to 3 as 0, 2, 3" in :
    withDag { graph =>
      val r = ShortestPaths.longestAcyclic(graph, 0)
      TraversalResult.pathTo(r)(3) shouldBe Seq(0, 2, 3)
    }

  it should "give the edges of the path, so a caller can total the weights" in :
    withDag { graph =>
      val edges = TraversalResult.pathEdgesTo(ShortestPaths.acyclic(graph, 0))(3)
      edges.map(_.attribute) shouldBe Seq(1.0, 1.0) // 0->1 then 1->3
      edges.map(_.attribute).sum shouldBe 2.0
      TraversalResult.pathEdgesTo(ShortestPaths.longestAcyclic(graph, 0))(3)
              .map(_.attribute).sum shouldBe 5.0 // 0->2 (4.0) then 2->3 (1.0)
    }

  it should "return just the vertex itself when it has no incoming edge" in :
    // True of the root, and equally of a vertex that was never reached -- the two are
    // indistinguishable from a parent map alone, because the root is simply absent from it.
    // That is a limitation of the representation, not of pathTo, and is part of what #76 is for.
    withDag { graph =>
      val r = ShortestPaths.acyclic(graph, 0)
      TraversalResult.pathTo(r)(0) shouldBe Seq(0)
      TraversalResult.pathTo(r)(99) shouldBe Seq(99)
    }

  it should "terminate on a parent map that contains a cycle" in :
    // No traversal in this library produces one, but the type permits it and Connexions is a
    // public case class anybody can construct. Without the visited set this walk would hang,
    // which #117 has just shown is a failure mode worth guarding rather than assuming away.
    val cyclicMap = Map(
      0 -> AttributedDirectedEdge(1.0, 1, 0),
      1 -> AttributedDirectedEdge(1.0, 0, 1))
    val r = VertexTraversalResult[Int, AttributedDirectedEdge[Int, Double]](cyclicMap)
    TraversalResult.pathEdgesTo(r)(0).size should be <= 2

  // =====================================================================================
  // Issue #118: DirectedGraph.topologicalOrder is computed once per instance.
  //
  // The concrete case behind it: the Java façade's scaladoc tells a caller to check isDAG before
  // calling AcyclicShortestPaths, isCyclic was TopologicalSort.sort(this).isEmpty, and
  // shortestPaths sorted again internally -- so following the façade's own advice cost two full
  // topological sorts. isCyclic is now derived from the cached order, and shortestPaths uses it.
  //
  // 1.14.0 weighed replacing this with a seeded TopologicalSort.sort, so that a caller could pin
  // which of several equal-cost paths AcyclicShortestPaths reports, and declined: that would cost
  // every caller the saving #118 bought in order to serve the few who want reproducibility. The
  // consequence is that this order cannot be seeded at all -- a val takes no `using` clause -- so
  // AcyclicShortestPaths is stable per graph instance and arbitrary across instances, which its
  // scaladoc now says in as many words. See #158.
  //
  // There is a second effect worth pinning, and it is the reason the reference-equality assertion
  // below is not pedantry. Since #131 TopologicalSort.sort takes `using Random = Random()`, and
  // this lazy val supplies none -- so each computation of it would draw a fresh generator and a
  // different valid order. Caching is therefore what makes this deterministic at all: one order
  // per graph instead of a fresh one per call. If the lazy val were ever turned back into a def,
  // these assertions would start failing intermittently rather than reliably, so they are worth
  // having.

  behavior of "DirectedGraph.topologicalOrder"

  it should "return the identical instance on every call" in :
    withDag { graph =>
      val first = graph.topologicalOrder
      assert(first.asInstanceOf[AnyRef] eq graph.topologicalOrder.asInstanceOf[AnyRef])
    }

  it should "be a valid topological order of the DAG" in :
    withDag { graph =>
      val order = graph.topologicalOrder.getOrElse(fail("expected a DAG"))
      order.toSet shouldBe Set(0, 1, 2, 3)
      // Every edge must run forwards in the order.
      val position = order.zipWithIndex.toMap
      for edge <- graph.edges do position(edge.white) should be < position(edge.black)
    }

  it should "agree with isCyclic, which is derived from it" in :
    withDag { graph =>
      graph.isCyclic shouldBe false
      graph.topologicalOrder should not be empty
    }

  it should "be None for a cyclic graph, and isCyclic true" in :
    val cyclic: Seq[Triplet[Int, Double, EdgeType]] = Seq(
      Triplet(0, 1, Some(1.0), Directed),
      Triplet(1, 0, Some(1.0), Directed))
    triplesToTryGraph[Int, Double](Vertex.createWithSet)(cyclic) match
      case Success(g: DirectedGraph[Int, Double] @unchecked) =>
        g.topologicalOrder shouldBe None
        g.isCyclic shouldBe true
      case Failure(x) => fail("graph construction failed", x)
