/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.traverse

import cats.kernel.Monoid
import com.phasmidsoftware.gryphon.adjunct.{AttributedDirectedEdge, DirectedEdge, DirectedGraph}

/**
 * Computes shortest paths in a weighted DAG using topological relaxation.
 *
 * The algorithm:
 *   1. Topologically sort the DAG.
 *   2. Relax all outgoing edges of each vertex in topological order.
 *
 * Since edges are relaxed in topological order, every predecessor of a vertex
 * has already been processed when we reach it — guaranteeing optimal substructure
 * without a priority queue. This gives O(V+E) time and correctly handles negative
 * edge weights (unlike Dijkstra).
 *
 * The edge-weight type `E` requires only a `Monoid` (for the zero distance and
 * addition) and an `Ordering` (for comparison) — a lighter constraint than
 * `Numeric`, consistent with `RelaxableVertex` and `WeightedUnionFind`.
 *
 * NOTE: the graph must be a DAG. If it contains a cycle, `TopologicalSort.sort`
 * returns `None` and this method throws `IllegalArgumentException`.
 */
object AcyclicShortestPaths:

  /**
   * Computes shortest paths from `start` to all reachable vertices in `graph`.
   *
   * @param graph the directed acyclic graph.
   * @param start the source vertex.
   * @tparam V the vertex type.
   * @tparam E the edge-weight type; must have `Monoid` (zero + combine) and `Ordering`.
   * @return a `VertexTraversalResult[V, DirectedEdge[V, E]]` mapping each reachable
   *         vertex (except `start`) to its shortest-path incoming edge.
   *
   * @throws IllegalArgumentException if the graph contains a cycle.
   */
  def shortestPaths[V, E: {Monoid, Ordering}](graph: DirectedGraph[V, E], start: V): VertexTraversalResult[V, DirectedEdge[V, E]] =
    relax(graph, start)(using summon[Monoid[E]], summon[Ordering[E]])

  /**
   * Computes LONGEST paths from `start` to all reachable vertices in `graph`.
   *
   * The textbook derives this by negating the weights, which cannot be done here: `E` carries
   * only `Monoid` and `Ordering`, and neither offers negation. Demanding a `Group[E]` or a
   * `Numeric[E]` would narrow the API for every caller in order to serve one method.
   *
   * It is also unnecessary. `relax` uses its two type classes in exactly two places — `Monoid` to
   * accumulate a distance, `Ordering` only to decide whether a candidate beats the incumbent — so
   * reversing the `Ordering` turns "keep the smaller" into "keep the larger" and changes nothing
   * else. Issue #114.
   *
   * This works for a DAG precisely because the topological order makes each vertex final by the
   * time it is reached, so "take the better candidate" is correct whichever direction better runs.
   * The same trick does NOT work for Dijkstra, whose correctness depends on the minimum being
   * extracted first.
   *
   * @param graph the directed acyclic graph.
   * @param start the source vertex.
   * @tparam V the vertex type.
   * @tparam E the edge-weight type; must have `Monoid` (zero + combine) and `Ordering`.
   * @return a `VertexTraversalResult[V, DirectedEdge[V, E]]` mapping each reachable
   *         vertex (except `start`) to its longest-path incoming edge.
   * @throws IllegalArgumentException if the graph contains a cycle.
   */
  def longestPaths[V, E: {Monoid, Ordering}](graph: DirectedGraph[V, E], start: V): VertexTraversalResult[V, DirectedEdge[V, E]] =
    relax(graph, start)(using summon[Monoid[E]], summon[Ordering[E]].reverse)

  /**
   * The relaxation shared by `shortestPaths` and `longestPaths`, which differ only in the
   * `Ordering` they pass. Taking it as an explicit parameter rather than a context bound is the
   * whole mechanism: `ord.lt` is the only place the direction is decided.
   *
   * @param graph the directed acyclic graph.
   * @param start the source vertex.
   * @param ord   decides whether a candidate distance beats the incumbent. Reversed, this yields
   *              longest rather than shortest paths.
   */
  private def relax[V, E](graph: DirectedGraph[V, E], start: V)(using mn: Monoid[E], ord: Ordering[E]): VertexTraversalResult[V, DirectedEdge[V, E]] =
    val topoOrder: Seq[V] = TopologicalSort.sort(graph)
            .getOrElse(throw IllegalArgumentException("AcyclicShortestPaths: graph contains a cycle"))

    // One pass in topological order, so each vertex is final by the time it is reached and the
    // state can simply be threaded through rather than mutated. `dist` and `pred` travel together
    // because they are updated together, on exactly the edges that relax.
    type State = (Map[V, E], Map[V, DirectedEdge[V, E]])

    val (_, pred) = topoOrder.foldLeft[State](Map(start -> mn.empty) -> Map.empty) {
      case (state@(dist, _), v) if !dist.contains(v) =>
        state // not reachable from start, so nothing leaving it can relax
      case (state, v) =>
        graph.filteredAdjacencies(_ => true)(v)
                .flatMap(_.maybeEdge[E].collect {
                  case e: AttributedDirectedEdge[V, E] => e
                })
                .foldLeft(state) {
                  case ((dist, pred), edge) =>
                    val newDist = mn.combine(dist(v), edge.attribute)
                    if dist.get(edge.black).forall(ord.lt(newDist, _)) then
                      (dist + (edge.black -> newDist)) -> (pred + (edge.black -> edge))
                    else
                      dist -> pred
                }
    }

    VertexTraversalResult(pred)