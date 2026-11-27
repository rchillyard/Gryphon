package com.phasmidsoftware.gryphon.traverse

import com.phasmidsoftware.gryphon.adjunct.UndirectedGraph
import com.phasmidsoftware.gryphon.core.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scala.util.{Random, Success}

/**
 * The contract the three MST algorithms share.
 *
 * They do not share a representation, and should not: Prim grows outwards from a start vertex and
 * answers with a parent map, while Kruskal and Borůvka need no start vertex and answer with an
 * ordered edge list whose order is information. 1.13.0 made that distinction deliberately.
 *
 * What they do share is that every answer is a set of edges with a total weight, and before this
 * change nothing in the type system said so — the two branches of `TraversalResult` are keyed
 * differently (by vertex, by position), so no function could take all three results.
 *
 * These tests are about `MSTResult` rather than about any algorithm.
 */
class MSTResultSpec extends AnyFlatSpec with Matchers:

  /** Sedgewick & Wayne's tinyEWG, whose MST weight is the well-known 1.81. */
  private def weighted: UndirectedGraph[Int, Double] =
    val ts: Seq[Triplet[Int, Double, EdgeType]] = Seq(
      Triplet(0, 7, Some(0.16), Undirected), Triplet(2, 3, Some(0.17), Undirected),
      Triplet(1, 7, Some(0.19), Undirected), Triplet(0, 2, Some(0.26), Undirected),
      Triplet(5, 7, Some(0.28), Undirected), Triplet(1, 3, Some(0.29), Undirected),
      Triplet(1, 5, Some(0.32), Undirected), Triplet(2, 7, Some(0.34), Undirected),
      Triplet(4, 5, Some(0.35), Undirected), Triplet(1, 2, Some(0.36), Undirected),
      Triplet(4, 7, Some(0.37), Undirected), Triplet(0, 4, Some(0.38), Undirected),
      Triplet(6, 2, Some(0.40), Undirected), Triplet(3, 6, Some(0.52), Undirected),
      Triplet(6, 0, Some(0.58), Undirected), Triplet(6, 4, Some(0.93), Undirected))
    UndirectedGraph.triplesToTryGraph[Int, Double](Vertex.createWithSet)(ts) match
      case Success(g: UndirectedGraph[Int, Double] @unchecked) => g
      case other => fail(s"construction failed: $other")

  behavior of "MSTResult"

  // The point of the trait. Before this, the three results had no common type at all: Prim's is
  // TraversalResult[V, Edge], Kruskal's is TraversalResult[Int, Edge] -- keyed by POSITION -- so
  // not even the trait unified them.
  it should "let one function take any of the three algorithms' results" in {
    def weightOf(r: MSTResult[Int, Double]): Double = r.totalWeight

    given Random = Random(42)

    val g = weighted
    weightOf(MST.kruskal(g)) shouldBe 1.81 +- 0.001
    weightOf(MST.boruvka(g)) shouldBe 1.81 +- 0.001
    weightOf(MST.prim(g, 0)) shouldBe 1.81 +- 0.001
  }

  it should "give all three the same edge count on a connected graph" in {
    given Random = Random(42)

    val g = weighted
    val results: Seq[MSTResult[Int, Double]] = Seq(MST.kruskal(g), MST.boruvka(g), MST.prim(g, 0))
    results.foreach(_.edges.size shouldBe g.order - 1) // a spanning tree of 8 vertices
  }

  // The total is the one number all three agree on exactly. WHICH tree they return can differ --
  // Prim varies with adjacency order, the other two are invariant but may still pick a different
  // equal-weight tree from each other -- so this is the right thing to assert, and the wrong
  // thing to replace with an expected edge list.
  it should "agree on the total weight even where the trees may differ" in {
    given Random = Random(42)

    val g = weighted
    val totals = Seq(MST.kruskal(g), MST.boruvka(g), MST.prim(g, 0)).map(_.totalWeight)

    // Compared with a tolerance, NOT with `distinct`. The first draft of this test used exact
    // equality and failed with two distinct totals from three algorithms that the test above had
    // just shown all equal 1.81 to within a thousandth. The cause is not disagreement: addition
    // of Doubles is not associative, `totalWeight` folds in each result's own edge order, and
    // those orders differ. See the note on `MSTResult.totalWeight`.
    totals.foreach(_ shouldBe totals.head +- 1e-9)
  }

  behavior of "the representations, which deliberately still differ"

  it should "keep Prim's parent map" in {
    given Random = Random(42)

    val result = MST.prim(weighted, 0)
    result.map.keySet should not contain 0 // the start vertex has no attaching edge
    result.map.keySet.size shouldBe 7
    result.edges.size shouldBe 7 // the same edges, viewed as a set
  }

  it should "keep Kruskal's ascending-weight order" in {
    val weights = MST.kruskal(weighted).edges.map(_.attribute)
    weights shouldBe weights.sorted
  }

  // MSTEdgeList is still a Seq, so everything that worked on the old result still works.
  it should "stay a Seq, so existing callers are unaffected" in {
    val mst = MST.kruskal(weighted)
    mst.size shouldBe 7
    mst.head.attribute shouldBe 0.16 +- 0.001
    mst.map(_.attribute).sum shouldBe 1.81 +- 0.001
  }
