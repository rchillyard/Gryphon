/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.adjunct

import com.phasmidsoftware.gryphon.adjunct.UndirectedGraph.triplesToTryGraph
import com.phasmidsoftware.gryphon.core.*
import com.phasmidsoftware.gryphon.parse.GraphParser
import com.phasmidsoftware.gryphon.traverse.ConnectedComponents
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
      case Failure(x) =>
        fail("parse failed", x)

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
      case Failure(x) =>
        fail("parse failed", x)

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
      case other =>
        fail(s"unexpected: $other")

  it should "return true for a chain 0-1-2-3" in :
    val triplets: Seq[Triplet[Int, Unit, EdgeType]] = Seq(
      Triplet(0, 1, None, Undirected),
      Triplet(1, 2, None, Undirected),
      Triplet(2, 3, None, Undirected)
    )
    triplesToTryGraph[Int, Unit](Vertex.createWithSet)(triplets) match
      case Success(g: UndirectedGraph[Int, Unit] @unchecked) =>
        g.isConnected shouldBe true
      case other =>
        fail(s"unexpected: $other")

  it should "return false for two disconnected edges 0-1 and 2-3" in :
    val triplets: Seq[Triplet[Int, Unit, EdgeType]] = Seq(
      Triplet(0, 1, None, Undirected),
      Triplet(2, 3, None, Undirected)
    )
    triplesToTryGraph[Int, Unit](Vertex.createWithSet)(triplets) match
      case Success(g: UndirectedGraph[Int, Unit] @unchecked) =>
        g.isConnected shouldBe false
      case other =>
        fail(s"unexpected: $other")

  it should "return true for a triangle" in :
    val triplets: Seq[Triplet[Int, Unit, EdgeType]] = Seq(
      Triplet(0, 1, None, Undirected),
      Triplet(1, 2, None, Undirected),
      Triplet(2, 0, None, Undirected)
    )
    triplesToTryGraph[Int, Unit](Vertex.createWithSet)(triplets) match
      case Success(g: UndirectedGraph[Int, Unit] @unchecked) =>
        g.isConnected shouldBe true
      case other =>
        fail(s"unexpected: $other")

  // -------------------------------------------------------------------------
  // isConnected — DirectedGraph throws
  // -------------------------------------------------------------------------

  behavior of "DirectedGraph and isConnected"

  // Until 1.8.0 this spec asserted that `g.isConnected` threw UnsupportedOperationException.
  // It no longer compiles: isConnected is not declared on a directed graph at all, because
  // connectivity of a digraph is either weak or strong and the library commits to neither.
  // There is no run-time behaviour left to assert, so what follows checks the replacement —
  // ConnectedComponents, which gives the weak reading.
  it should "be answered for a digraph by ConnectedComponents instead" in :
    // Attributed triplets: ConnectedComponents cannot currently handle attribute-less ones,
    // which surface as VertexPair connexions and make Connexions.addConnexion throw.
    val triplets: Seq[Triplet[Int, Unit, EdgeType]] = Seq(
      Triplet(0, 1, Some(()), Directed),
      Triplet(2, 3, Some(()), Directed)
    )
    DirectedGraph.triplesToTryGraph[Int, Unit](Vertex.createWithSet)(triplets) match
      case Success(g: DirectedGraph[Int, Unit] @unchecked) =>
        val componentMap = ConnectedComponents.components[Int, Unit](g).map
        componentMap.values.toSet.size shouldBe 2 // two weakly connected components
      case other =>
        fail(s"unexpected: $other")

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
      case other =>
        fail(s"unexpected: $other")

  it should "return true for a chain 0-1-2 (alternating colors)" in :
    val triplets: Seq[Triplet[Int, Unit, EdgeType]] = Seq(
      Triplet(0, 1, None, Undirected),
      Triplet(1, 2, None, Undirected)
    )
    triplesToTryGraph[Int, Unit](Vertex.createWithSet)(triplets) match
      case Success(g: UndirectedGraph[Int, Unit] @unchecked) =>
        g.isBipartite shouldBe true
      case other =>
        fail(s"unexpected: $other")

  it should "return false for a triangle (odd cycle)" in :
    val triplets: Seq[Triplet[Int, Unit, EdgeType]] = Seq(
      Triplet(0, 1, None, Undirected),
      Triplet(1, 2, None, Undirected),
      Triplet(2, 0, None, Undirected)
    )
    triplesToTryGraph[Int, Unit](Vertex.createWithSet)(triplets) match
      case Success(g: UndirectedGraph[Int, Unit] @unchecked) =>
        g.isBipartite shouldBe false
      case other =>
        fail(s"unexpected: $other")

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
      case other =>
        fail(s"unexpected: $other")

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
      case other =>
        fail(s"unexpected: $other")

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
      case other =>
        fail(s"unexpected: $other")

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
      case other =>
        fail(s"unexpected: $other")

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
      case other =>
        fail(s"unexpected: $other")

  // -------------------------------------------------------------------------
  // isBipartite — DirectedGraph throws
  // -------------------------------------------------------------------------

  behavior of "DirectedGraph and isBipartite"

  // As above: `g.isBipartite` on a digraph no longer compiles rather than throwing, so there is
  // nothing to assert at run time. The Java façade, which cannot express the distinction, still
  // refuses it there — see GraphPropertiesTest.
  it should "still be available on the undirected graph built from the same vertices" in :
    val triplets: Seq[Triplet[Int, Unit, EdgeType]] = Seq(
      Triplet(0, 1, None, Undirected)
    )
    UndirectedGraph.triplesToTryGraph[Int, Unit](Vertex.createWithSet)(triplets) match
      case Success(g: UndirectedGraph[Int, Unit] @unchecked) =>
        g.isBipartite shouldBe true
      case other =>
        fail(s"unexpected: $other")
  // -------------------------------------------------------------------------------------
  // Issue #82: isConnected's empty-graph case. The guard reads
  //   if order == 0 then true else ...
  // and the Stryker4s trial saw both the guard and the literal survive mutation, which is
  // one finding rather than two: isConnected was never called on an empty graph at all, so
  // the convention that the empty graph counts as connected was asserted nowhere. The
  // existing coverage is prim.graph (connected) and dfsu.graph (three components), neither
  // of them empty. Unusually for the trial's findings, scoverage showed this too, as an
  // uncovered branch at UndirectedGraph.scala:95 — mutation testing only made it concrete.

  behavior of "isConnected on an empty graph"

  it should "hold that the empty graph is connected" in :
    triplesToTryGraph[Int, Double](Vertex.createWithSet)(Nil) match
      case Success(g: UndirectedGraph[Int, Double] @unchecked) =>
        g.order shouldBe 0
        g.isConnected shouldBe true
      case Failure(x) =>
        fail("graph construction failed", x)

  it should "distinguish that from a one-vertex graph, which is also connected" in :
    // A self-loop is the smallest non-empty undirected graph the builder will make, and it
    // takes the other arm of the guard. Without this, flipping `order == 0` to `true` would
    // still pass everything.
    triplesToTryGraph[Int, Double](Vertex.createWithSet)(Seq(Triplet(0, 0, Some(1.0), Undirected))) match
      case Success(g: UndirectedGraph[Int, Double] @unchecked) =>
        g.order shouldBe 1
        g.isConnected shouldBe true
      case other =>
        fail(s"unexpected: $other")
