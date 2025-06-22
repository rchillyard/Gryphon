package com.phasmidsoftware.gryphon.core

import com.phasmidsoftware.gryphon.visit.Visitor

/**
 * A trait representing an abstract graph structure composed of vertices.
 *
 * @tparam V the type representing the attributes associated with the vertices in the graph
 *           V is invariant.
 *
 *           This trait provides a collection of fundamental operations for manipulating and
 *           interacting with graph vertexMap. It allows querying for the set of vertices, adding 
 *           new vertices to the graph, and defining custom behaviors for graph implementations.
 */
trait Graph[V] extends Traversable[V] {

  /**
   * Retrieves the vertex map representation of the graph.
   *
   * @return the vertex map, represented as a `VertexMap[V]`, which provides
   *         the internal mapping of vertices within the graph.
   */
  def vertexMap: VertexMap[V]

  /**
   * Adds a new vertex to the graph and returns a new graph instance with the updated vertex map.
   * NOTE: not used at present.
   * 
   * @param vertex the vertex to be added to the graph.
   *               The vertex contains its attribute and adjacencies, 
   *               which define its connections within the graph.
   * @return a new graph instance containing the existing vertex map and the newly added vertex.
   */
  def addVertex(vertex: Vertex[V]): Graph[V] =
    unit(vertexMap + vertex)

  /**
   * Creates a new graph using the provided vertex map.
   *
   * @param vertexMap the vertex map that defines the structure and vertices of the new graph
   * @return a new instance of the graph based on the given vertex map
   */
  def unit(vertexMap: VertexMap[V]): Graph[V]

  /**
   * Method to run depth-first-search on this Traversable.
   *
   * @param visitor the visitor, of type Visitor[V, J].
   * @param v       the starting vertex value.
   * @tparam J the journal type.
   * @return a new Visitor[V, J].
   */
  def dfs[J](visitor: Visitor[V, J])(v: V): Visitor[V, J] =
    vertexMap.dfs(visitor)(v)

  /**
   * Method to run depth-first-search on this Traversable, ensuring that every vertex is visited.
   *
   * @param visitor the visitor, of type Visitor[V, J].
   * @tparam J the journal type.
   * @return a new Visitor[V, J].
   */
  def dfsAll[J](visitor: Visitor[V, J]): Visitor[V, J] =
    vertexMap.dfsAll(visitor)

  /**
   * Method to run goal-terminated breadth-first-search on this VertexMap.
   *
   * CONSIDER add relax method as in bfsMutable.
   *
   * @param visitor the visitor, of type Visitor[V, J].
   * @param v       the starting vertex.
   * @param goal    a function which will return true when the goal is reached.
   * @tparam J the journal type.
   * @return a new Visitor[V, J].
   */
  def bfs[J](visitor: Visitor[V, J])(v: V)(goal: V => Boolean): Visitor[V, J] =
    vertexMap.bfs(visitor)(v)(goal)
}

/**
 * Companion object for the `Graph` class.
 * This object provides methods to operate on or create instances of the `Graph` type.
 *
 * A `Graph` is a generic structure representing a collection of vertices and the edges between them.
 * The vertices hold values of type `V`, and the graph supports various operations, such as adding vertices,
 * performing depth-first and breadth-first searches, and retrieving its internal vertex representation.
 *
 * This object contains utilities for interacting with and constructing `Graph` instances.
 */
object Graph

/**
 * An abstract base class representing a graph structure composed of vertices.
 * CONSIDER eliminating this class.
 *
 * @tparam V the type representing the attributes associated with the vertices within the graph.
 *           `V` is invariant, ensuring that the specific type of vertex attributes cannot be
 *           changed in different contexts of the same graph.
 *
 *           This class extends the `Graph` trait and serves as a foundation for building graph
 *           implementations. The graph structure is defined and manipulated using its associated
 *           `VertexMap`, which organizes and manages the vertices of the graph.
 * @constructor Creates a new `AbstractGraph` instance with the specified `vertexMap`.
 * @param vertexMap the `VertexMap` instance that holds and organizes the vertices of the graph.
 */
abstract class AbstractGraph[V](vertexMap: VertexMap[V]) extends Graph[V] {

  /**
   * Retrieves an iterator of all adjacencies from the vertexMap in the graph.
   * Each vertex's adjacencies are iterated over, yielding a sequence of adjacency objects.
   *
   * @return an iterator over adjacencies of type `Adjacency[V]` from the vertexMap in the graph.
   */
  def adjacencies: Iterator[Adjacency[V]] = for {
    vv <- vertexMap.vertices.iterator
    va <- vv.adjacencies.iterator
  } yield va

  /**
   * Filters the adjacencies of a given vertex based on a specified predicate.
   * The predicate determines which adjacencies of the vertex should be included in the result.
   *
   * @param predicate a function that takes a `Discoverable[V]` and returns a `Boolean`.
   *                  This function is used to filter adjacencies based on custom conditions.
   * @param v         the vertex whose adjacencies are to be filtered.
   *                  The vertex is of type `V`, representing the attributes of a graph vertex.
   * @return an `Iterator` of `Adjacency[V]`, containing the filtered adjacencies of the given vertex.
   */
  def filteredAdjacencies2(predicate: Discoverable[V] => Boolean)(v: V): Iterator[Adjacency[V]] = vertexMap.filteredAdjacencies2(predicate)(v)
}

/**
 * A trait representing a graph structure that incorporates both vertices and edges. 
 * This trait extends the capabilities of the `Graph` trait by supporting edge-based operations.
 *
 * @tparam V the type representing the attributes associated with the vertices in the graph.
 * @tparam E the type representing the attributes associated with the edges in the graph.
 */
trait EdgeGraph[V, E] extends Graph[V] with EdgeTraversable[V, E] {

  /**
   * Adds an edge to the graph. The edge connects two vertices and may carry an attribute of type `E`.
   * CONSIDER eliminating this method.
   *
   * @param edge the edge to be added, which is an instance of `Edge[E, V]`. The edge defines the connection
   *             between two vertices of type `V` and may have a direction and an associated attribute of type `E`.
   * @return a new `EdgeGraph[V, E]` instance that includes the newly added edge. The returned graph preserves
   *         all existing edges and vertices.
   */
  def addEdge(edge: Edge[E, V]): EdgeGraph[V, E]
}
