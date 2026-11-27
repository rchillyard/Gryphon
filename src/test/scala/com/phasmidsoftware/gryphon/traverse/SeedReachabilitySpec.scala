package com.phasmidsoftware.gryphon.traverse

import com.phasmidsoftware.gryphon.adjunct.DirectedGraph
import com.phasmidsoftware.gryphon.core.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scala.util.{Random, Success}

/**
 * Does a caller's seed reach every traversal? Issues #131, #153, #154.
 *
 * Three of the four methods are fixed in 1.14.0 and tested here. The fourth,
 * `AcyclicShortestPaths`, turned out not to be fixable by adding a parameter, and the last two
 * tests in this file are what establish that rather than assert it.
 *
 * These tests were written in 1.13.0 against the *unfixed* signatures and marked `ignore`: each
 * asserted behaviour the library did not yet have, namely that `given Random = Random(n)` in scope
 * settles the result. That worked because a method which gains a `(using Random = Random())`
 * parameter starts picking up an in-scope given with no change at the call site — so the fix was
 * applied, the `ignore`s were deleted, and the bodies below are unchanged from the day they were
 * written. They are the specifications of their issues, now running.
 *
 * ==The compiler agreed before the tests could==
 *
 * While the defect stood, this file carried `@nowarn("msg=unused local definition")`, because
 * every `given Random = r` here drew
 *
 * {{{
 * [warn] unused local definition
 * }}}
 *
 * — `-Wunused` observing that nothing consumed the generator. That warning '''was''' the defect,
 * stated by the compiler rather than by a test: the seed had nowhere to go. The `@nowarn` is gone
 * now, and under `-Werror` its absence is itself an assertion. If anyone reverts one of these
 * methods to taking no `Random`, the build fails on this file before a single test runs.
 *
 * ==Why `ignore` and not `pending`==
 *
 * Kept as a note for the next time this pattern is wanted. ScalaTest's `pending` runs the body and
 * reports a *failure* if it throws, so a pending test whose assertions do not yet hold breaks the
 * build. `pending` means "not yet written", not "not yet implemented".
 */
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

  it should "reach TopologicalSort.sort (#131)" in {
    val g = star
    seedShouldControl { r =>
      given Random = r
      TopologicalSort.sort(g)
    }
  }

  it should "reach Traversable.filteredAdjacentVertices (#153)" in {
    val g = star
    seedShouldControl { r =>
      given Random = r
      g.filteredAdjacentVertices(_ => true)(0).toList
    }
  }

  it should "reach ShortestPaths.bellmanFord (#154)" in {
    val g = diamondWeighted
    seedShouldControl { r =>
      given Random = r
      ShortestPaths.bellmanFord[Int, Double](g, 0).map(_.map.view.mapValues(_.toString).toMap)
    }
  }

  // --------------------------------------------------------------------------------------
  // AcyclicShortestPaths: the exception, pinned as an exception.
  //
  // It was absent from this spec in 1.13.0 with a note saying it would become testable once
  // #131 landed. It did not, and the note was wrong about why. The reasoning then was that it
  // varies for two reasons -- the topological order and the edge order within a vertex -- so a
  // seed reaching only the second could not settle it. The first half is right. The second is
  // not: the edge order within a vertex settles NOTHING, because each edge writes its own key
  // in the predecessor map. Two edges can only compete by running from one vertex to the same
  // target at equal cost, and `fromTriplets` refuses to build such a graph (#57).
  //
  // So all of the variation comes from the memoized `DirectedGraph.topologicalOrder`, which a
  // `lazy val` cannot take a generator for -- and `AcyclicShortestPaths` therefore has no
  // `Random` parameter at all, because one would have been inert. The two tests below are what
  // establish that, and they are the reason the method's scaladoc can state it as fact.
  //
  // There is no `given Random` anywhere in the two tests below, and that is not an omission.
  // `AcyclicShortestPaths` takes no generator, so one written here would be consumed by nothing
  // -- and `-Wunused` says so, which under `-Werror` fails the build. The compiler will not let
  // this file pretend a seed is in play where none can be. These tests therefore assert the
  // contract the scaladoc states, which is about graph instances rather than seeds.

  it should "be unreachable in AcyclicShortestPaths, whose only reachable order decides nothing" in {
    val g = diamondWeighted // one instance: the topological order is memoized on first use

    // The order a vertex's adjacencies come in IS seedable, and on this fixture it really does
    // vary -- vertex 0 has two of them. This is the order `relax` folds over.
    val adjacencyOrders = (0 until 20).map(s => g.filteredAdjacencies(_ => true)(0)(using Random(s)).map(_.vertex).toList)
    adjacencyOrders.distinct.size should be > 1

    // And it changes nothing, because each edge writes its own key in the predecessor map. That
    // is why the method has no `Random` parameter: there would be nothing for it to settle.
    val results = (0 until 20).map(_ => ShortestPaths.acyclic[Int, Double](g, 0).map.view.mapValues(_.toString).toMap)
    results.distinct.size shouldBe 1
  }

  it should "be beside the point for AcyclicShortestPaths, whose result varies between graph instances" in {
    // Same graph, rebuilt each time, so each instance memoizes its own topological order. This
    // is the variation a caller actually meets, and no seed reaches it -- which is exactly what
    // the method's scaladoc warns about and what #158 would have to address.
    val acrossInstances = (0 until 40).map { _ =>
      ShortestPaths.acyclic[Int, Double](diamondWeighted, 0).map.view.mapValues(_.toString).toMap
    }
    acrossInstances.distinct.size should be > 1

    // The costs, by contrast, are invariant -- which is why the scaladoc tells a caller who
    // wants a stable assertion to make it about those.
    acrossInstances.map(_.keySet).distinct.size shouldBe 1
  }
