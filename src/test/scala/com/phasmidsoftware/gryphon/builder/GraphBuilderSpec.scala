/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.builder

import com.phasmidsoftware.gryphon.adjunct.{DirectedGraph, UndirectedGraph}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import scala.util.{Failure, Success}

/**
 * Tests for GraphBuilder — loading graphs from classpath resources.
 *
 * prim.graph (Sedgewick tinyEWG) — 8 vertices (0–7), 16 undirected weighted edges.
 * MST total weight: 1.81
 */
class GraphBuilderSpec extends AnyFlatSpec with should.Matchers:

  given Numeric[Double] = scala.math.Numeric.DoubleIsFractional

  given Ordering[Double] = scala.math.Ordering.Double.TotalOrdering

  // -------------------------------------------------------------------------
  // UndirectedGraph — fromResource
  // -------------------------------------------------------------------------

  behavior of "GraphBuilder.undirected — fromResource"

  it should "successfully load prim.graph as an UndirectedGraph" in :
    GraphBuilder.undirected[Int, Double].fromResource("prim.graph") match
      case Success(graph) => succeed
      case Failure(x) => fail("failed to load prim.graph", x)

  it should "produce a graph with 8 vertices" in :
    GraphBuilder.undirected[Int, Double].fromResource("prim.graph") match
      case Success(graph) => graph.N shouldBe 8
      case Failure(x) => fail("failed to load prim.graph", x)

  it should "produce a graph with 16 edges" in :
    GraphBuilder.undirected[Int, Double].fromResource("prim.graph") match
      case Success(graph) => graph.M shouldBe 16
      case Failure(x) => fail("failed to load prim.graph", x)

  it should "return an UndirectedGraph instance" in :
    GraphBuilder.undirected[Int, Double].fromResource("prim.graph") match
      case Success(graph: UndirectedGraph[Int, Double]) => succeed
      case Success(_) => fail("not an UndirectedGraph")
      case Failure(x) => fail("failed to load prim.graph", x)

  it should "produce a graph whose MST (Kruskal) has total weight 1.81" in :
    import com.phasmidsoftware.gryphon.traverse.Kruskal
    GraphBuilder.undirected[Int, Double].fromResource("prim.graph") match
      case Success(graph) =>
        Kruskal.mst(graph).map(_.attribute).sum shouldBe 1.81 +- 0.001
      case Failure(x) => fail("failed to load prim.graph", x)

  it should "return Failure for a non-existent resource" in :
    GraphBuilder.undirected[Int, Double].fromResource("nosuchfile.graph") shouldBe a[Failure[?]]

  // -------------------------------------------------------------------------
  // DirectedGraph — fromResource
  // -------------------------------------------------------------------------

  behavior of "GraphBuilder.directed — fromResource"

  it should "successfully load directed.graph" in :
    GraphBuilder.directed[Int, Unit].fromResource("directed.graph") match
      case Success(graph) => succeed
      case Failure(x) => fail("failed to load directed.graph", x)

  it should "return a DirectedGraph instance" in :
    GraphBuilder.directed[Int, Unit].fromResource("directed.graph") match
      case Success(graph: DirectedGraph[Int, Unit]) => succeed
      case Success(_) => fail("not a DirectedGraph")
      case Failure(x) => fail("failed to load directed.graph", x)

  it should "produce a graph with 7 vertices from directed.graph" in :
    GraphBuilder.directed[Int, Unit].fromResource("directed.graph") match
      case Success(graph) => graph.N shouldBe 7
      case Failure(x) => fail("failed to load directed.graph", x)

  it should "produce a graph with 12 edges from directed.graph" in :
    GraphBuilder.directed[Int, Unit].fromResource("directed.graph") match
      case Success(graph) => graph.M shouldBe 12
      case Failure(x) => fail("failed to load directed.graph", x)

  // -------------------------------------------------------------------------
  // fromFile
  // -------------------------------------------------------------------------

  behavior of "GraphBuilder.undirected — fromFile"

  it should "return Failure for a non-existent file path" in :
    GraphBuilder.undirected[Int, Double].fromFile("/no/such/path/graph.graph") shouldBe a[Failure[?]]