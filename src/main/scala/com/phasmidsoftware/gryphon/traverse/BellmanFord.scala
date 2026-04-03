/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.traverse

import com.phasmidsoftware.gryphon.adjunct.{AttributedDirectedEdge, DirectedEdge, DirectedGraph}
import com.phasmidsoftware.visitor.core.Monoid
import scala.collection.mutable
import scala.util.Random

/**
 * Computes shortest paths in a weighted directed graph using the
 * Bellman-Ford-Moore algorithm (queue-based Bellman-Ford).
 *
 * Unlike Dijkstra, this algorithm correctly handles negative edge weights.
 * Unlike `AcyclicShortestPaths`, it works on graphs with cycles.
 * It detects negative cycles reachable from the source and returns `None` in that case.
 *
 * Algorithm (Bellman-Ford-Moore):
 *   1. Seed a queue with `start` at distance zero.
 *   2. Dequeue vertex `v`; for each outgoing edge v→w:
 *      if dist(w) > dist(v) + weight, relax and enqueue w (if not already queued).
 *   3. Track enqueue count per vertex — if any vertex is enqueued V times,
 *      a negative cycle is reachable → return None.
 *   4. Terminate when queue is empty → return Some(result).
 *
 * Time complexity: O(VE) worst case, much faster in practice due to early termination.
 *
 * The edge-weight type `E` requires `Monoid` (zero + combine) and `Ordering`
 * (comparison) — consistent with `AcyclicShortestPaths`.
 */
object BellmanFord:

  /**
   * Computes shortest paths from `start` to all reachable vertices in `graph`.
   *
   * @param graph the directed graph (may contain cycles and negative weights).
   * @param start the source vertex.
   * @tparam V the vertex type.
   * @tparam E the edge-weight type; must have `Monoid` and `Ordering`.
   * @return `Some(VertexTraversalResult)` mapping each reachable non-source vertex
   *         to its shortest-path incoming edge, or `None` if a negative cycle is
   *         reachable from `start`.
   */
  def shortestPaths[V, E: {Monoid, Ordering}](graph: DirectedGraph[V, E], start: V): Option[VertexTraversalResult[V, DirectedEdge[E, V]]] =
    given Random = Random(0)

    val em = implicitly[Monoid[E]]
    val eo = implicitly[Ordering[E]]
    val V = graph.N

    // NOTE Create all the working data structures.
    val dist: mutable.Map[V, E] = mutable.Map(start -> em.identity)
    val pred: mutable.Map[V, DirectedEdge[E, V]] = mutable.Map.empty
    val onQueue: mutable.Set[V] = mutable.Set(start)
    val enqueued: mutable.Map[V, Int] = mutable.Map(start -> 1)
    val queue: mutable.Queue[V] = mutable.Queue(start)

    // NOTE this is a mutable variable, but it is only used in the loop condition.
    var negativeCycle = false

    while queue.nonEmpty && !negativeCycle do
      val v = queue.dequeue()
      onQueue -= v
      for
        adj <- graph.filteredAdjacencies(_ => true)(v)
        edge <- adj.maybeEdge[E].collect { case e: AttributedDirectedEdge[E, V] => e }
        if !negativeCycle
      do
        val w = edge.black
        val newDist = em.combine(dist(v), edge.attribute)
        if dist.get(w).forall(eo.lt(newDist, _)) then
          dist(w) = newDist
          pred(w) = edge
          if !onQueue.contains(w) then
            queue.enqueue(w)
            onQueue += w
            val count = enqueued.getOrElse(w, 0) + 1
            enqueued(w) = count
            if count >= V then negativeCycle = true

    if negativeCycle then None
    else Some(VertexTraversalResult(pred.toMap))