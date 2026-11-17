package com.phasmidsoftware.gryphon.core

import com.phasmidsoftware.gryphon.adjunct.AttributedDirectedEdge
import com.phasmidsoftware.gryphon.core
import com.phasmidsoftware.visitor.core.*
import scala.annotation.unused
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
  def dfs[R, J <: Appendable[(V, Option[R]), J]](visitor: Visitor[V, Mapped[V, R], J], order: DfsOrder = DfsOrder.Pre)(v: V)(using ev: Evaluable[V, R], random: Random): Visitor[V, Mapped[V, R], J] =
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
  def dfsAll[R, J <: Appendable[(V, Option[R]), J]](visitor: Visitor[V, Mapped[V, R], J])(using ev: Evaluable[V, R], random: Random): Visitor[V, Mapped[V, R], J] =
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
  def bfs[R, J <: Appendable[(V, Option[R]), J]](visitor: Visitor[V, Mapped[V, R], J])(v: V, goal: V => Boolean = _ => false)(using ev: Evaluable[V, R], random: Random): Visitor[V, Mapped[V, R], J] =
    vertexMap.bfs(visitor)(v, goal)


  /**
   * Determines if the graph contains any cycles.
   *
   * @return true if the graph has at least one cycle, false otherwise.
   */
  def isCyclic: Boolean


  // isConnected and isBipartite are deliberately NOT declared here. They are meaningful for an
  // undirected graph and ambiguous for a directed one — connectivity of a digraph is either weak
  // or strong, and this library commits to neither — so they live on UndirectedGraph alone.
  // Until 1.8.0 they were abstract members of this trait and DirectedGraph implemented both as
  // `throw UnsupportedOperationException`, which made a run-time failure out of something the
  // compiler can decide. For a digraph, use ConnectedComponents for weak connectivity and
  // Kosaraju for strong.

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
  def undiscoveredEdges[E](v: V)(using @unused random: Random = Random()): Seq[Edge[V, E]] =
    filteredAdjacencies(_ => true)(v)
            .flatMap(_.maybeEdge[E])
            .toSeq

  /**
   * Returns the vertices reachable via attributed directed edges from v.
   * Useful for testing and inspection; the traversal engine manages visited
   * state internally via VisitedSet.
   */
  def undiscoveredVertices[E](v: V)(using @unused random: Random = Random()): Seq[V] =
    undiscoveredEdges(v).collect { case e: AttributedDirectedEdge[V, E] @unchecked => e.black }

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
   * The order of the graph: the number of vertices, which the vertex map knows.
   *
   * @return the number of vertices in the graph.
   */
  def order: Int = vertexMap.order

  // `size` is deliberately left abstract here. Until 1.9.0 this class defined it (as `M`) to be
  // `vertexMap.M`, which read a mutable counter that `VertexMap.+` incremented on the *receiver*
  // while returning a fresh map whose own count was always 0. Both concrete graphs overrode it
  // with `edges.size`, so nothing user-facing was wrong — but any new graph class would have
  // inherited the broken default. Counting edges needs to know what an edge is, which is
  // `EdgeGraph`'s business, not this class's.

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
   * @param v         the vertex whose adjacencies are to be filtered.
   * @return an iterator over the adjacencies of the given vertex that satisfy the predicate.
   */
  def filteredAdjacencies(predicate: Adjacency[V] => Boolean)(v: V): Iterator[Adjacency[V]] =
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
  /**
   * The kind of edge this graph admits.
   *
   * An undirected graph admits `UndirectedEdge`s and a directed one `DirectedEdge`s. Before 1.8.0
   * `addEdge` took any `Edge`, so either graph would accept either kind — and the mismatch was not
   * rejected but silently absorbed, leaving the graph inconsistent with itself: the adjacency was
   * added, so `degree` counted it, while `edges` and `M`, which select by edge type, did not.
   *
   * Making this a type member rather than narrowing `addEdge`'s parameter in each subclass is what
   * Scala allows: a parameter type may not be narrowed in an override, since that would break
   * substitutability, but an abstract type member may be refined.
   */
  type EdgeT <: Edge[V, E]

  /**
   * Adds an edge, which must be of the kind this graph admits.
   *
   * @param edge the edge to add.
   * @return a graph including `edge`.
   */
  def addEdge(edge: EdgeT): EdgeGraph[V, E]

  /**
   * Returns the number of self-loops in the graph
   * (edges where both endpoints are the same vertex).
   */
  def numberOfSelfLoops: Int =
    edges.count(e => e.white == e.black)

  /**
   * Returns the in-degree of `v`: the number of edges arriving at it.
   *
   * Unlike `degree`, which is defined for undirected graphs only, this is meaningful for both:
   * in an undirected graph every edge is traversable both ways, so the in-degree, the out-degree
   * and the degree coincide.
   *
   * Left abstract rather than given a default, because a default correct for one kind of graph
   * would be silently wrong for the other.
   *
   * @param v the vertex.
   * @return the number of edges arriving at `v`.
   */
  def inDegree(v: V): Int

  /**
   * Returns the out-degree of `v`: the number of edges leaving it.
   * See `inDegree` for why this is abstract and how it behaves on an undirected graph.
   *
   * @param v the vertex.
   * @return the number of edges leaving `v`.
   */
  def outDegree(v: V): Int