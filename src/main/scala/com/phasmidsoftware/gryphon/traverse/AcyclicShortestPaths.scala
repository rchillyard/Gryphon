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
 *
 * NOTE on reproducibility, because this object is the exception in the library. Every other
 * traversal here takes a `using Random` and a pinned seed settles its result (#67). These
 * methods take none. The costs they compute are invariant; which of several equal-cost paths
 * they report is fixed per graph '''instance''' — the relaxation order is the memoized
 * `DirectedGraph.topologicalOrder` — and arbitrary across instances and across runs. A seed
 * cannot change that, and adding a parameter for one would not help, since the only order a
 * generator could reach here is provably inert. `shortestPaths` explains why in full; #158
 * tracks what it would take to make this pinnable.
 */
object AcyclicShortestPaths:

  /**
   * Computes shortest paths from `start` to all reachable vertices in `graph`.
   *
   * ==Reproducibility: read this before relying on the result==
   *
   * '''The distances are invariant. Which of several equal-cost paths is reported is not, and
   * you cannot currently pin it.''' This method takes no `Random`, and that is not an oversight
   * — see below for why adding one would be worse than useless. The rest of the library's
   * traversals do take one (#67), so this is the exception, and it is worth understanding.
   *
   * '''What is stable.''' Everything, for a given graph ''instance''. The relaxation order comes
   * from `DirectedGraph.topologicalOrder`, a memoized `lazy val`, so the first call fixes it and
   * every later call on the same object agrees. Within one run, over one graph, this method is a
   * function.
   *
   * '''What is not.''' A different instance of the same graph — rebuilt, parsed again, or
   * reached through a derived graph — may memoize a different valid topological order, and
   * therefore report a different (equally short) path. Across runs of your program it is
   * arbitrary. If you are comparing output between runs, or asserting a particular predecessor
   * in a test, hold one graph instance; otherwise assert on the costs, which do not vary.
   *
   * ==Why there is no `Random` parameter==
   *
   * Two orders could in principle decide which equal-cost predecessor is recorded: the
   * topological order of the vertices, and the order a vertex's outgoing edges are relaxed in.
   *
   *  - The topological order is memoized in a `lazy val`, and a `val` takes no `using` clause.
   *    A generator cannot reach it. Keeping the cache is deliberate — it is what stops a caller
   *    who checks `isDAG` first from paying for two sorts (#118).
   *  - The edge order within a vertex is reachable, and settles nothing. Each edge `v -> w`
   *    updates its own key, so two edges from `v` can only compete when they run to the '''same'''
   *    `w` at equal cost — parallel edges, which Gryphon's adjacency structure does not admit:
   *    `fromTriplets` rejects such a graph outright (#57). Measured on a diamond with both equal-cost
   *    paths present, forty seeds give one result.
   *
   * So a `(using Random)` here would be a parameter that provably does nothing on any graph this
   * library can build, while reading as a guarantee of reproducibility it could not keep. That is
   * the same false promise as the `Random(0)` the Java façade used to pin, and it is rejected for
   * the same reason. `BellmanFord.shortestPaths` '''does''' take one, and genuinely needs it: it
   * is queue-based, so adjacency order decides the order vertices are dequeued and hence which
   * equal-cost predecessor wins.
   *
   * Pinning this properly means offering a seeded route past the memoized order. See #158.
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
   * The reproducibility note on `shortestPaths` applies here unchanged, and for the same
   * reasons: the lengths are invariant, which of several equal-length paths is reported is
   * stable per graph instance and arbitrary across them, and there is no `Random` to pin it
   * with because one would have nothing to do.
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
    // graph.topologicalOrder, not TopologicalSort.sort(graph): the graph caches it, so a caller
    // who checked isDAG first -- which the Java façade's own scaladoc tells them to do -- does not
    // pay for the sort twice. That double cost was the concrete case behind #118, and keeping it
    // is a deliberate choice, made again in 1.14.0 when the alternative was on the table.
    //
    // The consequence is stated in full on `shortestPaths` and must not be glossed: a `lazy val`
    // takes no `using` clause, so this order cannot be seeded, and it is the ONLY thing that
    // decides which equal-cost predecessor is recorded. The edge order below is reachable but
    // inert -- each edge writes its own key, so two can compete only if they are parallel edges
    // to one target, which `fromTriplets` refuses to build (#57). That is why neither this method
    // nor its callers take a generator: there is nothing for one to do. See #158.
    val topoOrder: Seq[V] = graph.topologicalOrder
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