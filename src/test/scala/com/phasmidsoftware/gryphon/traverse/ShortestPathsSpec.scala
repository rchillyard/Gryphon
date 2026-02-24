package com.phasmidsoftware.gryphon.traverse

import com.phasmidsoftware.gryphon.adjunct.DirectedGraph.triplesToTryGraph
import com.phasmidsoftware.gryphon.adjunct.{AttributedDirectedEdge, DirectedEdge, DirectedGraph}
import com.phasmidsoftware.gryphon.core.{EdgeType, Triplet, Vertex}
import com.phasmidsoftware.gryphon.parse.GraphParser
import com.phasmidsoftware.gryphon.util.TryUsing
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.io.Source
import scala.util.{Failure, Success, Try}

class ShortestPathsSpec extends AnyFlatSpec with Matchers {

  behavior of "ShortestPaths"

  ignore should "dijkstra" in {
    val p: GraphParser[Int, Double, EdgeType] = new GraphParser[Int, Double, EdgeType]
    val triedSource = Try(Source.fromResource("dijkstra.graph"))
    val zsy: Try[Seq[Triplet[Int, Double, EdgeType]]] = TryUsing.tryIt(triedSource) {
      (source: Source) => p.parseSource[Triplet[Int, Double, EdgeType]](p.parseTriple)(source)
    }
    zsy match {
      case Success(triplets) =>
        implicit val zzz: Numeric[Double] = scala.math.Numeric.DoubleIsFractional
        triplesToTryGraph[Int, Double](Vertex.createRelaxableWithSet)(triplets) match {
          case Success(graph: DirectedGraph[_, _]) =>
            graph.vertexMap.map.size shouldBe 8
            graph.edges.size shouldBe 16
            val sp = ShortestPaths.dijkstra(graph, 0)
//            println(sp)
            sp.vertexTraverse(0) shouldBe None
            sp.vertexTraverse(1) shouldBe Some(AttributedDirectedEdge(5, 0, 1))
            sp.vertexTraverse(2) shouldBe Some(AttributedDirectedEdge(1, 5, 2))
            sp.vertexTraverse(3) shouldBe Some(AttributedDirectedEdge(3, 2, 3))
            sp.vertexTraverse(4) shouldBe Some(AttributedDirectedEdge(9, 0, 4))
          // TODO fix these problems!
                      sp.vertexTraverse(5) shouldBe Some(AttributedDirectedEdge(4,4,5))
                      sp.vertexTraverse(6) shouldBe Some(AttributedDirectedEdge(11,2,6))
                      sp.vertexTraverse(7) shouldBe Some(AttributedDirectedEdge(8,0,7))
          case Failure(x) =>
            fail("parse failed: ", x)
          case _ => fail("parse failed: Graph is not an EdgeGraph")
        }

      case Failure(x) =>
        fail("parse failed", x)
    }

  }

  it should "undiscoveredEdges" in {

  }

}
