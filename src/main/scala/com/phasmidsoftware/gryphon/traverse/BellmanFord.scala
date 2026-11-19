/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.traverse

import cats.kernel.Monoid
import com.phasmidsoftware.gryphon.adjunct.{AttributedDirectedEdge, DirectedEdge, DirectedGraph}
import scala.annotation.tailrec
import scala.collection.immutable.Queue

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
  def shortestPaths[V, E: {Monoid, Ordering}](graph: DirectedGraph[V, E], start: V): Option[VertexTraversalResult[V, DirectedEdge[V, E]]] =
    val em = implicitly[Monoid[E]]
    val eo = implicitly[Ordering[E]]
    val vertexCount = graph.order

    /**
     * Everything the queue-based relaxation carries. These five travelled as five mutable
     * collections and a `var`; they are one immutable value now, threaded through the loop.
     *
     * @param dist     best known cost to each reached vertex.
     * @param pred     the edge by which each vertex was last relaxed — the result.
     * @param onQueue  which vertices are already waiting, so they are not queued twice.
     * @param enqueued how many times each vertex has been queued; `vertexCount` proves a cycle.
     * @param queue    the work still to do.
     */
    final case class State(
                                  dist: Map[V, E],
                                  pred: Map[V, DirectedEdge[V, E]],
                                  onQueue: Set[V],
                                  enqueued: Map[V, Int],
                                  queue: Queue[V]
                          )

    /**
     * Relaxes one edge leaving `v`. `None` means a negative cycle has been proved: a vertex
     * queued `vertexCount` times cannot be explained by any simple path.
     */
    def relax(state: State, v: V, edge: AttributedDirectedEdge[V, E]): Option[State] =
      val w = edge.black
      val newDist = em.combine(state.dist(v), edge.attribute)
      if !state.dist.get(w).forall(eo.lt(newDist, _)) then Some(state)
      else
        val relaxed = state.copy(dist = state.dist + (w -> newDist), pred = state.pred + (w -> edge))
        if relaxed.onQueue.contains(w) then Some(relaxed)
        else
          val count = relaxed.enqueued.getOrElse(w, 0) + 1
          // `<` is the tight bound: a vertex on a simple path can be enqueued at most
          // `vertexCount - 1` times, so the `vertexCount`-th enqueue proves a negative cycle.
          // Loosening it to `<=` would still prove the cycle, one round later, and return the
          // same `None` — so that mutation survives permanently and by design. It costs a
          // round of work, not correctness. See issue #83.
          Option.when(count < vertexCount)(relaxed.copy(
            queue = relaxed.queue.enqueue(w),
            onQueue = relaxed.onQueue + w,
            enqueued = relaxed.enqueued + (w -> count)
          ))

    @tailrec
    def loop(state: State): Option[State] =
      state.queue.dequeueOption match
        case None =>
          Some(state) // nothing left to relax: the distances are final
        case Some((v, rest)) =>
          val popped = state.copy(queue = rest, onQueue = state.onQueue - v)
          val relaxedAll = graph.filteredAdjacencies(_ => true)(v)
                  .flatMap(_.maybeEdge[E].collect { case e: AttributedDirectedEdge[V, E] => e })
                  .foldLeft(Option(popped)) {
                    case (None, _) => None // a negative cycle is already proved; stop
                    case (Some(acc), edge) => relax(acc, v, edge)
                  }
          relaxedAll match
            case None => None
            case Some(next) => loop(next)

    loop(State(Map(start -> em.empty), Map.empty, Set(start), Map(start -> 1), Queue(start)))
            .map(state => VertexTraversalResult(state.pred))