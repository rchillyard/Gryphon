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
   * @throws IllegalArgumentException if the graph contains a cycle.
   */
  def shortestPaths[V, E: {Monoid, Ordering}](graph: DirectedGraph[V, E], start: V): VertexTraversalResult[V, DirectedEdge[V, E]] =
    val mn = implicitly[Monoid[E]]
    val ord = implicitly[Ordering[E]]

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
                .flatMap(_.maybeEdge[E].collect { case e: AttributedDirectedEdge[V, E] => e })
                .foldLeft(state) { case ((dist, pred), edge) =>
                  val newDist = mn.combine(dist(v), edge.attribute)
                  if dist.get(edge.black).forall(ord.lt(newDist, _)) then
                    (dist + (edge.black -> newDist)) -> (pred + (edge.black -> edge))
                  else dist -> pred
                }
    }

    VertexTraversalResult(pred)