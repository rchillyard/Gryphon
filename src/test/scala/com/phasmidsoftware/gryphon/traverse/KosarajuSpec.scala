/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.traverse

import com.phasmidsoftware.gryphon.adjunct.DirectedGraph
import com.phasmidsoftware.gryphon.adjunct.DirectedGraph.triplesToTryGraph
import com.phasmidsoftware.gryphon.core.*
import com.phasmidsoftware.gryphon.parse.GraphParser
import com.phasmidsoftware.gryphon.traverse.Kosaraju.stronglyConnectedComponents
import com.phasmidsoftware.gryphon.util.TryUsing
import com.phasmidsoftware.visitor.core.Tracer
import java.io.{ByteArrayOutputStream, PrintStream}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import scala.io.Source
import scala.util.{Random, Success, Try}

/**
 * Tests for Kosaraju's strongly-connected-components algorithm.
 *
 * directed.graph has 7 vertices (0–6) and 12 directed edges:
 * 0->5, 0->2, 0->1, 3->6, 3->5, 3->4, 5->2, 6->4, 6->0, 3->2, 1->4, 2->6
 *
 * SCCs:
 * {0, 2, 5, 6} — cycle 0->5->2->6->0
 * {1}          — singleton (reached from 0, leads only to 4)
 * {3}          — singleton (source, reaches everything)
 * {4}          — singleton (sink, reached from 3, 6, 1)
 */
class KosarajuSpec extends AnyFlatSpec with should.Matchers:

  given Random = Random(42)

  // -------------------------------------------------------------------------
  // Shared fixture
  // -------------------------------------------------------------------------

  private def loadDirectedGraph: DirectedGraph[Int, Double] =
    val p = new GraphParser[Int, Double, EdgeType]
    val triedSource = Try(Source.fromResource("directed.graph"))
    val triplets = TryUsing.tryIt(triedSource) { source =>
      p.parseSource[Triplet[Int, Double, EdgeType]](p.parseTriple)(source)
    }.get
    triplesToTryGraph[Int, Double](Vertex.createWithSet)(triplets).get match
      case g: DirectedGraph[Int, Double] @unchecked => g
      case other => fail(s"unexpected graph type: $other")

  // -------------------------------------------------------------------------
  // Structure of the result
  // -------------------------------------------------------------------------

  behavior of "Kosaraju — result structure"

  it should "label all 7 vertices" in {
    val result = stronglyConnectedComponents[Int, Double](loadDirectedGraph)
    result.size shouldBe 7
  }

  it should "find exactly 4 SCCs" in {
    val result = stronglyConnectedComponents[Int, Double](loadDirectedGraph)
    result.values.toSet.size shouldBe 4
  }

  // -------------------------------------------------------------------------
  // Non-trivial SCC
  // -------------------------------------------------------------------------

  behavior of "Kosaraju — non-trivial SCC {0, 2, 5, 6}"

  it should "assign vertices 0, 2, 5, and 6 the same SCC id" in {
    val result = stronglyConnectedComponents[Int, Double](loadDirectedGraph)
    result(0) shouldBe result(2)
    result(2) shouldBe result(5)
    result(5) shouldBe result(6)
  }

  it should "assign the SCC {0,2,5,6} a different id from every singleton" in {
    val result = stronglyConnectedComponents[Int, Double](loadDirectedGraph)
    val scc0256 = result(0)
    result(1) should not be scc0256
    result(3) should not be scc0256
    result(4) should not be scc0256
  }

  // -------------------------------------------------------------------------
  // Singleton SCCs
  // -------------------------------------------------------------------------

  behavior of "Kosaraju — singleton SCCs"

  it should "assign vertices 1, 3, 4 each their own unique SCC id" in {
    val result = stronglyConnectedComponents[Int, Double](loadDirectedGraph)
    val singletons = Seq(result(1), result(3), result(4))
    singletons.distinct.size shouldBe 3
  }

  it should "assign vertex 1 a different SCC id from vertex 4 (1->4 is a cross edge)" in {
    val result = stronglyConnectedComponents[Int, Double](loadDirectedGraph)
    result(1) should not be result(4)
  }

  it should "assign vertex 3 a different SCC id from vertex 6 (3->6 is a cross edge)" in {
    val result = stronglyConnectedComponents[Int, Double](loadDirectedGraph)
    result(3) should not be result(6)
  }

  // -------------------------------------------------------------------------
  // Inline graph tests
  // -------------------------------------------------------------------------

  behavior of "Kosaraju — inline graphs"

  it should "find one SCC in a single-vertex graph" in {
    val triplets: Seq[Triplet[Int, Unit, EdgeType]] = Seq(
      Triplet(0, 0, None, Directed)   // self-loop
    )
    triplesToTryGraph[Int, Unit](Vertex.createWithSet)(triplets) match
      case Success(g: DirectedGraph[Int, Unit] @unchecked) =>
        val result = stronglyConnectedComponents[Int, Unit](g)
        result.values.toSet.size shouldBe 1
      case other => fail(s"unexpected: $other")
  }

  it should "find one SCC in a two-vertex mutual cycle" in {
    val triplets: Seq[Triplet[Int, Unit, EdgeType]] = Seq(
      Triplet(0, 1, None, Directed),
      Triplet(1, 0, None, Directed)
    )
    triplesToTryGraph[Int, Unit](Vertex.createWithSet)(triplets) match
      case Success(g: DirectedGraph[Int, Unit] @unchecked) =>
        val result = stronglyConnectedComponents[Int, Unit](g)
        result.values.toSet.size shouldBe 1
        result(0) shouldBe result(1)
      case other => fail(s"unexpected: $other")
  }

  it should "find two SCCs in a two-vertex one-way edge" in {
    val triplets: Seq[Triplet[Int, Unit, EdgeType]] = Seq(
      Triplet(0, 1, None, Directed)
    )
    triplesToTryGraph[Int, Unit](Vertex.createWithSet)(triplets) match
      case Success(g: DirectedGraph[Int, Unit] @unchecked) =>
        val result = stronglyConnectedComponents[Int, Unit](g)
        result.values.toSet.size shouldBe 2
        result(0) should not be result(1)
      case other => fail(s"unexpected: $other")
  }

  it should "find three SCCs in a three-vertex chain 0->1->2" in {
    val triplets: Seq[Triplet[Int, Unit, EdgeType]] = Seq(
      Triplet(0, 1, None, Directed),
      Triplet(1, 2, None, Directed)
    )
    triplesToTryGraph[Int, Unit](Vertex.createWithSet)(triplets) match
      case Success(g: DirectedGraph[Int, Unit] @unchecked) =>
        val result = stronglyConnectedComponents[Int, Unit](g)
        result.values.toSet.size shouldBe 3
        result(0) should not be result(1)
        result(1) should not be result(2)
      case other => fail(s"unexpected: $other")
  }

  it should "find one SCC in a three-vertex cycle 0->1->2->0" in {
    val triplets: Seq[Triplet[Int, Unit, EdgeType]] = Seq(
      Triplet(0, 1, None, Directed),
      Triplet(1, 2, None, Directed),
      Triplet(2, 0, None, Directed)
    )
    triplesToTryGraph[Int, Unit](Vertex.createWithSet)(triplets) match
      case Success(g: DirectedGraph[Int, Unit] @unchecked) =>
        val result = stronglyConnectedComponents[Int, Unit](g)
        result.values.toSet.size shouldBe 1
        result(0) shouldBe result(1)
        result(1) shouldBe result(2)
      case other => fail(s"unexpected: $other")
  }

  // -------------------------------------------------------------------------
  // Tracing
  // -------------------------------------------------------------------------

  private def captureOutput(block: PrintStream => Unit): List[String] =
    val baos = ByteArrayOutputStream()
    val ps = PrintStream(baos)
    block(ps)
    ps.flush()
    baos.toString.linesIterator.filter(_.nonEmpty).toList

  behavior of "Kosaraju — tracing"

  it should "emit the expected summary trace for a two-vertex mutual cycle" in {
    val triplets: Seq[Triplet[Int, Unit, EdgeType]] = Seq(
      Triplet(0, 1, None, Directed),
      Triplet(1, 0, None, Directed)
    )
    triplesToTryGraph[Int, Unit](Vertex.createWithSet)(triplets) match
      case Success(g: DirectedGraph[Int, Unit] @unchecked) =>
        val lines = captureOutput: ps =>
          given Tracer[Int] = Tracer.summary(out = ps)

          stronglyConnectedComponents[Int, Unit](g): Unit
        lines shouldBe List(
          "Kosaraju pass 1: post-order DFS on reversed graph",
          "starters: 2 elements",
          "Kosaraju pass 2: DFS on original graph, 2 starter vertices"
        )
      case other => fail(s"unexpected: $other")
  }

  it should "emit the expected verbose trace for a two-vertex mutual cycle" in {
    val triplets: Seq[Triplet[Int, Unit, EdgeType]] = Seq(
      Triplet(0, 1, None, Directed),
      Triplet(1, 0, None, Directed)
    )
    triplesToTryGraph[Int, Unit](Vertex.createWithSet)(triplets) match
      case Success(g: DirectedGraph[Int, Unit] @unchecked) =>
        val lines = captureOutput: ps =>
          given Tracer[Int] = Tracer.verbose(maxDepth = 1, out = ps)

          stronglyConnectedComponents[Int, Unit](g): Unit

        println(lines)
        lines should contain("Kosaraju pass 1: post-order DFS on reversed graph")
        lines should contain("starters: 0, 1")
        lines.count(_.contains("pass 1: seeding from")) shouldBe 1
        lines.count(_.contains("pass 2: SCC")) shouldBe 1
        lines.count(_.contains("pass 2: complete")) shouldBe 1
      case other => fail(s"unexpected: $other")
  }