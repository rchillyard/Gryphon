/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.traverse

import com.phasmidsoftware.gryphon.adjunct.DirectedGraph
import com.phasmidsoftware.visitor.core.{*, given}
import scala.annotation.tailrec
import scala.util.Random

/**
 * Type alias for the result of a strongly-connected-components analysis.
 * Maps every vertex to an integer SCC id (0, 1, 2, ...).
 * Vertices sharing the same id belong to the same SCC.
 */
type SCCResult[V] = Map[V, Int]

/**
 * Computes strongly connected components of a directed graph using Kosaraju's algorithm.
 *
 * The algorithm makes two passes:
 *
 * Pass 1 — post-order DFS over the **reversed** graph, collecting vertices in reverse
 * finish order.  Because `ListJournal` prepends, `result.result.map(_._1).toList`
 * already yields vertices with the last-finished vertex at the head — exactly the
 * seed order required for pass 2.
 *
 * Pass 2 — DFS over the **original** graph, seeding vertices in the order produced by
 * pass 1, with a shared visited set carried across seeds.  Each DFS tree in pass 2
 * is exactly one SCC.
 *
 * NOTE: intended for directed graphs only.
 * For undirected graphs see `ConnectedComponents`.
 */
object Kosaraju {

  /**
   * Finds the strongly connected components (SCCs) of a directed graph using Kosaraju's two-pass algorithm.
   *
   * @param graph  The directed graph for which to compute the SCCs.
   * @param random An optional implicit random instance used for any randomized operations (default: a new random instance).
   * @param tracer An optional tracer for pedagogical output; defaults to `Tracer.silent` (no output).
   *               Use `Tracer.verbose()` or `Tracer.summary()` to illuminate the two-pass structure.
   *               NOTE: the tracer covers Kosaraju's own loop structure only; inner `Traversal.dfs`
   *               calls use `Tracer.silent` so that traversal-level detail does not intermix with
   *               algorithm-level detail.
   * @return An SCCResult containing a mapping of each vertex to its corresponding strongly connected component (SCC) identifier.
   */
  def stronglyConnectedComponents[V, E](graph: DirectedGraph[V, E])(using random: Random = Random(), tracer: Tracer[V] = Tracer.silent): SCCResult[V] = {
    given Evaluable[V, V] with
      def evaluate(v: V): Option[V] = Some(v)

    /**
     * Pass 1: post-order DFS over the REVERSED graph, collecting vertices in reverse finish order.
     * ListJournal prepends, so within each component the result is in reverse
     * post-order (last-finished at head).
     * We prepend each component's list onto acc so the last-seeded component ends up at the front overall.
     *
     * @param reverseGraph the reversed directed graph.
     * @return a List[V] of vertices in "finish" order.
     */
    def pass1Kosaraju(reverseGraph: DirectedGraph[V, E]): List[V] = {
      tracer.trace(0, "Kosaraju pass 1: post-order DFS on reversed graph")

      given GraphNeighbours[V] = reverseGraph.vertexMap.neighboursGiven

      @tailrec
      def pass1Loop(unvisited: Set[V], vs: VisitedSet[V], acc: List[V]): List[V] =
        if unvisited.isEmpty
        then
          acc
        else
          given VisitedSet[V] = vs

          tracer.trace(1, s"pass 1: seeding from ${unvisited.head}, ${unvisited.size} unvisited")
          val result = Traversal.dfs(start = unvisited.head, visitor = JournaledVisitor.withListJournal[V, V], order = DfsOrder.Post)(using summon[GraphNeighbours[V]], summon[Evaluable[V, V]], vs, Tracer.silent)
          // ListJournal prepends: head = last-finished within this component.
          // visited ++ acc keeps the most-recently-finished component at the front.
          val visited: List[V] = result.result.map(_._1).toList
          tracer.trace(2, s"pass 1: finished component: $visited")
          val newVs: VisitedSet[V] = visited.foldLeft(vs)(_.markVisited(_))
          pass1Loop(unvisited -- visited.toSet, newVs, visited ++ acc)

      val allVertices = graph.vertexMap.keySet
      pass1Loop(allVertices, summon[VisitedSet[V]], Nil)
    }

    // ------------------------------------------------------------------
    // Pass 2: DFS over the ORIGINAL graph, one seed per unvisited vertex
    // in finish order. A shared visited set ensures each DFS tree is one SCC.
    // GraphNeighbours is fixed for the original graph throughout pass 2.
    // ------------------------------------------------------------------
    def pass2Kosaraju(finishOrder: List[V]): SCCResult[V] = {
      tracer.trace(0, s"Kosaraju pass 2: DFS on original graph, ${finishOrder.size} vertices in finish order")
      given GraphNeighbours[V] = graph.vertexMap.neighboursGiven

      @tailrec
      def pass2Loop(
                           seeds: List[V],
                           visited: Set[V],
                           componentMap: SCCResult[V],
                           sccId: Int
                   ): SCCResult[V] =
        seeds match
          case Nil =>
            tracer.trace(1, s"pass 2: complete, $sccId SCCs found")
            componentMap
          case v :: rest =>
            if visited.contains(v) then pass2Loop(rest, visited, componentMap, sccId)
            else
              tracer.trace(1, s"pass 2: SCC $sccId — seeding from $v")

              given VisitedSet[V] = visited.foldLeft(summon[VisitedSet[V]])(_.markVisited(_))

              val sccVertices: Set[V] =
                Traversal.dfs(v, JournaledVisitor.withListJournal[V, V])(using summon[GraphNeighbours[V]], summon[Evaluable[V, V]], summon[VisitedSet[V]], Tracer.silent).result.map(_._1).toSet
              tracer.trace(2, s"pass 2: SCC $sccId — members: $sccVertices")
              pass2Loop(
                rest,
                visited ++ sccVertices,
                componentMap ++ sccVertices.map(_ -> sccId),
                sccId + 1
              )

      pass2Loop(finishOrder, Set.empty, Map.empty, 0)
    }

    // ------Kosaraju's algorithm: two passes------------------------------------------------------------
    pass2Kosaraju(pass1Kosaraju(graph.reverse))
  }
}