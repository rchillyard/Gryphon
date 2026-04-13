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
import scala.util.{Failure, Success, Try}

/**
 * Tests for isBipartite and isConnected on UndirectedGraph,
 * and UnsupportedOperationException on DirectedGraph.
 */
class BipartiteAndConnectedSpec extends AnyFlatSpec with should.Matchers:

  private def withPrimGraph[A](f: UndirectedGraph[Int, Double] => A): A =
    val p = new GraphParser[Int, Double, EdgeType]
    val triedSource = Try(Source.fromResource("prim.graph"))
    TryUsing.tryIt(triedSource) { source =>
      p.parseSource[Triplet[Int, Double, EdgeType]](p.parseTriple)(source)
    } match
      case Success(triplets) =>
        triplesToTryGraph[Int, Double](Vertex.createWithSet)(triplets) match
          case Success(g: UndirectedGraph[Int, Double] @unchecked) => f(g)
          case Failure(x) => fail("graph construction failed", x)
          case _ => fail("not an UndirectedGraph")
      case Failure(x) => fail("parse failed", x)

  private def withDfsuGraph[A](f: UndirectedGraph[Int, Unit] => A): A =
    val p = new GraphParser[Int, Unit, EdgeType]
    val triedSource = Try(Source.fromResource("dfsu.graph"))
    TryUsing.tryIt(triedSource) { source =>
      p.parseSource[Triplet[Int, Unit, EdgeType]](p.parseTriple)(source)
    } match
      case Success(triplets) =>
        triplesToTryGraph[Int, Unit](Vertex.createWithSet)(triplets) match
          case Success(g: UndirectedGraph[Int, Unit] @unchecked) => f(g)
          case Failure(x) => fail("graph construction failed", x)
          case _ => fail("not an UndirectedGraph")
      case Failure(x) => fail("parse failed", x)

  // -------------------------------------------------------------------------
  // isConnected — UndirectedGraph
  // -------------------------------------------------------------------------

  behavior of "UndirectedGraph.isConnected"

  it should "return true for prim.graph (fully connected)" in :
    withPrimGraph { graph =>
      graph.isConnected shouldBe true
    }

  it should "return false for dfsu.graph (3 components)" in :
    withDfsuGraph { graph =>
      graph.isConnected shouldBe false
    }

  it should "return true for a single edge 0-1" in :
    val triplets: Seq[Triplet[Int, Unit, EdgeType]] = Seq(
      Triplet(0, 1, None, Undirected)
    )
    triplesToTryGraph[Int, Unit](Vertex.createWithSet)(triplets) match
      case Success(g: UndirectedGraph[Int, Unit] @unchecked) =>
        g.isConnected shouldBe true
      case other => fail(s"unexpected: $other")

  it should "return true for a chain 0-1-2-3" in :
    val triplets: Seq[Triplet[Int, Unit, EdgeType]] = Seq(
      Triplet(0, 1, None, Undirected),
      Triplet(1, 2, None, Undirected),
      Triplet(2, 3, None, Undirected)
    )
    triplesToTryGraph[Int, Unit](Vertex.createWithSet)(triplets) match
      case Success(g: UndirectedGraph[Int, Unit] @unchecked) =>
        g.isConnected shouldBe true
      case other => fail(s"unexpected: $other")

  it should "return false for two disconnected edges 0-1 and 2-3" in :
    val triplets: Seq[Triplet[Int, Unit, EdgeType]] = Seq(
      Triplet(0, 1, None, Undirected),
      Triplet(2, 3, None, Undirected)
    )
    triplesToTryGraph[Int, Unit](Vertex.createWithSet)(triplets) match
      case Success(g: UndirectedGraph[Int, Unit] @unchecked) =>
        g.isConnected shouldBe false
      case other => fail(s"unexpected: $other")

  it should "return true for a triangle" in :
    val triplets: Seq[Triplet[Int, Unit, EdgeType]] = Seq(
      Triplet(0, 1, None, Undirected),
      Triplet(1, 2, None, Undirected),
      Triplet(2, 0, None, Undirected)
    )
    triplesToTryGraph[Int, Unit](Vertex.createWithSet)(triplets) match
      case Success(g: UndirectedGraph[Int, Unit] @unchecked) =>
        g.isConnected shouldBe true
      case other => fail(s"unexpected: $other")

  // -------------------------------------------------------------------------
  // isConnected — DirectedGraph throws
  // -------------------------------------------------------------------------

  behavior of "DirectedGraph.isConnected"

  it should "throw UnsupportedOperationException" in :
    val triplets: Seq[Triplet[Int, Unit, EdgeType]] = Seq(
      Triplet(0, 1, None, Directed)
    )
    DirectedGraph.triplesToTryGraph[Int, Unit](Vertex.createWithSet)(triplets) match
      case Success(g: DirectedGraph[Int, Unit] @unchecked) =>
        an[UnsupportedOperationException] should be thrownBy g.isConnected
      case other => fail(s"unexpected: $other")

  // -------------------------------------------------------------------------
  // isBipartite — UndirectedGraph
  // -------------------------------------------------------------------------

  behavior of "UndirectedGraph.isBipartite"

  it should "return false for prim.graph (contains odd cycle)" in :
    withPrimGraph { graph =>
      graph.isBipartite shouldBe false
    }

  it should "return true for a single edge 0-1" in :
    val triplets: Seq[Triplet[Int, Unit, EdgeType]] = Seq(
      Triplet(0, 1, None, Undirected)
    )
    triplesToTryGraph[Int, Unit](Vertex.createWithSet)(triplets) match
      case Success(g: UndirectedGraph[Int, Unit] @unchecked) =>
        g.isBipartite shouldBe true
      case other => fail(s"unexpected: $other")

  it should "return true for a chain 0-1-2 (alternating colors)" in :
    val triplets: Seq[Triplet[Int, Unit, EdgeType]] = Seq(
      Triplet(0, 1, None, Undirected),
      Triplet(1, 2, None, Undirected)
    )
    triplesToTryGraph[Int, Unit](Vertex.createWithSet)(triplets) match
      case Success(g: UndirectedGraph[Int, Unit] @unchecked) =>
        g.isBipartite shouldBe true
      case other => fail(s"unexpected: $other")

  it should "return false for a triangle (odd cycle)" in :
    val triplets: Seq[Triplet[Int, Unit, EdgeType]] = Seq(
      Triplet(0, 1, None, Undirected),
      Triplet(1, 2, None, Undirected),
      Triplet(2, 0, None, Undirected)
    )
    triplesToTryGraph[Int, Unit](Vertex.createWithSet)(triplets) match
      case Success(g: UndirectedGraph[Int, Unit] @unchecked) =>
        g.isBipartite shouldBe false
      case other => fail(s"unexpected: $other")

  it should "return true for an even cycle 0-1-2-3-0" in :
    val triplets: Seq[Triplet[Int, Unit, EdgeType]] = Seq(
      Triplet(0, 1, None, Undirected),
      Triplet(1, 2, None, Undirected),
      Triplet(2, 3, None, Undirected),
      Triplet(3, 0, None, Undirected)
    )
    triplesToTryGraph[Int, Unit](Vertex.createWithSet)(triplets) match
      case Success(g: UndirectedGraph[Int, Unit] @unchecked) =>
        g.isBipartite shouldBe true
      case other => fail(s"unexpected: $other")

  it should "return false for an odd cycle 0-1-2-3-4-0" in :
    val triplets: Seq[Triplet[Int, Unit, EdgeType]] = Seq(
      Triplet(0, 1, None, Undirected),
      Triplet(1, 2, None, Undirected),
      Triplet(2, 3, None, Undirected),
      Triplet(3, 4, None, Undirected),
      Triplet(4, 0, None, Undirected)
    )
    triplesToTryGraph[Int, Unit](Vertex.createWithSet)(triplets) match
      case Success(g: UndirectedGraph[Int, Unit] @unchecked) =>
        g.isBipartite shouldBe false
      case other => fail(s"unexpected: $other")

  it should "return true for a complete bipartite graph K2,3" in :
    // {0,1} connected to {2,3,4} — every edge crosses the partition
    val triplets: Seq[Triplet[Int, Unit, EdgeType]] = Seq(
      Triplet(0, 2, None, Undirected),
      Triplet(0, 3, None, Undirected),
      Triplet(0, 4, None, Undirected),
      Triplet(1, 2, None, Undirected),
      Triplet(1, 3, None, Undirected),
      Triplet(1, 4, None, Undirected)
    )
    triplesToTryGraph[Int, Unit](Vertex.createWithSet)(triplets) match
      case Success(g: UndirectedGraph[Int, Unit] @unchecked) =>
        g.isBipartite shouldBe true
      case other => fail(s"unexpected: $other")

  it should "return true for a disconnected bipartite graph" in :
    // Two separate even paths: 0-1-2 and 3-4
    val triplets: Seq[Triplet[Int, Unit, EdgeType]] = Seq(
      Triplet(0, 1, None, Undirected),
      Triplet(1, 2, None, Undirected),
      Triplet(3, 4, None, Undirected)
    )
    triplesToTryGraph[Int, Unit](Vertex.createWithSet)(triplets) match
      case Success(g: UndirectedGraph[Int, Unit] @unchecked) =>
        g.isBipartite shouldBe true
      case other => fail(s"unexpected: $other")

  it should "return false for a disconnected graph where one component has an odd cycle" in :
    // Chain 0-1-2 (bipartite) plus triangle 3-4-5-3 (not bipartite)
    val triplets: Seq[Triplet[Int, Unit, EdgeType]] = Seq(
      Triplet(0, 1, None, Undirected),
      Triplet(1, 2, None, Undirected),
      Triplet(3, 4, None, Undirected),
      Triplet(4, 5, None, Undirected),
      Triplet(5, 3, None, Undirected)
    )
    triplesToTryGraph[Int, Unit](Vertex.createWithSet)(triplets) match
      case Success(g: UndirectedGraph[Int, Unit] @unchecked) =>
        g.isBipartite shouldBe false
      case other => fail(s"unexpected: $other")

  // -------------------------------------------------------------------------
  // isBipartite — DirectedGraph throws
  // -------------------------------------------------------------------------

  behavior of "DirectedGraph.isBipartite"

  it should "throw UnsupportedOperationException" in :
    val triplets: Seq[Triplet[Int, Unit, EdgeType]] = Seq(
      Triplet(0, 1, None, Directed)
    )
    DirectedGraph.triplesToTryGraph[Int, Unit](Vertex.createWithSet)(triplets) match
      case Success(g: DirectedGraph[Int, Unit] @unchecked) =>
        an[UnsupportedOperationException] should be thrownBy g.isBipartite
      case other => fail(s"unexpected: $other")