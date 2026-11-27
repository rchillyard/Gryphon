/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.traverse

import com.phasmidsoftware.gryphon.adjunct.UndirectedGraph
import com.phasmidsoftware.gryphon.adjunct.UndirectedGraph.triplesToTryGraph
import com.phasmidsoftware.gryphon.core.{EdgeType, Triplet, Vertex}
import com.phasmidsoftware.gryphon.parse.GraphParser
import com.phasmidsoftware.gryphon.util.TryUsing
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import scala.io.Source
import scala.util.{Failure, Success, Try}

/**
 * Tests for Borůvka's minimum spanning tree algorithm.
 *
 * prim.graph (Sedgewick tinyEWG) — 8 vertices (0–7), 16 undirected weighted edges.
 *
 * MST edges (7 edges for 8 vertices):
 * 0-7: 0.16,  2-3: 0.17,  1-7: 0.19,  0-2: 0.26,
 * 5-7: 0.28,  4-5: 0.35,  6-2: 0.40
 * Total MST weight: 1.81
 */
class BoruvkaSpec extends AnyFlatSpec with should.Matchers:

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

  behavior of "MST.boruvka — result structure"

  it should "produce exactly 7 MST edges (N-1 for 8 vertices)" in :
    withPrimGraph { graph =>
      MST.boruvka(graph).size shouldBe 7
    }

  // -------------------------------------------------------------------------
  // MST weight
  // -------------------------------------------------------------------------

  behavior of "MST.boruvka — total weight"

  it should "produce MST with total weight 1.81" in :
    withPrimGraph { graph =>
      MST.boruvka(graph).map(_.attribute).sum shouldBe 1.81 +- 0.001
    }

  // -------------------------------------------------------------------------
  // MST edge set — same result as Kruskal
  // -------------------------------------------------------------------------

  behavior of "MST.boruvka — edge set"

  it should "include edge 0-7 with weight 0.16" in :
    withPrimGraph { graph =>
      MST.boruvka(graph).map(_.attribute) should contain(0.16)
    }

  it should "include edge 2-3 with weight 0.17" in :
    withPrimGraph { graph =>
      MST.boruvka(graph).map(_.attribute) should contain(0.17)
    }

  it should "include edge 1-7 with weight 0.19" in :
    withPrimGraph { graph =>
      MST.boruvka(graph).map(_.attribute) should contain(0.19)
    }

  it should "include edge 0-2 with weight 0.26" in :
    withPrimGraph { graph =>
      MST.boruvka(graph).map(_.attribute) should contain(0.26)
    }

  it should "include edge 5-7 with weight 0.28" in :
    withPrimGraph { graph =>
      MST.boruvka(graph).map(_.attribute) should contain(0.28)
    }

  it should "include edge 4-5 with weight 0.35" in :
    withPrimGraph { graph =>
      MST.boruvka(graph).map(_.attribute) should contain(0.35)
    }

  it should "include edge 6-2 with weight 0.40" in :
    withPrimGraph { graph =>
      MST.boruvka(graph).map(_.attribute) should contain(0.40)
    }

  it should "not include edge 1-3 with weight 0.29 (cycle)" in :
    withPrimGraph { graph =>
      MST.boruvka(graph).map(_.attribute) should not contain 0.29
    }

  // -------------------------------------------------------------------------
  // Agree with Kruskal
  // -------------------------------------------------------------------------

  behavior of "MST.boruvka — agreement with Kruskal"

  it should "produce the same MST edge weights as Kruskal" in :
    withPrimGraph { graph =>
      val boruvkaWeights = MST.boruvka(graph).map(_.attribute).toSet
      val kruskalWeights = MST.kruskal(graph).map(_.attribute).toSet
      boruvkaWeights shouldBe kruskalWeights
    }

  // =====================================================================================
  // Issue #79: the in-round cycle guard in `mergeUniqueEdges`.
  //
  //     if wcAcc.isConnected(u, v) then (wcAcc, edges) else (wcAcc.connect(u, v), edges :+ edge)
  //
  // This was the one Stryker4s survivor nobody could kill: mutating the condition to `false`
  // passed the whole suite, and instrumenting the branch showed it never executed at all. Every
  // existing Borůvka test uses prim.graph, whose sixteen weights are distinct, and with distinct
  // weights the round's chosen edges can never close a cycle -- which is why the textbook states
  // the algorithm for distinct weights.
  //
  // The witness below was found by search: five graph families, each over triplet permutations
  // and 300 vertex relabellings, ~36,000 runs. Two things it taught us.
  //
  // First, the lever is NOT the triplet order, which is what an earlier search varied without
  // ever reaching the guard. It is the order of `vertices` (`vertexMap.keySet.toSeq`), because a
  // component's chosen edge comes from the FIRST of its vertices to offer a crossing edge --
  // `cheapestCrossingEdges` keeps the incumbent on a tie, and here every weight ties. Labelling
  // these six vertices 1..6 rather than 0..5 is the whole difference: it changes the hash order
  // enough that each pair picks the crossing edge going the same way round the 6-cycle, so all
  // three survive `uniqueCrossingEdges` (which keys by component-root PAIR) and together close
  // a cycle.
  //
  // Second, the guard is live, not dead. Measured with the condition stubbed to `false`:
  //
  //     edges = 6 on a 6-vertex connected graph -- a cycle, not a spanning tree
  //     total weight = 9.0, against the true 7.0
  //
  // Both assertions below therefore fail under the mutant. #79 asked whether to demonstrate a
  // graph that needs the guard or remove it as dead; this is the demonstration, so it stays.
  //
  // CAVEAT worth knowing: the witness depends on the iteration order of a hash map over the keys
  // 1..6. That is deterministic for a given Scala version but is not a guarantee across versions.
  // If it ever changes, these assertions still hold -- a correct MST is a correct MST -- but they
  // may stop exercising the guard, silently. If Stryker4s reports this survivor again, re-run the
  // search rather than assuming the case is unreachable.

  behavior of "Boruvka with tied weights"

  private val tiedCycle: Seq[Triplet[Int, Double, EdgeType]] = Seq(
    Triplet(1, 2, Some(1.0), com.phasmidsoftware.gryphon.core.Undirected),
    Triplet(3, 4, Some(1.0), com.phasmidsoftware.gryphon.core.Undirected),
    Triplet(5, 6, Some(1.0), com.phasmidsoftware.gryphon.core.Undirected),
    Triplet(2, 3, Some(2.0), com.phasmidsoftware.gryphon.core.Undirected),
    Triplet(4, 5, Some(2.0), com.phasmidsoftware.gryphon.core.Undirected),
    Triplet(6, 1, Some(2.0), com.phasmidsoftware.gryphon.core.Undirected)
  )

  private def withTiedCycle[A](f: UndirectedGraph[Int, Double] => A): A =
    triplesToTryGraph[Int, Double](Vertex.createWithSet)(tiedCycle) match
      case Success(g: UndirectedGraph[Int, Double] @unchecked) => f(g)
      case other => fail(s"graph construction failed: $other")

  it should "return a spanning TREE, not a cycle, when a round's edges would close one" in :
    withTiedCycle { g =>
      g.order shouldBe 6
      MST.boruvka(g).size shouldBe 5 // 6 under the mutant
    }

  it should "agree with Kruskal on total weight" in :
    withTiedCycle { g =>
      MST.boruvka(g).map(_.attribute).sum shouldBe 7.0 // 9.0 under the mutant
      MST.boruvka(g).map(_.attribute).sum shouldBe MST.kruskal(g).map(_.attribute).sum
    }

  it should "choose every cheap edge and exactly two of the three expensive ones" in :
    // The three weight-1.0 pair edges must all be in: each is the unique cheapest edge of its
    // own cut. Exactly two of the three weight-2.0 crossing edges join them into a tree; the
    // third would close the cycle and is what the guard rejects.
    withTiedCycle { g =>
      val weights = MST.boruvka(g).map(_.attribute)
      weights.count(_ == 1.0) shouldBe 3
      weights.count(_ == 2.0) shouldBe 2
    }
