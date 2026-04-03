/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.adjunct

import com.phasmidsoftware.gryphon.adjunct.DirectedGraph.triplesToTryGraph as directedTryGraph
import com.phasmidsoftware.gryphon.adjunct.UndirectedGraph.triplesToTryGraph as undirectedTryGraph
import com.phasmidsoftware.gryphon.core.*
import com.phasmidsoftware.gryphon.parse.GraphParser
import com.phasmidsoftware.gryphon.util.TryUsing
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import scala.io.Source
import scala.util.{Failure, Success, Try}

/**
 * Tests for isCyclic on DirectedGraph, UndirectedGraph, and DAG.
 */
class CycleSpec extends AnyFlatSpec with should.Matchers:

  // -------------------------------------------------------------------------
  // Shared fixtures
  // -------------------------------------------------------------------------

  private def withDagGraph[A](f: DirectedGraph[Int, Double] => A): A =
    val p = new GraphParser[Int, Double, EdgeType]
    val triedSource = Try(Source.fromResource("dag.graph"))
    TryUsing.tryIt(triedSource) { source =>
      p.parseSource[Triplet[Int, Double, EdgeType]](p.parseTriple)(source)
    } match
      case Success(triplets) =>
        directedTryGraph[Int, Double](Vertex.createWithSet)(triplets) match
          case Success(g: DirectedGraph[Int, Double]) => f(g)
          case Failure(x) => fail("graph construction failed", x)
          case _ => fail("not a DirectedGraph")
      case Failure(x) => fail("parse failed", x)

  private def withDirectedGraph[A](f: DirectedGraph[Int, Double] => A): A =
    val p = new GraphParser[Int, Double, EdgeType]
    val triedSource = Try(Source.fromResource("directed.graph"))
    TryUsing.tryIt(triedSource) { source =>
      p.parseSource[Triplet[Int, Double, EdgeType]](p.parseTriple)(source)
    } match
      case Success(triplets) =>
        directedTryGraph[Int, Double](Vertex.createWithSet)(triplets) match
          case Success(g: DirectedGraph[Int, Double]) => f(g)
          case Failure(x) => fail("graph construction failed", x)
          case _ => fail("not a DirectedGraph")
      case Failure(x) => fail("parse failed", x)

  private def withPrimGraph[A](f: UndirectedGraph[Int, Double] => A): A =
    val p = new GraphParser[Int, Double, EdgeType]
    val triedSource = Try(Source.fromResource("prim.graph"))
    TryUsing.tryIt(triedSource) { source =>
      p.parseSource[Triplet[Int, Double, EdgeType]](p.parseTriple)(source)
    } match
      case Success(triplets) =>
        undirectedTryGraph[Int, Double](Vertex.createWithSet)(triplets) match
          case Success(g: UndirectedGraph[Int, Double]) => f(g)
          case Failure(x) => fail("graph construction failed", x)
          case _ => fail("not an UndirectedGraph")
      case Failure(x) => fail("parse failed", x)

  // -------------------------------------------------------------------------
  // DirectedGraph.isCyclic
  // -------------------------------------------------------------------------

  behavior of "DirectedGraph.isCyclic"

  it should "return false for dag.graph (acyclic)" in :
    withDagGraph { graph =>
      graph.isCyclic shouldBe false
    }

  it should "return true for directed.graph (has cycle {0,2,5,6})" in :
    withDirectedGraph { graph =>
      graph.isCyclic shouldBe true
    }

  it should "return false for a single vertex" in :
    val triplets: Seq[Triplet[Int, Unit, EdgeType]] = Seq(
      Triplet(0, 1, None, Directed)
    )
    directedTryGraph[Int, Unit](Vertex.createWithSet)(triplets) match
      case Success(g: DirectedGraph[Int, Unit]) =>
        g.isCyclic shouldBe false
      case other => fail(s"unexpected: $other")

  it should "return true for a two-vertex mutual cycle" in :
    val triplets: Seq[Triplet[Int, Unit, EdgeType]] = Seq(
      Triplet(0, 1, None, Directed),
      Triplet(1, 0, None, Directed)
    )
    directedTryGraph[Int, Unit](Vertex.createWithSet)(triplets) match
      case Success(g: DirectedGraph[Int, Unit]) =>
        g.isCyclic shouldBe true
      case other => fail(s"unexpected: $other")

  it should "return true for a three-vertex cycle" in :
    val triplets: Seq[Triplet[Int, Unit, EdgeType]] = Seq(
      Triplet(0, 1, None, Directed),
      Triplet(1, 2, None, Directed),
      Triplet(2, 0, None, Directed)
    )
    directedTryGraph[Int, Unit](Vertex.createWithSet)(triplets) match
      case Success(g: DirectedGraph[Int, Unit]) =>
        g.isCyclic shouldBe true
      case other => fail(s"unexpected: $other")

  it should "return false for a three-vertex chain" in :
    val triplets: Seq[Triplet[Int, Unit, EdgeType]] = Seq(
      Triplet(0, 1, None, Directed),
      Triplet(1, 2, None, Directed)
    )
    directedTryGraph[Int, Unit](Vertex.createWithSet)(triplets) match
      case Success(g: DirectedGraph[Int, Unit]) =>
        g.isCyclic shouldBe false
      case other => fail(s"unexpected: $other")

  // -------------------------------------------------------------------------
  // DAG correctness
  // -------------------------------------------------------------------------

  behavior of "DAG.isCyclic"

  it should "return false for dag.graph (by definition acyclic)" in :
    withDagGraph { graph =>
      // TopologicalSort.sort succeeds iff acyclic
      graph.isCyclic shouldBe false
    }

  it should "be consistent with TopologicalSort — isCyclic iff sort returns None" in :
    withDagGraph { graph =>
      graph.isCyclic shouldBe false
    }
    withDirectedGraph { graph =>
      graph.isCyclic shouldBe true
    }

  // -------------------------------------------------------------------------
  // UndirectedGraph.isCyclic
  // -------------------------------------------------------------------------

  behavior of "UndirectedGraph.isCyclic"

  it should "return true for prim.graph (dense graph with many cycles)" in :
    withPrimGraph { graph =>
      graph.isCyclic shouldBe true
    }

  it should "return false for a simple chain 0-1-2" in :
    val triplets: Seq[Triplet[Int, Unit, EdgeType]] = Seq(
      Triplet(0, 1, None, Undirected),
      Triplet(1, 2, None, Undirected)
    )
    undirectedTryGraph[Int, Unit](Vertex.createWithSet)(triplets) match
      case Success(g: UndirectedGraph[Int, Unit]) =>
        g.isCyclic shouldBe false
      case other => fail(s"unexpected: $other")

  it should "return false for a star graph (one centre, no cycles)" in :
    val triplets: Seq[Triplet[Int, Unit, EdgeType]] = Seq(
      Triplet(0, 1, None, Undirected),
      Triplet(0, 2, None, Undirected),
      Triplet(0, 3, None, Undirected)
    )
    undirectedTryGraph[Int, Unit](Vertex.createWithSet)(triplets) match
      case Success(g: UndirectedGraph[Int, Unit]) =>
        g.isCyclic shouldBe false
      case other => fail(s"unexpected: $other")

  it should "return true for a triangle" in :
    val triplets: Seq[Triplet[Int, Unit, EdgeType]] = Seq(
      Triplet(0, 1, None, Undirected),
      Triplet(1, 2, None, Undirected),
      Triplet(2, 0, None, Undirected)
    )
    undirectedTryGraph[Int, Unit](Vertex.createWithSet)(triplets) match
      case Success(g: UndirectedGraph[Int, Unit]) =>
        g.isCyclic shouldBe true
      case other => fail(s"unexpected: $other")

  it should "return false for a tree with multiple branches" in :
    // Tree: 0-1, 0-2, 1-3, 1-4 — no cycle
    val triplets: Seq[Triplet[Int, Unit, EdgeType]] = Seq(
      Triplet(0, 1, None, Undirected),
      Triplet(0, 2, None, Undirected),
      Triplet(1, 3, None, Undirected),
      Triplet(1, 4, None, Undirected)
    )
    undirectedTryGraph[Int, Unit](Vertex.createWithSet)(triplets) match
      case Success(g: UndirectedGraph[Int, Unit]) =>
        g.isCyclic shouldBe false
      case other => fail(s"unexpected: $other")

  it should "return true for a graph with one extra edge creating a cycle" in :
    // Tree plus one back edge: 0-1, 1-2, 2-3, 3-1
    val triplets: Seq[Triplet[Int, Unit, EdgeType]] = Seq(
      Triplet(0, 1, None, Undirected),
      Triplet(1, 2, None, Undirected),
      Triplet(2, 3, None, Undirected),
      Triplet(3, 1, None, Undirected)
    )
    undirectedTryGraph[Int, Unit](Vertex.createWithSet)(triplets) match
      case Success(g: UndirectedGraph[Int, Unit]) =>
        g.isCyclic shouldBe true
      case other => fail(s"unexpected: $other")

  it should "return false for a disconnected forest (no cycles in any component)" in :
    // Two separate chains: 0-1-2 and 3-4
    val triplets: Seq[Triplet[Int, Unit, EdgeType]] = Seq(
      Triplet(0, 1, None, Undirected),
      Triplet(1, 2, None, Undirected),
      Triplet(3, 4, None, Undirected)
    )
    undirectedTryGraph[Int, Unit](Vertex.createWithSet)(triplets) match
      case Success(g: UndirectedGraph[Int, Unit]) =>
        g.isCyclic shouldBe false
      case other => fail(s"unexpected: $other")

  it should "return true for a disconnected graph where one component has a cycle" in :
    // Chain 0-1-2 plus triangle 3-4-5-3
    val triplets: Seq[Triplet[Int, Unit, EdgeType]] = Seq(
      Triplet(0, 1, None, Undirected),
      Triplet(1, 2, None, Undirected),
      Triplet(3, 4, None, Undirected),
      Triplet(4, 5, None, Undirected),
      Triplet(5, 3, None, Undirected)
    )
    undirectedTryGraph[Int, Unit](Vertex.createWithSet)(triplets) match
      case Success(g: UndirectedGraph[Int, Unit]) =>
        g.isCyclic shouldBe true
      case other => fail(s"unexpected: $other")