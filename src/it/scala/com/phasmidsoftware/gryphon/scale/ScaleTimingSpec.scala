package com.phasmidsoftware.gryphon.scale

import com.phasmidsoftware.gryphon.traverse.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scala.util.Random

/**
 * What does each algorithm cost at 1000 vertices? Issue #165.
 *
 * ==These are reported, not asserted==
 *
 * Every figure is printed through `info` and almost nothing is checked. A hard performance
 * assertion would be flaky on shared CI hardware and would fail for reasons that have nothing to
 * do with this library — a noisy neighbour on the build machine is not a regression. The one
 * assertion made is a sanity bound so loose that only a catastrophic regression could cross it.
 *
 * The value is reproducibility. The README has quoted timings since 1.9.0 against *"a 1491-vertex,
 * 6000-edge DAG"* that was never committed, so those numbers cannot be re-derived, checked or
 * challenged by anyone — including whoever wrote them. Running this spec re-derives every figure
 * in #164 on demand, against fixtures that are in the repository.
 *
 * ==Read the output, not the pass==
 *
 * A green run here means nothing crashed. The useful output is the printed table, and the thing
 * to look at is the RATIOS: sparse to dense is 4.3 times the edges, so an algorithm that is
 * linear in edges should take a little over four times as long. `MST.prim` takes 24 times as long
 * and `ShortestPaths.dijkstra` 21, which is how #164 was found. Everything else behaves.
 */
class ScaleTimingSpec extends AnyFlatSpec with Matchers:

  private given Random = Random(42)

  private def time[A](label: String)(f: => A): Long =
    val t0 = System.nanoTime()
    val _ = f
    val ms = (System.nanoTime() - t0) / 1000000L
    info(f"$label%-44s $ms%7d ms")
    ms

  behavior of "the undirected algorithms"

  it should "report timings on the sparse and dense graphs" in {
    for (name, g) <- Seq("sparse" -> ScaleGraphs.sparse, "dense" -> ScaleGraphs.dense) do
      info(s"--- $name: ${g.order} vertices, ${g.size} edges ---")
      time(s"$name isConnected")(g.isConnected)
      time(s"$name isCyclic")(g.isCyclic)
      time(s"$name isBipartite")(g.isBipartite)
      time(s"$name Kruskal.mst")(Kruskal.mst[Int, Double](g).toSeq.size)
      time(s"$name Boruvka.mst")(Boruvka.mst[Int, Double](g).toSeq.size)
      time(s"$name ConnectedComponents")(ConnectedComponents.components[Int, Double](g).partition.size)
    succeed
  }

  // Prim on the SPARSE graph only. The dense case takes over two minutes (#164), which is too
  // long even here; the ratio between sparse and dense is recorded on that issue instead. Add
  // the dense case back when #164 is fixed.
  it should "report Prim on the sparse graph, which is slow enough to be the point" in {
    val ms = time("sparse MST.prim")(MST.prim[Int, Double](ScaleGraphs.sparse, 0).keySet.size)
    // A sanity bound, not a performance target: Prim took about 5.7 s when #164 was filed, so a
    // minute means something has gone badly wrong rather than merely slowly.
    ms should be < 60000L
  }

  behavior of "the directed algorithms"

  it should "report timings on the sparse and dense DAGs" in {
    for (name, g) <- Seq("sparseDag" -> ScaleGraphs.sparseDag, "denseDag" -> ScaleGraphs.denseDag) do
      info(s"--- $name: ${g.order} vertices, ${g.size} edges ---")
      time(s"$name topologicalOrder")(g.topologicalOrder.map(_.size))
      time(s"$name TopologicalSort.sort")(TopologicalSort.sort(g).map(_.size))
      time(s"$name BellmanFord")(BellmanFord.shortestPaths[Int, Double](g, 0).map(_.size))
      time(s"$name AcyclicShortestPaths")(AcyclicShortestPaths.shortestPaths[Int, Double](g, 0).size)
      time(s"$name Kosaraju")(Kosaraju.stronglyConnectedComponents[Int, Double](g).size)
      time(s"$name ShortestPaths.dijkstra")(ShortestPaths.dijkstra[Int, Double](g, 0).keySet.size)
    succeed
  }
