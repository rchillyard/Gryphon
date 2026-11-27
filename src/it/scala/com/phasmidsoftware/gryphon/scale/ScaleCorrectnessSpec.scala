package com.phasmidsoftware.gryphon.scale

import com.phasmidsoftware.gryphon.adjunct.AttributedDirectedEdge
import com.phasmidsoftware.gryphon.traverse.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scala.util.Random

/**
 * Do the algorithms still give right answers on graphs 100 times larger than the rest of the
 * suite uses? Issue #165.
 *
 * ==Why these assertions, and not expected values==
 *
 * Nothing here asserts a literal. A 1000-vertex answer cannot be checked by eye, and writing one
 * down would only pin whatever the library happened to produce on the day — which is worse than
 * no test, because it looks like a specification and is really a snapshot. Worse still here,
 * since most of these results legitimately vary between runs (see `InvarianceSpec` and the
 * `@note Result:` lines).
 *
 * So each test asserts a '''property that must hold of any correct answer''': a spanning tree has
 * `V-1` edges, every arc runs forwards in a topological order, three shortest-path algorithms
 * agree on the distances even though they may disagree about which path achieved them. Those are
 * the statements a scale-dependent bug would break, and they are true whatever order the
 * traversal happened to take.
 *
 * ==Cross-checking is the strongest tool here==
 *
 * Three independent MST algorithms must agree on the total weight; three shortest-path algorithms
 * must agree on the distances. Agreement between implementations that share no code is far better
 * evidence at this size than any single expected value, and it is what would catch an error that
 * only appears above a few dozen vertices.
 */
class ScaleCorrectnessSpec extends AnyFlatSpec with Matchers:

  private given Random = Random(42)

  private val V = ScaleGraphs.vertices

  behavior of "the undirected graphs"

  it should "load with the shape the fixtures claim" in {
    ScaleGraphs.sparse.order shouldBe V
    ScaleGraphs.sparse.size shouldBe 7000
    ScaleGraphs.dense.order shouldBe V
    ScaleGraphs.dense.size shouldBe 30000
  }

  // Connectivity is guaranteed by construction -- each fixture is built from a spanning tree
  // outwards -- so a failure here means the builder or the traversal is wrong, not the fixture.
  it should "be connected, which is how they were generated" in {
    ScaleGraphs.sparse.isConnected shouldBe true
    ScaleGraphs.dense.isConnected shouldBe true
  }

  it should "find exactly one connected component, covering every vertex" in {
    for g <- Seq(ScaleGraphs.sparse, ScaleGraphs.dense) do
      val result = ConnectedComponents.components[Int, Double](g)
      result.partition.size shouldBe 1
      result.partition.flatten shouldBe (0 until V).toSet
      result.size shouldBe V // inherited: the VERTEX count, not the component count
  }

  // The cross-check that matters. Kruskal and Boruvka read the edge list; Prim walks adjacencies.
  // They share no traversal code, so agreement on the total weight is strong evidence, where a
  // single expected number would only be a snapshot.
  it should "have three MST algorithms agree on the weight of the sparse graph" in {
    val g = ScaleGraphs.sparse
    val kruskal = MST.kruskal[Int, Double](g).toSeq
    val boruvka = MST.boruvka[Int, Double](g).toSeq
    val prim = MST.prim[Int, Double](g, 0)

    kruskal.size shouldBe V - 1 // a spanning tree of a connected graph
    boruvka.size shouldBe V - 1

    val kruskalWeight = kruskal.map(_.attribute).sum
    boruvka.map(_.attribute).sum shouldBe kruskalWeight +- 1e-9
    prim.keySet.toSeq.flatMap(prim.vertexTraverse).map(_.attribute).sum shouldBe kruskalWeight +- 1e-9
  }

  // The dense graph gets Kruskal and Boruvka only. Prim takes over two minutes on it (#164),
  // which is not a reasonable cost even for a deliberate suite -- and the sparse case above
  // already cross-checks all three. Put Prim back here when #164 is fixed: this assertion
  // passing in reasonable time is a natural completion test for that issue.
  it should "have Kruskal and Boruvka agree on the weight of the dense graph" in {
    val g = ScaleGraphs.dense
    val kruskal = MST.kruskal[Int, Double](g).toSeq
    val boruvka = MST.boruvka[Int, Double](g).toSeq
    kruskal.size shouldBe V - 1
    boruvka.size shouldBe V - 1
    boruvka.map(_.attribute).sum shouldBe kruskal.map(_.attribute).sum +- 1e-9
  }

  it should "reach every vertex from a full depth-first sweep" in {
    for g <- Seq(ScaleGraphs.sparse, ScaleGraphs.dense) do
      ConnectedComponents.components[Int, Double](g).map.keySet shouldBe (0 until V).toSet
  }

  behavior of "the directed acyclic graphs"

  it should "load with the shape the fixtures claim, and be acyclic" in {
    for (g, edges) <- Seq(ScaleGraphs.sparseDag -> 7000, ScaleGraphs.denseDag -> 30000) do
      g.order shouldBe V
      g.size shouldBe edges
      g.isCyclic shouldBe false
  }

  // The defining property of a topological order, asserted over every one of the 7000 or 30000
  // arcs. Any valid order satisfies it, so this is immune to which order the traversal picked --
  // which it must be, since that varies (#158).
  it should "put every arc forwards in the topological order" in {
    for g <- Seq(ScaleGraphs.sparseDag, ScaleGraphs.denseDag) do
      val order = g.topologicalOrder.getOrElse(fail("expected a DAG"))
      order.size shouldBe V
      order.toSet shouldBe (0 until V).toSet
      val position = order.iterator.zipWithIndex.toMap
      g.edges.foreach(e => position(e.white) should be < position(e.black))
  }

  // Three shortest-path algorithms, three different strategies: a priority queue, a work queue,
  // and one relaxation pass in topological order. They may disagree about WHICH shortest path
  // they report -- that is documented and expected -- but never about its cost.
  it should "have three shortest-path algorithms agree on the distances" in {
    for g <- Seq(ScaleGraphs.sparseDag, ScaleGraphs.denseDag) do
      val dijkstra = distances(ShortestPaths.dijkstra[Int, Double](g, 0))
      val acyclic = distances(ShortestPaths.acyclic[Int, Double](g, 0))
      // Computed once, not once per vertex: walking every predecessor chain is O(V * depth), and
      // the fixtures contain a 1000-long path, so recomputing it inside the loop below would turn
      // a millisecond check into a minute-long one.
      val bellman = distances(ShortestPaths.bellmanFord[Int, Double](g, 0)
              .getOrElse(fail("a DAG has no negative cycle, so this cannot be None")))

      // Vertex 0 is the source and absent from every result; the fixtures include the path
      // 0 -> 1 -> ... -> 999, so every other vertex is reachable.
      dijkstra.keySet shouldBe (1 until V).toSet
      acyclic.keySet shouldBe dijkstra.keySet
      bellman.keySet shouldBe dijkstra.keySet

      for v <- 1 until V do
        acyclic(v) shouldBe dijkstra(v) +- 1e-9
        bellman(v) shouldBe dijkstra(v) +- 1e-9
  }

  it should "find 1000 singleton strongly connected components, a DAG having no others" in {
    for g <- Seq(ScaleGraphs.sparseDag, ScaleGraphs.denseDag) do
      val sccs = StronglyConnectedComponents.kosaraju[Int, Double](g)
      sccs.keySet shouldBe (0 until V).toSet
      // Grouping is invariant even though the numbering is not, so this is the safe assertion --
      // and since 1.15.0 the result offers it directly rather than making each caller derive it.
      sccs.partition.size shouldBe V
      sccs.partition.flatten shouldBe (0 until V).toSet
  }

  /** Walks each vertex's recorded predecessor edge back to the source, summing the weights. */
  private def distances(result: TraversalResult[Int, ? <: com.phasmidsoftware.gryphon.adjunct.DirectedEdge[Int, Double]]): Map[Int, Double] =
    def cost(v: Int, seen: Set[Int]): Double =
      if v == 0 then 0.0
      else if seen.contains(v) then fail(s"cycle in the predecessor map at $v")
      else result.vertexTraverse(v) match
        case Some(e: AttributedDirectedEdge[Int, Double] @unchecked) => e.attribute + cost(e.white, seen + v)
        case other => fail(s"no predecessor edge for $v: $other")

    result.keySet.map(v => v -> cost(v, Set.empty)).toMap
