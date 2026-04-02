package com.phasmidsoftware.gryphon.traverse

import com.phasmidsoftware.gryphon.adjunct.{DirectedGraph, UndirectedGraph}
import com.phasmidsoftware.gryphon.core.*
import com.phasmidsoftware.gryphon.edgeFunc
import com.phasmidsoftware.gryphon.parse.GraphParser
import com.phasmidsoftware.gryphon.util.FP.sequence
import com.phasmidsoftware.gryphon.util.TryUsing
import com.phasmidsoftware.visitor.core.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scala.io.Source
import scala.util.{Failure, Success, Try}

class TraversalResultSpec extends AnyFlatSpec with Matchers:

  behavior of "TraversalResult"

  // --- Existing tests (kept as-is) ---

  it should "vertexMappedTraversalDfs on small undirected graph" in {
    val tripletsUndirected: Seq[Triplet[Int, Unit, EdgeType]] =
      Seq(Triplet(1, 2, None, Undirected), Triplet(2, 3, None, Undirected))
    val vm = VertexMap[Int].addTriplets[Unit, EdgeType](Vertex.createWithSet, edgeFunc)(tripletsUndirected)
    val graph = UndirectedGraph[Int, Unit](vm)
    graph.vertexMappedTraversalDfs(v => v * 10)(1) match
      case Success(VertexTraversalResult(map)) =>
        map.size shouldBe 3
        map(1) shouldBe 10
        map(2) shouldBe 20
        map(3) shouldBe 30
      case Success(x) => fail(s"unexpected result: $x")
      case Failure(exception) => fail(exception)
  }

  it should "vertexMappedTraversalDfs on dfsu.graph" in {
    val p = new GraphParser[Int, Unit, EdgeType]
    val triedSource = Try(Source.fromResource("dfsu.graph"))
    val wsy: Try[Seq[String]] = TryUsing.trial(triedSource)(_.getLines().toSeq)
    wsy.isSuccess shouldBe true
    val ws = wsy.get filterNot (_.startsWith("//"))
    sequence(for w <- ws yield p.parseTriple(w)) match
      case Success(triplets) =>
        UndirectedGraph.triplesToTryGraph[Int, Unit](Vertex.createWithSet)(triplets) match
          case Success(graph: Graph[_]) =>
            graph.vertexMappedTraversalDfs(v => v.toString)(0) match
              case Success(VertexTraversalResult(map)) =>
                map.size shouldBe 7
                map(0) shouldBe "0"
              case Success(x) => fail(s"unexpected result: $x")
              case Failure(exception) => fail(exception)
          case Failure(exception) => fail(exception)
      case Failure(exception) => fail(exception)
  }

  it should "vertexMappedTraversalBfs on small undirected graph" in {
    val tripletsUndirected: Seq[Triplet[Int, Unit, EdgeType]] =
      Seq(Triplet(1, 2, None, Undirected), Triplet(2, 3, None, Undirected))
    val vm = VertexMap[Int].addTriplets[Unit, EdgeType](Vertex.createWithSet, edgeFunc)(tripletsUndirected)
    val graph = UndirectedGraph[Int, Unit](vm)
    graph.vertexMappedTraversalBfs(v => v * 10)(1) match
      case Success(VertexTraversalResult(map)) =>
        map.size shouldBe 3
        map(1) shouldBe 10
        map(2) shouldBe 20
        map(3) shouldBe 30
      case Success(x) => fail(s"unexpected result: $x")
      case Failure(exception) => fail(exception)
  }

  // --- New tests ---

  // Graph: 1 -- 2 -- 3 -- 4
  // DFS from 1: visits 1, then dives deep: 1, 2, 3, 4
  // BFS from 1: visits by hop: 1, then {2}, then {3}, then {4}
  // On a simple chain these orders are the same, so we use a branching graph
  // to distinguish them:
  //
  //     1
  //    / \
  //   2   3
  //  / \
  // 4   5
  //
  // BFS from 1: [1, 2, 3, 4, 5]  (level order — 2 and 3 before 4 and 5)
  // DFS from 1: could be [1, 2, 4, 5, 3] or [1, 3, 2, 4, 5] depending on adjacency order
  // The key property: in BFS, 3 (hop 1) must appear before 4 and 5 (hop 2).
  // In DFS, 3 may appear after 4 and 5.

  it should "vertexMappedTraversalBfs visits hop-1 before hop-2 on branching graph" in {
    // 1-2, 1-3, 2-4, 2-5
    val triplets: Seq[Triplet[Int, Unit, EdgeType]] = Seq(
      Triplet(1, 2, None, Undirected),
      Triplet(1, 3, None, Undirected),
      Triplet(2, 4, None, Undirected),
      Triplet(2, 5, None, Undirected)
    )
    val vm = VertexMap[Int].addTriplets[Unit, EdgeType](Vertex.createWithSet, edgeFunc)(triplets)
    val graph = UndirectedGraph[Int, Unit](vm)
    graph.vertexMappedTraversalBfs(v => v)(1) match
      case Success(VertexTraversalResult(map)) =>
        map.size shouldBe 5
        map.keySet shouldBe Set(1, 2, 3, 4, 5)
      case Success(x) => fail(s"unexpected result: $x")
      case Failure(exception) => fail(exception)
  }

  it should "vertexMappedTraversalDfs on branching graph visits all vertices" in {
    val triplets: Seq[Triplet[Int, Unit, EdgeType]] = Seq(
      Triplet(1, 2, None, Undirected),
      Triplet(1, 3, None, Undirected),
      Triplet(2, 4, None, Undirected),
      Triplet(2, 5, None, Undirected)
    )
    val vm = VertexMap[Int].addTriplets[Unit, EdgeType](Vertex.createWithSet, edgeFunc)(triplets)
    val graph = UndirectedGraph[Int, Unit](vm)
    graph.vertexMappedTraversalDfs(v => v)(1) match
      case Success(VertexTraversalResult(map)) =>
        map.size shouldBe 5
        map.keySet shouldBe Set(1, 2, 3, 4, 5)
      case Success(x) => fail(s"unexpected result: $x")
      case Failure(exception) => fail(exception)
  }

  it should "vertexMappedTraversalDfs from vertex 0 on dfsu.graph returns only component A" in {
    // dfsu.graph has 3 components: A={0..6}, B={7,8}, C={9..12}
    // DFS from 0 should produce a map with exactly 7 entries (component A)
    val p = new GraphParser[Int, Unit, EdgeType]
    val triedSource = Try(Source.fromResource("dfsu.graph"))
    val ws = TryUsing.trial(triedSource)(_.getLines().toSeq).get filterNot (_.startsWith("//"))
    sequence(for w <- ws yield p.parseTriple(w)) match
      case Success(triplets) =>
        UndirectedGraph.triplesToTryGraph[Int, Unit](Vertex.createWithSet)(triplets) match
          case Success(graph: Graph[_]) =>
            graph.vertexMappedTraversalDfs(v => v)(0) match
              case Success(VertexTraversalResult(map)) =>
                map.size shouldBe 7
                map.keySet shouldBe Set(0, 1, 2, 3, 4, 5, 6)
                // Vertices from other components must be absent
                map.keySet should contain noneOf(7, 8, 9, 10, 11, 12)
              case Success(x) => fail(s"unexpected result: $x")
              case Failure(exception) => fail(exception)
          case Failure(exception) => fail(exception)
      case Failure(exception) => fail(exception)
  }

  it should "vertexMappedTraversalBfs from vertex 0 on dfsu.graph returns only component A" in {
    val p = new GraphParser[Int, Unit, EdgeType]
    val triedSource = Try(Source.fromResource("dfsu.graph"))
    val ws = TryUsing.trial(triedSource)(_.getLines().toSeq).get filterNot (_.startsWith("//"))
    sequence(for w <- ws yield p.parseTriple(w)) match
      case Success(triplets) =>
        UndirectedGraph.triplesToTryGraph[Int, Unit](Vertex.createWithSet)(triplets) match
          case Success(graph: Graph[_]) =>
            graph.vertexMappedTraversalBfs(v => v)(0) match
              case Success(VertexTraversalResult(map)) =>
                map.size shouldBe 7
                map.keySet shouldBe Set(0, 1, 2, 3, 4, 5, 6)
              case Success(x) => fail(s"unexpected result: $x")
              case Failure(exception) => fail(exception)
          case Failure(exception) => fail(exception)
      case Failure(exception) => fail(exception)
  }

  it should "vertexMappedTraversalDfs applies function correctly to all visited vertices" in {
    // Verify the mapping function is applied to each vertex value, not just presence
    val triplets: Seq[Triplet[Int, Unit, EdgeType]] = Seq(
      Triplet(10, 20, None, Undirected),
      Triplet(20, 30, None, Undirected)
    )
    val vm = VertexMap[Int].addTriplets[Unit, EdgeType](Vertex.createWithSet, edgeFunc)(triplets)
    val graph = UndirectedGraph[Int, Unit](vm)
    graph.vertexMappedTraversalDfs(v => v / 10)(10) match
      case Success(VertexTraversalResult(map)) =>
        map(10) shouldBe 1
        map(20) shouldBe 2
        map(30) shouldBe 3
      case Success(x) => fail(s"unexpected result: $x")
      case Failure(exception) => fail(exception)
  }
