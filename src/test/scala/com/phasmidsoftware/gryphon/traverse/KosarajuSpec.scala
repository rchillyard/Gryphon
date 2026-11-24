/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.traverse

import com.phasmidsoftware.gryphon.adjunct.DirectedGraph
import com.phasmidsoftware.gryphon.adjunct.DirectedGraph.triplesToTryGraph
import com.phasmidsoftware.gryphon.core.*
import com.phasmidsoftware.gryphon.parse.GraphParser
import com.phasmidsoftware.gryphon.traverse.Kosaraju.stronglyConnectedComponents
import com.phasmidsoftware.gryphon.util.TryUsing
import com.phasmidsoftware.visitor.core.Tracer
import java.io.{ByteArrayOutputStream, PrintStream}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import scala.io.Source
import scala.util.{Random, Success, Try}

/**
 * Tests for Kosaraju's strongly-connected-components algorithm.
 *
 * directed.graph has 7 vertices (0–6) and 12 directed edges:
 * 0->5, 0->2, 0->1, 3->6, 3->5, 3->4, 5->2, 6->4, 6->0, 3->2, 1->4, 2->6
 *
 * SCCs:
 * {0, 2, 5, 6} — cycle 0->5->2->6->0
 * {1}          — singleton (reached from 0, leads only to 4)
 * {3}          — singleton (source, reaches everything)
 * {4}          — singleton (sink, reached from 3, 6, 1)
 */
class KosarajuSpec extends AnyFlatSpec with should.Matchers:

  given Random = Random(42)

  // -------------------------------------------------------------------------
  // Shared fixture
  // -------------------------------------------------------------------------

  private def loadDirectedGraph(filename: String): DirectedGraph[Int, Unit] =
    val p = new GraphParser[Int, Unit, EdgeType]
    val triedSource = Try(Source.fromResource(filename))
    val triplets = TryUsing.tryIt(triedSource) { source =>
      p.parseSource[Triplet[Int, Unit, EdgeType]](p.parseTriple)(source)
    }.get
    triplesToTryGraph[Int, Unit](Vertex.createWithSet)(triplets).get match
      case g: DirectedGraph[Int, Unit] @unchecked => g

  // -------------------------------------------------------------------------
  // Structure of the result
  // -------------------------------------------------------------------------

  behavior of "Kosaraju — result structure"

  it should "label all 7 vertices" in {
    val result = stronglyConnectedComponents[Int, Unit](loadDirectedGraph("directed.graph"))
    result.size shouldBe 7
  }

  it should "find exactly 4 SCCs" in {
    val result = stronglyConnectedComponents[Int, Unit](loadDirectedGraph("directed.graph"))
    result.values.toSet.size shouldBe 4
  }

  // Sedgewick & Wayne, Algorithms 4th ed., figure for Algorithm 4.6 (p587).
  //
  // Asserted as a partition — a set of sets — rather than as a Map from component id to members.
  // The ids are assigned in the order pass 2 discovers components, which is an implementation
  // detail: a correct reordering of either pass would renumber them and break the test while
  // nothing was actually wrong. The partition is the thing the textbook states, and the thing
  // the algorithm is obliged to produce.
  //
  // (Empirically the ids are stable even though neighbour iteration is shuffled — 100 runs over
  // several graphs, including ones where components could tie, give one labelling. But nothing
  // documents that, so it is not worth depending on.)
  it should "find the five strong components of directedSCC.graph" in {
    val result: SCCResult[Int] = stronglyConnectedComponents[Int, Unit](loadDirectedGraph("directedSCC.graph"))
    result.values.toSet.size shouldBe 5
    result.groupBy(_._2).values.map(_.keySet).toSet shouldBe
            Set(Set(1), Set(0, 2, 3, 4, 5), Set(9, 10, 11, 12), Set(6, 8), Set(7))
  }

  // -------------------------------------------------------------------------
  // Non-trivial SCC
  // -------------------------------------------------------------------------

  behavior of "Kosaraju — non-trivial SCC {0, 2, 5, 6}"

  it should "assign vertices 0, 2, 5, and 6 the same SCC id" in {
    val result = stronglyConnectedComponents[Int, Unit](loadDirectedGraph("directed.graph"))
    result(0) shouldBe result(2)
    result(2) shouldBe result(5)
    result(5) shouldBe result(6)
  }

  it should "assign the SCC {0,2,5,6} a different id from every singleton" in {
    val result = stronglyConnectedComponents[Int, Unit](loadDirectedGraph("directed.graph"))
    val scc0256 = result(0)
    result(1) should not be scc0256
    result(3) should not be scc0256
    result(4) should not be scc0256
  }

  // -------------------------------------------------------------------------
  // Singleton SCCs
  // -------------------------------------------------------------------------

  behavior of "Kosaraju — singleton SCCs"

  it should "assign vertices 1, 3, 4 each their own unique SCC id" in {
    val result = stronglyConnectedComponents[Int, Unit](loadDirectedGraph("directed.graph"))
    val singletons = Seq(result(1), result(3), result(4))
    singletons.distinct.size shouldBe 3
  }

  it should "assign vertex 1 a different SCC id from vertex 4 (1->4 is a cross edge)" in {
    val result = stronglyConnectedComponents[Int, Unit](loadDirectedGraph("directed.graph"))
    result(1) should not be result(4)
  }

  it should "assign vertex 3 a different SCC id from vertex 6 (3->6 is a cross edge)" in {
    val result = stronglyConnectedComponents[Int, Unit](loadDirectedGraph("directed.graph"))
    result(3) should not be result(6)
  }

  // -------------------------------------------------------------------------
  // Inline graph tests
  // -------------------------------------------------------------------------

  behavior of "Kosaraju — inline graphs"

  it should "find one SCC in a single-vertex graph" in {
    val triplets: Seq[Triplet[Int, Unit, EdgeType]] = Seq(
      Triplet(0, 0, None, Directed)   // self-loop
    )
    triplesToTryGraph[Int, Unit](Vertex.createWithSet)(triplets) match
      case Success(g: DirectedGraph[Int, Unit] @unchecked) =>
        val result = stronglyConnectedComponents[Int, Unit](g)
        result.values.toSet.size shouldBe 1
      case other => fail(s"unexpected: $other")
  }

  it should "find one SCC in a two-vertex mutual cycle" in {
    val triplets: Seq[Triplet[Int, Unit, EdgeType]] = Seq(
      Triplet(0, 1, None, Directed),
      Triplet(1, 0, None, Directed)
    )
    triplesToTryGraph[Int, Unit](Vertex.createWithSet)(triplets) match
      case Success(g: DirectedGraph[Int, Unit] @unchecked) =>
        val result = stronglyConnectedComponents[Int, Unit](g)
        result.values.toSet.size shouldBe 1
        result(0) shouldBe result(1)
      case other =>
        fail(s"unexpected: $other")
  }

  it should "find two SCCs in a two-vertex one-way edge" in {
    val triplets: Seq[Triplet[Int, Unit, EdgeType]] = Seq(
      Triplet(0, 1, None, Directed)
    )
    triplesToTryGraph[Int, Unit](Vertex.createWithSet)(triplets) match
      case Success(g: DirectedGraph[Int, Unit] @unchecked) =>
        val result = stronglyConnectedComponents[Int, Unit](g)
        result.values.toSet.size shouldBe 2
        result(0) should not be result(1)
      case other =>
        fail(s"unexpected: $other")
  }

  it should "find three SCCs in a three-vertex chain 0->1->2" in {
    val triplets: Seq[Triplet[Int, Unit, EdgeType]] = Seq(
      Triplet(0, 1, None, Directed),
      Triplet(1, 2, None, Directed)
    )
    triplesToTryGraph[Int, Unit](Vertex.createWithSet)(triplets) match
      case Success(g: DirectedGraph[Int, Unit] @unchecked) =>
        val result = stronglyConnectedComponents[Int, Unit](g)
        result.values.toSet.size shouldBe 3
        result(0) should not be result(1)
        result(1) should not be result(2)
      case other =>
        fail(s"unexpected: $other")
  }

  it should "find one SCC in a three-vertex cycle 0->1->2->0" in {
    val triplets: Seq[Triplet[Int, Unit, EdgeType]] = Seq(
      Triplet(0, 1, None, Directed),
      Triplet(1, 2, None, Directed),
      Triplet(2, 0, None, Directed)
    )
    triplesToTryGraph[Int, Unit](Vertex.createWithSet)(triplets) match
      case Success(g: DirectedGraph[Int, Unit] @unchecked) =>
        val result = stronglyConnectedComponents[Int, Unit](g)
        result.values.toSet.size shouldBe 1
        result(0) shouldBe result(1)
        result(1) shouldBe result(2)
      case other =>
        fail(s"unexpected: $other")
  }

  // -------------------------------------------------------------------------
  // Tracing
  // -------------------------------------------------------------------------

  private def captureOutput(block: PrintStream => Unit): List[String] =
    val baos = ByteArrayOutputStream()
    val ps = PrintStream(baos)
    block(ps)
    ps.flush()
    baos.toString.linesIterator.filter(_.nonEmpty).toList

  behavior of "Kosaraju — tracing"

  it should "emit the expected summary trace for a two-vertex mutual cycle" in {
    val triplets: Seq[Triplet[Int, Unit, EdgeType]] = Seq(
      Triplet(0, 1, None, Directed),
      Triplet(1, 0, None, Directed)
    )
    triplesToTryGraph[Int, Unit](Vertex.createWithSet)(triplets) match
      case Success(g: DirectedGraph[Int, Unit] @unchecked) =>
        val lines = captureOutput: ps =>
          given Tracer[Int] = Tracer.summary(out = ps)

          stronglyConnectedComponents[Int, Unit](g): Unit
        lines shouldBe List(
          "Kosaraju pass 1: post-order DFS on reversed graph",
          "starters: 2 elements",
          "Kosaraju pass 2: DFS on original graph, 2 starter vertices"
        )
      case other =>
        fail(s"unexpected: $other")
  }

  it should "emit the expected verbose trace for a two-vertex mutual cycle" in {
    val triplets: Seq[Triplet[Int, Unit, EdgeType]] = Seq(
      Triplet(0, 1, None, Directed),
      Triplet(1, 0, None, Directed)
    )
    triplesToTryGraph[Int, Unit](Vertex.createWithSet)(triplets) match
      case Success(g: DirectedGraph[Int, Unit] @unchecked) =>
        val lines = captureOutput: ps =>
          given Tracer[Int] = Tracer.verbose(maxDepth = 1, out = ps)

          stronglyConnectedComponents[Int, Unit](g): Unit

        println(lines)
        lines should contain("Kosaraju pass 1: post-order DFS on reversed graph")
        lines should contain("starters: 0, 1")
        lines.count(_.contains("pass 1: seeding from")) shouldBe 1
        lines.count(_.contains("pass 2: SCC")) shouldBe 1
        lines.count(_.contains("pass 2: complete")) shouldBe 1
      case other =>
        fail(s"unexpected: $other")
  }
  // -------------------------------------------------------------------------
  // Invariants
  //
  // These assert properties that any correct SCC decomposition must have,
  // rather than transcribed expectations for one fixture. A transcribed
  // expectation only catches a change; an invariant catches a wrong answer.
  // -------------------------------------------------------------------------

  /** The 12 edges of directed.graph, as documented in this file's header. */
  private val directedGraphEdges: Seq[(Int, Int)] =
    Seq(0 -> 5, 0 -> 2, 0 -> 1, 3 -> 6, 3 -> 5, 3 -> 4,
      5 -> 2, 6 -> 4, 6 -> 0, 3 -> 2, 1 -> 4, 2 -> 6)

  /** Builds a directed graph from an edge list, for inline fixtures. */
  private def withGraph[A](edges: (Int, Int)*)(f: DirectedGraph[Int, Unit] => A): A =
    val triplets: Seq[Triplet[Int, Unit, EdgeType]] =
      edges.map { case (a, b) => Triplet(a, b, None, Directed) }
    triplesToTryGraph[Int, Unit](Vertex.createWithSet)(triplets) match
      case Success(g: DirectedGraph[Int, Unit] @unchecked) => f(g)
      case other => fail(s"graph construction failed: $other")

  /**
   * Is the condensation of `edges` under `scc` acyclic?
   *
   * The condensation contracts each strongly connected component to a single vertex, with an
   * edge A -> B exactly where some edge of the original runs from a vertex of A to a vertex of
   * B, with A != B. It is acyclic for *any* correct SCC decomposition: were two distinct
   * components mutually reachable they would be one component.
   *
   * It catches *splitting* a component: the halves remain mutually reachable, so the
   * condensation gains a cycle between them. It does NOT catch *merging* — a merge makes the
   * cross-component edges internal, so they vanish from the condensation, and collapsing every
   * vertex into one component yields a single vertex with no edges, trivially acyclic.
   * Verified by mutating a known-correct map and watching which corruptions survive.
   * `componentsAreMutuallyReachable` is the other half.
   *
   * Deliberately implemented here, by Kahn's algorithm, rather than by calling Gryphon's own
   * `isCyclic`: checking one traversal with another from the same engine would let a shared
   * fault hide exactly the error this is meant to expose.
   */
  private def condensationIsAcyclic(edges: Seq[(Int, Int)], scc: SCCResult[Int]): Boolean =
    val condensed: Set[(Int, Int)] =
      edges.collect { case (u, v) if scc(u) != scc(v) => scc(u) -> scc(v) }.toSet

    @annotation.tailrec
    def peel(remaining: Set[Int], es: Set[(Int, Int)]): Boolean =
      if remaining.isEmpty then true
      else
        val sources = remaining.diff(es.map(_._2))
        if sources.isEmpty then false // every surviving vertex has an incoming edge: a cycle
        else peel(remaining -- sources, es.filterNot((u, _) => sources.contains(u)))

    peel(scc.values.toSet, condensed)

  /**
   * Does every component contain exactly the vertices mutually reachable with its members?
   *
   * For a representative r, the set reachable from r intersected with the set that can reach r
   * is, by definition, r's strongly connected component. Comparing that against what the
   * algorithm reported is therefore a complete, independent check: it rejects merging,
   * splitting and wholesale collapse alike. Verified by mutating a known-correct map.
   *
   * It is strictly stronger than `condensationIsAcyclic`, which is kept because it is the
   * classic statement of the property and tests a different formulation of it — but on its own
   * that one only catches splitting. Computed from the edge list alone, independently of
   * Gryphon's own traversals, so that a fault in the engine cannot hide the error this is
   * meant to expose.
   */
  private def componentsAreMutuallyReachable(edges: Seq[(Int, Int)], scc: SCCResult[Int]): Boolean =
    def closure(adj: Map[Int, Set[Int]], start: Int): Set[Int] =
      @annotation.tailrec
      def loop(frontier: Set[Int], seen: Set[Int]): Set[Int] =
        val next = frontier.flatMap(v => adj.getOrElse(v, Set.empty)).diff(seen)
        if next.isEmpty then seen else loop(next, seen ++ next)

      loop(Set(start), Set(start))

    val forward = edges.groupBy(_._1).view.mapValues(_.map(_._2).toSet).toMap
    val backward = edges.groupBy(_._2).view.mapValues(_.map(_._1).toSet).toMap

    scc.groupBy(_._2).forall { (_, members) =>
      val component = members.keySet
      val r = component.head
      closure(forward, r).intersect(closure(backward, r)) == component
    }

  behavior of "Kosaraju — the condensation"

  it should "be acyclic for directed.graph" in {
    val result = stronglyConnectedComponents[Int, Unit](loadDirectedGraph("directed.graph"))
    condensationIsAcyclic(directedGraphEdges, result) shouldBe true
  }

  it should "be acyclic for a graph with two separate cycles" in {
    val edges = Seq(0 -> 1, 1 -> 2, 2 -> 0, 3 -> 4, 4 -> 5, 5 -> 3, 2 -> 3)
    withGraph(edges*) { g =>
      condensationIsAcyclic(edges, stronglyConnectedComponents[Int, Unit](g)) shouldBe true
    }
  }

  behavior of "Kosaraju — the labelling partitions the vertices"

  it should "give every vertex of directed.graph exactly one component" in {
    val result = stronglyConnectedComponents[Int, Unit](loadDirectedGraph("directed.graph"))
    result.keySet shouldBe Set(0, 1, 2, 3, 4, 5, 6)
    result.groupBy(_._2).values.map(_.size).sum shouldBe 7
  }

  behavior of "Kosaraju — two non-trivial SCCs"

  // Every other fixture here has at most one SCC of size > 1, so nothing could catch an
  // implementation that merged two of them. This one can.
  it should "keep two disjoint cycles apart, joined one way" in {
    withGraph(0 -> 1, 1 -> 2, 2 -> 0, 3 -> 4, 4 -> 5, 5 -> 3, 2 -> 3) { g =>
      val result = stronglyConnectedComponents[Int, Unit](g)
      result.values.toSet.size shouldBe 2
      result(0) shouldBe result(1)
      result(1) shouldBe result(2)
      result(3) shouldBe result(4)
      result(4) shouldBe result(5)
      result(0) should not be result(3)
    }
  }

  it should "keep them apart when nothing joins them at all" in {
    withGraph(0 -> 1, 1 -> 2, 2 -> 0, 3 -> 4, 4 -> 5, 5 -> 3) { g =>
      val result = stronglyConnectedComponents[Int, Unit](g)
      result.values.toSet.size shouldBe 2
      result.groupBy(_._2).values.map(_.size).toSet shouldBe Set(3)
    }
  }

  behavior of "Kosaraju — a DAG"

  // The existing DAG case is the path 0->1->2. dag.graph branches, which is where a wrong
  // implementation would spuriously merge.
  it should "give every vertex of dag.graph its own component" in {
    // dag.graph has no attribute column, so its edge type is Unit, not Double (#56).
    val p = new GraphParser[Int, Unit, EdgeType]
    val triedSource = Try(Source.fromResource("dag.graph"))
    val triplets = TryUsing.tryIt(triedSource) { source =>
      p.parseSource[Triplet[Int, Unit, EdgeType]](p.parseTriple)(source)
    }.get
    triplesToTryGraph[Int, Unit](Vertex.createWithSet)(triplets).get match
      case g: DirectedGraph[Int, Unit] @unchecked =>
        val result = stronglyConnectedComponents[Int, Unit](g)
        result.size shouldBe 7
        result.values.toSet.size shouldBe 7 // all singletons
  }

  it should "give every vertex of a diamond its own component" in {
    withGraph(0 -> 1, 0 -> 2, 1 -> 3, 2 -> 3) { g =>
      stronglyConnectedComponents[Int, Unit](g).values.toSet.size shouldBe 4
    }
  }

  behavior of "Kosaraju — a digraph that is really undirected"

  // Where every edge has its reverse, each weakly connected component is one SCC. The existing
  // case is the two-vertex mutual cycle, which is too small to distinguish this from weaker
  // behaviours.
  it should "collapse a bidirectional path to a single component" in {
    withGraph(0 -> 1, 1 -> 0, 1 -> 2, 2 -> 1, 2 -> 3, 3 -> 2) { g =>
      val result = stronglyConnectedComponents[Int, Unit](g)
      result.values.toSet.size shouldBe 1
      result.size shouldBe 4
    }
  }

  it should "give one component per bidirectional island" in {
    withGraph(0 -> 1, 1 -> 0, 2 -> 3, 3 -> 2) { g =>
      val result = stronglyConnectedComponents[Int, Unit](g)
      result.values.toSet.size shouldBe 2
      result(0) shouldBe result(1)
      result(2) shouldBe result(3)
      result(0) should not be result(2)
    }
  }

  behavior of "Kosaraju — components are mutually reachable"

  it should "hold for directed.graph" in {
    val result = stronglyConnectedComponents[Int, Unit](loadDirectedGraph("directed.graph"))
    componentsAreMutuallyReachable(directedGraphEdges, result) shouldBe true
  }

  it should "hold for two cycles joined one way" in {
    val edges = Seq(0 -> 1, 1 -> 2, 2 -> 0, 3 -> 4, 4 -> 5, 5 -> 3, 2 -> 3)
    withGraph(edges*) { g =>
      componentsAreMutuallyReachable(edges, stronglyConnectedComponents[Int, Unit](g)) shouldBe true
    }
  }

  it should "hold for a bidirectional path" in {
    val edges = Seq(0 -> 1, 1 -> 0, 1 -> 2, 2 -> 1, 2 -> 3, 3 -> 2)
    withGraph(edges*) { g =>
      componentsAreMutuallyReachable(edges, stronglyConnectedComponents[Int, Unit](g)) shouldBe true
    }
  }
