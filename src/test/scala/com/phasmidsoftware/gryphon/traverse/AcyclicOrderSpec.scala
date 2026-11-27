/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.traverse

import com.phasmidsoftware.gryphon.adjunct.DirectedGraph
import com.phasmidsoftware.gryphon.core.{Directed, EdgeType, Triplet, Vertex}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scala.util.{Random, Success}

/**
 * The order-taking overloads on the acyclic shortest-paths methods — issue #158.
 *
 * ==What #158 was==
 *
 * `ShortestPaths.acyclic` relaxes in `DirectedGraph.topologicalOrder`, a memoized `lazy val`.
 * That order is what decides which of several equal-cost paths is reported, and a `lazy val`
 * takes no `using` clause, so no seed could reach it: the answer was stable for one graph
 * instance and arbitrary across instances, with no way to pin it.
 *
 * The fix is not a `Random` — #154 considered one and it would have decided nothing, since the
 * only order a generator reaches inside the algorithm is provably inert. It is to take the
 * '''order itself''', which is the thing that actually decides the answer.
 *
 * ==What has to be true for that to be worth anything==
 *
 * Three things, and all three are asserted below rather than argued:
 *
 *   1. The order genuinely changes the answer. If two valid topological orders gave the same
 *      predecessors, the parameter would be decoration.
 *   2. Supplying an order makes the result reproducible, where the cached form could not be.
 *   3. An order that is not topological is rejected, rather than silently producing a wrong
 *      answer — which is what an unchecked bad order would do, since the relaxation would read a
 *      vertex before its predecessors were final.
 */
class AcyclicOrderSpec extends AnyFlatSpec with Matchers:

  given Ordering[Double] = scala.math.Ordering.Double.TotalOrdering

  private def directed(ts: Seq[Triplet[Int, Double, EdgeType]]): DirectedGraph[Int, Double] =
    DirectedGraph.triplesToTryGraph[Int, Double](Vertex.createWithSet)(ts) match
      case Success(g: DirectedGraph[Int, Double] @unchecked) => g
      case other => fail(s"construction failed: $other")

  /**
   * A diamond with two equal-cost routes to vertex 3, each costing 2.0.
   *
   * This is the shape the whole issue is about. Which of `1 -> 3` and `2 -> 3` is recorded
   * depends entirely on which of 1 and 2 is relaxed first, which is a property of the
   * topological order and of nothing else. A graph without a tie would prove nothing here.
   */
  private def diamond: DirectedGraph[Int, Double] = directed(Seq(
    Triplet(0, 1, Some(1.0), Directed),
    Triplet(0, 2, Some(1.0), Directed),
    Triplet(1, 3, Some(1.0), Directed),
    Triplet(2, 3, Some(1.0), Directed)))

  private def predecessorOf3(g: DirectedGraph[Int, Double], order: Seq[Int]): String =
    ShortestPaths.acyclic[Int, Double](g, 0, order).vertexTraverse(3).toString

  behavior of "ShortestPaths.acyclic with a supplied order"

  // (1) Without this the parameter would be decoration: both orders are valid topological orders
  // of the same graph, and they disagree about the answer. That disagreement IS issue #158.
  it should "let the order decide which equal-cost predecessor is recorded" in {
    val g = diamond
    predecessorOf3(g, Seq(0, 1, 2, 3)) should not be predecessorOf3(g, Seq(0, 2, 1, 3))
  }

  // ... while the cost, which is what most callers assert on, is the same either way.
  it should "give the same cost whichever order is used" in {
    val g = diamond
    val a = ShortestPaths.acyclic[Int, Double](g, 0, Seq(0, 1, 2, 3))
    val b = ShortestPaths.acyclic[Int, Double](g, 0, Seq(0, 2, 1, 3))
    TraversalResult.pathEdgesTo(a)(3).map(_.attribute).sum shouldBe
            TraversalResult.pathEdgesTo(b)(3).map(_.attribute).sum +- 1e-9
  }

  // (2) The payoff. A seeded TopologicalSort.sort produces a reproducible order, and the order
  // carries that reproducibility into the shortest-path tree -- which the two-argument form
  // cannot do, because no seed reaches the memoized order it uses.
  it should "be reproducible when the order comes from a seeded TopologicalSort" in {
    val g = diamond

    def treeFor(seed: Int): String =
      given Random = Random(seed)
      val order = TopologicalSort.sort(g).getOrElse(fail("diamond is a DAG"))
      predecessorOf3(g, order)

    treeFor(11) shouldBe treeFor(11)
    treeFor(11) shouldBe treeFor(11) // and again: memoization is not doing this for us

    // Non-vacuity: the seed has to be making a real choice, or the line above would hold just as
    // well on an implementation that ignored it.
    (0 until 40).map(treeFor).distinct.size should be > 1
  }

  it should "agree with the cached form when given the graph's own order" in {
    val g = diamond
    val cached = g.topologicalOrder.getOrElse(fail("diamond is a DAG"))
    ShortestPaths.acyclic[Int, Double](g, 0, cached).map shouldBe
            ShortestPaths.acyclic[Int, Double](g, 0).map
  }

  it should "work for longestAcyclic too" in {
    val g = diamond
    val order = Seq(0, 1, 2, 3)
    val longest = ShortestPaths.longestAcyclic[Int, Double](g, 0, order)
    TraversalResult.pathEdgesTo(longest)(3).map(_.attribute).sum shouldBe 2.0 +- 1e-9
  }

  // ---------------------------------------------------------------------------
  // (3) Validation
  // ---------------------------------------------------------------------------

  // An unchecked bad order does not throw by itself -- the relaxation simply reads a vertex
  // before its predecessors are final and returns a wrong answer. That is the failure mode the
  // check exists to convert into an exception, and #162 is the counter-example where the
  // equivalent check was too expensive to make.
  it should "reject an order that is not topological" in {
    val thrown = the[IllegalArgumentException] thrownBy
            ShortestPaths.acyclic[Int, Double](diamond, 0, Seq(3, 1, 2, 0))
    thrown.getMessage should include("not topological")
  }

  it should "reject an order that omits a vertex" in {
    val thrown = the[IllegalArgumentException] thrownBy
            ShortestPaths.acyclic[Int, Double](diamond, 0, Seq(0, 1, 2))
    thrown.getMessage should include("every vertex exactly once")
  }

  it should "reject an order that repeats a vertex" in {
    val thrown = the[IllegalArgumentException] thrownBy
            ShortestPaths.acyclic[Int, Double](diamond, 0, Seq(0, 1, 1, 2, 3))
    thrown.getMessage should include("repeats a vertex")
  }

  it should "reject an order containing a vertex the graph does not have" in {
    val thrown = the[IllegalArgumentException] thrownBy
            ShortestPaths.acyclic[Int, Double](diamond, 0, Seq(0, 1, 2, 99))
    thrown.getMessage should include("every vertex exactly once")
  }
