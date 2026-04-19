/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.traverse

import com.phasmidsoftware.gryphon.adjunct.UndirectedGraph
import com.phasmidsoftware.gryphon.adjunct.UndirectedGraph.triplesToTryGraph
import com.phasmidsoftware.gryphon.core.{EdgeType, Triplet, Vertex}
import com.phasmidsoftware.gryphon.parse.GraphParser
import com.phasmidsoftware.gryphon.util.TryUsing
import com.phasmidsoftware.visitor.core.given_Monoid_Double
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
          case Success(graph: UndirectedGraph[Int, Double]) => f(graph)
          case Failure(x) => fail("graph construction failed", x)
          case _ => fail("not an UndirectedGraph[Int, Double]")
      case Failure(x) => fail("parse failed", x)

  // Deduplicate pred-map values by unordered vertex pair, giving true MST edge set.
  private def mstEdges(graph: UndirectedGraph[Int, Double]) =
    Boruvka.mst(graph).map.values
            .map(e => Set(e.white, e.black) -> e.attribute)
            .toMap

  // -------------------------------------------------------------------------
  // Result structure
  // -------------------------------------------------------------------------

  behavior of "Boruvka.mst — result structure"

  it should "label all 8 vertices" in :
    withPrimGraph { graph =>
      Boruvka.mst(graph).map.size shouldBe 8
    }

  it should "produce exactly 7 distinct MST edges (N-1 for 8 vertices)" in :
    withPrimGraph { graph =>
      mstEdges(graph).size shouldBe 7
    }

  // -------------------------------------------------------------------------
  // MST weight
  // -------------------------------------------------------------------------

  behavior of "Boruvka.mst — total weight"

  it should "produce MST with total weight 1.81" in :
    withPrimGraph { graph =>
      val totalWeight = mstEdges(graph).values.sum
      totalWeight shouldBe 1.81 +- 0.001
    }

  // -------------------------------------------------------------------------
  // MST edge set — same result as Kruskal and Prim
  // -------------------------------------------------------------------------

  behavior of "Boruvka.mst — edge set"

  it should "include edge 0-7 with weight 0.16" in :
    withPrimGraph { graph =>
      mstEdges(graph).values should contain(0.16)
    }

  it should "include edge 2-3 with weight 0.17" in :
    withPrimGraph { graph =>
      mstEdges(graph).values should contain(0.17)
    }

  it should "include edge 1-7 with weight 0.19" in :
    withPrimGraph { graph =>
      mstEdges(graph).values should contain(0.19)
    }

  it should "include edge 0-2 with weight 0.26" in :
    withPrimGraph { graph =>
      mstEdges(graph).values should contain(0.26)
    }

  it should "include edge 5-7 with weight 0.28" in :
    withPrimGraph { graph =>
      mstEdges(graph).values should contain(0.28)
    }

  it should "include edge 4-5 with weight 0.35" in :
    withPrimGraph { graph =>
      mstEdges(graph).values should contain(0.35)
    }

  it should "include edge 6-2 with weight 0.40" in :
    withPrimGraph { graph =>
      mstEdges(graph).values should contain(0.40)
    }

  it should "not include edge 1-3 with weight 0.29 (cycle)" in :
    withPrimGraph { graph =>
      mstEdges(graph).values should not contain 0.29
    }

  // -------------------------------------------------------------------------
  // Agree with Kruskal
  // -------------------------------------------------------------------------

  behavior of "Boruvka.mst — agreement with Kruskal"

  it should "produce the same MST edge weights as Kruskal" in :
    withPrimGraph { graph =>
      val boruvkaWeights = mstEdges(graph).values.toSet
      val kruskalWeights = Kruskal.mst(graph).map(_.attribute).toSet
      boruvkaWeights shouldBe kruskalWeights
    }