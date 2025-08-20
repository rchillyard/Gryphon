package com.phasmidsoftware.gryphon.traverse

import com.phasmidsoftware.gryphon.adjunct.DirectedGraph
import com.phasmidsoftware.gryphon.adjunct.DirectedGraph.triplesToTryGraph
import com.phasmidsoftware.gryphon.core.{EdgeType, Triplet}
import com.phasmidsoftware.gryphon.parse.GraphParser
import com.phasmidsoftware.gryphon.util.TryUsing
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.io.Source
import scala.util.{Failure, Success, Try}

class ShortestPathsSpec extends AnyFlatSpec with Matchers {

  behavior of "ShortestPaths"

  it should "dijkstra" in {
    val p: GraphParser[Int, Double, EdgeType] = new GraphParser[Int, Double, EdgeType]
    val triedSource = Try(Source.fromResource("dijkstra.graph"))
    val zsy: Try[Seq[Triplet[Int, Double, EdgeType]]] = TryUsing.tryIt(triedSource) {
      (source: Source) => p.parseSource[Triplet[Int, Double, EdgeType]](p.parseTriple)(source)
    }
    zsy match {
      case Success(triplets) =>
        triplesToTryGraph[Int, Double](triplets) match {
          case Success(graph: DirectedGraph[_, _]) =>
            graph.vertexMap.map.size shouldBe 8
            graph.edges.size shouldBe 16
            import scala.math.Numeric.DoubleIsFractional
            val sp = ShortestPaths.dijkstra(graph, 2)
            println(sp)
          // TODO we don't actually check this!
          //            graph match {
          //              case g: EdgeTraversable[Int, Double] =>
          //                val traversal = Traversal.edgeTraversal[Int, Double, String](edge => s"${edge.white} -> ${edge.black}")(g)
          //                println(traversal)
          //              case _ =>
          //              // Do nothing
          //            }
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
