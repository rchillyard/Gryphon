/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.core

import com.phasmidsoftware.gryphon.traverse.{BFSTraversal, Connexions, DFSTraversal, TraversalResult}
import com.phasmidsoftware.visitor.core.*
import scala.util.{Random, Try}

/**
 * Trait to define the behavior of a graph-like structure which can be traversed by dfs, dfsAll, and bfs.
 *
 * The traversal engine is provided by Visitor 1.8.0. The mutable `discovered` flag pattern
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
   * This reads each adjacency's own `vertex`, which is the end away from `v`. That matters on
   * undirected graphs: a two-way triplet stores a second, `flipped` adjacency at the far end, and
   * `AdjacencyEdge.vertex` answers `white` rather than `black` for it. Reading `black` off the
   * edge instead would therefore give `v` itself for half the adjacencies of an undirected graph.
   * `Graph.undiscoveredVertices` did exactly that and hid the error behind a filter that admitted
   * only `AttributedDirectedEdge` — the one kind never stored flipped, since `DirectedGraph`
   * passes `bothWays = _ => false` — so it returned `Nil` on undirected graphs rather than wrong
   * answers. It was deleted in 1.12.0 in favour of this method; see issue #106.
   *
   * @param v      the vertex whose adjacent vertices are to be returned.
   * @param random controls adjacency ordering (implicit); see `filteredAdjacencies`.
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
   * @param random    the generator determining the order of the adjacencies. A caller with a
   *                  `given Random` in scope supplies it and gets a reproducible order; the
   *                  default is only reached when nobody has one (#67).
   *
   * @return an iterator over the adjacencies of the given vertex that satisfy the predicate.
   */
  def filteredAdjacencies(predicate: Adjacency[V] => Boolean)(v: V)(using random: Random = Random()): Iterator[Adjacency[V]]

  /**
   * Performs a depth-first search (DFS) traversal starting from the specified vertex.
   *
   * Internally uses `Traversal.dfs` from Visitor 1.8.0.  A `given GraphNeighbours[V]`
   * is derived from `adjacentVertices`. No `Evaluable` is needed: since Visitor 1.8.0 a
   * visitor carries whatever it needs to turn a node into a journal entry.
   *
   * @param visitor the visitor, accumulating results into its journal.
   * @param order   whether a vertex is recorded before or after its descendants; pre-order by default.
   * @param v       the starting vertex.
   * @param random  controls adjacency ordering (implicit); see `filteredAdjacencies`.
   * @tparam E the journal's element type.
   * @tparam J the journal type.
   * @return the updated visitor after traversal.
   */
  def dfs[E, J <: Appendable[E, J]](visitor: Visitor[V, E, J], order: DfsOrder = DfsOrder.Pre)(v: V)(using random: Random = Random()): Visitor[V, E, J]

  /**
   * Performs a DFS traversal for all vertices in the graph using the supplied visitor.
   * Vertices not reachable from the first traversal are visited in subsequent passes.
   *
   * @param visitor the visitor, accumulating results into its journal.
   * @param random  controls adjacency ordering (implicit); see `filteredAdjacencies`.
   * @tparam E the journal's element type.
   * @tparam J the journal type.
   * @return the updated visitor after traversing all vertices.
   */
  def dfsAll[E, J <: Appendable[E, J]](visitor: Visitor[V, E, J])(using random: Random = Random()): Visitor[V, E, J]

  /**
   * Performs a breadth-first search (BFS) traversal starting from the specified vertex.
   *
   * @param visitor the visitor, accumulating results into its journal.
   * @param v       the starting vertex.
   * @param goal    an optional early-termination predicate; traversal halts after recording
   *                the first node for which `goal` returns true.
   *
   * @param random  controls adjacency ordering (implicit); see `filteredAdjacencies`.
   * @tparam E the journal's element type.
   * @tparam J the journal type.
   * @return the updated visitor after traversal.
   */
  def bfs[E, J <: Appendable[E, J]](visitor: Visitor[V, E, J])(v: V, goal: V => Boolean = _ => false)(using random: Random = Random()): Visitor[V, E, J]


  /**
   * Performs a DFS traversal applying `fulfill` to each visited vertex and returns a
   * `VertexTraversalResult` mapping each vertex to its result.
   *
   * @param fulfill transformation function applied to each vertex.
   * @param start   the starting vertex.
   * @param random  controls adjacency ordering (implicit); see `filteredAdjacencies`.
   * @tparam T the result type produced by `fulfill`.
   * @return `Try[TraversalResult[V, T]]` containing the traversal result or a failure.
   */
  def vertexMappedTraversalDfs[T](fulfill: V => T)(start: V)(using random: Random = Random()): Try[TraversalResult[V, T]] =
    Try(DFSTraversal(fulfill).run(this)(start))

  /**
   * Performs a BFS traversal applying `fulfill` to each visited vertex and returns a
   * `VertexTraversalResult` mapping each vertex to its result.
   *
   * @param fulfill transformation function applied to each vertex.
   * @param start   the starting vertex.
   * @param random  controls adjacency ordering (implicit); see `filteredAdjacencies`.
   * @tparam T the result type produced by `fulfill`.
   * @return `Try[TraversalResult[V, T]]` containing the traversal result or a failure.
   */
  def vertexMappedTraversalBfs[T](fulfill: V => T)(start: V)(using random: Random = Random()): Try[TraversalResult[V, T]] =
    Try(BFSTraversal(fulfill).run(this)(start))

  /**
   * Retrieves the connexions (edges) reachable from `start` via DFS.
   *
   * @param start  the starting vertex.
   * @param random the generator determining adjacency order. Which predecessor each vertex
   *               records depends on that order, so a caller wanting a reproducible parent
   *               map must have a `given Random` in scope here (#67).
   *
   * @tparam E the type of the edges in the graph.
   * @return a `Connexions[V, E]` object representing the discovered connexions.
   */
  def getConnexions[E](start: V)(using random: Random = Random()): Connexions[V, E] = {
    // Visitor 1.8.0's ParentedVisitor does expose the predecessor *vertex*, but what this
    // method needs is the incoming *edge*, and Visitor has no notion of an edge — Neighbours
    // yields nodes. So we still run a manual DFS that tracks the incoming adjacency for each
    // discovered vertex (i.e. the edge used to reach it from its DFS parent). Lifting this into
    // the engine would require V to carry the edge; see Gryphon #53 and Visitor #20.
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
   * CONSIDER making this protected again but that requires multiple instances in each type of Traversal. (#93)
   * Blocked on issue #67: the NOTE below is that issue, and the trade-off only looks bad while
   * multiple Random-derived instances are dangerous.
   *
   * NOTE: do not be tempted to add a default Random implementation for this context.
   * We need to enforce consistency of Random instances.
   */
  def graphNeighbours(using random: Random): GraphNeighbours[V] = (v: V) => adjacentVertices(v)

  // traversalResult, which unwrapped a journal into a VertexTraversalResult, is gone with the
  // two bodies that used it: UnweightedTraversal.run does that unwrapping now, once.
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