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
 * `aStar` is the odd one out, and is chosen on a different question. The three above are
 * single-source: they answer "how far is everything from `start`". A* answers "how do I get from
 * `start` to '''this''' vertex", and uses a heuristic to avoid settling the rest of the graph on
 * the way. Ask it for one destination; ask `dijkstra` for all of them.
 *
 * `longestAcyclic` is `acyclic` with the ordering reversed, which is the only difference between
 * longest and shortest paths on a DAG.
 *
 * ==What varies==
 *
 * Never the distances. What can differ between runs is '''which''' of several equal-cost paths
 * is reported — see each method's `@note`. `dijkstra`, `aStar` and `bellmanFord` take a `Random`
 * that settles it. `acyclic` and `longestAcyclic` do not, because a generator would have nothing
 * to reach: their answer is decided by the topological order, which is memoized in a `lazy val`.
 * Since 1.15.0 each has an overload '''taking that order''', so a caller who needs a reproducible
 * answer supplies one they computed with their own seed (#158).
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
   * Runs A* from `start`, stopping once `goal` is settled.
   *
   * Dijkstra settles vertices in order of cost from the start and so spreads outwards in every
   * direction. A* settles them in order of `g + heuristic(v)` -- cost so far plus estimated cost
   * remaining -- so the frontier grows towards the goal and usually far fewer vertices are
   * settled before it is reached. Measured in `AStarSpec` on a 12x12 grid with an expensive wall:
   * '''62 vertices settled with a Manhattan heuristic against 126 with none''', for the same
   * optimal cost.
   *
   * ==The heuristic must be consistent==
   *
   * Not merely admissible (never overestimating) but '''consistent''': `h(v) <= cost(v, w) + h(w)`
   * for every edge. The engine will not re-open a settled vertex, which is correct for a
   * consistent heuristic and silently wrong for an inconsistent one -- the path returned may be
   * suboptimal, with nothing raised. See [[AStarTraversal]] for the full reasoning.
   *
   * A constant zero is consistent and turns this into Dijkstra. Manhattan distance on a grid
   * whose minimum step cost is 1 is consistent.
   *
   * ==Reading the answer==
   *
   * The result is a shortest-path tree of the part of the graph A* settled, in the same shape
   * `dijkstra` returns, so `TraversalResult.pathEdgesTo(result)(goal)` reconstructs the path.
   * Unlike `dijkstra` the tree is '''partial''' by design: vertices the search never needed are
   * absent, which is the entire point. Do not read it as the full single-source answer.
   *
   * @param graph     the weighted directed graph.
   * @param start     the source vertex.
   * @param goal      the target vertex.
   * @param heuristic the estimated remaining cost to `goal`; must be consistent.
   * @param random    controls adjacency ordering.
   * @tparam V the vertex type.
   * @tparam E the edge-weight type; must be Monoid and Ordering.
   * @return the shortest-path tree over the settled region, mapping each settled vertex to its
   *         cheapest incoming edge; the start vertex is absent.
   * @note Result: varies with adjacency order; pin a seed with `given Random` to fix it.
   *       The cost of the path to `goal` does not vary; which of several equal-cost paths is
   *       reported does, and so does which other vertices appear in the tree.
   */
  def aStar[V, E: {Monoid, Ordering}](graph: DirectedGraph[V, E], start: V, goal: V, heuristic: V => E)(using random: Random = Random()): TraversalResult[V, AttributedDirectedEdge[V, E]] =
    AStarTraversal[V, E](goal, heuristic).run(graph)(start)

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
  def acyclic[V, E: {Monoid, Ordering}](graph: DirectedGraph[V, E], start: V)(using random: Random = Random()): VertexTraversalResult[V, DirectedEdge[V, E]] =
    AcyclicShortestPaths.shortestPaths(graph, start)(using summon[Monoid[E]], summon[Ordering[E]], random)

  /**
   * Runs the acyclic shortest-paths algorithm, relaxing in the order the caller supplies.
   *
   * The reproducible form. The two-argument method relaxes in `DirectedGraph.topologicalOrder`,
   * a memoized `lazy val` that no seed can reach; this one takes the order as an argument:
   *
   * {{{
   * given Random = Random(42)
   * ShortestPaths.acyclic(dag, 0, TopologicalSort.sort(dag).get)
   * }}}
   *
   * The order is what decides which of several equal-cost predecessors is recorded, so the order
   * is the parameter. A `(using Random)` would not have reached it -- see
   * `AcyclicShortestPaths.shortestPaths` for why that was rejected under #154, and #158 for this.
   *
   * @param graph the directed acyclic graph.
   * @param start the source vertex.
   * @param order a topological order of `graph`, which is validated.
   * @tparam V the vertex type.
   * @tparam E the edge-weight type; must have `Monoid` and `Ordering`.
   * @return the shortest-path tree; the start vertex is absent.
   * @throws IllegalArgumentException if `order` is not a topological order of `graph`.
   * @note Result: a function of `graph`, `start` and `order` -- fully reproducible, which the
   *       two-argument form cannot promise.
   */
  def acyclic[V, E: {Monoid, Ordering}](graph: DirectedGraph[V, E], start: V, order: Seq[V])(using random: Random): VertexTraversalResult[V, DirectedEdge[V, E]] =
    AcyclicShortestPaths.shortestPaths(graph, start, order)(using summon[Monoid[E]], summon[Ordering[E]], random)

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
   * @note Result: as for `acyclic` -- varies between graph instances. Pin it with the
   *       three-argument overload, which takes the topological order (#158), together with the
   *       `Random` that settles the edge order within a vertex (#184).
   */
  def longestAcyclic[V, E: {Monoid, Ordering}](graph: DirectedGraph[V, E], start: V)(using random: Random = Random()): VertexTraversalResult[V, DirectedEdge[V, E]] =
    AcyclicShortestPaths.longestPaths(graph, start)(using summon[Monoid[E]], summon[Ordering[E]], random)

  /**
   * Runs the acyclic LONGEST-paths algorithm, relaxing in the order the caller supplies.
   *
   * The reproducible form, exactly as for `acyclic` above (#158).
   *
   * @param graph the directed acyclic graph.
   * @param start the source vertex.
   * @param order a topological order of `graph`, which is validated.
   * @tparam V the vertex type.
   * @tparam E the edge-weight type; must have `Monoid` and `Ordering`.
   * @return the longest-path tree; the start vertex is absent.
   * @throws IllegalArgumentException if `order` is not a topological order of `graph`.
   * @note Result: a function of `graph`, `start` and `order` -- fully reproducible.
   */
  def longestAcyclic[V, E: {Monoid, Ordering}](graph: DirectedGraph[V, E], start: V, order: Seq[V])(using random: Random): VertexTraversalResult[V, DirectedEdge[V, E]] =
    AcyclicShortestPaths.longestPaths(graph, start, order)(using summon[Monoid[E]], summon[Ordering[E]], random)

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