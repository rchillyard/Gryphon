package com.phasmidsoftware.gryphon.core

import com.phasmidsoftware.visitor.core.{*, given}

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
  def dfs[R, J <: Appendable[(V, Option[R])]](visitor: Visitor[V, R, J], order: DfsOrder = DfsOrder.Pre)(v: V)(using Evaluable[V, R]): Visitor[V, R, J] =
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
  def dfsAll[R, J <: Appendable[(V, Option[R])]](visitor: Visitor[V, R, J])(using Evaluable[V, R]): Visitor[V, R, J] =
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
  def bfs[R, J <: Appendable[(V, Option[R])]](visitor: Visitor[V, R, J])(v: V, goal: V => Boolean = _ => false)(using Evaluable[V, R]): Visitor[V, R, J] =
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
  def bfse[E, R, J <: Appendable[(Edge[E, V], Option[R])]](visitor: Visitor[Edge[E, V], R, J])(v: V)(goal: V => Boolean)(using Evaluable[Edge[E, V], R]): Visitor[Edge[E, V], R, J] =
    vertexMap.bfse(visitor)(v)(goal)

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
    * Retrieves an iterator over all adjacencies in the graph.
    *
    * @return an iterator of `Adjacency[V]` across all vertices.
    */
  def adjacencies: Iterator[Adjacency[V]] = for
    vv <- vertexMap.vertices.iterator
    va <- vv.adjacencies.iterator
  yield va

  /**
    * Filters the adjacencies of a given vertex based on a predicate.
    *
    * @param predicate determines which adjacencies to include.
    * @param v         the vertex whose adjacencies are filtered.
    * @return an iterator of matching adjacencies.
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
  def adjacentVertices(v: V): Iterator[V] = vertexMap.adjacentVertices(v)

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
  def addEdge(edge: Edge[E, V]): EdgeGraph[V, E]