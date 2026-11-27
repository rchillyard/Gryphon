package com.phasmidsoftware.gryphon.traverse

import cats.kernel.Monoid
import com.phasmidsoftware.gryphon.adjunct.UndirectedGraph
import com.phasmidsoftware.gryphon.core.Edge
import scala.util.Random

/**
 * Minimum spanning trees: the problem, with the three algorithms that solve it.
 *
 * ==Which one to use==
 *
 * All three return a spanning tree of minimum total weight, and the total is the same for each.
 * They differ in what they read, which decides both their performance and their reproducibility:
 *
 *  - `prim` walks adjacencies outwards from a start vertex, so it needs one, and it '''varies'''
 *    with adjacency order — pin a seed to settle which of several equal-weight trees you get.
 *  - `kruskal` sorts the edge list and adds edges that join two components. Needs no start
 *    vertex, and is '''invariant''' — measured on a cycle whose weights are all tied.
 *  - `boruvka` works in rounds, each vertex choosing its cheapest outgoing edge. Also needs no
 *    start vertex, also '''invariant''', and the one that parallelises naturally (#75).
 *
 * On a disconnected graph `kruskal` and `boruvka` return the spanning '''forest''', while `prim`
 * returns only the tree containing `start`.
 *
 * ==Why they live here==
 *
 * Until 1.15.0 these were three unrelated top-level objects — `MST.prim`, but `Kruskal.mst` and
 * `Boruvka.mst`, the same problem named in opposite directions. A caller who found one learned
 * nothing about the others, and the choice between them, which is the real decision, was nowhere.
 * See #171. The implementations still live in their own files; only the entry point moved.
 */
object MST:

  /**
   * Runs Prim's algorithm from `start`, returning the MST as a traversal result.
   *
   * Each entry `v → edge` in the result records the cheapest edge connecting
   * `v` to the growing MST. The start vertex is absent (it has no predecessor).
   *
   * @param graph  the weighted undirected graph.
   * @param start  the source vertex.
   * @param random controls adjacency ordering (implicit).
   * @tparam V the vertex type.
   * @tparam E the edge-weight type; must be Monoid and Ordering.
   * @return a `TraversalResult[V, Edge[V, E]]` mapping each MST vertex to its
   *         cheapest incoming edge.
   * @note Result: varies with adjacency order; pin a seed with `given Random` to fix it.
   *       The total weight does not vary -- a minimum spanning tree is minimum -- but which of
   *       several equal-weight trees comes back does.
   */
  def prim[V, E: {Monoid, Ordering}](graph: UndirectedGraph[V, E], start: V)(using random: Random = Random()): TraversalResult[V, Edge[V, E]] =
    PrimTraversal[V, E]().run(graph)(start)

  /**
   * Runs Kruskal's algorithm, returning the MST as an edge list in non-decreasing weight order.
   *
   * Sorts the edges and adds each one whose endpoints are in different components, so it needs
   * no start vertex, and on a disconnected graph it returns the spanning forest.
   *
   * @param graph the weighted undirected graph.
   * @tparam V the vertex type.
   * @tparam E the edge-weight type; must have an `Ordering`.
   * @return the MST edges, in the order Kruskal added them.
   * @note Result: invariant. Measured on a cycle whose weights are ALL tied, where any spanning
   *       subset is an MST: forty seeds, one answer. Kruskal sorts `graph.edges`, which is not
   *       shuffled, and never consults an adjacency iterator -- unlike `prim`, which varies.
   */
  def kruskal[V, E: Ordering](graph: UndirectedGraph[V, E]): AbstractEdgeTraversalResult[Edge[V, E]] =
    Kruskal.mst(graph)

  /**
   * Runs Borůvka's algorithm, returning the MST as an edge list in the order it added them.
   *
   * Proceeds in rounds, each component choosing its cheapest outgoing edge, so it needs no start
   * vertex, and on a disconnected graph it returns the spanning forest.
   *
   * @param graph the weighted undirected graph.
   * @tparam V the vertex type.
   * @tparam E the edge-weight type; must have `Monoid` and `Ordering`.
   * @return the MST edges, in round order.
   * @note Result: invariant. Measured as for `kruskal`, on fully tied weights.
   */
  def boruvka[V, E: {Monoid, Ordering}](graph: UndirectedGraph[V, E]): AbstractEdgeTraversalResult[Edge[V, E]] =
    Boruvka.mst(graph)
