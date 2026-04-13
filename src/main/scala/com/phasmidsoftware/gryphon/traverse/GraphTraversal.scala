package com.phasmidsoftware.gryphon.traverse

import com.phasmidsoftware.gryphon.adjunct.{AttributedDirectedEdge, UndirectedEdge}
import com.phasmidsoftware.gryphon.core.{Edge, Traversable}
import com.phasmidsoftware.visitor.core.{*, given}
import scala.collection.mutable
import scala.util.Random

/**
 * A family of graph traversal algorithms unified under a single abstraction.
 *
 * All four classic algorithms — DFS, BFS, Dijkstra, Prim — share the same
 * underlying `Traversal` engine from Visitor. They differ only in:
 *   - the frontier type (Stack, Queue, IndexedPrioQueue)
 *   - the frontier element type (V for DFS/BFS, (E,V) for Dijkstra/Prim)
 *   - the cost-update strategy (none, cumulative, edge-only)
 *
 * @tparam V the vertex type
 * @tparam E the edge-attribute type (Unit for DFS/BFS)
 * @tparam R the result type recorded per vertex
 */
trait GraphTraversal[V, E, R]:

  /**
   * Runs the traversal from `start` on `graph`, returning a `TraversalResult`.
   *
   * @param graph  the graph to traverse.
   * @param start  the starting vertex.
   * @param random controls adjacency ordering.
   * @return a `TraversalResult[V, R]` mapping each visited vertex to its result.
   */
  def run(graph: Traversable[V])(start: V)(using random: Random = Random()): TraversalResult[V, R]

// ============================================================
// DFS
// ============================================================

/**
 * Depth-first search traversal. E = Unit, R = V.
 * Each visited vertex maps to itself in the result.
 *
 * @tparam V the vertex type.
 */
case class DFSTraversal[V]() extends GraphTraversal[V, Unit, V]:

  def run(graph: Traversable[V])(start: V)(using random: Random = Random()): TraversalResult[V, V] =
    given Evaluable[V, V] with
      def evaluate(v: V): Option[V] = Some(v)

    given GraphNeighbours[V] = graph.graphNeighbours

    val visitor = JournaledVisitor.withQueueJournal[V, V]
    val result = Traversal.dfs(start, visitor)
    VertexTraversalResult(
      result.result.iterator.collect { case (v, Some(r)) => v -> r }.toMap
    )

// ============================================================
// BFS
// ============================================================

/**
 * Breadth-first search traversal. E = Unit, R = V.
 * Each visited vertex maps to itself in the result.
 *
 * @tparam V the vertex type.
 */
case class BFSTraversal[V]() extends GraphTraversal[V, Unit, V]:

  def run(graph: Traversable[V])(start: V)(using random: Random = Random()): TraversalResult[V, V] =
    given Evaluable[V, V] with
      def evaluate(v: V): Option[V] = Some(v)

    given GraphNeighbours[V] = graph.graphNeighbours

    val visitor = JournaledVisitor.withQueueJournal[V, V]
    val result = Traversal.bfs(start, visitor)
    VertexTraversalResult(
      result.result.iterator.collect { case (v, Some(r)) => v -> r }.toMap
    )

// ============================================================
// WeightedTraversal — shared base for Dijkstra and Prim
// ============================================================

/**
 * Abstract base class for weighted graph traversals using a tuple `(E, V)` frontier.
 *
 * Encapsulates the shared structure of Dijkstra and Prim: both use an
 * `IndexedPrioQueue[(E, V)]` frontier, `CostUpdate` with `decreaseKey`, and
 * mutable `pred`/`bestCost` maps owned exclusively by `CostUpdate`.
 *
 * The context bound is `E: {Zero, Ordering}` — both algorithms need a seed
 * cost (`Zero.identity`) and a cost ordering (`Ordering`). Only Dijkstra
 * additionally needs `Monoid.combine` for path cost accumulation; Prim
 * never accumulates costs and so extends this class directly with `E: {Zero, Ordering}`.
 * `DijkstraTraversal` extends this with the additional `Monoid` bound.
 *
 * Concrete subclasses differ in exactly three ways:
 *   - `edgeCost` — how the frontier cost is computed from an edge:
 *     cumulative (`Monoid.combine`) for Dijkstra; edge weight only for Prim.
 *   - `destination` — how the destination vertex is extracted from an edge:
 *     always `e.black` for directed; `e.other(v)` for undirected.
 *   - `filterEdge` — which edges are admitted and what concrete `R` type they have:
 *     only `AttributedDirectedEdge` for Dijkstra; all edges for Prim.
 *
 * NOTE on lazy evaluation: `ev._1` and `ev._2` must be extracted via strict `val`
 * with explicit type ascriptions inside `Neighbours`. Using tuple pattern matching
 * `val (accCost, v) = ev` can generate a lazy binding that captures incorrectly
 * when the resulting `Iterator` is consumed lazily, causing `edgeCost` to receive
 * `zero` instead of the actual accumulated cost (a Heisenbug).
 *
 * @tparam V the vertex type.
 * @tparam E the edge-weight type; must have Zero and Ordering.
 * @tparam R the result type; must be a subtype of Edge[V, E].
 */
abstract class WeightedTraversal[V, E: {Zero, Ordering}, R <: Edge[V, E]]
        extends GraphTraversal[V, E, R]:

  /**
   * Computes the frontier cost for a neighbour reachable via `e` from a node
   * settled at `accCost`.
   *
   * Dijkstra: `Monoid[E].combine(accCost, e.attribute)` — cumulative path cost.
   * Prim:     `e.attribute`                              — edge weight only.
   */
  protected def edgeCost(accCost: E, e: Edge[V, E], v: V): E

  /**
   * Extracts the destination vertex from edge `e`, viewed from vertex `v`.
   *
   * Dijkstra: always `e.black`.
   * Prim:     `e.other(v)` for undirected edges, `e.black` for directed.
   */
  protected def destination(v: V, e: Edge[V, E]): V

  /**
   * Admits an edge and casts it to the concrete result type `R`, or rejects it.
   *
   * Dijkstra: admits only `AttributedDirectedEdge` instances.
   * Prim:     admits all edges.
   */
  protected def filterEdge(e: Edge[V, E]): Option[R]

  def run(graph: Traversable[V])(start: V)(using random: Random = Random()): TraversalResult[V, R] =
    val zero = summon[Zero[E]]

    // pred: cheapest known incoming edge per vertex (typed as R for cast-free access).
    // bestCost: current best frontier cost per vertex — used by CostUpdate to locate
    // the stale frontier entry for decreaseKey.
    // Both maps are owned exclusively by CostUpdate; Neighbours is pure.
    val pred: mutable.Map[V, R] = mutable.Map.empty
    val bestCost: mutable.Map[V, E] = mutable.Map(start -> zero.identity)

    // Ordering: compare by cost component only.
    given Ordering[(E, V)] = Ordering.by(_._1)

    // Evaluable: when (cost, v) is settled, return its predecessor edge.
    given Evaluable[(E, V), R] with
      def evaluate(ev: (E, V)): Option[R] = pred.get(ev._2)

    // Neighbours: pure — expand (accCost, v) into (newCost, neighbour) pairs.
    // No side effects; all bookkeeping is owned by CostUpdate.
    // IMPORTANT: use ev._1 / ev._2, not pattern matching, to avoid lazy binding bug.
    given Neighbours[(E, V), (E, V)] with
      def neighbours(ev: (E, V)): Iterator[(E, V)] =
        val accCost: E = ev._1
        val v: V = ev._2
        graph.filteredAdjacencies(_ => true)(v)
                .flatMap(_.maybeEdge[E])
                .flatMap(filterEdge)
                .map(e => (edgeCost(accCost, e, v), destination(v, e)))

    // CostUpdate: after settling (cost, v), re-check each neighbour.
    // Owns all writes to bestCost and pred.
    //   - None:         first discovery — record cost and pred.
    //   - Some(oldCost) with improvement and w in frontier — decreaseKey.
    //   - otherwise:    no-op.
    given CostUpdate[(E, V), IndexedPrioQueue] with
      def update(frontier: IndexedPrioQueue[(E, V)], ev: (E, V)): IndexedPrioQueue[(E, V)] =
        val accCost: E = ev._1
        val v: V = ev._2
        graph.filteredAdjacencies(_ => true)(v)
                .flatMap(_.maybeEdge[E])
                .flatMap(filterEdge)
                .foldLeft(frontier) { (pq, e) =>
                  val newCost = edgeCost(accCost, e, v)
                  val w = destination(v, e)
                  bestCost.get(w) match
                    case None =>
                      bestCost(w) = newCost
                      pred(w) = e
                      pq
                    case Some(oldCost) if summon[Ordering[E]].lt(newCost, oldCost) && pq.contains((oldCost, w)) =>
                      bestCost(w) = newCost
                      pred(w) = e
                      pq.decreaseKey((oldCost, w), (newCost, w))
                    case _ => pq
                }

    given IndexedPrioQueue[(E, V)] = IndexedPrioQueue.empty[(E, V)]

    val visitor: Visitor[(E, V), R, QueueJournal[((E, V), Option[R])]] =
      JournaledVisitor.withQueueJournal[(E, V), R]
    val result = Traversal.bestFirstWeighted((zero.identity, start), visitor)

    // Unwrap tuple keys; source vertex has no predecessor so it's filtered by collect.
    VertexTraversalResult(
      result.result.iterator.collect { case ((_, v), Some(e)) => v -> e }.toMap
    )

// ============================================================
// Dijkstra
// ============================================================

/**
 * Dijkstra's shortest-path traversal.
 *
 * Uses cumulative path cost as the frontier priority. Only `AttributedDirectedEdge`
 * instances are admitted — the result type is `TraversalResult[V, AttributedDirectedEdge[V, E]]`,
 * preserving full edge information without casts.
 *
 * Requires `E: {Monoid, Ordering}` — `Monoid` extends `Zero` so the `Zero`
 * requirement of `WeightedTraversal` is automatically satisfied, and `combine`
 * is additionally available for cumulative path cost computation.
 *
 * @tparam V the vertex type.
 * @tparam E the edge-weight type; must have Monoid and Ordering.
 */
case class DijkstraTraversal[V, E: {Monoid, Ordering}]()
        extends WeightedTraversal[V, E, AttributedDirectedEdge[V, E]]:

  protected def edgeCost(accCost: E, e: Edge[V, E], v: V): E =
    summon[Monoid[E]].combine(accCost, e.attribute)

  protected def destination(v: V, e: Edge[V, E]): V =
    e.black

  protected def filterEdge(e: Edge[V, E]): Option[AttributedDirectedEdge[V, E]] = e match
    case ade: AttributedDirectedEdge[V, E] => Some(ade)
    case _ => None

// ============================================================
// Prim
// ============================================================

/**
 * Prim's minimum spanning tree traversal.
 *
 * Uses individual edge weight (not cumulative) as the frontier priority, so the
 * cheapest available edge to any unvisited vertex is always chosen next.
 * All edge types are admitted — the result type is `TraversalResult[V, Edge[V, E]]`.
 *
 * Requires only `E: {Zero, Ordering}` — Prim never accumulates path costs so
 * `Monoid.combine` is not needed. `Zero.identity` seeds the initial frontier cost
 * for the start vertex, which is immediately replaced by actual edge weights.
 *
 * @tparam V the vertex type.
 * @tparam E the edge-weight type; must have Zero and Ordering.
 */
case class PrimTraversal[V, E: {Zero, Ordering}]()
        extends WeightedTraversal[V, E, Edge[V, E]]:

  protected def edgeCost(accCost: E, e: Edge[V, E], v: V): E =
    e.attribute

  protected def destination(v: V, e: Edge[V, E]): V = e match
    case ue: UndirectedEdge[V, E] => ue.other(v)
    case de => de.black

  protected def filterEdge(e: Edge[V, E]): Option[Edge[V, E]] =
    Some(e)