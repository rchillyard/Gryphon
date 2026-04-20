/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.builder

import com.phasmidsoftware.gryphon.adjunct.UndirectedGraph
import com.phasmidsoftware.gryphon.traverse.{Boruvka, Kruskal}
import com.phasmidsoftware.visitor.core.given_Monoid_Double
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import scala.util.{Failure, Success}

/**
 * Integration tests for GraphBuilder using the Northeastern University tunnel network.
 *
 * tunnels.graph — 80 buildings (String codes), 79 undirected weighted edges.
 * Edge weights are tunnel construction costs in dollars (stored as Double).
 *
 * Known results (Tunnels_Gryphon.java, verified since 2018):
 * Buildings:  80
 * MST edges:  79
 * Total cost: $6,648,954
 */
class TunnelsSpec extends AnyFlatSpec with should.Matchers:

  given Ordering[Double] = scala.math.Ordering.Double.TotalOrdering

  given Numeric[Double] = scala.math.Numeric.DoubleIsFractional

  private val KNOWN_MST_COST = 6_648_954.0
  private val KNOWN_MST_EDGES = 79
  private val KNOWN_BUILDINGS = 80

  private def withTunnelGraph[A](f: UndirectedGraph[String, Double] => A): A =
    GraphBuilder.undirected[String, Double].fromResource("tunnels.graph") match
      case Success(graph) => f(graph)
      case Failure(x) => fail("failed to load tunnels.graph", x)

  // -------------------------------------------------------------------------
  // Structure
  // -------------------------------------------------------------------------

  behavior of "GraphBuilder — tunnels.graph structure"

  it should "load tunnels.graph successfully" in :
    GraphBuilder.undirected[String, Double].fromResource("tunnels.graph") shouldBe a[Success[?]]

  it should "produce a graph with 80 buildings" in :
    withTunnelGraph { graph => graph.N shouldBe KNOWN_BUILDINGS }

  it should "produce a graph with 79 edges" in :
    withTunnelGraph { graph => graph.M shouldBe KNOWN_MST_EDGES }

  // -------------------------------------------------------------------------
  // Kruskal
  // -------------------------------------------------------------------------

  behavior of "Kruskal — tunnels.graph"

  it should "produce 79 MST edges" in :
    withTunnelGraph { graph =>
      Kruskal.mst(graph).size shouldBe KNOWN_MST_EDGES
    }

  it should "produce total cost of 6648954" in :
    withTunnelGraph { graph =>
      Kruskal.mst(graph).map(_.attribute).sum shouldBe KNOWN_MST_COST +- 1.0
    }

  // -------------------------------------------------------------------------
  // Boruvka
  // -------------------------------------------------------------------------

  behavior of "Boruvka — tunnels.graph"

  it should "produce 79 MST edges" in :
    withTunnelGraph { graph =>
      Boruvka.mst(graph).size shouldBe KNOWN_MST_EDGES
    }

  it should "produce total cost of 6648954" in :
    withTunnelGraph { graph =>
      Boruvka.mst(graph).map(_.attribute).sum shouldBe KNOWN_MST_COST +- 1.0
    }

  // -------------------------------------------------------------------------
  // Agreement
  // -------------------------------------------------------------------------

  behavior of "MST algorithm agreement — tunnels.graph"

  it should "Kruskal and Boruvka agree on total cost" in :
    withTunnelGraph { graph =>
      val kruskalTotal = Kruskal.mst(graph).map(_.attribute).sum
      val boruvkaTotal = Boruvka.mst(graph).map(_.attribute).sum
      kruskalTotal shouldBe boruvkaTotal +- 1.0
    }