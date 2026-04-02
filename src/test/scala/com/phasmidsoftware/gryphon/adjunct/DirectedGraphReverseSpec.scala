/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.adjunct

import com.phasmidsoftware.gryphon.adjunct.DirectedGraph.triplesToTryGraph
import com.phasmidsoftware.gryphon.core.*
import com.phasmidsoftware.gryphon.parse.GraphParser
import com.phasmidsoftware.gryphon.util.TryUsing
import com.phasmidsoftware.visitor.core.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import scala.io.Source
import scala.util.*

/**
 * Tests for DirectedGraph.reverse.
 *
 * dag.graph has 7 vertices (0–6) and 11 directed edges:
 * 0->5, 0->2, 0->1, 3->6, 3->5, 3->4, 5->2, 6->4, 6->0, 3->2, 1->4
 *
 * Reversed edge set:
 * 5->0, 2->0, 1->0, 6->3, 5->3, 4->3, 2->5, 4->6, 0->6, 2->3, 4->1
 *
 * Adjacency lists in the reversed graph (outgoing edges per vertex):
 * 0: {6}
 * 1: {0}
 * 2: {0, 5, 3}
 * 3: (none)
 * 4: {3, 6, 1}
 * 5: {0, 3}
 * 6: {3}
 */
class DirectedGraphReverseSpec extends AnyFlatSpec with should.Matchers:

  // -------------------------------------------------------------------------
  // Shared fixture — load dag.graph once and expose both the original and its
  // reverse.  Tests that don't need the graph file use inline triplets instead.
  // -------------------------------------------------------------------------

  /** Parses dag.graph and returns (originalGraph, reversedGraph). */
  private def loadDagGraphs: (DirectedGraph[Int, Double], DirectedGraph[Int, Double]) =
    val p = new GraphParser[Int, Double, EdgeType]
    val triedSource = Try(Source.fromResource("dag.graph"))
    val triplets = TryUsing.tryIt(triedSource) { source =>
      p.parseSource[Triplet[Int, Double, EdgeType]](p.parseTriple)(source)
    }.get
    triplesToTryGraph[Int, Double](Vertex.createWithSet)(triplets).get match
      case g: DirectedGraph[Int, Double] @unchecked => (g, g.reverse)
      case other => fail(s"unexpected graph type: $other")

  // -------------------------------------------------------------------------
  // Structure tests
  // -------------------------------------------------------------------------

  behavior of "DirectedGraph.reverse — structure"

  it should "preserve vertex count" in {
    val (original, reversed) = loadDagGraphs
    reversed.N shouldBe original.N   // 7 vertices
    reversed.N shouldBe 7
  }

  it should "preserve edge count" in {
    val (original, reversed) = loadDagGraphs
    reversed.M shouldBe original.M   // 11 edges
    reversed.M shouldBe 11
  }

  it should "contain all original vertices" in {
    val (original, reversed) = loadDagGraphs
    val originalKeys = original.vertexMap.keySet
    val reversedKeys = reversed.vertexMap.keySet
    reversedKeys shouldBe originalKeys
  }

  it should "produce a different edge set from the original" in {
    val (original, reversed) = loadDagGraphs
    val originalEdges = original.edges.map(e => (e.white, e.black)).toSet
    val reversedEdges = reversed.edges.map(e => (e.white, e.black)).toSet
    reversedEdges should not equal originalEdges
  }

  it should "have edge set equal to the original with white and black swapped" in {
    val (original, reversed) = loadDagGraphs
    val originalFlipped = original.edges.map(e => (e.black, e.white)).toSet
    val reversedEdges = reversed.edges.map(e => (e.white, e.black)).toSet
    reversedEdges shouldBe originalFlipped
  }

  it should "be its own inverse — reversing twice yields the original edge set" in {
    val (original, reversed) = loadDagGraphs
    val doubleReversed = reversed.reverse
    val originalEdges = original.edges.map(e => (e.white, e.black)).toSet
    val doubleReversedEdges = doubleReversed.edges.map(e => (e.white, e.black)).toSet
    doubleReversedEdges shouldBe originalEdges
  }

  // -------------------------------------------------------------------------
  // Per-vertex adjacency tests
  // -------------------------------------------------------------------------

  behavior of "DirectedGraph.reverse — adjacency lists"

  it should "give vertex 0 exactly one outgoing edge (to 6)" in {
    val (_, reversed) = loadDagGraphs

    given Random = Random(42)

    reversed.adjacentVertices(0).toSet shouldBe Set(6)
  }

  it should "give vertex 1 exactly one outgoing edge (to 0)" in {
    val (_, reversed) = loadDagGraphs

    given Random = Random(42)

    reversed.adjacentVertices(1).toSet shouldBe Set(0)
  }

  it should "give vertex 2 exactly three outgoing edges (to 0, 5, 3)" in {
    val (_, reversed) = loadDagGraphs

    given Random = Random(42)

    reversed.adjacentVertices(2).toSet shouldBe Set(0, 5, 3)
  }

  it should "give vertex 3 no outgoing edges" in {
    val (_, reversed) = loadDagGraphs

    given Random = Random(42)

    reversed.adjacentVertices(3).toSet shouldBe Set.empty[Int]
  }

  it should "give vertex 4 exactly three outgoing edges (to 3, 6, 1)" in {
    val (_, reversed) = loadDagGraphs

    given Random = Random(42)

    reversed.adjacentVertices(4).toSet shouldBe Set(3, 6, 1)
  }

  it should "give vertex 5 exactly two outgoing edges (to 0 and 3)" in {
    val (_, reversed) = loadDagGraphs

    given Random = Random(42)

    reversed.adjacentVertices(5).toSet shouldBe Set(0, 3)
  }

  it should "give vertex 6 exactly one outgoing edge (to 3)" in {
    val (_, reversed) = loadDagGraphs

    given Random = Random(42)

    reversed.adjacentVertices(6).toSet shouldBe Set(3)
  }

  // -------------------------------------------------------------------------
  // Edge attribute preservation
  // -------------------------------------------------------------------------

  behavior of "DirectedGraph.reverse — edge attributes"

  it should "preserve edge weights after reversal" in {
    val (original, reversed) = loadDagGraphs
    // Build a map of (from, to) -> weight for both graphs, then compare
    // with from/to swapped.
    val originalWeights = original.edges
            .collect { case e: AttributedDirectedEdge[Double, Int] @unchecked => (e.white, e.black) -> e.attribute }
            .toMap
    val reversedWeights = reversed.edges
            .collect { case e: AttributedDirectedEdge[Double, Int] @unchecked => (e.white, e.black) -> e.attribute }
            .toMap
    // For every original edge (u->v, w), the reversed graph must have (v->u, w).
    originalWeights.foreach { case ((u, v), w) =>
      reversedWeights.get((v, u)) shouldBe Some(w)
    }
  }

  // -------------------------------------------------------------------------
  // Minimal inline graph tests (no file I/O)
  // -------------------------------------------------------------------------

  behavior of "DirectedGraph.reverse — inline graphs"

  it should "reverse a single-edge graph" in {
    val triplets: Seq[Triplet[Int, Double, EdgeType]] = Seq(
      Triplet(0, 1, Some(1.0), Directed)
    )
    triplesToTryGraph[Int, Double](Vertex.createWithSet)(triplets) match
      case Success(g: DirectedGraph[Int, Double] @unchecked) =>
        val r = g.reverse
        r.N shouldBe 2
        r.M shouldBe 1
        r.edges.map(e => (e.white, e.black)).toSet shouldBe Set((1, 0))
      case other => fail(s"unexpected: $other")
  }

  it should "reverse a two-vertex cycle and recover the original" in {
    val triplets: Seq[Triplet[Int, Double, EdgeType]] = Seq(
      Triplet(0, 1, Some(1.0), Directed),
      Triplet(1, 0, Some(2.0), Directed)
    )
    triplesToTryGraph[Int, Double](Vertex.createWithSet)(triplets) match
      case Success(g: DirectedGraph[Int, Double] @unchecked) =>
        val original = g.edges.map(e => (e.white, e.black)).toSet
        val doubleReversed = g.reverse.reverse.edges.map(e => (e.white, e.black)).toSet
        doubleReversed shouldBe original
      case other => fail(s"unexpected: $other")
  }

  it should "reverse a three-vertex chain 0->1->2 to 2->1->0" in {
    val triplets: Seq[Triplet[Int, Unit, EdgeType]] = Seq(
      Triplet(0, 1, None, Directed),
      Triplet(1, 2, None, Directed)
    )
    triplesToTryGraph[Int, Unit](Vertex.createWithSet)(triplets) match
      case Success(g: DirectedGraph[Int, Unit] @unchecked) =>
        val r = g.reverse
        r.edges.map(e => (e.white, e.black)).toSet shouldBe Set((1, 0), (2, 1))
      case other => fail(s"unexpected: $other")
  }

  it should "preserve isolated vertices (no edges) through reversal" in {
    // Build a one-edge graph, then manually add an isolated vertex.
    val triplets: Seq[Triplet[Int, Unit, EdgeType]] = Seq(
      Triplet(0, 1, None, Directed)
    )
    triplesToTryGraph[Int, Unit](Vertex.createWithSet)(triplets) match
      case Success(g: DirectedGraph[Int, Unit] @unchecked) =>
        // Add vertex 99 with no edges via addVertex
        val withIsolated = g.addVertex(Vertex.createWithSet(99)).asInstanceOf[DirectedGraph[Int, Unit]]
        val r = withIsolated.reverse
        r.N shouldBe 3            // 0, 1, 99
        r.vertexMap.keySet should contain(99)

        given Random = Random(42)

        r.adjacentVertices(99).toSet shouldBe Set.empty[Int]
      case other => fail(s"unexpected: $other")
  }