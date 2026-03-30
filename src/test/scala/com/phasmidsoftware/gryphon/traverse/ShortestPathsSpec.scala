package com.phasmidsoftware.gryphon.traverse

import com.phasmidsoftware.gryphon.adjunct.DirectedGraph.triplesToTryGraph
import com.phasmidsoftware.gryphon.adjunct.{AttributedDirectedEdge, DirectedGraph}
import com.phasmidsoftware.gryphon.core.{EdgeType, Triplet, Vertex}
import com.phasmidsoftware.gryphon.parse.GraphParser
import com.phasmidsoftware.gryphon.util.TryUsing
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scala.io.Source
import scala.util.{Failure, Success, Try}

class ShortestPathsSpec extends AnyFlatSpec with Matchers:

  behavior of "ShortestPaths"

  it should "Dijkstra" in {
    val p = new GraphParser[Int, Double, EdgeType]
    val triedSource = Try(Source.fromResource("dijkstra.graph"))
    val zsy: Try[Seq[Triplet[Int, Double, EdgeType]]] = TryUsing.tryIt(triedSource) {
      source => p.parseSource[Triplet[Int, Double, EdgeType]](p.parseTriple)(source)
    }
    zsy match
      case Success(triplets) =>
        given Numeric[Double] = scala.math.Numeric.DoubleIsFractional

        given Ordering[Double] = scala.math.Ordering.Double.TotalOrdering

        triplesToTryGraph[Int, Double](Vertex.createWithSet)(triplets) match
          case Success(graph: DirectedGraph[Int, Double] @unchecked) =>
            graph.vertexMap.map.size shouldBe 8
            graph.edges.size shouldBe 16
            val sp = ShortestPaths.dijkstra(graph, 0)
            sp.vertexTraverse(0) shouldBe None
            sp.vertexTraverse(1) shouldBe Some(AttributedDirectedEdge(5.0, 0, 1))
            sp.vertexTraverse(2) shouldBe Some(AttributedDirectedEdge(1.0, 5, 2))
            sp.vertexTraverse(3) shouldBe Some(AttributedDirectedEdge(3.0, 2, 3))
            sp.vertexTraverse(4) shouldBe Some(AttributedDirectedEdge(9.0, 0, 4))
            sp.vertexTraverse(5) shouldBe Some(AttributedDirectedEdge(4.0, 4, 5))
            sp.vertexTraverse(6) shouldBe Some(AttributedDirectedEdge(11.0, 2, 6))
            sp.vertexTraverse(7) shouldBe Some(AttributedDirectedEdge(8.0, 0, 7))
          case Failure(x) => fail("parse failed", x)
          case _ => fail("Graph is not a DirectedGraph[Int, Double]")
      case Failure(x) => fail("parse failed", x)
  }

  it should "undiscoveredEdges" in {
    val p = new GraphParser[Int, Double, EdgeType]
    val triedSource = Try(Source.fromResource("dijkstra.graph"))
    val zsy: Try[Seq[Triplet[Int, Double, EdgeType]]] = TryUsing.tryIt(triedSource) {
      source => p.parseSource[Triplet[Int, Double, EdgeType]](p.parseTriple)(source)
    }
    zsy match
      case Success(triplets) =>
        given Numeric[Double] = scala.math.Numeric.DoubleIsFractional

        given Ordering[Double] = scala.math.Ordering.Double.TotalOrdering

        triplesToTryGraph[Int, Double](Vertex.createWithSet)(triplets) match
          case Success(graph: DirectedGraph[Int, Double]) =>
            val neighbours = ShortestPaths.undiscoveredEdges(graph)(0)
            neighbours should not be empty
          case Failure(x) => fail("parse failed", x)
          case _ => fail("Graph is not a DirectedGraph[Int, Double]")
      case Failure(x) => fail("parse failed", x)
  }