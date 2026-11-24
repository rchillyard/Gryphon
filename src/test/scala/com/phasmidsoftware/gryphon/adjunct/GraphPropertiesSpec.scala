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
import scala.util.{Failure, Random, Success, Try}

/**
 * Tests for degree methods on UndirectedGraph and numberOfSelfLoops on EdgeGraph.
 *
 * prim.graph degree summary:
 * vertex 0: 4  (0-7, 0-2, 0-4, 0-6)
 * vertex 1: 4  (1-7, 1-3, 1-5, 1-2)
 * vertex 2: 5  (2-3, 2-7, 2-1, 2-6, 2-0)
 * vertex 3: 3  (3-2, 3-1, 3-6)
 * vertex 4: 3  (4-5, 4-7, 4-0)
 * vertex 5: 3  (5-7, 5-1, 5-4)
 * vertex 6: 4  (6-2, 6-3, 6-0, 6-4)
 * vertex 7: 5  (7-0, 7-1, 7-5, 7-2, 7-4)
 * maxDegree = 5, meanDegree = 31/8 = 3.875
 */
class GraphPropertiesSpec extends AnyFlatSpec with should.Matchers:

  given Random = Random(42)

  private def withPrimGraph[A](f: UndirectedGraph[Int, Double] => A): A =
    val p = new GraphParser[Int, Double, EdgeType]
    val triedSource = Try(Source.fromResource("prim.graph"))
    TryUsing.tryIt(triedSource) { source =>
      p.parseSource[Triplet[Int, Double, EdgeType]](p.parseTriple)(source)
    } match
      case Success(triplets) =>
        triplesToTryGraph[Int, Double](Vertex.createWithSet)(triplets) match
          case Success(graph: UndirectedGraph[Int, Double] @unchecked) => f(graph)
          case Failure(x) => fail("graph construction failed", x)
      case Failure(x) => fail("parse failed", x)

  // -------------------------------------------------------------------------
  // degree
  // -------------------------------------------------------------------------

  behavior of "UndirectedGraph.degree"

  it should "give vertex 0 degree 4" in :
    withPrimGraph { graph =>
      graph.degree(0) shouldBe 4
    }

  it should "give vertex 2 degree 5" in :
    withPrimGraph { graph =>
      graph.degree(2) shouldBe 5
    }

  it should "give vertex 7 degree 5" in :
    withPrimGraph { graph =>
      graph.degree(7) shouldBe 5
    }

  it should "give vertex 3 degree 3" in :
    withPrimGraph { graph =>
      graph.degree(3) shouldBe 3
    }

  // -------------------------------------------------------------------------
  // maxDegree
  // -------------------------------------------------------------------------

  behavior of "UndirectedGraph.maxDegree"

  it should "return 5 for prim.graph" in :
    withPrimGraph { graph =>
      graph.maxDegree shouldBe 5
    }

  it should "return 1 for a single-edge graph" in :
    val triplets: Seq[Triplet[Int, Double, EdgeType]] = Seq(
      Triplet(0, 1, Some(1.0), Undirected)
    )
    triplesToTryGraph[Int, Double](Vertex.createWithSet)(triplets) match
      case Success(graph: UndirectedGraph[Int, Double] @unchecked) =>
        graph.maxDegree shouldBe 1
      case other => fail(s"unexpected: $other")

  // -------------------------------------------------------------------------
  // meanDegree
  // -------------------------------------------------------------------------

  behavior of "UndirectedGraph.meanDegree"

  it should "return 3.875 for prim.graph" in :
    withPrimGraph { graph =>
      graph.meanDegree shouldBe 4.0 +- 0.001
    }

  it should "satisfy meanDegree == 2*M/N for prim.graph (handshaking lemma)" in :
    // For any undirected graph: sum of degrees = 2 * number of edges
    withPrimGraph { graph =>
      graph.meanDegree shouldBe (2.0 * graph.size / graph.order) +- 0.001
    }

  it should "return 1.0 for a single-edge graph" in :
    val triplets: Seq[Triplet[Int, Double, EdgeType]] = Seq(
      Triplet(0, 1, Some(1.0), Undirected)
    )
    triplesToTryGraph[Int, Double](Vertex.createWithSet)(triplets) match
      case Success(graph: UndirectedGraph[Int, Double] @unchecked) =>
        graph.meanDegree shouldBe 1.0 +- 0.001
      case other => fail(s"unexpected: $other")

  // -------------------------------------------------------------------------
  // numberOfSelfLoops
  // -------------------------------------------------------------------------

  behavior of "EdgeGraph.numberOfSelfLoops"

  it should "return 0 for prim.graph (no self-loops)" in :
    withPrimGraph { graph =>
      graph.numberOfSelfLoops shouldBe 0
    }

  it should "return 0 for a simple two-edge graph" in :
    val triplets: Seq[Triplet[Int, Double, EdgeType]] = Seq(
      Triplet(0, 1, Some(1.0), Undirected),
      Triplet(1, 2, Some(2.0), Undirected)
    )
    triplesToTryGraph[Int, Double](Vertex.createWithSet)(triplets) match
      case Success(graph: UndirectedGraph[Int, Double] @unchecked) =>
        graph.numberOfSelfLoops shouldBe 0
      case other => fail(s"unexpected: $other")

  it should "return 1 for a graph with one self-loop" in :
    val triplets: Seq[Triplet[Int, Double, EdgeType]] = Seq(
      Triplet(0, 1, Some(1.0), Undirected),
      Triplet(0, 0, Some(0.5), Undirected)
    )
    triplesToTryGraph[Int, Double](Vertex.createWithSet)(triplets) match
      case Success(graph: UndirectedGraph[Int, Double] @unchecked) =>
        graph.numberOfSelfLoops shouldBe 1
      case other => fail(s"unexpected: $other")

  it should "count multiple self-loops correctly" in :
    val triplets: Seq[Triplet[Int, Double, EdgeType]] = Seq(
      Triplet(0, 0, Some(1.0), Undirected),
      Triplet(1, 1, Some(2.0), Undirected),
      Triplet(0, 1, Some(3.0), Undirected)
    )
    triplesToTryGraph[Int, Double](Vertex.createWithSet)(triplets) match
      case Success(graph: UndirectedGraph[Int, Double] @unchecked) =>
        graph.numberOfSelfLoops shouldBe 2
      case other => fail(s"unexpected: $other")
  behavior of "inDegree and outDegree"

  // 0→1, 0→2, 1→2, 3→0. Built directly, since the parser's triples are undirected here.
  private def digraph: DirectedGraph[Int, Double] =
    Seq((0, 1), (0, 2), (1, 2), (3, 0)).foldLeft(DirectedGraph[Int, Double]) { case (g, (f, t)) =>
      g.addEdge(AttributedDirectedEdge(1.0, f, t)).asInstanceOf[DirectedGraph[Int, Double]]
    }

  it should "count arcs leaving a vertex" in :
    digraph.outDegree(0) shouldBe 2
    digraph.outDegree(1) shouldBe 1
    digraph.outDegree(2) shouldBe 0 // a sink
    digraph.outDegree(3) shouldBe 1

  it should "count arcs arriving at a vertex" in :
    digraph.inDegree(0) shouldBe 1
    digraph.inDegree(1) shouldBe 1
    digraph.inDegree(2) shouldBe 2
    digraph.inDegree(3) shouldBe 0 // a source

  // Every arc leaves one vertex and arrives at one, so each sum is the edge count.
  // This catches a white/black mix-up that per-vertex assertions could miss.
  it should "sum to the edge count, both ways" in :
    val g = digraph
    val vs = g.vertexMap.keySet
    vs.toSeq.map(g.inDegree).sum shouldBe g.size
    vs.toSeq.map(g.outDegree).sum shouldBe g.size

  it should "swap when the graph is reversed" in :
    val g = digraph
    val r = g.reverse
    for v <- g.vertexMap.keySet do
      r.inDegree(v) shouldBe g.outDegree(v)
      r.outDegree(v) shouldBe g.inDegree(v)

  it should "equal the degree for an undirected graph" in :
    withPrimGraph { graph =>
      for v <- graph.vertexMap.keySet do
        graph.inDegree(v) shouldBe graph.degree(v)
        graph.outDegree(v) shouldBe graph.degree(v)
    }

  behavior of "an undirected edge with no attribute"

  // The builder used to make these AttributedDirectedEdges, which UndirectedGraph.edges then
  // discarded: the adjacency was there, so degree counted it, but edges and M did not. That is
  // fixed, and this is the graph where such an edge belongs — one whose edge type is Unit.
  it should "appear in edges and M, consistently with degree" in :
    val triplets: Seq[Triplet[Int, Unit, EdgeType]] = Seq(
      Triplet(0, 1, None, Undirected)
    )
    triplesToTryGraph[Int, Unit](Vertex.createWithSet)(triplets) match
      case Success(graph: UndirectedGraph[Int, Unit] @unchecked) =>
        graph.edges.size shouldBe 1
        graph.size shouldBe 1
        graph.degree(0) shouldBe 1
        graph.degree(1) shouldBe 1
      case other => fail(s"unexpected: $other")

  // The attribute is the Unit value, not `None` masquerading as one. Up to 1.11.0 this read
  // back as None typed as E, which threw ClassCastException wherever E was not Unit (#56).
  it should "carry the Unit value as its attribute" in :
    val triplets: Seq[Triplet[Int, Unit, EdgeType]] = Seq(
      Triplet(0, 1, None, Undirected)
    )
    triplesToTryGraph[Int, Unit](Vertex.createWithSet)(triplets) match
      case Success(graph: UndirectedGraph[Int, Unit] @unchecked) =>
        graph.edges.next().attribute shouldBe ()
      case other => fail(s"unexpected: $other")

  // This used to be "mix with attributed edges without being lost", and it passed: a Double
  // graph would accept the attributeless triplet and give it an attribute of the wrong type.
  // Mixing is now refused at construction, which is the point of #56 — the alternative was an
  // edge whose `attribute` threw when anybody read it, far from the line that caused it.
  it should "be refused in a graph whose edge type is not Unit" in :
    val triplets: Seq[Triplet[Int, Double, EdgeType]] = Seq(
      Triplet(0, 1, Some(1.5), Undirected),
      Triplet(1, 2, None, Undirected)
    )
    triplesToTryGraph[Int, Double](Vertex.createWithSet)(triplets) match
      case Failure(x: GraphException) =>
        x.getMessage should include("1 triplet(s) have no attribute")
        x.getMessage should include("Triplet(1,2,None,Undirected)")
      case other => fail(s"expected a Failure naming the offending triplet, got: $other")

  it should "be refused even when every triplet lacks an attribute" in :
    val triplets: Seq[Triplet[Int, Double, EdgeType]] = Seq(
      Triplet(0, 1, None, Undirected)
    )
    triplesToTryGraph[Int, Double](Vertex.createWithSet)(triplets) should matchPattern {
      case Failure(_: GraphException) =>
    }
