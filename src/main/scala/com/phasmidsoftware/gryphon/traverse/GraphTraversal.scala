/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.traverse

import com.phasmidsoftware.gryphon.adjunct.{AttributedDirectedEdge, DirectedEdge}
import com.phasmidsoftware.gryphon.core.{Edge, Traversable}
import com.phasmidsoftware.visitor.core.{*, given}
import scala.collection.mutable
import scala.util.Random

/**
 * A family of graph traversal algorithms unified under a single abstraction.
 *
 * All four classic algorithms — DFS, BFS, Dijkstra, Prim — share the same
 * underlying `Traversal` engine from Visitor V1.2.0. They differ only in:
 *   - the frontier type (Stack, Queue, PrioQueue)
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

  /**
   * Converts a raw Visitor journal into a VertexTraversalResult.
   */
  protected def toTraversalResult[J <: Iterable[(V, Option[R])]](visitor: Visitor[V, R, ?] {def result: J}): TraversalResult[V, R] =
    VertexTraversalResult(
      visitor.result.iterator.collect { case (v, Some(r)) => v -> r }.toMap
    )

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
// Dijkstra
// ============================================================

/**
 * Dijkstra's shortest-path traversal.
 * Prioritises by cumulative cost from source.
 * R = DirectedEdge[E, V] — the cheapest incoming edge for each settled vertex.
 *
 * @tparam V the vertex type.
 * @tparam E the edge-weight type; must be Numeric and Ordering.
 */
case class DijkstraTraversal[V, E: {Numeric, Ordering}]() extends GraphTraversal[V, E, DirectedEdge[E, V]]:

  def run(graph: Traversable[V])(start: V)(using random: Random = Random()): TraversalResult[V, DirectedEdge[E, V]] =
    val en = summon[Numeric[E]]
    val cost: mutable.Map[V, E] = mutable.Map(start -> en.zero)
    val pred: mutable.Map[V, DirectedEdge[E, V]] = mutable.Map.empty

    def relax(v: V): Unit =
      val c = cost.getOrElse(v, en.zero)
      for
        adj <- graph.filteredAdjacencies(_ => true)(v)
        e <- adj.maybeEdge[E].collect { case e: AttributedDirectedEdge[E, V] => e }
        newCost = en.plus(c, e.attribute)
        if cost.get(e.black).forall(en.lt(newCost, _))
      do
        cost(e.black) = newCost
        pred(e.black) = e

    relax(start)

    given Ordering[V] = Ordering.by(v => cost.getOrElse(v, en.fromInt(Int.MaxValue)))

    given Evaluable[V, DirectedEdge[E, V]] with
      def evaluate(v: V): Option[DirectedEdge[E, V]] =
        val result = pred.get(v)
        relax(v)
        result

    given GraphNeighbours[V] = graph.graphNeighbours

    val visitor = JournaledVisitor.withQueueJournal[V, DirectedEdge[E, V]]
    val result = Traversal.bestFirst(start, visitor)

    // Source vertex has no predecessor; all others have Some(edge)
    VertexTraversalResult(
      result.result.iterator.collect { case (v, Some(e)) => v -> e }.toMap
    )

// ============================================================
// Prim
// ============================================================

/**
 * Prim's minimum spanning tree traversal.
 * Prioritises by individual edge weight only (ignoring cumulative path cost).
 * R = Edge[E, V] — the cheapest incoming edge for each vertex in the MST.
 *
 * @tparam V the vertex type.
 * @tparam E the edge-weight type; must be Numeric and Ordering.
 */
case class PrimTraversal[V, E: {Numeric, Ordering}]() extends GraphTraversal[V, E, Edge[E, V]]:

  def run(graph: Traversable[V])(start: V)(using random: Random = Random()): TraversalResult[V, Edge[E, V]] =
    val en = summon[Numeric[E]]
    // cost here is just the edge weight — NOT cumulative
    val cost: mutable.Map[V, E] = mutable.Map(start -> en.zero)
    val pred: mutable.Map[V, Edge[E, V]] = mutable.Map.empty

    def relax(v: V): Unit =
      for
        adj <- graph.filteredAdjacencies(_ => true)(v)
        e <- adj.maybeEdge[E].collect { case e: AttributedDirectedEdge[E, V] => e }
        // Prim: cost is edge weight only, not cumulative
        if cost.get(e.black).forall(en.lt(e.attribute, _))
      do
        cost(e.black) = e.attribute
        pred(e.black) = e

    relax(start)

    given Ordering[V] = Ordering.by(v => cost.getOrElse(v, en.fromInt(Int.MaxValue)))

    given Evaluable[V, Edge[E, V]] with
      def evaluate(v: V): Option[Edge[E, V]] =
        val result = pred.get(v)
        relax(v)
        result

    given GraphNeighbours[V] = graph.graphNeighbours

    val visitor = JournaledVisitor.withQueueJournal[V, Edge[E, V]]
    val result = Traversal.bestFirst(start, visitor)

    // Source vertex has no MST edge; all others have Some(edge)
    VertexTraversalResult(
      result.result.iterator.collect { case (v, Some(e)) => v -> e }.toMap
    )
