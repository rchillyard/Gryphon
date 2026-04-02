/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.adjunct

import com.phasmidsoftware.gryphon.adjunct.UndirectedGraph.triplesToTryGraph
import com.phasmidsoftware.gryphon.core.*
import com.phasmidsoftware.gryphon.parse.GraphParser
import com.phasmidsoftware.gryphon.util.TryUsing
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import scala.io.Source
import scala.util.{Failure, Random, Success, Try}

/**
 * Tests for degree methods on UndirectedGraph and numberOfSelfLoops on EdgeGraph.
 *
 * prim.graph degree summary:
 * vertex 0: 4  (0-7, 0-2, 0-4, 0-6)
 * vertex 1: 4  (1-7, 1-3, 1-5, 1-2)
 * vertex 2: 5  (2-3, 2-7, 2-1, 2-6, 2-0)
 * vertex 3: 3  (3-2, 3-1, 3-6)
 * vertex 4: 3  (4-5, 4-7, 4-0)
 * vertex 5: 3  (5-7, 5-1, 5-4)
 * vertex 6: 4  (6-2, 6-3, 6-0, 6-4)
 * vertex 7: 5  (7-0, 7-1, 7-5, 7-2, 7-4)
 * maxDegree = 5, meanDegree = 31/8 = 3.875
 */
class GraphPropertiesSpec extends AnyFlatSpec with should.Matchers:

  given Random = Random(42)

  private def withPrimGraph[A](f: UndirectedGraph[Int, Double] => A): A =
    val p = new GraphParser[Int, Double, EdgeType]
    val triedSource = Try(Source.fromResource("prim.graph"))
    TryUsing.tryIt(triedSource) { source =>
      p.parseSource[Triplet[Int, Double, EdgeType]](p.parseTriple)(source)
    } match
      case Success(triplets) =>
        triplesToTryGraph[Int, Double](Vertex.createWithSet)(triplets) match
          case Success(graph: UndirectedGraph[Int, Double]) => f(graph)
          case Failure(x) => fail("graph construction failed", x)
          case _ => fail("not an UndirectedGraph[Int, Double]")
      case Failure(x) => fail("parse failed", x)

  // -------------------------------------------------------------------------
  // degree
  // -------------------------------------------------------------------------

  behavior of "UndirectedGraph.degree"

  it should "give vertex 0 degree 4" in :
    withPrimGraph { graph =>
      graph.degree(0) shouldBe 4
    }

  it should "give vertex 2 degree 5" in :
    withPrimGraph { graph =>
      graph.degree(2) shouldBe 5
    }

  it should "give vertex 7 degree 5" in :
    withPrimGraph { graph =>
      graph.degree(7) shouldBe 5
    }

  it should "give vertex 3 degree 3" in :
    withPrimGraph { graph =>
      graph.degree(3) shouldBe 3
    }

  // -------------------------------------------------------------------------
  // maxDegree
  // -------------------------------------------------------------------------

  behavior of "UndirectedGraph.maxDegree"

  it should "return 5 for prim.graph" in :
    withPrimGraph { graph =>
      graph.maxDegree shouldBe 5
    }

  it should "return 1 for a single-edge graph" in :
    val triplets: Seq[Triplet[Int, Double, EdgeType]] = Seq(
      Triplet(0, 1, Some(1.0), Undirected)
    )
    triplesToTryGraph[Int, Double](Vertex.createWithSet)(triplets) match
      case Success(g: UndirectedGraph[Int, Double]) =>
        g.maxDegree shouldBe 1
      case other => fail(s"unexpected: $other")

  // -------------------------------------------------------------------------
  // meanDegree
  // -------------------------------------------------------------------------

  behavior of "UndirectedGraph.meanDegree"

  it should "return 3.875 for prim.graph" in :
    withPrimGraph { graph =>
      graph.meanDegree shouldBe 4.0 +- 0.001
    }

  it should "satisfy meanDegree == 2*M/N for prim.graph (handshaking lemma)" in :
    // For any undirected graph: sum of degrees = 2 * number of edges
    withPrimGraph { graph =>
      graph.meanDegree shouldBe (2.0 * graph.M / graph.N) +- 0.001
    }

  it should "return 1.0 for a single-edge graph" in :
    val triplets: Seq[Triplet[Int, Double, EdgeType]] = Seq(
      Triplet(0, 1, Some(1.0), Undirected)
    )
    triplesToTryGraph[Int, Double](Vertex.createWithSet)(triplets) match
      case Success(g: UndirectedGraph[Int, Double]) =>
        g.meanDegree shouldBe 1.0 +- 0.001
      case other => fail(s"unexpected: $other")

  // -------------------------------------------------------------------------
  // numberOfSelfLoops
  // -------------------------------------------------------------------------

  behavior of "EdgeGraph.numberOfSelfLoops"

  it should "return 0 for prim.graph (no self-loops)" in :
    withPrimGraph { graph =>
      graph.numberOfSelfLoops shouldBe 0
    }

  it should "return 0 for a simple two-edge graph" in :
    val triplets: Seq[Triplet[Int, Double, EdgeType]] = Seq(
      Triplet(0, 1, Some(1.0), Undirected),
      Triplet(1, 2, Some(2.0), Undirected)
    )
    triplesToTryGraph[Int, Double](Vertex.createWithSet)(triplets) match
      case Success(g: UndirectedGraph[Int, Double]) =>
        g.numberOfSelfLoops shouldBe 0
      case other => fail(s"unexpected: $other")

  it should "return 1 for a graph with one self-loop" in :
    val triplets: Seq[Triplet[Int, Double, EdgeType]] = Seq(
      Triplet(0, 1, Some(1.0), Undirected),
      Triplet(0, 0, Some(0.5), Undirected)
    )
    triplesToTryGraph[Int, Double](Vertex.createWithSet)(triplets) match
      case Success(g: UndirectedGraph[Int, Double]) =>
        g.numberOfSelfLoops shouldBe 1
      case other => fail(s"unexpected: $other")

  it should "count multiple self-loops correctly" in :
    val triplets: Seq[Triplet[Int, Double, EdgeType]] = Seq(
      Triplet(0, 0, Some(1.0), Undirected),
      Triplet(1, 1, Some(2.0), Undirected),
      Triplet(0, 1, Some(3.0), Undirected)
    )
    triplesToTryGraph[Int, Double](Vertex.createWithSet)(triplets) match
      case Success(g: UndirectedGraph[Int, Double]) =>
        g.numberOfSelfLoops shouldBe 2
      case other => fail(s"unexpected: $other")