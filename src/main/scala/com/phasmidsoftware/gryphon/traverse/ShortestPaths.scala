package com.phasmidsoftware.gryphon.traverse

import cats.kernel.Monoid
import com.phasmidsoftware.gryphon.adjunct.{AttributedDirectedEdge, DirectedEdge, DirectedGraph}
import scala.util.Random

/**
 * Shortest paths: the problem, with the algorithms that solve it.
 *
 * ==Which one to use==
 *
 * All of them compute the same distances. They differ in what they assume about the graph, and
 * the right choice is usually the most restrictive one your graph satisfies, since each extra
 * assumption buys speed:
 *
 *  - `acyclic` relaxes once in topological order. Linear, allows negative weights, and requires
 *    a DAG. The fastest when it applies.
 *  - `dijkstra` settles vertices by priority. Requires non-negative weights.
 *  - `bellmanFord` relaxes over a work queue. Allows negative weights and detects a negative
 *    cycle, returning `None`. The most general and the slowest.
 *
 * `longestAcyclic` is `acyclic` with the ordering reversed, which is the only difference between
 * longest and shortest paths on a DAG.
 *
 * ==What varies==
 *
 * Never the distances. What can differ between runs is '''which''' of several equal-cost paths
 * is reported — see each method's `@note`. `dijkstra` and `bellmanFord` take a `Random` that
 * settles it; `acyclic` and `longestAcyclic` cannot be pinned at all, for reasons given there
 * and tracked as #158.
 *
 * ==Why they live here==
 *
 * Until 1.15.0 these were four unrelated top-level objects — `ShortestPaths.dijkstra`, but
 * `BellmanFord.shortestPaths` and `AcyclicShortestPaths.shortestPaths`, the same problem named
 * in opposite directions. A caller who found one learned nothing about the others, and the
 * choice between them, which is the real decision, was nowhere. See #171. The implementations
 * still live in their own files; only the entry point moved.
 */
object ShortestPaths:

  /**
   * Runs Dijkstra's algorithm from `start`, returning the shortest-path tree.
   *
   * @param graph  the weighted directed graph.
   * @param start  the source vertex.
   * @param random controls adjacency ordering.
   * @tparam V the vertex type.
   * @tparam E the edge-weight type; must be Monoid and Ordering.
   * @return a `TraversalResult[V, AttributedDirectedEdge[V, E]]` mapping each settled
   *         vertex to its cheapest incoming edge, or None for the start vertex.
   * @note Result: varies with adjacency order; pin a seed with `given Random` to fix it.
   *       The distances do not vary; which of several equal-cost paths is reported does.
   */
  def dijkstra[V, E: {Monoid, Ordering}](graph: DirectedGraph[V, E], start: V)(using random: Random = Random()): TraversalResult[V, AttributedDirectedEdge[V, E]] =
    DijkstraTraversal[V, E]().run(graph)(start)

  /**
   * Runs the Bellman-Ford-Moore algorithm from `start`, allowing negative edge weights.
   *
   * Unlike `dijkstra` it admits negative weights; unlike `acyclic` it admits cycles. It cannot
   * give an answer when a negative cycle is reachable from `start` — every circuit of such a
   * cycle is cheaper than the last — and reports that by returning `None`.
   *
   * @param graph the weighted directed graph; may contain cycles and negative weights.
   * @param start the source vertex.
   * @param random settles which of several equal-cost predecessors is recorded.
   * @tparam V the vertex type.
   * @tparam E the edge-weight type; must have `Monoid` and `Ordering`.
   * @return the shortest-path tree, or `None` if a negative cycle is reachable from `start`.
   * @note Result: varies with adjacency order; pin a seed with `given Random` to fix it.
   *       The distances do not vary; which of several equal-cost predecessors is recorded does.
   */
  def bellmanFord[V, E: {Monoid, Ordering}](graph: DirectedGraph[V, E], start: V)(using random: Random = Random()): Option[VertexTraversalResult[V, DirectedEdge[V, E]]] =
    BellmanFord.shortestPaths(graph, start)

  /**
   * Runs the acyclic shortest-paths algorithm: one relaxation pass in topological order.
   *
   * Linear in the size of the graph — faster than both `dijkstra` and `bellmanFord` — and
   * negative weights are allowed, a DAG having no cycles for them to make cheaper.
   *
   * @param graph the directed acyclic graph.
   * @param start the source vertex.
   * @tparam V the vertex type.
   * @tparam E the edge-weight type; must have `Monoid` and `Ordering`.
   * @return the shortest-path tree; the start vertex is absent.
   * @throws IllegalArgumentException if `graph` contains a cycle.
   * @note Result: varies between graph instances, and NO seed can pin it -- the relaxation order
   *       comes from the memoized `DirectedGraph.topologicalOrder`, which a `lazy val` cannot
   *       take a generator for. Stable for one graph instance. See #158, and the fuller
   *       explanation on `AcyclicShortestPaths.shortestPaths`.
   */
  def acyclic[V, E: {Monoid, Ordering}](graph: DirectedGraph[V, E], start: V): VertexTraversalResult[V, DirectedEdge[V, E]] =
    AcyclicShortestPaths.shortestPaths(graph, start)

  /**
   * Runs the acyclic LONGEST-paths algorithm, which is `acyclic` with the ordering reversed.
   *
   * The textbook derives this by negating the weights, which is not possible here: `E` carries
   * only `Monoid` and `Ordering`, and neither offers negation. It is also unnecessary --
   * reversing the `Ordering` turns "keep the smaller" into "keep the larger" and changes nothing
   * else. See `AcyclicShortestPaths.longestPaths` and #114.
   *
   * @param graph the directed acyclic graph.
   * @param start the source vertex.
   * @tparam V the vertex type.
   * @tparam E the edge-weight type; must have `Monoid` and `Ordering`.
   * @return the longest-path tree; the start vertex is absent.
   * @throws IllegalArgumentException if `graph` contains a cycle.
   * @note Result: as for `acyclic` -- varies between graph instances and cannot be pinned (#158).
   */
  def longestAcyclic[V, E: {Monoid, Ordering}](graph: DirectedGraph[V, E], start: V): VertexTraversalResult[V, DirectedEdge[V, E]] =
    AcyclicShortestPaths.longestPaths(graph, start)

// undiscoveredEdges and undiscoveredVertices were here, duplicating the methods of the same
// names on Graph with the receiver moved into a parameter. Nothing called them: not src/main,
// not the specs — ShortestPathsSpec exercises Graph's pair — and not the Java façade.
//
// They were published in 1.10.0 and are in the 1.11.0 jars, so removing them is a binary break
// against both: the release carrying this must be 1.12.0, not 1.11.1. The four filters are in
// src/main/mima-filters/1.10.0.backwards.excludes, in a section marked as not belonging to
// 1.11.0. See #106, which also asks whether Graph's surviving pair should be renamed, since
// neither has anything to do with what has been discovered: `filteredAdjacencies(_ => true)`
// filters nothing.