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
 * Pass 1 — post-order DFS over the original graph, collecting vertices in reverse
 * finish order.  Because `ListJournal` prepends, `result.result.map(_._1).toList`
 * already yields vertices with the last-finished vertex at the head — exactly the
 * seed order required for pass 2.
 *
 * Pass 2 — DFS over the reversed graph, seeding vertices in the order produced by
 * pass 1, with a shared visited set carried across seeds.  Each DFS tree in pass 2
 * is exactly one SCC.
 *
 * NOTE: intended for directed graphs only.
 * For undirected graphs see `ConnectedComponents`.
 */
object Kosaraju:

  /**
   * Identifies all strongly connected components of `graph`.
   *
   * @param graph  the directed graph to analyse.
   * @param random controls adjacency ordering during traversal.
   * @tparam V the vertex attribute type.
   * @tparam E the edge attribute type.
   * @return an `SCCResult[V]` mapping every vertex to its SCC id.
   */
  def components[V, E](graph: DirectedGraph[V, E])(using random: Random = Random()): SCCResult[V] =
    given Evaluable[V, V] with
      def evaluate(v: V): Option[V] = Some(v)

    // ------------------------------------------------------------------
    // Pass 1: post-order DFS over the original graph, visiting every vertex.
    // ListJournal prepends, so within each component the result is in reverse
    // post-order (last-finished at head). We prepend each component's list onto
    // acc so the last-seeded component ends up at the front overall.
    // ------------------------------------------------------------------
    val finishOrder: List[V] =
      given GraphNeighbours[V] = graph.vertexMap.neighboursGiven

      val allVertices = graph.vertexMap.keySet

      @tailrec
      def pass1Loop(unvisited: Set[V], vs: VisitedSet[V], acc: List[V]): List[V] =
        if unvisited.isEmpty then acc
        else
          val seed = unvisited.head

          given VisitedSet[V] = vs

          val result = Traversal.dfs(seed, JournaledVisitor.withListJournal[V, V], DfsOrder.Post)
          // ListJournal prepends: head = last-finished within this component.
          // visited ++ acc keeps the most-recently-finished component at the front.
          val visited = result.result.map(_._1).toList
          val newVs = visited.foldLeft(vs)(_.markVisited(_))
          pass1Loop(unvisited -- visited.toSet, newVs, visited ++ acc)

      val order = pass1Loop(allVertices, summon[VisitedSet[V]], Nil)
      order
    // ------------------------------------------------------------------
    // Pass 2: DFS over the reversed graph, one seed per unvisited vertex
    // in finish order. A shared visited set ensures each DFS tree is one SCC.
    // GraphNeighbours is fixed for the reversed graph throughout pass 2.
    // ------------------------------------------------------------------
    val reversed = graph.reverse

    given GraphNeighbours[V] = reversed.vertexMap.neighboursGiven

    @tailrec
    def loop(
                    seeds: List[V],
                    visited: Set[V],
                    componentMap: SCCResult[V],
                    sccId: Int
            ): SCCResult[V] =
      seeds match
        case Nil => componentMap
        case v :: rest =>
          if visited.contains(v) then loop(rest, visited, componentMap, sccId)
          else
            given VisitedSet[V] = visited.foldLeft(summon[VisitedSet[V]])(_.markVisited(_))

            val sccVertices: Set[V] =
              Traversal.dfs(v, JournaledVisitor.withListJournal[V, V]).result.map(_._1).toSet
            loop(
              rest,
              visited ++ sccVertices,
              componentMap ++ sccVertices.map(_ -> sccId),
              sccId + 1
            )

    loop(finishOrder, Set.empty, Map.empty, 0)