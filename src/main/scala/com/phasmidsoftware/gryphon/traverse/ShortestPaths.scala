package com.phasmidsoftware.gryphon.traverse

import com.phasmidsoftware.gryphon.adjunct.{AttributedDirectedEdge, DirectedEdge}
import com.phasmidsoftware.gryphon.core
import com.phasmidsoftware.visitor.core.{*, given}
import scala.collection.mutable
import scala.util.Random

/**
  * Computes shortest paths in a weighted directed graph using Dijkstra's algorithm,
  * built on the Visitor V1.2.0 typeclass engine.
  *
  * The traversal node type is V. Cost and predecessor state are maintained in
  * mutable maps updated via Evaluable as each vertex is settled.
  * Traversal.bestFirst drives the priority-queue exploration.
  */
object ShortestPaths:

  /**
    * Runs Dijkstra's algorithm from start, returning the shortest-path tree as a
   * VertexTraversalResult mapping each settled vertex to its incoming edge.
    *
    * @param traversable the weighted directed graph.
    * @param start       the source vertex.
    * @tparam V the vertex type.
    * @tparam E the edge-weight type; must be Numeric and Ordering.
   * @return  a VertexTraversalResult[V, DirectedEdge[E, V]] where each vertex maps to the
    *         cheapest incoming edge, or None for the start vertex itself.
    */
  def dijkstra[V, E: {Numeric, Ordering}](traversable: core.Traversable[V], start: V)(using random: Random = Random()): TraversalResult[V, DirectedEdge[E, V]] =
    val en = implicitly[Numeric[E]]

    // Best known cost and incoming edge for each settled vertex.
    val cost: mutable.Map[V, E] = mutable.Map(start -> en.zero)
    val pred: mutable.Map[V, DirectedEdge[E, V]] = mutable.Map.empty

    // Relax all outgoing attributed directed edges from v.
    def relax(v: V): Unit =
      val c = cost.getOrElse(v, en.zero)
      for
        adj <- traversable.filteredAdjacencies(_ => true)(v)
        e <- adj.maybeEdge[E].collect { case e: AttributedDirectedEdge[E, V] => e }
        newCost = en.plus(c, e.attribute)
        if cost.get(e.black).forall(en.lt(newCost, _))
      do
        cost(e.black) = newCost
        pred(e.black) = e

    // Seed costs for start's neighbours before traversal so the PrioQueue
    // has meaningful ordering from the very first dequeue.
    relax(start)

    // PrioQueue orders vertices by current best cost; unknown vertices sort last.
    given Ordering[V] = Ordering.by(v => cost.getOrElse(v, en.fromInt(Int.MaxValue)))

    // Evaluable: called when v is settled — record pred and relax outgoing edges.
    given Evaluable[V, DirectedEdge[E, V]] with
      def evaluate(v: V): Option[DirectedEdge[E, V]] =
        val result = pred.get(v)
        relax(v)
        result

    given GraphNeighbours[V] = new GraphNeighbours[V]:
      def neighbours(v: V): Iterator[V] = traversable.adjacentVertices(v)

    val result = Traversal.bestFirst(
      start,
      JournaledVisitor.withQueueJournal[V, DirectedEdge[E, V]]
    )

    // start has no predecessor; all other settled vertices have Some(edge).
    VertexTraversalResult(
      result.result.iterator.collect { case (v, Some(edge)) => v -> edge }.toMap
    )
