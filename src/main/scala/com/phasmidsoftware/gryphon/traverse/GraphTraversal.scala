package com.phasmidsoftware.gryphon.traverse

import cats.kernel.Monoid
import com.phasmidsoftware.gryphon.adjunct.{AttributedDirectedEdge, UndirectedEdge}
import com.phasmidsoftware.gryphon.core.{Edge, EvaluableGraphNeighboursTraversal, Traversable}
// Visitor's own Monoid is hidden so that the wildcard does not make it ambiguous with Cats'.
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
// UnweightedTraversal — shared base for DFS and BFS
// ============================================================

/**
 * The visitor used by the unweighted traversals: each visited vertex is journaled
 * together with the value it evaluates to. Named because it appears in the signature
 * of `UnweightedTraversal.search`, which subclasses implement.
 *
 * @tparam V the vertex type.
 * @tparam T the type each visited vertex is mapped to.
 */
type UnweightedVisitor[V, T] = Visitor[V, Mapped[V, T], QueueJournal[(V, Option[T])]]

/**
 * Abstract base class for the unweighted traversals, DFS and BFS.
 *
 * Mirrors [[WeightedTraversal]] for the `E = Unit` half of the family: the
 * `EvaluableGraphNeighboursTraversal` wiring, the journaled visitor and the unwrapping
 * of the result all live here, and a subclass supplies only the engine entry point.
 * DFS and BFS then differ in exactly one expression.
 *
 * This is the single implementation of a mapped unweighted traversal.
 * `Traversable.vertexMappedTraversalDfs` and `vertexMappedTraversalBfs` delegate to it; until
 * 1.11.0 they carried a copy of this body, so Gryphon had two parallel APIs for one operation,
 * one of which had no callers at all and the other only tests. `fulfill` is what the two had
 * that this class lacked, so it moves here rather than the duplication staying.
 *
 * NOTE that the frontier is not named here, unlike in `WeightedTraversal` where the
 * `IndexedPrioQueue` is a `given`. The stack and the queue live inside Visitor's
 * `Traversal.dfs` and `Traversal.bfs`, so for these two the frontier is a consequence
 * of which engine the subclass names rather than something this class can vary.
 *
 * @tparam V the vertex type.
 * @tparam T the type each visited vertex is mapped to.
 * @param fulfill the transformation applied to each visited vertex; `identity` for a
 *                traversal that simply reports the vertices it reached.
 */
abstract class UnweightedTraversal[V, T](fulfill: V => T) extends GraphTraversal[V, Unit, T]:

  /**
   * Runs Visitor's traversal engine from `start`, with the frontier that engine implies.
   *
   * DFS: `Traversal.dfs` — a stack, so the deepest frontier element is expanded next.
   * BFS: `Traversal.bfs` — a queue, so the earliest-discovered element is expanded next.
   *
   * `GraphNeighbours` is supplied by the enclosing `GraphNeighboursTraversal`, which is
   * what carries the caller's `Random` into adjacency ordering; taking it as a context
   * parameter here is what keeps that single source of neighbours for both subclasses.
   *
   * @param start   the starting vertex.
   * @param visitor the journaled visitor to record into.
   * @return the visitor, after the traversal has run.
   */
  protected def search(start: V, visitor: UnweightedVisitor[V, T])(using GraphNeighbours[V]): UnweightedVisitor[V, T]

  def run(graph: Traversable[V])(start: V)(using random: Random = Random()): TraversalResult[V, T] =
    new EvaluableGraphNeighboursTraversal[V, T, TraversalResult[V, T]](fulfill)(graph) {
      def traversal: TraversalResult[V, T] =
        val visitor = JournaledVisitor.withQueueJournal[V, T]
        val result = search(start, visitor)
        VertexTraversalResult(
          result.result.iterator.collect { case (v, Some(r)) => v -> r }.toMap
        )
    }.traversal

// ============================================================
// DFS
// ============================================================

/**
 * Depth-first search traversal, mapping each visited vertex through `fulfill`.
 *
 * `DFSTraversal[V]()` is the identity case, where each visited vertex maps to itself.
 *
 * @tparam V the vertex type.
 * @tparam T the type each visited vertex is mapped to.
 * @param fulfill the transformation applied to each visited vertex.
 */
case class DFSTraversal[V, T](fulfill: V => T) extends UnweightedTraversal[V, T](fulfill):

  protected def search(start: V, visitor: UnweightedVisitor[V, T])(using GraphNeighbours[V]): UnweightedVisitor[V, T] =
    Traversal.dfs(start, visitor)

object DFSTraversal:
  /**
   * The identity traversal: each visited vertex maps to itself.
   */
  def apply[V](): DFSTraversal[V, V] = DFSTraversal(identity)

// ============================================================
// BFS
// ============================================================

/**
 * Breadth-first search traversal, mapping each visited vertex through `fulfill`.
 *
 * `BFSTraversal[V]()` is the identity case, where each visited vertex maps to itself.
 *
 * @tparam V the vertex type.
 * @tparam T the type each visited vertex is mapped to.
 * @param fulfill the transformation applied to each visited vertex.
 */
case class BFSTraversal[V, T](fulfill: V => T) extends UnweightedTraversal[V, T](fulfill):

  protected def search(start: V, visitor: UnweightedVisitor[V, T])(using GraphNeighbours[V]): UnweightedVisitor[V, T] =
    Traversal.bfs(start, visitor)

object BFSTraversal:
  /**
   * The identity traversal: each visited vertex maps to itself.
   */
  def apply[V](): BFSTraversal[V, V] = BFSTraversal(identity)

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
 * The context bound is `E: {Monoid, Ordering}` — both algorithms need a seed
 * cost (`Zero.identity`) and a cost ordering (`Ordering`). Only Dijkstra
 * additionally needs `Monoid.combine` for path cost accumulation; Prim
 * never accumulates costs and so extends this class directly with `E: {Monoid, Ordering}`.
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
abstract class WeightedTraversal[V, E: {Monoid, Ordering}, R <: Edge[V, E]]
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

  /**
   * The order the frontier is kept in.
   *
   * Dijkstra and Prim compare by the cost component alone, which is this default. A* overrides it
   * to compare by `g + h(v)`, while the frontier element still carries the true `g` -- that, and
   * the goal below, are the whole difference between A* and Dijkstra, which is why they are seams
   * here rather than literals in `run`.
   *
   * An override must agree with `Ordering[E]` about which of two entries for the SAME vertex is
   * better, because `CostUpdate` decides improvements by `Ordering[E]` and then calls
   * `decreaseKey` on this queue. A* satisfies that: `h(v)` is constant for a given `v`, so a
   * decrease in `g` is a decrease in `g + h(v)`.
   */
  protected def frontierOrdering: Ordering[(E, V)] = Ordering.by(_._1)

  /**
   * Whether settling `v` ends the traversal.
   *
   * The engine tests this AFTER dequeue and visit, not at discovery, which is what makes it a
   * sound stopping rule: a vertex is settled at its final cost, so stopping there still reports
   * an optimal path to it. Stopping at discovery would not.
   *
   * The default never stops early, so Dijkstra and Prim cover everything reachable.
   */
  protected def isGoal(v: V): Boolean = false

  def run(graph: Traversable[V])(start: V)(using random: Random = Random()): TraversalResult[V, R] =
    val zero = summon[Monoid[E]]

    // pred: cheapest known incoming edge per vertex (typed as R for cast-free access).
    // bestCost: current best frontier cost per vertex — used by CostUpdate to locate
    // the stale frontier entry for decreaseKey.
    // Both maps are owned exclusively by CostUpdate; Neighbours is pure.
    val pred: mutable.Map[V, R] = mutable.Map.empty
    val bestCost: mutable.Map[V, E] = mutable.Map(start -> zero.empty)

    // Ordering: by default the cost component only; A* compares by g + h(v). See frontierOrdering.
    given Ordering[(E, V)] = frontierOrdering

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
                    case _ =>
                      pq
                }

    given IndexedPrioQueue[(E, V)] = IndexedPrioQueue.empty[(E, V)]

    val visitor: Visitor[(E, V), Mapped[(E, V), R], QueueJournal[((E, V), Option[R])]] =
      JournaledVisitor.withQueueJournal[(E, V), R]
    val result = Traversal.bestFirstWeighted((zero.empty, start), visitor, ev => isGoal(ev._2))

    // Unwrap tuple keys; source vertex has no predecessor so it's filtered by collect.
    VertexTraversalResult(
      result.result.iterator.collect { case ((_, v), Some(e)) => v -> e }.toMap
    )

// ============================================================
// Dijkstra
// ============================================================

/**
 * Dijkstra's shortest-path traversal.
 * See [Dijkstra's Algorithm](https://en.wikipedia.org/wiki/Dijkstra%27s_algorithm).
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
/**
 * The shared half of Dijkstra and A*: cumulative cost over directed attributed edges.
 *
 * All three of `WeightedTraversal`'s abstract members are the same for both, and necessarily so
 * rather than by coincidence -- A* is Dijkstra with the frontier ordered by `g + h(v)` and a
 * stopping condition, so anything true of how Dijkstra reads the graph is true of how A* reads
 * it. A change to `filterEdge` here would have to be made twice if these were written out twice.
 *
 * @tparam V the vertex type.
 * @tparam E the edge-weight type; must have `Monoid` and `Ordering`.
 */
abstract class CumulativeDirectedTraversal[V, E: {Monoid, Ordering}]
        extends WeightedTraversal[V, E, AttributedDirectedEdge[V, E]]:

  protected def edgeCost(accCost: E, e: Edge[V, E], v: V): E =
    summon[Monoid[E]].combine(accCost, e.attribute)

  protected def destination(v: V, e: Edge[V, E]): V =
    e.black

  protected def filterEdge(e: Edge[V, E]): Option[AttributedDirectedEdge[V, E]] = e match
    case ade: AttributedDirectedEdge[V, E] => Some(ade)
    case _ => None

case class DijkstraTraversal[V, E: {Monoid, Ordering}]()
        extends CumulativeDirectedTraversal[V, E]

// ============================================================
// A*
// ============================================================

/**
 * The A* shortest-path traversal: Dijkstra, guided towards `goal` by `heuristic`.
 * See [A* search algorithm](https://en.wikipedia.org/wiki/A*_search_algorithm).
 *
 * Dijkstra settles vertices in order of `g`, the cost from the start. A* settles them in order of
 * `f = g + h(v)`, where `h` estimates the cost remaining to the goal, so the frontier grows
 * towards the goal instead of outwards in all directions. Both return the same optimal path; A*
 * usually reaches it having settled fewer vertices. Measured in `AStarSpec` on a 12x12 grid with
 * an expensive wall: '''62 vertices settled with a Manhattan heuristic against 126 with none''',
 * for the same optimal cost. The comparison there is A* against A*, both stopping at the same
 * goal, because `dijkstra` has no goal and would be beaten by any early stop at all.
 *
 * ==The heuristic must be CONSISTENT, not merely admissible==
 *
 * Admissible means `h` never overestimates. Consistent (monotone) is stronger: for every edge
 * `v -> w`, `h(v) <= cost(v, w) + h(w)`.
 *
 * The engine's `VisitedSet` will not re-open a settled vertex. With a consistent heuristic that
 * is correct, because A* never needs to. With an admissible-but-inconsistent one it does need to,
 * '''and this will quietly return a suboptimal path rather than raise anything'''. Supporting
 * inconsistent heuristics would need a re-openable visited set in Visitor, which is a separate
 * question and not one to meet by surprise.
 *
 * Manhattan distance on a grid whose minimum step cost is 1 is consistent. So is the constant
 * zero, which makes A* exactly Dijkstra -- a useful thing to compare against in a test.
 *
 * @param goal      the vertex to stop at, once it is settled.
 * @param heuristic the estimated remaining cost from a vertex to `goal`; must be consistent.
 * @tparam V the vertex type.
 * @tparam E the edge-weight type; must have `Monoid` and `Ordering`.
 */
case class AStarTraversal[V, E: {Monoid, Ordering}](goal: V, heuristic: V => E)
        extends CumulativeDirectedTraversal[V, E]:

  // f = g + h(v). The element keeps the true g, which is what CostUpdate relaxes on; only the
  // comparison adds h. Since h(v) is fixed for a given v, the two agree about improvements.
  override protected def frontierOrdering: Ordering[(E, V)] =
    Ordering.by((gv: (E, V)) => summon[Monoid[E]].combine(gv._1, heuristic(gv._2)))

  override protected def isGoal(v: V): Boolean = v == goal

// ============================================================
// Prim
// ============================================================

/**
 * Prim's minimum spanning tree traversal.
 * See [Prim's Algorithm](https://en.wikipedia.org/wiki/Prim%27s_algorithm).
 *
 * Uses individual edge weight (not cumulative) as the frontier priority, so the
 * cheapest available edge to any unvisited vertex is always chosen next.
 * All edge types are admitted — the result type is `TraversalResult[V, Edge[V, E]]`.
 *
 * Requires only `E: {Monoid, Ordering}` — Prim never accumulates path costs so
 * `Monoid.combine` is not needed. `Zero.identity` seeds the initial frontier cost
 * for the start vertex, which is immediately replaced by actual edge weights.
 *
 * @tparam V the vertex type.
 * @tparam E the edge-weight type; must have Zero and Ordering.
 */
case class PrimTraversal[V, E: {Monoid, Ordering}]()
        extends WeightedTraversal[V, E, Edge[V, E]]:

  protected def edgeCost(accCost: E, e: Edge[V, E], v: V): E =
    e.attribute

  protected def destination(v: V, e: Edge[V, E]): V = e match
    case ue: UndirectedEdge[V, E] => ue.other(v)
    case de => de.black

  protected def filterEdge(e: Edge[V, E]): Option[Edge[V, E]] =
    Some(e)