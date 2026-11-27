package com.phasmidsoftware.gryphon.traverse

import com.phasmidsoftware.gryphon.adjunct.DirectedGraph
import com.phasmidsoftware.gryphon.core.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scala.util.{Random, Success}

/**
 * Does a caller's seed reach every traversal? Issues #131, #153, #154.
 *
 * Every test here is `ignore`, and each asserts behaviour the library does not yet have: that
 * `given Random = Random(n)` in scope settles the result. Today these methods take no `Random`,
 * so the given sits unused and the traversal builds a fresh generator per vertex.
 *
 * **They compile, and they will pass unchanged once the fix lands.** That is the point of writing
 * them against the current signatures rather than the intended ones: a method that gains a
 * `(using Random = Random())` parameter starts picking up an in-scope given with no change at the
 * call site. So each of these is a working specification of its issue — delete the `ignore` and
 * watch it go green.
 *
 * `ignore` rather than `pending`: ScalaTest's `pending` runs the body and reports a *failure* if
 * it throws, so a pending test whose assertions do not yet hold breaks the build. `pending` means
 * "not yet written", not "not yet implemented".
 *
 * ==The compiler already agrees==
 *
 * Without the `@nowarn` below, every `given Random = r` here draws
 *
 * {{{
 * [warn] unused local definition
 * }}}
 *
 * which is `-Wunused` observing that nothing consumes the generator. That warning '''is''' the
 * defect, stated by the compiler rather than by a test: the seed has nowhere to go. It is
 * suppressed only because `-Werror` would otherwise fail the build on a file whose whole purpose
 * is to record the problem.
 *
 * When #131, #153 and #154 land, those methods will take the given implicitly, the warnings will
 * stop, and the `@nowarn` can come off along with the `ignore`s.
 */
@scala.annotation.nowarn("msg=unused local definition")
class SeedReachabilitySpec extends AnyFlatSpec with Matchers:

  // A source with five leaves. Shuffle-sensitive: whatever order the sort's loop picks its start
  // vertices in, several children remain unvisited when the source is expanded, so their relative
  // order in the result is decided by adjacency iteration. A diamond would not do -- see
  // TopologicalSortTest, where structure forces the traversal.
  private def star: DirectedGraph[Int, Unit] =
    val triplets: Seq[Triplet[Int, Unit, EdgeType]] = (1 to 5).map(i => Triplet(0, i, None, Directed)).toSeq
    DirectedGraph.triplesToTryGraph[Int, Unit](Vertex.createWithSet)(triplets) match
      case Success(g: DirectedGraph[Int, Unit] @unchecked) => g
      case other => fail(s"construction failed: $other")

  // Two equal-cost paths from 0 to 3. The distance is 2.0 either way; which predecessor is
  // recorded for vertex 3 depends on the order edges are relaxed in.
  private def diamondWeighted: DirectedGraph[Int, Double] =
    val triplets: Seq[Triplet[Int, Double, EdgeType]] = Seq(
      Triplet(0, 1, Some(1.0), Directed),
      Triplet(0, 2, Some(1.0), Directed),
      Triplet(1, 3, Some(1.0), Directed),
      Triplet(2, 3, Some(1.0), Directed))
    DirectedGraph.triplesToTryGraph[Int, Double](Vertex.createWithSet)(triplets) match
      case Success(g: DirectedGraph[Int, Double] @unchecked) => g
      case other => fail(s"construction failed: $other")

  /** Asserts that an in-scope seed settles `f`, and that the fixture could show it if it did. */
  private def seedShouldControl[A](f: Random => A): org.scalatest.Assertion =
    val _: org.scalatest.Assertion = f(Random(42)) shouldBe f(Random(42))
    // The second half is what stops this passing vacuously: a fixture whose result cannot vary
    // would satisfy the first assertion whether or not the seed did any work.
    (0 until 20).map(s => f(Random(s))).distinct.size should be > 1

  behavior of "a caller's seed"

  ignore should "reach TopologicalSort.sort (#131)" in {
    val g = star
    seedShouldControl { r =>
      given Random = r
      TopologicalSort.sort(g)
    }
  }

  ignore should "reach Traversable.filteredAdjacentVertices (#153)" in {
    val g = star
    seedShouldControl { r =>
      given Random = r
      g.filteredAdjacentVertices(_ => true)(0).toList
    }
  }

  ignore should "reach BellmanFord.shortestPaths (#154)" in {
    val g = diamondWeighted
    seedShouldControl { r =>
      given Random = r
      BellmanFord.shortestPaths[Int, Double](g, 0).map(_.map.view.mapValues(_.toString).toMap)
    }
  }

  // AcyclicShortestPaths is deliberately absent. It varies for two independent reasons -- the
  // order it relaxes in, from `topologicalOrder`, and the edge order within each vertex -- so a
  // seed reaching only the second would not settle it. It becomes testable here once #131 is
  // fixed as well; see the discussion on #154.
