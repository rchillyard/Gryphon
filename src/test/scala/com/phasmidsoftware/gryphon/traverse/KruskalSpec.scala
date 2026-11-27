/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.traverse

import com.phasmidsoftware.gryphon.adjunct.UndirectedGraph
import com.phasmidsoftware.gryphon.adjunct.UndirectedGraph.triplesToTryGraph
import com.phasmidsoftware.gryphon.core.*
import com.phasmidsoftware.gryphon.parse.GraphParser
import com.phasmidsoftware.gryphon.util.TryUsing
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import scala.io.Source
import scala.util.{Failure, Success, Try}

/**
 * Tests for Kruskal's minimum spanning tree algorithm.
 *
 * prim.graph (Sedgewick tinyEWG) — 8 vertices (0–7), 16 undirected weighted edges.
 *
 * MST edges (7 edges for 8 vertices), in weight order:
 * 0-7: 0.16,  2-3: 0.17,  1-7: 0.19,  0-2: 0.26,
 * 5-7: 0.28,  4-5: 0.35,  6-2: 0.40
 * Total MST weight: 1.81
 */
class KruskalSpec extends AnyFlatSpec with should.Matchers:

  given Numeric[Double] = scala.math.Numeric.DoubleIsFractional

  given Ordering[Double] = scala.math.Ordering.Double.TotalOrdering

  // -------------------------------------------------------------------------
  // Shared fixture
  // -------------------------------------------------------------------------

  private def withPrimGraph[A](f: UndirectedGraph[Int, Double] => A): A =
    val p = new GraphParser[Int, Double, EdgeType]
    val triedSource = Try(Source.fromResource("prim.graph"))
    val zsy = TryUsing.tryIt(triedSource) { source =>
      p.parseSource[Triplet[Int, Double, EdgeType]](p.parseTriple)(source)
    }
    zsy match
      case Success(triplets) =>
        triplesToTryGraph[Int, Double](Vertex.createWithSet)(triplets) match
          case Success(graph: UndirectedGraph[Int, Double] @unchecked) => f(graph)
          case Failure(x) => fail("graph construction failed", x)
      case Failure(x) =>
        fail("parse failed", x)

  // -------------------------------------------------------------------------
  // Result structure
  // -------------------------------------------------------------------------

  behavior of "MST.kruskal — result structure"

  it should "produce exactly 7 MST edges (N-1 for 8 vertices)" in :
    withPrimGraph { graph =>
      MST.kruskal(graph).size shouldBe 7
    }

  it should "return edges in non-decreasing weight order" in :
    withPrimGraph { graph =>
      val weights = MST.kruskal(graph).map(_.attribute)
      weights shouldBe weights.sorted
    }

  // -------------------------------------------------------------------------
  // MST weight
  // -------------------------------------------------------------------------

  behavior of "MST.kruskal — total weight"

  it should "produce MST with total weight 1.81" in :
    withPrimGraph { graph =>
      MST.kruskal(graph).map(_.attribute).sum shouldBe 1.81 +- 0.001
    }

  // -------------------------------------------------------------------------
  // MST edge set
  // -------------------------------------------------------------------------

  behavior of "MST.kruskal — edge set"

  it should "include edge 0-7 with weight 0.16" in :
    withPrimGraph { graph =>
      MST.kruskal(graph).map(_.attribute) should contain(0.16)
    }

  it should "include edge 2-3 with weight 0.17" in :
    withPrimGraph { graph =>
      MST.kruskal(graph).map(_.attribute) should contain(0.17)
    }

  it should "include edge 1-7 with weight 0.19" in :
    withPrimGraph { graph =>
      MST.kruskal(graph).map(_.attribute) should contain(0.19)
    }

  it should "include edge 0-2 with weight 0.26" in :
    withPrimGraph { graph =>
      MST.kruskal(graph).map(_.attribute) should contain(0.26)
    }

  it should "include edge 5-7 with weight 0.28" in :
    withPrimGraph { graph =>
      MST.kruskal(graph).map(_.attribute) should contain(0.28)
    }

  it should "include edge 4-5 with weight 0.35" in :
    withPrimGraph { graph =>
      MST.kruskal(graph).map(_.attribute) should contain(0.35)
    }

  it should "include edge 6-2 with weight 0.40" in :
    withPrimGraph { graph =>
      MST.kruskal(graph).map(_.attribute) should contain(0.40)
    }

  it should "not include edge 1-3 with weight 0.29 (would create cycle)" in :
    withPrimGraph { graph =>
      MST.kruskal(graph).map(_.attribute) should not contain 0.29
    }

  // -------------------------------------------------------------------------
  // Per-vertex endpoint checks
  // -------------------------------------------------------------------------

  behavior of "MST.kruskal — edge endpoints"

  it should "include an edge connecting vertices 0 and 7" in :
    withPrimGraph { graph =>
      val mst = MST.kruskal(graph)
      mst.exists(e => Set(e.white, e.black) == Set(0, 7)) shouldBe true
    }

  it should "include an edge connecting vertices 2 and 3" in :
    withPrimGraph { graph =>
      val mst = MST.kruskal(graph)
      mst.exists(e => Set(e.white, e.black) == Set(2, 3)) shouldBe true
    }

  it should "include an edge connecting vertices 4 and 5" in :
    withPrimGraph { graph =>
      val mst = MST.kruskal(graph)
      mst.exists(e => Set(e.white, e.black) == Set(4, 5)) shouldBe true
    }

  it should "include an edge connecting vertices 6 and 2" in :
    withPrimGraph { graph =>
      val mst = MST.kruskal(graph)
      mst.exists(e => Set(e.white, e.black) == Set(6, 2)) shouldBe true
    }

  // -------------------------------------------------------------------------
  // Inline graph tests
  // -------------------------------------------------------------------------

  behavior of "MST.kruskal — inline graphs"

  it should "find the single MST edge in a two-vertex graph" in :
    val triplets: Seq[Triplet[Int, Double, EdgeType]] = Seq(Triplet(0, 1, Some(1.0), Undirected))
    triplesToTryGraph[Int, Double](Vertex.createWithSet)(triplets) match
      case Success(graph: UndirectedGraph[Int, Double] @unchecked) =>
        val mst = MST.kruskal(graph)
        mst.size shouldBe 1
        mst.head.attribute shouldBe 1.0
      case other =>
        fail(s"unexpected: $other")

  it should "find both edges in a three-vertex path (no cycle possible)" in :
    val triplets: Seq[Triplet[Int, Double, EdgeType]] = Seq(
      Triplet(0, 1, Some(1.0), Undirected),
      Triplet(1, 2, Some(2.0), Undirected)
    )
    triplesToTryGraph[Int, Double](Vertex.createWithSet)(triplets) match
      case Success(graph: UndirectedGraph[Int, Double] @unchecked) =>
        val mst = MST.kruskal(graph)
        mst.size shouldBe 2
        mst.map(_.attribute) should contain allOf(1.0, 2.0)
      case other =>
        fail(s"unexpected: $other")

  it should "skip the heavy edge in a triangle" in :
    val triplets: Seq[Triplet[Int, Double, EdgeType]] = Seq(
      Triplet(0, 1, Some(1.0), Undirected),
      Triplet(1, 2, Some(2.0), Undirected),
      Triplet(0, 2, Some(3.0), Undirected)
    )
    triplesToTryGraph[Int, Double](Vertex.createWithSet)(triplets) match
      case Success(graph: UndirectedGraph[Int, Double] @unchecked) =>
        val mst = MST.kruskal(graph)
        mst.size shouldBe 2
        mst.map(_.attribute) should contain(1.0)
        mst.map(_.attribute) should contain(2.0)
        mst.map(_.attribute) should not contain 3.0
      case other =>
        fail(s"unexpected: $other")
  // =====================================================================================
  // Issue #130: the MST result is the edge-keyed TraversalResult, not a bare Seq.
  //
  // Every other test in this file exercises the Seq side, which is the point -- extending Seq is
  // what made the change cost callers nothing, and 46 sites across the suite kept working
  // untouched. These cover the side that is new: the TraversalResult interface.
  //
  // Note that `edgeTraverse(i)` and `apply(i)` are deliberately the same operation. A positional
  // index IS what the edge-keyed branch has always meant by a key, so Seq's `apply` and the
  // branch's `edgeTraverse` cannot sensibly differ.

  behavior of "MST.kruskal as a TraversalResult"

  it should "answer vertexTraverse with None, having no vertex key" in :
    withPrimGraph { g =>
      val mst = MST.kruskal(g)
      mst.vertexTraverse(0) shouldBe None
      mst.vertexTraverse(7) shouldBe None
    }

  it should "key the edges by position" in :
    withPrimGraph { g =>
      val mst = MST.kruskal(g)
      mst.keySet shouldBe Range(0, mst.size).toSet
      mst.edgeTraverse(0) shouldBe mst.head
      mst.edgeTraverse(mst.size - 1) shouldBe mst.last
    }

  it should "agree with itself as a Seq" in :
    withPrimGraph { g =>
      val mst = MST.kruskal(g)
      mst.size shouldBe 7 // 8 vertices, connected
      mst.toSeq.size shouldBe mst.size
      Range(0, mst.size).map(mst.edgeTraverse) shouldBe mst.toSeq
    }

  it should "hold the edges in ascending weight order, which is what the order means here" in :
    withPrimGraph { g =>
      val weights = MST.kruskal(g).map(_.attribute).toSeq
      weights shouldBe weights.sorted
    }

  it should "give Boruvka the same type and the same multiset of weights" in :
    // Different edge ORDER -- Borůvka's is round order, Kruskal's is weight order -- but the same
    // type, which is the point of #130, and necessarily the same edges.
    //
    // Compared as sorted multisets rather than by total, deliberately. Summing the same weights
    // in a different order gives a different Double: these two produce 1.81 and
    // 1.8099999999999998 on prim.graph. That is floating-point addition being non-associative,
    // not a disagreement about the MST, and asserting on the total would make this test a
    // hostage to the order each algorithm happens to emit.
    withPrimGraph { g =>
      val k = MST.kruskal(g)
      val b = MST.boruvka(g)
      b.size shouldBe k.size
      b.map(_.attribute).toSeq.sorted shouldBe k.map(_.attribute).toSeq.sorted
      b.map(_.attribute).sum shouldBe k.map(_.attribute).sum +- 1e-9
      b.vertexTraverse(0) shouldBe None
    }
