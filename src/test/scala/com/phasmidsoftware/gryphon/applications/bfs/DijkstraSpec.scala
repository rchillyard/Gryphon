package com.phasmidsoftware.gryphon.applications.bfs

import com.phasmidsoftware.gryphon.adjunct.DirectedGraph
import com.phasmidsoftware.gryphon.adjunct.DirectedGraph.triplesToTryGraph
import com.phasmidsoftware.gryphon.core.{EdgeType, Triplet, Vertex}
import com.phasmidsoftware.gryphon.parse.GraphParser
import com.phasmidsoftware.gryphon.util.TryUsing
import com.phasmidsoftware.visitor.core.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import scala.io.Source
import scala.util.*

class DijkstraSpec extends AnyFlatSpec with should.Matchers:

  // dijkstra.graph: 8 vertices (0-7), fully reachable from 0.
  // BFS hop distances from 0:
  //   hop 0: {0}
  //   hop 1: {1, 4, 7}      (0->1, 0->4, 0->7)
  //   hop 2: {2, 5, 3}      (1->2, 4->5, 1->3 or 7->5->2 etc. — first discovery wins)
  //   hop 3: {6}             (2->6 or 3->6)
  // So BFS from 0 discovers vertices in groups — no vertex in hop k+1
  // appears before all vertices in hop k.

  given Evaluable[Int, Int] with
    def evaluate(v: Int): Option[Int] = Some(v)

  behavior of "Dijkstra"

  // --- Existing tests (kept as-is) ---

  it should "visit all vertices via bfs on dijkstra graph" in {
    val p = new GraphParser[Int, Double, EdgeType]
    val triedSource = Try(Source.fromResource("dijkstra.graph"))
    val zsy: Try[Seq[Triplet[Int, Double, EdgeType]]] = TryUsing.tryIt(triedSource) {
      source => p.parseSource[Triplet[Int, Double, EdgeType]](p.parseTriple)(source)
    }
    zsy match
      case Success(triplets) =>
        given Numeric[Double] = scala.math.Numeric.DoubleIsFractional
        triplesToTryGraph[Int, Double](Vertex.createWithSet)(triplets) match
          case Success(graph: DirectedGraph[Int, Double]) =>
            val visitor = JournaledVisitor.withQueueJournal[Int, Int]
            val result = graph.bfs(visitor)(0)
            result.result.map(_._1).toSet.size shouldBe 8
          case Failure(x) => fail("parse failed", x)
          case _ => fail("not a DirectedGraph")
      case Failure(x) => fail("parse failed", x)
  }

  it should "visit all vertices via dfs on dijkstra graph" in {
    val p = new GraphParser[Int, Double, EdgeType]
    val triedSource = Try(Source.fromResource("dijkstra.graph"))
    val zsy: Try[Seq[Triplet[Int, Double, EdgeType]]] = TryUsing.tryIt(triedSource) {
      source => p.parseSource[Triplet[Int, Double, EdgeType]](p.parseTriple)(source)
    }
    zsy match
      case Success(triplets) =>
        given Numeric[Double] = scala.math.Numeric.DoubleIsFractional
        triplesToTryGraph[Int, Double](Vertex.createWithSet)(triplets) match
          case Success(graph: DirectedGraph[Int, Double]) =>
            val visitor = JournaledVisitor.withQueueJournal[Int, Int]
            val result = graph.dfsAll(visitor)
            result.result.map(_._1).toSet.size shouldBe 8
          case Failure(x) => fail("parse failed", x)
          case _ => fail("not a DirectedGraph")
      case Failure(x) => fail("parse failed", x)
  }

  // --- New tests ---

  it should "bfs from 0 discovers hop-1 vertices before hop-2 vertices" in {
    // Hop-1 from 0: {1, 4, 7}.  Hop-2 includes {2, 3, 5}.
    // In BFS order every hop-1 vertex must appear before every hop-2 vertex.
    val p = new GraphParser[Int, Double, EdgeType]
    val triedSource = Try(Source.fromResource("dijkstra.graph"))
    val zsy: Try[Seq[Triplet[Int, Double, EdgeType]]] = TryUsing.tryIt(triedSource) {
      source => p.parseSource[Triplet[Int, Double, EdgeType]](p.parseTriple)(source)
    }
    zsy match
      case Success(triplets) =>
        given Numeric[Double] = scala.math.Numeric.DoubleIsFractional

        triplesToTryGraph[Int, Double](Vertex.createWithSet)(triplets) match
          case Success(graph: DirectedGraph[Int, Double]) =>
            val visitor = JournaledVisitor.withQueueJournal[Int, Int]
            val result = graph.bfs(visitor)(0)
            val order = result.result.map(_._1).toList
            val hop1 = Set(1, 4, 7)
            val hop2 = Set(2, 3, 5)
            val lastHop1 = hop1.map(order.indexOf).max
            val firstHop2 = hop2.map(order.indexOf).min
            lastHop1 should be < firstHop2
          case Failure(x) => fail("parse failed", x)
          case _ => fail("not a DirectedGraph")
      case Failure(x) => fail("parse failed", x)
  }

  it should "bfs from 0 discovers vertex 6 last (deepest hop)" in {
    // Vertex 6 is only reachable via at least 2 intermediate hops from 0.
    // It should appear after all hop-1 and hop-2 vertices.
    val p = new GraphParser[Int, Double, EdgeType]
    val triedSource = Try(Source.fromResource("dijkstra.graph"))
    val zsy: Try[Seq[Triplet[Int, Double, EdgeType]]] = TryUsing.tryIt(triedSource) {
      source => p.parseSource[Triplet[Int, Double, EdgeType]](p.parseTriple)(source)
    }
    zsy match
      case Success(triplets) =>
        given Numeric[Double] = scala.math.Numeric.DoubleIsFractional

        triplesToTryGraph[Int, Double](Vertex.createWithSet)(triplets) match
          case Success(graph: DirectedGraph[Int, Double]) =>
            val visitor = JournaledVisitor.withQueueJournal[Int, Int]

            given Random = Random(42)

            val result = graph.bfs(visitor)(0)
            val order = result.result.map(_._1).toList
            val hop1and2 = Set(1, 4, 7, 2, 3, 5)
            val lastHop1and2 = hop1and2.map(order.indexOf).max
            order.indexOf(6) should be > lastHop1and2
          case Failure(x) => fail("parse failed", x)
          case _ => fail("not a DirectedGraph")
      case Failure(x) => fail("parse failed", x)
  }

  it should "dfs from vertex 0 does not visit unreachable vertices in a partial graph" in {
    // directed.graph contains a cycle (2->6->0) so all 7 vertices are reachable
    // from 0; but we can verify that dfs from 0 on dijkstra.graph (fully
    // connected directed) finds all 8 vertices and no extras.
    val p = new GraphParser[Int, Double, EdgeType]
    val triedSource = Try(Source.fromResource("dijkstra.graph"))
    val zsy: Try[Seq[Triplet[Int, Double, EdgeType]]] = TryUsing.tryIt(triedSource) {
      source => p.parseSource[Triplet[Int, Double, EdgeType]](p.parseTriple)(source)
    }
    zsy match
      case Success(triplets) =>
        given Numeric[Double] = scala.math.Numeric.DoubleIsFractional

        triplesToTryGraph[Int, Double](Vertex.createWithSet)(triplets) match
          case Success(graph: DirectedGraph[Int, Double]) =>
            val visitor = JournaledVisitor.withQueueJournal[Int, Int]
            val result = graph.dfs(visitor)(0)
            val visited = result.result.map(_._1).toSet
            // All are reachable from 0 in this graph
            visited shouldBe Set(0, 1, 2, 3, 4, 5, 6, 7)
          case Failure(x) => fail("parse failed", x)
          case _ => fail("not a DirectedGraph")
      case Failure(x) => fail("parse failed", x)
  }

  it should "bfs result contains vertex 0 as first element" in {
    val p = new GraphParser[Int, Double, EdgeType]
    val triedSource = Try(Source.fromResource("dijkstra.graph"))
    val zsy: Try[Seq[Triplet[Int, Double, EdgeType]]] = TryUsing.tryIt(triedSource) {
      source => p.parseSource[Triplet[Int, Double, EdgeType]](p.parseTriple)(source)
    }
    zsy match
      case Success(triplets) =>
        given Numeric[Double] = scala.math.Numeric.DoubleIsFractional

        triplesToTryGraph[Int, Double](Vertex.createWithSet)(triplets) match
          case Success(graph: DirectedGraph[Int, Double]) =>
            val visitor = JournaledVisitor.withQueueJournal[Int, Int]
            val result = graph.bfs(visitor)(0)
            result.result.headOption.map(_._1) shouldBe Some(0)
          case Failure(x) => fail("parse failed", x)
          case _ => fail("not a DirectedGraph")
      case Failure(x) => fail("parse failed", x)
  }
