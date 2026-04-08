package com.phasmidsoftware.gryphon.core

import com.phasmidsoftware.gryphon.adjunct.AttributedDirectedEdge
import com.phasmidsoftware.gryphon.core
import com.phasmidsoftware.visitor.core.{*, given}
import scala.util.Random

/**
 * A trait representing an abstract graph structure composed of vertices.
 *
 * All traversal methods delegate to the `VertexMap` which in turn uses the
 * Visitor V1.2.0 typeclass engine (`Traversal.dfs` / `Traversal.bfs`).
 * The mutable `discovered` flag pattern has been removed throughout; visited-node
 * tracking is handled by the immutable `VisitedSet[V]` inside the engine.
 *
 * @tparam V the type representing the vertex attributes (invariant).
 */
trait Graph[V] extends Traversable[V]:

  /**
   * Performs DFS from `v`, accumulating results into `visitor`.
   *
   * @param visitor the visitor accumulating results.
   * @param v       the starting vertex.
   * @tparam R the result type extracted from each node by `Evaluable`.
   * @tparam J the journal type.
   * @return the updated visitor after traversal.
   */
  def dfs[R, J <: Appendable[(V, Option[R])]](visitor: Visitor[V, R, J], order: DfsOrder = DfsOrder.Pre)(v: V)(using ev: Evaluable[V, R], random: Random): Visitor[V, R, J] =
    vertexMap.dfs(visitor, order)(v)

  /**
   * Performs DFS for all vertices in the graph, including those unreachable from any
   * single start vertex.
   *
   * @param visitor the visitor accumulating results.
   * @tparam R the result type.
   * @tparam J the journal type.
   * @return the updated visitor after traversing all vertices.
   */
  def dfsAll[R, J <: Appendable[(V, Option[R])]](visitor: Visitor[V, R, J])(using ev: Evaluable[V, R], random: Random): Visitor[V, R, J] =
    vertexMap.dfsAll(visitor)

  /**
   * Performs BFS from `v`, accumulating results into `visitor`.
   *
   * @param visitor the visitor accumulating results.
   * @param v       the starting vertex.
   * @param goal    early-termination predicate (default: never stop early).
   * @tparam R the result type.
   * @tparam J the journal type.
   * @return the updated visitor after traversal.
   */
  def bfs[R, J <: Appendable[(V, Option[R])]](visitor: Visitor[V, R, J])(v: V, goal: V => Boolean = _ => false)(using ev: Evaluable[V, R], random: Random): Visitor[V, R, J] =
    vertexMap.bfs(visitor)(v, goal)

  /**
   * BFS over edges rather than vertices.
   *
   * @param visitor the visitor processing edges.
   * @param v       the starting vertex.
   * @param goal    early-termination predicate on destination vertices.
   * @tparam E the type of the edge attribute.
   * @tparam R the result type.
   * @tparam J the journal type.
   * @return the visitor after traversal.
   */
  def bfse[E, R, J <: Appendable[(Edge[V, E], Option[R])]](visitor: Visitor[Edge[V, E], R, J])(v: V)(goal: V => Boolean)(using ev: Evaluable[Edge[V, E], R], random: Random): Visitor[Edge[V, E], R, J] =
    vertexMap.bfse(visitor)(v)(goal)

  /**
   * Determines if the graph contains any cycles.
   *
   * @return true if the graph has at least one cycle, false otherwise.
   */
  def isCyclic: Boolean


  /**
   * Returns true if this graph is connected.
   *
   * For undirected graphs: true iff every vertex is reachable from every other.
   * For directed graphs: semantics (strong vs weak connectivity) are
   * implementation-defined and not yet supported — throws UnsupportedOperationException.
   */
  def isConnected: Boolean

  /**
   * Returns true if this graph is bipartite (2-colorable).
   *
   * For undirected graphs: true iff the graph contains no odd-length cycle.
   * For directed graphs: not yet implemented — throws UnsupportedOperationException.
   */
  def isBipartite: Boolean

  /**
   * Retrieves the vertex map representation of the graph.
   *
   * @return the `VertexMap[V]` backing this graph.
   */
  def vertexMap: VertexMap[V]

  /**
   * Adds a new vertex to the graph, returning a new graph instance.
   *
   * @param vertex the vertex to be added.
   * @return a new graph containing the added vertex.
   */
  def addVertex(vertex: Vertex[V]): Graph[V] =
    unit(vertexMap + vertex)

  /**
   * Creates a new graph using the provided vertex map.
   *
   * @param vertexMap the vertex map for the new graph.
   * @return a new graph instance.
   */
  def unit(vertexMap: VertexMap[V]): Graph[V]

  /**
   * Returns the directed edges reachable from v.
   */
  def undiscoveredEdges[E](v: V)(using random: Random = Random()): Seq[Edge[V, E]] =
    filteredAdjacencies(_ => true)(v)
            .flatMap(_.maybeEdge[E])
            .toSeq

  /**
   * Returns the vertices reachable via attributed directed edges from v.
   * Useful for testing and inspection; the traversal engine manages visited
   * state internally via VisitedSet.
   */
  def undiscoveredVertices[E](v: V)(using random: Random = Random()): Seq[V] =
    undiscoveredEdges(v).collect { case e: AttributedDirectedEdge[V, E] => e.black }

  def debug: String = vertexMap.debug

/**
 * Companion object for `Graph`.
 */
object Graph

/**
 * An abstract base class for graph implementations backed by a `VertexMap`.
 *
 * @tparam V the type of the vertex attributes (invariant).
 * @param vertexMap the `VertexMap` holding the vertices of this graph.
 */
abstract class AbstractGraph[V](vertexMap: VertexMap[V]) extends Graph[V]:

  /**
   * Returns the number of vertices in the graph.
   *
   * @return the number of vertices in the graph.
   */
  def N: Int = vertexMap.N

  /**
   * Returns the number of edges in the graph.
   *
   * @return the number of edges in the graph.
   */
  def M: Int = vertexMap.M

  /**
   * Retrieves an iterator over all adjacencies in the graph.
   *
   * @return an iterator of `Adjacency[V]` across all vertices.
   */
  def adjacencies: Iterator[Adjacency[V]] = for
    vv <- vertexMap.vertices.iterator
    va <- vv.adjacencies.iterator
  yield va

  /**
   * Filters the adjacencies of a given vertex based on a specified predicate.
   *
   * @param predicate a function that evaluates each `Adjacency[V]`
   *                  and returns true if the adjacency satisfies the specified condition.
   *
   * @param v         the vertex whose adjacencies are to be filtered.
   * @return an iterator over the adjacencies of the given vertex that satisfy the predicate.
   */
  def filteredAdjacencies(predicate: Adjacency[V] => Boolean)(v: V)(using random: Random): Iterator[Adjacency[V]] =
    vertexMap.filteredAdjacencies(predicate)(v)

  /**
   * Retrieves the vertex associated with the given key.
   *
   * @param key the vertex attribute to look up.
   * @return `Some(vertex)` if found, `None` otherwise.
   */
  def get(key: V): Option[Vertex[V]] = vertexMap.get(key)

  /**
   * Returns an iterator over the vertices adjacent to `v`.
   *
   * @param v the vertex to query.
   * @return an iterator of adjacent vertex attributes.
   */
  def adjacentVertices(v: V)(using random: Random): Iterator[V] = vertexMap.adjacentVertices(v)

/**
 * A trait for graphs that carry both vertices and edges, supporting edge-based operations.
 *
 * @tparam V the type of the vertex attributes.
 * @tparam E the type of the edge attributes.
 */
trait EdgeGraph[V, E] extends Graph[V] with EdgeTraversable[V, E]:

  /**
   * Adds an edge to the graph.
   *
   * @param edge the edge to add.
   * @return a new `EdgeGraph[V, E]` containing the added edge.
   */
  def addEdge(edge: Edge[V, E]): EdgeGraph[V, E]

  /**
   * Returns the number of self-loops in the graph
   * (edges where both endpoints are the same vertex).
   */
  def numberOfSelfLoops: Int =
    edges.count(e => e.white == e.black)
