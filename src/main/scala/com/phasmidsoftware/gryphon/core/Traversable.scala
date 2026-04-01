/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.core

import com.phasmidsoftware.gryphon.traverse.{Connexions, TraversalResult, VertexTraversalResult}
import com.phasmidsoftware.visitor.core.{Traversal, *, given}
import scala.util.{Random, Try}

/**
 * Trait to define the behavior of a graph-like structure which can be traversed by dfs, dfsAll, and bfs.
 *
 * The traversal engine is provided by Visitor V1.2.0. The mutable `discovered` flag pattern
 * has been replaced by the immutable `VisitedSet[V]` typeclass. Callers continue to pass a
 * `Visitor` in and receive an updated `Visitor` back; the new typeclass machinery is an
 * implementation detail.
 *
 * @tparam V the underlying key (attribute) type for a vertex.
 */
trait Traversable[V] {

  /**
   * Returns the number of vertices in the graph.
   *
   * @return the number of vertices in the graph.
   */
  def N: Int

  /**
   * Returns the number of edges in the graph.
   *
   * @return the number of edges in the graph.
   */
  def M: Int

  /**
   * Retrieves the vertex associated with the given key.
   *
   * @param key the key of type V for which the associated vertex is to be retrieved.
   * @return an Option containing the associated Vertex if found, or None if not.
   */
  def get(key: V): Option[Vertex[V]]

  /**
   * Retrieves the adjacent vertices connected to the specified vertex.
   *
   * @param v the vertex whose adjacent vertices are to be returned.
   * @return an iterator over the vertices adjacent to the specified vertex.
   */
  def adjacentVertices(v: V)(using random: Random = Random()): Iterator[V]

  /**
   * Filters the adjacent vertices of a given vertex based on a specified predicate.
   *
   * @param predicate a function that evaluates each adjacent vertex and returns true
   *                  if the vertex satisfies the specified condition.
   *
   * @param v         the vertex whose adjacent vertices are to be filtered.
   * @return an iterator over the vertices that are adjacent to v and satisfy the predicate.
   */
  def filteredAdjacentVertices(predicate: V => Boolean)(v: V)(using random: Random = Random()): Iterator[V] =
    adjacentVertices(v).filter(predicate)

  /**
   * Filters the adjacencies of a given vertex based on a specified predicate.
   *
   * @param predicate a function that evaluates each `Adjacency[V]`
   *                  and returns true if the adjacency satisfies the specified condition.
   * @param v         the vertex whose adjacencies are to be filtered.
   * @return an iterator over the adjacencies of the given vertex that satisfy the predicate.
   */
  def filteredAdjacencies(predicate: Adjacency[V] => Boolean)(v: V)(using random: Random = Random()): Iterator[Adjacency[V]]

  /**
   * Performs a depth-first search (DFS) traversal starting from the specified vertex.
   *
   * Internally uses `Traversal.dfs` from Visitor V1.2.0.  A `given GraphNeighbours[V]`
   * is derived from `adjacentVertices` and a `given Evaluable[V, R]` must be in scope
   * at the call site (or provided implicitly by the implementation).
   *
   * @param visitor the visitor, accumulating results into its journal.
   * @param v       the starting vertex.
   * @tparam R the result type extracted from each node by `Evaluable`.
   * @tparam J the journal type.
   * @return the updated visitor after traversal.
   */
  def dfs[R, J <: Appendable[(V, Option[R])]](visitor: Visitor[V, R, J], order: DfsOrder = DfsOrder.Pre)(v: V)(using ev: Evaluable[V, R], random: Random = Random()): Visitor[V, R, J]

  /**
   * Performs a DFS traversal for all vertices in the graph using the supplied visitor.
   * Vertices not reachable from the first traversal are visited in subsequent passes.
   *
   * @param visitor the visitor, accumulating results into its journal.
   * @tparam R the result type extracted from each node.
   * @tparam J the journal type.
   * @return the updated visitor after traversing all vertices.
   */
  def dfsAll[R, J <: Appendable[(V, Option[R])]](visitor: Visitor[V, R, J])(using ev: Evaluable[V, R], random: Random = Random()): Visitor[V, R, J]

  /**
   * Performs a breadth-first search (BFS) traversal starting from the specified vertex.
   *
   * @param visitor the visitor, accumulating results into its journal.
   * @param v       the starting vertex.
   * @param goal    an optional early-termination predicate; traversal halts after recording
   *                the first node for which `goal` returns true.
   *
   * @tparam R the result type extracted from each node.
   * @tparam J the journal type.
   * @return the updated visitor after traversal.
   */
  def bfs[R, J <: Appendable[(V, Option[R])]](visitor: Visitor[V, R, J])(v: V, goal: V => Boolean = _ => false)(using ev: Evaluable[V, R], random: Random = Random()): Visitor[V, R, J]

  /**
   * Performs a BFS traversal visiting edges rather than vertices, starting from `v`.
   * Traversal stops when `goal` returns true for a destination vertex.
   *
   * @param visitor the visitor responsible for processing edges.
   * @param v       the starting vertex.
   * @param goal    early-termination predicate on destination vertices.
   * @tparam E the type of the edge attribute.
   * @tparam R the result type.
   * @tparam J the journal type.
   * @return the visitor after traversal.
   */
  def bfse[E, R, J <: Appendable[(Edge[E, V], Option[R])]](visitor: Visitor[Edge[E, V], R, J])(v: V)(goal: V => Boolean)(using ev: Evaluable[Edge[E, V], R], random: Random = Random()): Visitor[Edge[E, V], R, J]

  /**
   * Performs a DFS traversal applying `f` to each visited vertex and returns a
   * `VertexTraversalResult` mapping each vertex to its result.
   *
   * @param f     transformation function applied to each vertex.
   * @param start the starting vertex.
   * @tparam T the result type produced by `f`.
   * @return `Try[TraversalResult[V, T]]` containing the traversal result or a failure.
   */
  def vertexMappedTraversalDfs[T](f: V => T)(start: V)(using random: Random = Random()): Try[TraversalResult[V, T]] = {
    given Evaluable[V, T] with
      def evaluate(v: V): Option[T] = Some(f(v))

    given GraphNeighbours[V] = graphNeighbours

    val visitor = JournaledVisitor.withQueueJournal[V, T]
    val result = Traversal.dfs(start, visitor)
    Try {
      result.result.foldLeft(VertexTraversalResult.empty[V, T]) {
        case (acc, (v, Some(r))) =>
          acc + (v, r)
        case (acc, _) =>
          acc
      }
    }
  }

  /**
   * Performs a BFS traversal applying `fulfill` to each visited vertex and returns a
   * `VertexTraversalResult` mapping each vertex to its result.
   *
   * @param fulfill transformation function applied to each vertex.
   * @param start   the starting vertex.
   * @tparam T the result type produced by `fulfill`.
   * @return `Try[TraversalResult[V, T]]` containing the traversal result or a failure.
   */
  def vertexMappedTraversalBfs[T](fulfill: V => T)(start: V)(using random: Random = Random()): Try[TraversalResult[V, T]] = {
    given Evaluable[V, T] with
      def evaluate(v: V): Option[T] = Some(fulfill(v))

    given GraphNeighbours[V] = graphNeighbours

    val visitor = JournaledVisitor.withQueueJournal[V, T]
    val result = Traversal.bfs(start, visitor)
    Try {
      result.result.foldLeft(VertexTraversalResult.empty[V, T]) {
        case (acc, (v, Some(r))) =>
          acc + (v, r)
        case (acc, _) =>
          acc
      }
    }
  }

  /**
   * Retrieves the connexions (edges) reachable from `start` via DFS.
   *
   * @param start the starting vertex.
   * @tparam E the type of the edges in the graph.
   * @return a `Connexions[V, E]` object representing the discovered connexions.
   */
  def getConnexions[E](start: V): Connexions[V, E] = {
    // The V1.2.0 engine does not expose parent information through Evaluable,
    // so we run a manual DFS that explicitly tracks the incoming adjacency for
    // each discovered vertex (i.e. the edge used to reach it from its DFS parent).
    import scala.annotation.tailrec
    import scala.collection.mutable

    // Stack holds (child, incomingAdjacency) pairs.
    // start has no incoming adjacency (it is the root).
    val visited = mutable.Set.empty[V]
    val result = mutable.Map.empty[V, Option[Adjacency[V]]]

    @tailrec
    def loop(stack: List[(V, Option[Adjacency[V]])]): Unit = stack match
      case Nil => ()
      case (v, incoming) :: rest =>
        if visited.contains(v) then loop(rest)
        else
          visited += v
          result(v) = incoming
          // Push unvisited neighbours with the adjacency used to reach them.
          val children: List[(V, Option[Adjacency[V]])] =
            filteredAdjacencies(_ => true)(v)
                    .filterNot(a => visited.contains(a.vertex))
                    .map(a => (a.vertex, Some(a)))
                    .toList
          loop(children ::: rest)

    loop(List((start, None)))

    result.foldLeft[Connexions[V, E]](Connexions.empty[V, E]) {
      case (acc, (child, Some(AdjacencyEdge[V, E] (connexion, flipped)))) =>
      // When flipped, the connexion's nominal direction is child->parent,
      // so we reconstruct it as parent->child using the parent recorded in result.
      val effectiveConnexion: Connexion[V] =
        if flipped then VertexPair(connexion.black, child)
        else connexion
      acc.addConnexion(child, effectiveConnexion)
      case (acc, _) =>
        acc
    }
  }

  /**
   * Derives a `GraphNeighbours[V]` instance from `adjacentVertices`.
   * Used internally by the default implementations of `vertexMappedTraversalDfs`,
   * `vertexMappedTraversalBfs`, and `getConnexions`.
   *
   * CONSIDER making this protected again but that requires multiple instances in each type of Traversal.
   *
   * NOTE: do not be tempted to add a default Random implementation for this context.
   * We need to enforce consistency of Random instances.
   */
  def graphNeighbours(using random: Random): GraphNeighbours[V] = new GraphNeighbours[V] {
    def neighbours(v: V): Iterator[V] = adjacentVertices(v)
  }
}

/**
 * A trait representing a traversable structure that encompasses both vertices and edges.
 *
 * @tparam V the type of the vertices.
 * @tparam E the type of the attribute associated with an edge.
 */
trait EdgeTraversable[V, E] extends Traversable[V] {
  /**
   * Retrieves an iterator over all the edges in the graph.
   *
   * @return an `Iterator[Edge[E, V]]` over all edges.
   */
  def edges: Iterator[Edge[E, V]]
}