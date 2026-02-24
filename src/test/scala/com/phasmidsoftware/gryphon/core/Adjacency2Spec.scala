package com.phasmidsoftware.gryphon.core

import com.phasmidsoftware.gryphon.adjunct.{AttributedDirectedEdge, UndirectedEdge}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class Adjacency2Spec extends AnyFlatSpec with Matchers:

  behavior of "AdjacencyVertex"

  it should "return its vertex" in {
    AdjacencyVertex(42).vertex shouldBe 42
  }

  it should "return None for maybeEdge" in {
    AdjacencyVertex("x").maybeEdge[Int] shouldBe None
  }

  it should "construct from a Vertex" in {
    val v = Vertex.createWithBag(7)
    AdjacencyVertex(v).vertex shouldBe 7
  }

  behavior of "AdjacencyEdge"

  it should "return black vertex when not flipped (directed)" in {
    val edge = AttributedDirectedEdge(1.0, 3, 5)
    AdjacencyEdge[Int, Double](edge).vertex shouldBe 5
  }

  it should "return white vertex when flipped (undirected reverse)" in {
    val edge = UndirectedEdge(1.0, 3, 5)
    AdjacencyEdge[Int, Double](edge, flipped = true).vertex shouldBe 3
  }

  it should "return Some(edge) for maybeEdge on a directed edge" in {
    val edge = AttributedDirectedEdge(2.5, 1, 2)
    AdjacencyEdge[Int, Double](edge).maybeEdge[Double] shouldBe Some(edge)
  }

  it should "return None for maybeEdge on a plain VertexPair connexion" in {
    val pair = VertexPair(1, 2)
    AdjacencyEdge[Int, Nothing](pair).maybeEdge[Double] shouldBe None
  }

  it should "default flipped to false" in {
    val edge = AttributedDirectedEdge(0.0, 10, 20)
    val adj = AdjacencyEdge[Int, Double](edge)
    adj.flipped shouldBe false
    adj.vertex shouldBe 20
  }