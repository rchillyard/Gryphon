/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.traverse

import com.phasmidsoftware.gryphon.adjunct.{AttributedDirectedEdge, DirectedGraph}
import com.phasmidsoftware.gryphon.core.{Directed, EdgeType, Triplet, Vertex}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scala.util.{Random, Success}

/**
 * A* — issue #162.
 *
 * ==What is actually being claimed==
 *
 * A* returns the same optimal cost as Dijkstra while settling fewer vertices. Only the first half
 * is a correctness property; the second is the entire reason to use it, so both are asserted.
 *
 * The comparison that means something is '''A* against A*''': Manhattan heuristic versus the
 * constant zero, both stopping at the same goal. Comparing against `dijkstra` instead would prove
 * nothing, because `dijkstra` has no goal and settles the whole graph — it would be beaten by any
 * search that stops early, heuristic or not.
 */
class AStarSpec extends AnyFlatSpec with Matchers:

  given Ordering[Double] = scala.math.Ordering.Double.TotalOrdering

  private def directed(ts: Seq[Triplet[Int, Double, EdgeType]]): DirectedGraph[Int, Double] =
    DirectedGraph.triplesToTryGraph[Int, Double](Vertex.createWithSet)(ts) match
      case Success(g: DirectedGraph[Int, Double] @unchecked) => g
      case other => fail(s"construction failed: $other")

  // ---------------------------------------------------------------------------
  // A 12x12 grid with an expensive wall
  // ---------------------------------------------------------------------------

  private val N = 12
  private def vertex(r: Int, c: Int): Int = r * N + c
  private def row(v: Int): Int = v / N
  private def col(v: Int): Int = v % N

  private val start = vertex(0, 0)
  private val goal = vertex(N - 1, N - 1)

  /** Column 5 is costly to enter, except at the last two rows, which leave a gap to go round by. */
  private def isWall(v: Int): Boolean = col(v) == 5 && row(v) <= 9

  private def enterCost(v: Int): Double = if isWall(v) then 10.0 else 1.0

  private def grid: DirectedGraph[Int, Double] =
    val ts = for
      r <- 0 until N
      c <- 0 until N
      (dr, dc) <- Seq((0, 1), (1, 0), (0, -1), (-1, 0))
      nr = r + dr
      nc = c + dc
      if nr >= 0 && nr < N && nc >= 0 && nc < N
    yield Triplet[Int, Double, EdgeType](vertex(r, c), vertex(nr, nc), Some(enterCost(vertex(nr, nc))), Directed)
    directed(ts.toSeq)

  /**
   * Manhattan distance to the goal.
   *
   * Consistent, because every edge costs at least 1 and a single step changes the Manhattan
   * distance by at most 1: `h(v) - h(w) <= 1 <= cost(v, w)`. Consistency is what this engine
   * requires — see the note on the inconsistent case at the end of this spec.
   */
  private def manhattan(v: Int): Double =
    (math.abs(row(v) - row(goal)) + math.abs(col(v) - col(goal))).toDouble

  /** Consistent, trivially, and turns A* into Dijkstra-with-a-goal. */
  private def zero(v: Int): Double = 0.0

  private def costOf(result: TraversalResult[Int, AttributedDirectedEdge[Int, Double]], target: Int): Double =
    TraversalResult.pathEdgesTo(result)(target).map(_.attribute).sum

  behavior of "ShortestPaths.aStar"

  it should "find the same optimal cost as Dijkstra" in {
    given Random = Random(42)

    val g = grid
    val viaDijkstra = ShortestPaths.dijkstra[Int, Double](g, start)
    val viaAStar = ShortestPaths.aStar[Int, Double](g, start, goal, manhattan)

    costOf(viaAStar, goal) shouldBe costOf(viaDijkstra, goal) +- 1e-9
  }

  // The point of the algorithm, and the measurement that justifies the issue. Both searches stop
  // at the same goal, so the only difference between them is the heuristic.
  it should "settle fewer vertices than the same search with no heuristic" in {
    given Random = Random(42)

    val g = grid
    val guided = ShortestPaths.aStar[Int, Double](g, start, goal, manhattan)
    val unguided = ShortestPaths.aStar[Int, Double](g, start, goal, zero)

    info(s"settled: Manhattan ${guided.size}, zero-heuristic ${unguided.size}, grid ${N * N}")

    guided.size should be < unguided.size
    costOf(guided, goal) shouldBe costOf(unguided, goal) +- 1e-9
  }

  it should "reconstruct a connected path from start to goal" in {
    given Random = Random(42)

    val g = grid
    val result = ShortestPaths.aStar[Int, Double](g, start, goal, manhattan)
    val path = TraversalResult.pathTo(result)(goal)

    path.head shouldBe start
    path.last shouldBe goal
    // Every consecutive pair is a grid step: adjacent, and never diagonal.
    path.sliding(2).foreach {
      case Seq(u, v) =>
        (math.abs(row(u) - row(v)) + math.abs(col(u) - col(v))) shouldBe 1
      case _ => ()
    }
  }

  it should "stop immediately when the goal is the start" in {
    given Random = Random(42)

    val result = ShortestPaths.aStar[Int, Double](grid, start, start, manhattan)
    TraversalResult.pathEdgesTo(result)(start) shouldBe Seq.empty
  }

  // ---------------------------------------------------------------------------
  // The consistency requirement, asserted rather than only documented
  // ---------------------------------------------------------------------------

  /**
   * Four vertices where an admissible but INCONSISTENT heuristic makes A* return a worse path,
   * silently. This is the documented limitation, so it is tested: if Visitor ever gains a
   * re-openable visited set, this test fails and sends the next reader to the scaladoc.
   *
   * {{{
   *   0 --1.0--> 1 --1.0--> 2 --1.0--> 3        true shortest 0 -> 3 is 3.0
   *   0 --2.5--------------> 2                  the tempting shortcut
   * }}}
   *
   * `h(1) = 2.0` is admissible (the true remaining cost from 1 is 2.0) but inconsistent, because
   * `h(1) <= cost(1, 2) + h(2)` would require `2.0 <= 1.0 + 0.0`. The effect: vertex 2 is settled
   * early at 2.5 via the shortcut, and when 1 later offers it 2.0 the engine will not re-open it.
   */
  private def tricky: DirectedGraph[Int, Double] = directed(Seq(
    Triplet(0, 1, Some(1.0), Directed),
    Triplet(1, 2, Some(1.0), Directed),
    Triplet(2, 3, Some(1.0), Directed),
    Triplet(0, 2, Some(2.5), Directed)))

  private def inconsistent(v: Int): Double = if v == 1 then 2.0 else 0.0

  it should "be optimal on the tricky graph with a consistent heuristic" in {
    given Random = Random(42)

    val g = tricky
    costOf(ShortestPaths.dijkstra[Int, Double](g, 0), 3) shouldBe 3.0 +- 1e-9
    costOf(ShortestPaths.aStar[Int, Double](g, 0, 3, zero), 3) shouldBe 3.0 +- 1e-9
  }

  // Asserting the wrong answer on purpose. The scaladoc says an inconsistent heuristic returns a
  // suboptimal path with nothing raised; an untested warning of that kind quietly stops being
  // true. 3.5 is the shortcut 0 -> 2 -> 3, which is what the engine commits to.
  it should "return a SUBOPTIMAL path for an inconsistent heuristic, and raise nothing" in {
    given Random = Random(42)

    val result = ShortestPaths.aStar[Int, Double](tricky, 0, 3, inconsistent)
    costOf(result, 3) shouldBe 3.5 +- 1e-9
  }

  // ---------------------------------------------------------------------------
  // Invariance
  // ---------------------------------------------------------------------------

  // The cost is a property of the graph. Which equal-cost path is reported is not, which is what
  // the `@note Result:` line on `aStar` claims and what a seed pins.
  it should "give one cost over many seeds, and repeat itself for one seed" in {
    val g = grid
    val costs = (0 until 20).map { s =>
      given Random = Random(s)
      costOf(ShortestPaths.aStar[Int, Double](g, start, goal, manhattan), goal)
    }
    costs.distinct.size shouldBe 1

    def treeFor(seed: Int): String =
      given Random = Random(seed)
      val r = ShortestPaths.aStar[Int, Double](g, start, goal, manhattan)
      TraversalResult.pathTo(r)(goal).mkString(",")

    treeFor(7) shouldBe treeFor(7)
  }
