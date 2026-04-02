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
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scala.io.Source
import scala.util.{Failure, Success, Try}

class PrimSpec extends AnyFlatSpec with Matchers:

  // Numeric and Ordering instances needed throughout
  given Numeric[Double]  = scala.math.Numeric.DoubleIsFractional
  given Ordering[Double] = scala.math.Ordering.Double.TotalOrdering

  // -----------------------------------------------------------------------
  // Helper: load prim.graph and build an UndirectedGraph
  // -----------------------------------------------------------------------

  private def withPrimGraph[A](f: UndirectedGraph[Int, Double] => A): A =
    val p           = new GraphParser[Int, Double, EdgeType]
    val triedSource = Try(Source.fromResource("prim.graph"))
    val zsy         = TryUsing.tryIt(triedSource) { source =>
      p.parseSource[Triplet[Int, Double, EdgeType]](p.parseTriple)(source)
    }
    zsy match
      case Success(triplets) =>
        triplesToTryGraph[Int, Double](Vertex.createWithSet)(triplets) match
          case Success(graph: UndirectedGraph[Int, Double] @unchecked) =>
            f(graph)
          case Failure(x) =>
            fail("graph construction failed", x)
          case _          =>
            fail("not an UndirectedGraph[Int, Double]")
      case Failure(x) =>
        fail("parse failed", x)


  private def maybeEdges(result: TraversalResult[Int, Edge[Double, Int]]): Option[Seq[Edge[Double, Int]]] =
    FP.sequence(result.keySet.iterator.map(result.vertexTraverse)).map(_.toSeq)

  private def totalEdgeCost(result: TraversalResult[Int, Edge[Double, Int]]): Option[Double] =
    for (w <- maybeEdges(result)) yield w.map(_.attribute).sum

  // -----------------------------------------------------------------------
  // Structural tests — graph shape
  // -----------------------------------------------------------------------

  "prim.graph" should "have 8 vertices" in:
    withPrimGraph { graph =>
      graph.N shouldBe 8
    }

  it should "have 16 edges (each undirected edge stored once)" in:
    withPrimGraph { graph =>
      graph.M shouldBe 16
    }

  // -----------------------------------------------------------------------
  // MST correctness
  // -----------------------------------------------------------------------

  behavior of "PrimTraversal"

  it should "produce an MST with exactly 7 edges (N-1 for 8 vertices)" in:
    withPrimGraph { graph =>
      println(s"0: ${graph.adjacentVertices(0).toList}")
      println(s"7: ${graph.adjacentVertices(7).toList}")
      val result = PrimTraversal[Int, Double]().run(graph)(0)
      println(s"result keys: ${result.keySet}")
      // Source vertex 0 has no predecessor; all 7 others do
      result.size shouldBe 7
    }

  it should "produce an MST whose total weight is 1.81" in:
    withPrimGraph { graph =>
      val result: TraversalResult[Int, Edge[Double, Int]] = PrimTraversal[Int, Double]().run(graph)(0)
      totalEdgeCost(result).getOrElse(fail("sequence failed")) shouldBe 1.81 +- 1e-10
    }

  it should "connect vertex 7 via edge with weight 0.16" in:
    withPrimGraph { graph =>
      val result = PrimTraversal[Int, Double]().run(graph)(0)
      result.vertexTraverse(7) match
        case Some(e) => e.attribute shouldBe 0.16 +- 1e-10
        case None    => fail("vertex 7 has no MST predecessor")
    }

  it should "connect vertex 2 via edge with weight 0.26" in:
    withPrimGraph { graph =>
      val result = PrimTraversal[Int, Double]().run(graph)(0)
      result.vertexTraverse(2) match
        case Some(e) => e.attribute shouldBe 0.26 +- 1e-10
        case None    => fail("vertex 2 has no MST predecessor")
    }

  it should "connect vertex 3 via edge with weight 0.17" in:
    withPrimGraph { graph =>
      val result = PrimTraversal[Int, Double]().run(graph)(0)
      result.vertexTraverse(3) match
        case Some(e) => e.attribute shouldBe 0.17 +- 1e-10
        case None    => fail("vertex 3 has no MST predecessor")
    }

  it should "connect vertex 1 via edge with weight 0.19" in:
    withPrimGraph { graph =>
      val result = PrimTraversal[Int, Double]().run(graph)(0)
      result.vertexTraverse(1) match
        case Some(e) => e.attribute shouldBe 0.19 +- 1e-10
        case None    => fail("vertex 1 has no MST predecessor")
    }

  it should "connect vertex 5 via edge with weight 0.28" in:
    withPrimGraph { graph =>
      val result = PrimTraversal[Int, Double]().run(graph)(0)
      result.vertexTraverse(5) match
        case Some(e) => e.attribute shouldBe 0.28 +- 1e-10
        case None    => fail("vertex 5 has no MST predecessor")
    }

  it should "connect vertex 4 via edge with weight 0.35" in:
    withPrimGraph { graph =>
      val result = PrimTraversal[Int, Double]().run(graph)(0)
      result.vertexTraverse(4) match
        case Some(e) => e.attribute shouldBe 0.35 +- 1e-10
        case None    => fail("vertex 4 has no MST predecessor")
    }

  it should "connect vertex 6 via edge with weight 0.40" in:
    withPrimGraph { graph =>
      val result = PrimTraversal[Int, Double]().run(graph)(0)
      result.vertexTraverse(6) match
        case Some(e) => e.attribute shouldBe 0.40 +- 1e-10
        case None    => fail("vertex 6 has no MST predecessor")
    }

  it should "have no predecessor for the source vertex 0" in:
    withPrimGraph { graph =>
      val result = PrimTraversal[Int, Double]().run(graph)(0)
      result.vertexTraverse(0) shouldBe None
    }

  it should "produce an MST whose edge weights are independent of start vertex" in:
    // The MST is unique (all edge weights are distinct), so starting from
    // vertex 3 should yield the same set of edge weights.
    withPrimGraph { graph =>
      val result0 = PrimTraversal[Int, Double]().run(graph)(0)
      val result3 = PrimTraversal[Int, Double]().run(graph)(3)
      (maybeEdges(result0), maybeEdges(result3)) match {
        case (Some(edges0), Some(edges3)) =>
          edges0.map(_.attribute).toList.sorted shouldBe edges3.map(_.attribute).toList.sorted
        case _ =>
          fail("test failed")
      }
    }