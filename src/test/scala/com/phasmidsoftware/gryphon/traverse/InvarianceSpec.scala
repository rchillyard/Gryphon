package com.phasmidsoftware.gryphon.traverse

import com.phasmidsoftware.gryphon.adjunct.{DirectedGraph, UndirectedGraph}
import com.phasmidsoftware.gryphon.core.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scala.util.{Random, Success}

/**
 * Guards the invariance claims — issue #156.
 *
 * Every public method now carries an `@note Result:` line saying whether its answer can differ
 * between calls, and the README tabulates all of them. Those lines were produced by measurement
 * rather than by reading signatures, and this spec is what keeps them honest: it re-runs the
 * measurements, so a claim that stops being true fails the build instead of quietly becoming a
 * lie in published documentation.
 *
 * ==Why this exists at all==
 *
 * The subject has a history of confident wrong answers, every one from reasoning about code
 * instead of running it. Three in this round alone:
 *
 *   - "Relaxation-based algorithms cannot be affected by adjacency order." True of the distances,
 *     false of the predecessors, which is what is returned (#154).
 *   - "`AcyclicShortestPaths` needs a generator like its siblings." It does not — the only order a
 *     generator could reach there decides nothing, which forty seeds showed and an argument did
 *     not (#158).
 *   - "`Kosaraju` gives forty distinct results over forty seeds." A measurement artifact: the
 *     result is a bare `Map`, so `.map` eta-expanded to the collection method and forty distinct
 *     *function objects* were compared. The real answer is that its numbering varies and its
 *     grouping does not.
 *
 * ==Reading the fixtures==
 *
 * A fixture that cannot vary proves nothing, so each is chosen to be the hardest case for the
 * claim it tests: fully tied weights for the MST algorithms, where any spanning subset is a valid
 * answer; a DAG of singleton components for Kosaraju, where only the numbering can differ.
 * Where a method is claimed to vary, the test asserts that it '''does''' — which is what catches
 * a generator quietly ceasing to be threaded through.
 */
class InvarianceSpec extends AnyFlatSpec with Matchers:

  private val seeds = 0 until 40

  private def undirected(ts: Seq[Triplet[Int, Double, EdgeType]]): UndirectedGraph[Int, Double] =
    UndirectedGraph.triplesToTryGraph[Int, Double](Vertex.createWithSet)(ts) match
      case Success(g: UndirectedGraph[Int, Double] @unchecked) => g
      case other => fail(s"construction failed: $other")

  private def directed(ts: Seq[Triplet[Int, Double, EdgeType]]): DirectedGraph[Int, Double] =
    DirectedGraph.triplesToTryGraph[Int, Double](Vertex.createWithSet)(ts) match
      case Success(g: DirectedGraph[Int, Double] @unchecked) => g
      case other => fail(s"construction failed: $other")

  /** A 5-cycle with every weight tied, so any four edges form an MST. */
  private def tiedCycle: UndirectedGraph[Int, Double] =
    undirected((0 until 5).map(i => Triplet(i, (i + 1) % 5, Some(1.0), Undirected)).toSeq)

  /** A DAG whose every strongly connected component is a singleton: only numbering can vary. */
  private def singletonSccs: DirectedGraph[Int, Double] =
    directed(Seq(
      Triplet(0, 1, Some(1.0), Directed), Triplet(0, 2, Some(1.0), Directed),
      Triplet(0, 3, Some(1.0), Directed), Triplet(0, 4, Some(1.0), Directed),
      Triplet(1, 5, Some(1.0), Directed), Triplet(2, 5, Some(1.0), Directed)))

  private def distinctOverSeeds[A](f: Random => A): Int =
    seeds.map(s => f(Random(s))).distinct.size

  behavior of "a method documented as invariant"

  // The claim #156 singled out as needing measurement rather than assumption. Both algorithms
  // read the edge list, which is not shuffled, and never an adjacency iterator -- so "MST
  // algorithm" does not predict the answer, and Prim below is the contrast that proves it.
  it should "hold for MST.kruskal, even with every weight tied" in {
    val g = tiedCycle
    distinctOverSeeds(_ => MST.kruskal[Int, Double](g).toSeq) shouldBe 1
  }

  it should "hold for MST.boruvka, even with every weight tied" in {
    val g = tiedCycle
    distinctOverSeeds(_ => MST.boruvka[Int, Double](g).toSeq) shouldBe 1
  }

  it should "hold for the graph predicates and counts" in {
    val g = tiedCycle
    distinctOverSeeds(_ => g.isCyclic) shouldBe 1
    distinctOverSeeds(_ => g.isConnected) shouldBe 1
    distinctOverSeeds(_ => g.isBipartite) shouldBe 1
    distinctOverSeeds(_ => g.order) shouldBe 1
    distinctOverSeeds(_ => g.size) shouldBe 1
    distinctOverSeeds(_ => g.edges.toList) shouldBe 1
  }

  it should "hold for the grouping Kosaraju produces, though not its numbering" in {
    val g = singletonSccs
    val numberings = seeds.map { s =>
      given Random = Random(s)
      StronglyConnectedComponents.kosaraju[Int, Double](g)
    }
    // Grouping: a property of the graph.
    numberings.map(m => m.groupBy((_, id) => id).values.map(_.keySet).toSet).distinct.size shouldBe 1
    // Numbering: not. This half must keep failing to be constant, or the advice to compare
    // groupings rather than ids would be unnecessary caution rather than a real warning.
    numberings.distinct.size should be > 1
  }

  behavior of "a method documented as varying"

  // The contrast with Kruskal and Boruvka above: Prim walks adjacencies, so it varies. If this
  // ever started returning one answer, the generator would have stopped being threaded through
  // and the `@note` on `MST.prim` would be wrong.
  it should "hold for MST.prim on tied weights" in {
    val g = undirected(Seq(
      Triplet(0, 1, Some(1.0), Undirected), Triplet(0, 2, Some(1.0), Undirected),
      Triplet(1, 3, Some(1.0), Undirected), Triplet(2, 3, Some(1.0), Undirected)))
    distinctOverSeeds { r =>
      given Random = r
      val result = MST.prim[Int, Double](g, 0)
      result.keySet.toSeq.sorted.map(v => v -> result.vertexTraverse(v).toString)
    } should be > 1
  }

  it should "hold for the adjacency layer" in {
    val g = tiedCycle
    distinctOverSeeds { r => given Random = r; g.adjacentVertices(0).toList } should be > 1
    distinctOverSeeds { r => given Random = r; g.filteredAdjacentVertices(_ => true)(0).toList } should be > 1
  }

  it should "hold for TopologicalSort.sort" in {
    val g = singletonSccs
    distinctOverSeeds { r => given Random = r; TopologicalSort.sort(g) } should be > 1
  }
