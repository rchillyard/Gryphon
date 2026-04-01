/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.traverse

import com.phasmidsoftware.gryphon.adjunct.UndirectedGraph.triplesToTryGraph
import com.phasmidsoftware.gryphon.core.*
import com.phasmidsoftware.gryphon.parse.GraphParser
import com.phasmidsoftware.gryphon.util.TryUsing
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import scala.io.Source
import scala.util.{Failure, Random, Success, Try}

class PrimSpec extends AnyFlatSpec with should.Matchers:

  // prim.graph: 8 vertices (0-7), 16 undirected weighted edges.
  // MST (Sedgewick & Wayne): 7 edges, total weight 1.81
  //   0-7 (0.16), 2-3 (0.17), 1-7 (0.19), 0-2 (0.26),
  //   5-7 (0.28), 4-5 (0.35), 6-2 (0.40)

  given Random = Random(42)

  given Numeric[Double] = scala.math.Numeric.DoubleIsFractional

  given Ordering[Double] = scala.math.Ordering.Double.TotalOrdering

  private def loadGraph: Try[Graph[Int]] =
    val p = new GraphParser[Int, Double, EdgeType]
    val triedSource = Try(Source.fromResource("prim.graph"))
    val zsy: Try[Seq[Triplet[Int, Double, EdgeType]]] = TryUsing.tryIt(triedSource) {
      source => p.parseSource[Triplet[Int, Double, EdgeType]](p.parseTriple)(source)
    }
    zsy.flatMap(triplets => triplesToTryGraph[Int, Double](Vertex.createWithSet)(triplets))
            .collect { case g: Graph[Int] => g }

  behavior of "Prim"

  it should "parse prim.graph correctly" in {
    pending // TODO Issue #8
    loadGraph match
      case Success(graph) =>
        graph.N shouldBe 8
        graph.M shouldBe 16
      case Failure(x) => fail("parse failed", x)
  }

  it should "produce MST with exactly 7 edges for 8 vertices" in {
    pending // TODO Issue #8
    loadGraph match
      case Success(graph) =>
        val result = PrimTraversal[Int, Double]().run(graph)(0)
        // MST for n vertices has n-1 edges; source vertex has no entry
        result.asInstanceOf[VertexTraversalResult[Int, Edge[Double, Int]]].map.size shouldBe 7
      case Failure(x) => fail("parse failed", x)
  }

  it should "include all 8 vertices in MST (source + 7 entries)" in {
    pending // TODO Issue #8
    loadGraph match
      case Success(graph) =>
        val result = PrimTraversal[Int, Double]().run(graph)(0)
        val mstMap = result.asInstanceOf[VertexTraversalResult[Int, Edge[Double, Int]]].map
        // All vertices except source appear as keys
        mstMap.keySet shouldBe Set(1, 2, 3, 4, 5, 6, 7)
      case Failure(x) => fail("parse failed", x)
  }

  it should "produce MST with correct total weight of 1.81" in {
    pending // TODO Issue #8
    loadGraph match
      case Success(graph) =>
        val result = PrimTraversal[Int, Double]().run(graph)(0)
        val mstMap = result.asInstanceOf[VertexTraversalResult[Int, Edge[Double, Int]]].map
        val totalWeight = mstMap.values.map(_.attribute).sum
        totalWeight shouldBe 1.81 +- 0.001
      case Failure(x) => fail("parse failed", x)
  }

  it should "include the minimum weight edge 0-7 (0.16)" in {
    loadGraph match
      case Success(graph) =>
        val result = PrimTraversal[Int, Double]().run(graph)(0)
        val mstMap = result.asInstanceOf[VertexTraversalResult[Int, Edge[Double, Int]]].map
        result.vertexTraverse(7) match
          case Some(edge) => edge.attribute shouldBe 0.16 +- 0.001
          case None => fail("vertex 7 not in MST")
      case Failure(x) => fail("parse failed", x)
  }

  it should "include edge to vertex 2 with weight 0.26 (via 0-2)" in {
    loadGraph match
      case Success(graph) =>
        val result = PrimTraversal[Int, Double]().run(graph)(0)
        result.vertexTraverse(2) match
          case Some(edge) => edge.attribute shouldBe 0.26 +- 0.001
          case None => fail("vertex 2 not in MST")
      case Failure(x) => fail("parse failed", x)
  }

  it should "include edge to vertex 3 with weight 0.17 (via 2-3)" in {
    loadGraph match
      case Success(graph) =>
        val result = PrimTraversal[Int, Double]().run(graph)(0)
        result.vertexTraverse(3) match
          case Some(edge) => edge.attribute shouldBe 0.17 +- 0.001
          case None => fail("vertex 3 not in MST")
      case Failure(x) => fail("parse failed", x)
  }

  it should "include edge to vertex 6 with weight 0.40 (via 2-6)" in {
    pending // TODO Issue #8
    loadGraph match
      case Success(graph) =>
        val result = PrimTraversal[Int, Double]().run(graph)(0)
        result.vertexTraverse(6) match
          case Some(edge) => edge.attribute shouldBe 0.40 +- 0.001
          case None => fail("vertex 6 not in MST")
      case Failure(x) => fail("parse failed", x)
  }

  it should "not include any edge heavier than the maximum MST edge weight (0.40)" in {
    pending // TODO Issue #8
    loadGraph match
      case Success(graph) =>
        val result = PrimTraversal[Int, Double]().run(graph)(0)
        println(s"result: $result")
        result.size shouldBe 7

        val keySet = result.keySet
        println(s"keySet: $keySet")

        // CONSIDER removing this cast.
        val mstMap = result.asInstanceOf[VertexTraversalResult[Int, Edge[Double, Int]]].map
        mstMap.values.map(_.attribute).foreach { w =>
          w should be <= 0.401
        }
      case Failure(x) => fail("parse failed", x)
  }