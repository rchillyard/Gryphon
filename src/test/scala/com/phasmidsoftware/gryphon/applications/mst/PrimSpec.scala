/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.applications.mst

import com.phasmidsoftware.gryphon.adjunct.UndirectedGraph
import com.phasmidsoftware.gryphon.adjunct.UndirectedGraph.triplesToTryGraph
import com.phasmidsoftware.gryphon.core.{Edge, EdgeType, Triplet, Vertex}
import com.phasmidsoftware.gryphon.parse.GraphParser
import com.phasmidsoftware.gryphon.traverse.{PrimTraversal, TraversalResult}
import com.phasmidsoftware.gryphon.util.{FP, TryUsing}
import com.phasmidsoftware.visitor.core.given
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scala.io.Source
import scala.util.{Failure, Success, Try}

class PrimSpec extends AnyFlatSpec with Matchers:

  // prim.graph: 8 vertices (0-7), 16 undirected weighted edges.
  // MST (Sedgewick & Wayne tinyEWG): 7 edges, total weight 1.81
  //   0-7 (0.16), 2-3 (0.17), 1-7 (0.19), 0-2 (0.26),
  //   5-7 (0.28), 4-5 (0.35), 6-2 (0.40)

  // Monoid[Double] is resolved automatically from Visitor V1.4.0.
  given Ordering[Double] = scala.math.Ordering.Double.TotalOrdering

  // -----------------------------------------------------------------------
  // Helper: load prim.graph and build an UndirectedGraph
  // -----------------------------------------------------------------------

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
          case _ => fail("not an UndirectedGraph[Int, Double]")
      case Failure(x) => fail("parse failed", x)

  private def maybeEdges(result: TraversalResult[Int, Edge[Int, Double]]): Option[Seq[Edge[Int, Double]]] =
    FP.sequence(result.keySet.iterator.map(result.vertexTraverse)).map(_.toSeq)

  private def totalEdgeCost(result: TraversalResult[Int, Edge[Int, Double]]): Option[Double] =
    for w <- maybeEdges(result) yield w.map(_.attribute).sum

  // -----------------------------------------------------------------------
  // Structural tests — graph shape
  // -----------------------------------------------------------------------

  "prim.graph" should "have 8 vertices" in :
    withPrimGraph { graph =>
      graph.N shouldBe 8
    }

  it should "have 16 edges (each undirected edge stored once)" in :
    withPrimGraph { graph =>
      graph.M shouldBe 16
    }

  // -----------------------------------------------------------------------
  // MST correctness
  // -----------------------------------------------------------------------

  behavior of "PrimTraversal"

  it should "produce an MST with exactly 7 edges (N-1 for 8 vertices)" in :
    withPrimGraph { graph =>
      val result = PrimTraversal[Int, Double]().run(graph)(0)
      result.size shouldBe 7
    }

  it should "include all non-source vertices as MST keys" in :
    withPrimGraph { graph =>
      val result = PrimTraversal[Int, Double]().run(graph)(0)
      result.keySet shouldBe Set(1, 2, 3, 4, 5, 6, 7)
    }

  it should "produce an MST whose total weight is 1.81" in :
    withPrimGraph { graph =>
      val result: TraversalResult[Int, Edge[Int, Double]] = PrimTraversal[Int, Double]().run(graph)(0)
      totalEdgeCost(result).getOrElse(fail("sequence failed")) shouldBe 1.81 +- 1e-10
    }

  it should "not include any edge heavier than the maximum MST edge (0.40)" in :
    withPrimGraph { graph =>
      val result = PrimTraversal[Int, Double]().run(graph)(0)
      result.keySet.foreach { key =>
        result.vertexTraverse(key) match
          case Some(e) => e.attribute should be <= 0.401
          case None => fail(s"vertex $key has no MST edge")
      }
    }

  it should "connect vertex 7 via edge with weight 0.16" in :
    withPrimGraph { graph =>
      val result = PrimTraversal[Int, Double]().run(graph)(0)
      result.vertexTraverse(7) match
        case Some(e) => e.attribute shouldBe 0.16 +- 1e-10
        case None => fail("vertex 7 has no MST predecessor")
    }

  it should "connect vertex 2 via edge with weight 0.26" in :
    withPrimGraph { graph =>
      val result = PrimTraversal[Int, Double]().run(graph)(0)
      result.vertexTraverse(2) match
        case Some(e) => e.attribute shouldBe 0.26 +- 1e-10
        case None => fail("vertex 2 has no MST predecessor")
    }

  it should "connect vertex 3 via edge with weight 0.17" in :
    withPrimGraph { graph =>
      val result = PrimTraversal[Int, Double]().run(graph)(0)
      result.vertexTraverse(3) match
        case Some(e) => e.attribute shouldBe 0.17 +- 1e-10
        case None => fail("vertex 3 has no MST predecessor")
    }

  it should "connect vertex 1 via edge with weight 0.19" in :
    withPrimGraph { graph =>
      val result = PrimTraversal[Int, Double]().run(graph)(0)
      result.vertexTraverse(1) match
        case Some(e) => e.attribute shouldBe 0.19 +- 1e-10
        case None => fail("vertex 1 has no MST predecessor")
    }

  it should "connect vertex 5 via edge with weight 0.28" in :
    withPrimGraph { graph =>
      val result = PrimTraversal[Int, Double]().run(graph)(0)
      result.vertexTraverse(5) match
        case Some(e) => e.attribute shouldBe 0.28 +- 1e-10
        case None => fail("vertex 5 has no MST predecessor")
    }

  it should "connect vertex 4 via edge with weight 0.35" in :
    withPrimGraph { graph =>
      val result = PrimTraversal[Int, Double]().run(graph)(0)
      result.vertexTraverse(4) match
        case Some(e) => e.attribute shouldBe 0.35 +- 1e-10
        case None => fail("vertex 4 has no MST predecessor")
    }

  it should "connect vertex 6 via edge with weight 0.40" in :
    withPrimGraph { graph =>
      val result = PrimTraversal[Int, Double]().run(graph)(0)
      result.vertexTraverse(6) match
        case Some(e) => e.attribute shouldBe 0.40 +- 1e-10
        case None => fail("vertex 6 has no MST predecessor")
    }

  it should "have no predecessor for the source vertex 0" in :
    withPrimGraph { graph =>
      val result = PrimTraversal[Int, Double]().run(graph)(0)
      result.vertexTraverse(0) shouldBe None
    }

  it should "produce an MST whose edge weights are independent of start vertex" in :
    // The MST is unique (all edge weights distinct), so starting from
    // vertex 3 should yield the same set of edge weights as from vertex 0.
    withPrimGraph { graph =>
      val result0 = PrimTraversal[Int, Double]().run(graph)(0)
      val result3 = PrimTraversal[Int, Double]().run(graph)(3)
      (maybeEdges(result0), maybeEdges(result3)) match
        case (Some(edges0), Some(edges3)) =>
          edges0.map(_.attribute).toList.sorted shouldBe edges3.map(_.attribute).toList.sorted
        case _ =>
          fail("maybeEdges returned None")
    }