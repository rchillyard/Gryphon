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
   * The *order* of the graph: the number of vertices.
   *
   * @return the number of vertices in the graph.
   */
  def order: Int

  /**
   * @return the number of vertices in the graph.
   * @deprecated `order` is the standard graph-theory term for this, and is what the Java façade
   *             has always called it. A single capital letter also reads as a type parameter in
   *             Scala, which `N` is not.
   */
  @deprecated("Use order", "1.9.0")
  def N: Int = order

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
  def filteredAdjacentVertices(predicate: V => Boolean)(v: V): Iterator[V] =
    adjacentVertices(v).filter(predicate)

  /**
   * Filters the adjacencies of a given vertex based on a specified predicate.
   *
   * @param predicate a function that evaluates each `Adjacency[V]`
   *                  and returns true if the adjacency satisfies the specified condition.
   * @param v         the vertex whose adjacencies are to be filtered.
   * @return an iterator over the adjacencies of the given vertex that satisfy the predicate.
   */
  def filteredAdjacencies(predicate: Adjacency[V] => Boolean)(v: V): Iterator[Adjacency[V]]

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
   * Performs a DFS traversal applying `fulfill` to each visited vertex and returns a
   * `VertexTraversalResult` mapping each vertex to its result.
   *
   * @param fulfill transformation function applied to each vertex.
   * @param start the starting vertex.
   * @tparam T the result type produced by `fulfill`.
   * @return `Try[TraversalResult[V, T]]` containing the traversal result or a failure.
   */
  def vertexMappedTraversalDfs[T](fulfill: V => T)(start: V)(using random: Random = Random()): Try[TraversalResult[V, T]] =
    new EvaluableGraphNeighboursTraversal[V, T, Try[TraversalResult[V, T]]](fulfill)(this) {
      def traversal: Try[TraversalResult[V, T]] = Try {
        val visitor = JournaledVisitor.withQueueJournal[V, T]
        traversalResult(Traversal.dfs(start, visitor))
      }
    }.traversal

  /**
   * Performs a BFS traversal applying `fulfill` to each visited vertex and returns a
   * `VertexTraversalResult` mapping each vertex to its result.
   *
   * @param fulfill transformation function applied to each vertex.
   * @param start   the starting vertex.
   * @tparam T the result type produced by `fulfill`.
   * @return `Try[TraversalResult[V, T]]` containing the traversal result or a failure.
   */
  def vertexMappedTraversalBfs[T](fulfill: V => T)(start: V)(using random: Random = Random()): Try[TraversalResult[V, T]] =
    new EvaluableGraphNeighboursTraversal[V, T, Try[TraversalResult[V, T]]](fulfill)(this) {
      def traversal = Try {
        val visitor = JournaledVisitor.withQueueJournal[V, T]
        traversalResult(Traversal.bfs(start, visitor))
      }
    }.traversal

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

    // Stack holds (child, incomingAdjacency) pairs; `start` has no incoming adjacency, being the
    // root. `visited` and `result` are carried as arguments rather than mutated — the loop was
    // already tail-recursive, so they had only to be threaded through it.
    @tailrec
    def loop(
                    stack: List[(V, Option[Adjacency[V]])],
                    visited: Set[V],
                    result: Map[V, Option[Adjacency[V]]]
            ): Map[V, Option[Adjacency[V]]] = stack match
      case Nil =>
        result
      case (v, _) :: rest if visited.contains(v) =>
        loop(rest, visited, result)
      case (v, incoming) :: rest =>
        val seen = visited + v
        // Push unvisited neighbours with the adjacency used to reach them.
        val children: List[(V, Option[Adjacency[V]])] =
          filteredAdjacencies(_ => true)(v)
                  .filterNot(a => seen.contains(a.vertex))
                  .map(a => (a.vertex, Some(a)))
                  .toList
        loop(children ::: rest, seen, result + (v -> incoming))

    loop(List((start, None)), Set.empty, Map.empty)
            .foldLeft[Connexions[V, E]](Connexions.empty[V, E]) {
      case (acc, (child, Some(AdjacencyEdge(connexion, _)))) =>
      acc.addConnexion(child, connexion)
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
  def graphNeighbours(using random: Random): GraphNeighbours[V] = (v: V) => adjacentVertices(v)

  /**
   * Processes the results of a traversal by aggregating vertex-to-result mappings
   * and returning a complete `VertexTraversalResult` instance.
   *
   * @param visitor The visitor containing the traversal results captured in a journal.
   *                The journal consists of entries where each vertex is optionally
   *                associated with a result of type `T`.
   * @tparam T The result type associated with each vertex in the traversal.
   * @return A `VertexTraversalResult` mapping vertices to their respective results
   *         as processed from the input `result`.
   */
  private def traversalResult[T](visitor: Visitor[V, T, QueueJournal[(V, Option[T])]]) =
    visitor.result.foldLeft(VertexTraversalResult.empty[V, T]) {
      case (acc, (v, Some(r))) =>
        acc + (v, r)
      case (acc, _) =>
        acc
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
   * @return an `Iterator[Edge[V, E]]` over all edges.
   */
  def edges: Iterator[Edge[V, E]]

  /**
   * The *size* of the graph: the number of edges.
   *
   * Declared here rather than on `Traversable`, because counting edges requires knowing what an
   * edge is. `Traversable` does not: a `VertexMap` is one, and has no idea whether an adjacency
   * it holds is half of an undirected edge or the whole of a directed one. Putting the count on
   * `Traversable` is what forced `VertexMap` to carry the mutable `edgeCount` that gave wrong
   * answers until 1.9.0.
   *
   * @return the number of edges in the graph.
   */
  def size: Int

  /**
   * @return the number of edges in the graph.
   * @deprecated `size` is the standard graph-theory term for this, and is what the Java façade
   *             has always called it. See the note on [[Traversable.N]].
   */
  @deprecated("Use size", "1.9.0")
  def M: Int = size
}

/**
 * Represents an abstract class for implementing traversal mechanisms on a given traversable structure.
 * The common feature of subclasses is that they all delegate to the `graphNeighbours` method for the GraphNeighbours typeclass.
 *
 * @tparam V the type of the vertices in the traversable structure
 * @tparam R the type representing the result of the traversal
 * @param traversable the traversable structure containing the vertices
 * @param random      an implicit random number generator used in the traversal logic
 */
abstract class GraphNeighboursTraversal[V, R](traversable: Traversable[V])(using random: Random) {
  given nbrs: GraphNeighbours[V] = traversable.graphNeighbours

  def traversal: R
}

/**
 * An abstract class that extends the functionality of `GraphNeighboursTraversal` by incorporating
 * an evaluable mechanism for the vertices of the graph. This class allows the evaluation of graph
 * vertices to produce a value of type `T` through the provided fulfillment function.
 *
 * @tparam V The type representing a vertex in the graph.
 * @tparam T The type produced when a vertex is evaluated.
 * @tparam R The type representing the result of the traversal operation.
 * @param fulfill     A function that maps a vertex of type `V` to a value of type `T`.
 * @param traversable A traversable collection of vertices of type `V` representing the graph.
 */
abstract class EvaluableGraphNeighboursTraversal[V, T, R](fulfill: V => T)(traversable: Traversable[V])(using random: Random) extends
        GraphNeighboursTraversal[V, R](traversable) {
  given Evaluable[V, T] with
    def evaluate(v: V): Option[T] = Some(fulfill(v))
}