package com.phasmidsoftware.gryphon.traverse

import com.phasmidsoftware.gryphon.adjunct.{AttributedDirectedEdge, DirectedEdge, UndirectedEdge}
import com.phasmidsoftware.gryphon.core.{Edge, Traversable}
import com.phasmidsoftware.visitor.core.{*, given}
import scala.collection.mutable
import scala.util.Random

/**
 * A family of graph traversal algorithms unified under a single abstraction.
 *
 * All four classic algorithms — DFS, BFS, Dijkstra, Prim — share the same
 * underlying `Traversal` engine from Visitor V1.3.0. They differ only in:
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
    val result  = Traversal.dfs(start, visitor)
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
    val result  = Traversal.bfs(start, visitor)
    VertexTraversalResult(
      result.result.iterator.collect { case (v, Some(r)) => v -> r }.toMap
    )

// ============================================================
// Dijkstra
// ============================================================

/**
 * Dijkstra's shortest-path traversal using a tuple `(E, V)` frontier.
 *
 * The frontier element is `(accumulatedCost, vertex)`. `Ordering[(E, V)]` orders
 * by cost only, so the cheapest-known vertex is always settled next.
 * Cost accumulation lives in `Neighbours[(E,V),(E,V)]` — no mutable cost map
 * drives the ordering. A mutable `pred` map records the cheapest incoming edge
 * per vertex; `CostUpdate` calls `decreaseKey` whenever a cheaper path is found.
 *
 * The external API is unchanged: `TraversalResult[V, DirectedEdge[E, V]]`.
 *
 * @tparam V the vertex type.
 * @tparam E the edge-weight type; must be Numeric and Ordering.
 */
case class DijkstraTraversal[V, E: {Numeric, Ordering}]() extends GraphTraversal[V, E, DirectedEdge[E, V]]:

  def run(graph: Traversable[V])(start: V)(using random: Random = Random()): TraversalResult[V, DirectedEdge[E, V]] =
    val en = summon[Numeric[E]]

    // pred: the cheapest known incoming edge for each settled or frontier vertex.
    // bestCost: current best known cost to reach each frontier vertex — used by
    // CostUpdate to locate the old (stale) frontier entry for decreaseKey.
    val pred:     mutable.Map[V, AttributedDirectedEdge[E, V]] = mutable.Map.empty
    val bestCost: mutable.Map[V, E]                            = mutable.Map(start -> en.zero)

    // Ordering: compare by cost component only.
    given Ordering[(E, V)] = Ordering.by(_._1)

    // Evaluable: when (cost, v) is settled, return its predecessor edge.
    given Evaluable[(E, V), AttributedDirectedEdge[E, V]] with
      def evaluate(ev: (E, V)): Option[AttributedDirectedEdge[E, V]] = pred.get(ev._2)

    // Neighbours: pure — expand (accCost, v) into (accCost + edgeWeight, neighbour) pairs.
    // No side effects; all bookkeeping is owned by CostUpdate.
    given Neighbours[(E, V), (E, V)] with
      def neighbours(ev: (E, V)): Iterator[(E, V)] =
        // Note: accCost must be extracted strictly via ev._1 rather than
        // pattern matching `val (accCost, v) = ev`. The pattern match can
        // generate a lazy binding which causes accCost to be captured
        // incorrectly inside the subsequent map lambda.
        val accCost: E = ev._1
        val v: V = ev._2
        graph.filteredAdjacencies(_ => true)(v)
                .flatMap(_.maybeEdge[E])
                .collect { case e: AttributedDirectedEdge[E, V] => e }
                .map(e => (en.plus(accCost, e.attribute), e.black))

    // CostUpdate: after settling (cost, v), re-check each neighbour.
    // Owns all writes to bestCost and pred.
    // - First time seeing w (not in bestCost): record cost and pred.
    // - Cheaper path to a frontier vertex: decreaseKey and update records.
    // - Already settled or no improvement: no-op.
    given CostUpdate[(E, V), IndexedPrioQueue] with
      def update(frontier: IndexedPrioQueue[(E, V)], ev: (E, V)): IndexedPrioQueue[(E, V)] =
        //        println(s"CostUpdate called for $ev, frontier size=${frontier.size}")
        val (accCost, v) = ev
        graph.filteredAdjacencies(_ => true)(v)
                .flatMap(_.maybeEdge[E])
                .collect { case e: AttributedDirectedEdge[E, V] => e }
                .foldLeft(frontier) { (pq, e) =>
                  val newCost = en.plus(accCost, e.attribute)
                  val w = e.black
                  bestCost.get(w) match
                    case None =>
//                      println(s"firstDiscovery: ($newCost,$w)")
                      bestCost(w) = newCost
                      pred(w) = e
                      pq
                    case Some(oldCost) if en.lt(newCost, oldCost) && pq.contains((oldCost, w)) =>
//                      println(s"decreaseKey: ($oldCost,$w) -> ($newCost,$w)")
                      bestCost(w) = newCost
                      pred(w) = e
                      pq.decreaseKey((oldCost, w), (newCost, w))
                    case _ => pq
                }

    given IndexedPrioQueue[(E, V)] = IndexedPrioQueue.empty[(E, V)]

    val visitor: Visitor[(E, V), AttributedDirectedEdge[E, V], QueueJournal[((E, V), Option[AttributedDirectedEdge[E, V]])]] =
      JournaledVisitor.withQueueJournal[(E, V), AttributedDirectedEdge[E, V]]

//    val testPQ = IndexedPrioQueue.empty[(E, V)]
//    val testPQ2 = testPQ.offer((8.0.asInstanceOf[E], 7.asInstanceOf[V]))
//            .offer((5.0.asInstanceOf[E], 1.asInstanceOf[V]))
//            .offer((9.0.asInstanceOf[E], 4.asInstanceOf[V]))
//    val (a, pq1) = testPQ2.take
//    val (b, pq2) = pq1.take
//    val (c, _) = pq2.take

    val result  = Traversal.bestFirstWeighted((en.zero, start), visitor)


    // Unwrap tuple keys; source vertex has no predecessor so it's filtered by collect.
    VertexTraversalResult(
      result.result.iterator.collect { case ((_, v), Some(e)) => v -> e }.toMap
    )

// ============================================================
// Prim
// ============================================================

/**
 * Prim's minimum spanning tree traversal using a tuple `(E, V)` frontier.
 *
 * The frontier element is `(edgeWeight, vertex)` — not cumulative cost.
 * `Ordering[(E, V)]` orders by edge weight only, so the cheapest available
 * edge to any unvisited vertex is always chosen next.
 * A mutable `pred` map records the cheapest incoming edge per frontier vertex;
 * `CostUpdate` calls `decreaseKey` whenever a cheaper edge to a frontier vertex
 * is found.
 *
 * The external API is unchanged: `TraversalResult[V, Edge[E, V]]`.
 *
 * @tparam V the vertex type.
 * @tparam E the edge-weight type; must be Numeric and Ordering.
 */
case class PrimTraversal[V, E: {Numeric, Ordering}]() extends GraphTraversal[V, E, Edge[E, V]]:

  def run(graph: Traversable[V])(start: V)(using random: Random = Random()): TraversalResult[V, Edge[E, V]] =
    val en = summon[Numeric[E]]

    // pred: cheapest known incoming edge for each frontier/settled vertex.
    // bestCost: current best edge weight to each frontier vertex.
    val pred:     mutable.Map[V, Edge[E, V]] = mutable.Map.empty
    val bestCost: mutable.Map[V, E]          = mutable.Map(start -> en.zero)

    // Ordering: compare by edge weight component only.
    given Ordering[(E, V)] = Ordering.by(_._1)

    // Evaluable: when (weight, v) is settled, return its predecessor edge.
    given Evaluable[(E, V), Edge[E, V]] with
      def evaluate(ev: (E, V)): Option[Edge[E, V]] = pred.get(ev._2)

    // Neighbours: pure — expand (_, v) into (edgeWeight, neighbour) pairs.
    // No side effects; all bookkeeping is owned by CostUpdate.
    given Neighbours[(E, V), (E, V)] with
      def neighbours(ev: (E, V)): Iterator[(E, V)] =
        val weight: E = ev._1
        val v: V = ev._2
        graph.filteredAdjacencies(_ => true)(v)
                .flatMap(_.maybeEdge[E])
                .map { e =>
                  val w = e match
                    case ue: UndirectedEdge[E, V] => ue.other(v)
                    case de => de.black
                  (e.attribute, w)
                }

    // CostUpdate: after settling (weight, v), re-check each neighbour.
    // Owns all writes to bestCost and pred.
    // - First time seeing w (not in bestCost): record weight and pred.
    // - Cheaper edge to a frontier vertex: decreaseKey and update records.
    // - Already settled or no improvement: no-op.
    given CostUpdate[(E, V), IndexedPrioQueue] with
      def update(frontier: IndexedPrioQueue[(E, V)], ev: (E, V)): IndexedPrioQueue[(E, V)] =
        //        println(s"CostUpdate called for $ev, frontier size=${frontier.size}")
        val (_, v) = ev
        graph.filteredAdjacencies(_ => true)(v)
                .flatMap(_.maybeEdge[E])
                .foldLeft(frontier) { (pq, e) =>
                  val w = e match
                    case ue: UndirectedEdge[E, V] => ue.other(v)
                    case de                        => de.black
                  val weight = e.attribute
                  bestCost.get(w) match
                    case None =>
                      // First discovery — Neighbours already offered (weight, w) via offerAll.
                      bestCost(w) = weight
                      pred(w)     = e
                      pq
                    case Some(oldWeight) if en.lt(weight, oldWeight) && pq.contains((oldWeight, w)) =>
                      // Cheaper edge found and w is still in the frontier.
                      bestCost(w) = weight
                      pred(w)     = e
                      pq.decreaseKey((oldWeight, w), (weight, w))
                    case _ => pq
                }

    given IndexedPrioQueue[(E, V)] = IndexedPrioQueue.empty[(E, V)]

    val visitor = JournaledVisitor.withQueueJournal[(E, V), Edge[E, V]]
    // DEBUG
    val debugNbrs = summon[Neighbours[(E, V), (E, V)]]
    //    println(s"Neighbours of (0.0, 0): ${debugNbrs.neighbours((en.zero, start)).toList}")
    val result  = Traversal.bestFirstWeighted((en.zero, start), visitor)

    // Unwrap tuple keys; source vertex has no predecessor so it's filtered by collect.
    VertexTraversalResult(
      result.result.iterator.collect { case ((_, v), Some(e)) => v -> e }.toMap
    )