package com.phasmidsoftware.gryphon.traverse

import com.phasmidsoftware.gryphon.adjunct.DirectedGraph
import com.phasmidsoftware.visitor.core.{Traversal, *, given}

/**
 * Represents a topological sort of a directed graph, mapping each vertex to its
 * position in topological order.
 *
 * @param map a mapping of vertices of type `V` to their position (Int) in topological order.
 * @tparam V the type representing a vertex in the graph.
 */
case class TopologicalSort[V](map: Map[V, Int]) extends AbstractVertexTraversalResult[V, Int](map):
  /**
   * Creates a new instance with the provided vertex-to-position mapping.
   *
   * @param m the updated mapping.
   * @return a new `TraversalResult[V, Int]`.
   */
  def unit(m: Map[V, Int]): TraversalResult[V, Int] = copy(map = m)

/**
 * Provides functionality for topological sorting of directed graphs using the
 * Visitor V1.2.0 typeclass engine.
 *
 * Post-order DFS is used: a vertex is recorded only after all its descendants
 * have been recorded, naturally yielding reverse topological order.
 */
object TopologicalSort:

  /**
   * Performs a topological sort on the given directed graph.
   *
   * Uses post-order DFS (`DfsOrder.Post`) via `Traversal.dfs` so that each vertex
   * appears in the journal only after all vertices reachable from it. The resulting
   * list is in reverse post-order (i.e. topological order).
   *
   * Returns `None` if the graph is cyclic.
   *
   * @param graph the directed graph to sort.
   * @tparam V the vertex attribute type.
   * @tparam E the edge attribute type.
   * @return `Some(Seq[V])` in topological order, or `None` if the graph is cyclic.
   */
  def sort[V, E](graph: DirectedGraph[V, E]): Option[Seq[V]] =
    given Evaluable[V, V] with
      def evaluate(v: V): Option[V] = Some(v)

    given GraphNeighbours[V] = (v: V) => graph.adjacentVertices(v)

    // Run post-order DFS over all vertices, seeding each unvisited component in turn.
    // We manage the visited set explicitly so components share it across iterations.
    type J = ListJournal[(V, Option[V])]
    import scala.annotation.tailrec
    val allVertices = graph.vertexMap.keySet

    @tailrec
    def loop(vis: Visitor[V, V, J], visited: Set[V]): Visitor[V, V, J] =
      val unvisited = allVertices.diff(visited)
      if unvisited.isEmpty then vis
      else
        val next = unvisited.head
        val vs: VisitedSet[V] = visited.foldLeft(summon[VisitedSet[V]])(_.markVisited(_))
        val result = Traversal.dfs(next, vis, DfsOrder.Post)(using summon[GraphNeighbours[V]], summon[Evaluable[V, V]], vs)
        val nowVisited = visited ++ result.result.map(_._1)
        loop(result, nowVisited + next)

    val finalVisitor = loop(JournaledVisitor.withListJournal[V, V], Set.empty)
    // ListJournal prepends, so head = last recorded = root in post-order = topological order
    val orderedVertices: List[V] = finalVisitor.result.map(_._1).toList
    Option.when(acyclic(graph, orderedVertices))(orderedVertices)

  /**
   * Computes a `TopologicalSort` mapping from the given directed graph, or returns
   * `None` if the graph is cyclic.
   *
   * @param graph the directed graph to sort.
   * @tparam V the vertex attribute type.
   * @tparam E the edge attribute type.
   * @return `Some(TopologicalSort[V])` with each vertex mapped to its position,
   *         or `None` if the graph is cyclic.
   */
  def traversal[V, E](graph: DirectedGraph[V, E]): Option[TopologicalSort[V]] =
    sort(graph).map { topologicalOrder =>
      val vertexOrder: Seq[(V, Int)] = topologicalOrder.zipWithIndex.map((v, i) => v -> i)
      TopologicalSort(vertexOrder.toMap)
    }

  /**
   * Determines whether a directed graph is acyclic given a proposed topological order.
   *
   * For every directed edge (white → black), white must appear no later than black
   * in the topological order. If this holds for all edges the graph is acyclic.
   *
   * @param graph           the directed graph to check.
   * @param topologicalSort the proposed vertex ordering.
   * @return `true` if acyclic, `false` if cyclic.
   */
  private def acyclic[V, E](graph: DirectedGraph[V, E], topologicalSort: List[V]): Boolean =
    graph.edges.forall { edge =>
      val pos1 = topologicalSort.indexOf(edge.white)
      val pos2 = topologicalSort.indexOf(edge.black)
      pos1 <= pos2
    }