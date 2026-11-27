/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.traverse

import com.phasmidsoftware.gryphon.adjunct.UndirectedGraph
import com.phasmidsoftware.gryphon.adjunct.UndirectedGraph.triplesToTryGraph
import com.phasmidsoftware.gryphon.core.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import scala.util.{Random, Success}

/**
 * The three MST algorithms on a DISCONNECTED graph, where the answer is a spanning forest
 * rather than a spanning tree. Issue #117.
 *
 * Nothing exercised this before, and the three behaved quite differently:
 *
 *   - `MST.kruskal` was already correct. It is a `foldLeft` over all sorted edges with no early
 *     exit, so it runs out of edges and returns what it accumulated, which is the forest.
 *   - `MST.boruvka` DID NOT TERMINATE. Its only exit was "exactly one component remains", which
 *     a disconnected graph never reaches; once each component is internally spanned there are no
 *     crossing edges, so nothing merged and the `@tailrec` loop spun with identical arguments.
 *   - `MST.prim` grows from one start vertex, so it returns that vertex's component and stops.
 *     Defensible, but it was undocumented and untested, so a caller got a partial answer with
 *     nothing to say so.
 *
 * NOTE on a regression here: reintroducing the Borůvka defect would make these tests HANG rather
 * than fail, because the loop neither blocks nor allocates and so cannot be interrupted by a
 * ScalaTest time limit. A CI job timeout is the backstop.
 *
 * Also worth knowing before optimising Kruskal: stopping it once it has `order - 1` edges is a
 * standard improvement on a connected graph, and would break this file — on a disconnected graph
 * that count is never reached, so the early exit either never fires (harmless) or, if written as
 * "stop when the tree is complete", silently returns a partial forest.
 */
class SpanningForestSpec extends AnyFlatSpec with should.Matchers:

  given Random = Random(42)

  /** Two components: 0-1 at 1.0, and 2-3 at 2.0. No edge between them. */
  private val twoComponents: Seq[Triplet[Int, Double, EdgeType]] = Seq(
    Triplet(0, 1, Some(1.0), Undirected),
    Triplet(2, 3, Some(2.0), Undirected)
  )

  /** Three components, the third a triangle, so one of its edges must be excluded. */
  private val threeComponents: Seq[Triplet[Int, Double, EdgeType]] = Seq(
    Triplet(0, 1, Some(1.0), Undirected),
    Triplet(2, 3, Some(2.0), Undirected),
    Triplet(4, 5, Some(3.0), Undirected),
    Triplet(5, 6, Some(4.0), Undirected),
    Triplet(4, 6, Some(9.0), Undirected) // the expensive edge of the triangle: excluded
  )

  private def withGraph[A](ts: Seq[Triplet[Int, Double, EdgeType]])
                          (f: UndirectedGraph[Int, Double] => A): A =
    triplesToTryGraph[Int, Double](Vertex.createWithSet)(ts) match
      case Success(g: UndirectedGraph[Int, Double] @unchecked) => f(g)
      case other => fail(s"graph construction failed: $other")

  behavior of "Kruskal on a disconnected graph"

  it should "span both components" in :
    withGraph(twoComponents) { g =>
      val forest = MST.kruskal(g)
      forest.size shouldBe 2
      forest.map(_.attribute).toSet shouldBe Set(1.0, 2.0)
    }

  it should "span three components, excluding the triangle's expensive edge" in :
    withGraph(threeComponents) { g =>
      val forest = MST.kruskal(g)
      // 7 vertices, 3 components => 7 - 3 = 4 edges in the spanning forest.
      forest.size shouldBe 4
      forest.map(_.attribute).toSet shouldBe Set(1.0, 2.0, 3.0, 4.0)
      forest.map(_.attribute) should not contain 9.0
    }

  behavior of "Boruvka on a disconnected graph"

  it should "terminate, and span both components" in :
    withGraph(twoComponents) { g =>
      val forest = MST.boruvka(g)
      forest.size shouldBe 2
      forest.map(_.attribute).toSet shouldBe Set(1.0, 2.0)
    }

  it should "terminate, and span three components, excluding the triangle's expensive edge" in :
    withGraph(threeComponents) { g =>
      val forest = MST.boruvka(g)
      forest.size shouldBe 4
      forest.map(_.attribute).toSet shouldBe Set(1.0, 2.0, 3.0, 4.0)
      forest.map(_.attribute) should not contain 9.0
    }

  it should "agree with Kruskal on total weight" in :
    // The two algorithms may choose different edges where weights tie, but a minimum spanning
    // forest has a unique total weight, so this holds whatever they pick.
    withGraph(threeComponents) { g =>
      MST.boruvka(g).map(_.attribute).sum shouldBe MST.kruskal(g).map(_.attribute).sum
    }

  behavior of "Prim on a disconnected graph"

  it should "return only the component containing the start vertex" in :
    // Unlike the other two, Prim grows outward from one vertex and so cannot reach the rest.
    // Pinning it here so the difference is a documented choice rather than a surprise.
    withGraph(twoComponents) { g =>
      val reached = MST.prim(g, 0).keySet
      reached should contain noneOf(2, 3)
      reached.subsetOf(Set(0, 1)) shouldBe true
    }
