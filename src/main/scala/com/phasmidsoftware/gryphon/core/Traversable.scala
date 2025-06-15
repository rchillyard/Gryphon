/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.core

import com.phasmidsoftware.gryphon.core
import com.phasmidsoftware.gryphon.traverse.{Traversal, VertexTraversal}
import com.phasmidsoftware.gryphon.util.GraphException
import com.phasmidsoftware.gryphon.visit.*

import scala.collection.immutable.Queue
import scala.collection.mutable
import scala.util.{Failure, Success, Try, Using}

/**
 * Trait to define the behavior of a graph-like structure which can be traversed by dfs, dfsAll, and bfsMutable.
 *
 * @tparam V the underlying key (attribute) type for a vertex.
 */
trait Traversable[V] {

  /**
   * Retrieves the adjacent vertices connected to the specified vertex in the graph-like structure.
   *
   * @param v the vertex whose adjacent vertices are to be returned.
   * @return an iterator over the vertices adjacent to the specified vertex.
   */
  def adjacencies(v: V): Iterator[V]

  /**
   * Filters the vertices adjacent to a given vertex based on a specified predicate.
   *
   * @param predicate a function that determines whether an adjacent vertex should be included.
   *                  It takes a vertex of type `V` and returns a boolean indicating whether the vertex satisfies the condition.
   * @param v         the vertex whose adjacent vertices are to be filtered.
   * @return an iterator containing the adjacent vertices that satisfy the predicate.
   * @throws GraphException if the vertex is not found in the map
   */
  def filteredAdjacencies(predicate: V => Boolean)(v: V): Iterator[V] =
    adjacencies(v).filter(predicate)

  /**
   * Retrieves the adjacent vertices connected to the specified vertex in the graph-like structure.
   *
   * @param v the vertex whose adjacent vertices are to be returned.
   * @return an iterator over the vertices adjacent to the specified vertex.
   */
  def undiscoveredAdjacencies(v: V): Iterator[V]

  /**
   * Method to run depth-first-search on this Traversable.
   *
   * @param visitor the visitor, of type Visitor[V, J].
   * @param v       the starting vertex.
   * @tparam J the journal type.
   * @return a new Visitor[V, J].
   */
  def dfs[J](visitor: Visitor[V, J])(v: V): Visitor[V, J]

  /**
   * Method to run depth-first-search on this Traversable, ensuring that every vertex is visited.
   *
   * @param visitor the visitor, of type Visitor[V, J].
   * @tparam J the journal type.
   * @return a new Visitor[V, J].
   */
  def dfsAll[J](visitor: Visitor[V, J]): Visitor[V, J]

  /**
   * Performs a special Depth-First Search (DFS) on a graph starting from the given vertex `v`.
   * It utilizes a special visitor consisting of a tuple of vertices.
   *
   * @param f       A function that takes a vertex and returns a tuple containing two vertices.
   * @param visitor A visitor function used to process graph elements during the DFS traversal.
   * @param v       The starting vertex for the DFS traversal.
   * @return The visitor after completing the DFS traversal.
   */
  def dfsA[J](visitor: Visitor[(V, V), J])(v: V): Visitor[(V, V), J]

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
  def bfs[J](visitor: Visitor[V, J])(v: V)(goal: V => Boolean): Visitor[V, J]

  /**
   * Performs a depth-first search (DFS) traversal starting from the specified vertex
   * and returns a mapping of vertices to their respective traversal results.
   *
   * @param start the starting vertex for the DFS traversal.
   * @param ev    an implicit instance of `MappedJournalMap` required for journal operations.
   * @tparam E the edge type (not used directly in this method).
   * @tparam T the type of the traversal result associated with each vertex.
   * @return a `Traversal` object representing the mapping of vertices to traversal results.
   * @throws exception if an error occurs during the DFS traversal.
   */
  def vertexMappedTraversalDfs[E, T](start: V)(implicit ev: MappedJournalMap[V, T]): Traversal[V, T] = {
    val result: Try[Visitor[V, Map[V, T]]] =
      Using(PostKeyedVisitor.create[V, T, Map[V, T]]()) {
        (visitor: Visitor[V, Map[V, T]]) =>
          dfs[Map[V, T]](visitor)(start)
      }
    result match {
      case Success(visitor: Visitor[V, Map[V, T]]) =>
        // TODO check that this is really OK
        val map: Map[V, T] = visitor.journals.head
        map.keys.foldLeft(VertexTraversal.empty[V, T]) { (m, v) => m + (v -> map(v)) }
      case Failure(exception) =>
        throw exception
    }
  }

  /**
   * Performs a depth-first search (DFS) traversal starting from the specified vertex
   * and returns a mapping of vertices to their respective traversal results.
   *
   * @param start the starting vertex for the DFS traversal.
   * @param ev    an implicit instance of `MappedJournalMap` required for journal operations.
   * @tparam E the edge type (not used directly in this method).
   * @tparam T the type of the traversal result associated with each vertex.
   * @return a `Traversal` object representing the mapping of vertices to traversal results.
   * @throws exception if an error occurs during the DFS traversal.
   */
  def vertexVertexIterableTraversalDfs[E](start: V)(implicit ev: IterableJournalQueue[(V, V)]): Traversal[V, (V, V)] = {
    type VV = (V, V)
    type Journal = Queue[VV]
    type MyVisitor = Visitor[VV, Journal]
    val result: Try[MyVisitor] =
      Using(PreVisitor[VV, Journal]()) {
        (visitor: MyVisitor) =>
          dfsA[Journal](visitor)(start)
      }
    result match {
      case Success(visitor: Visitor[VV, Journal]) =>
        // TODO check that this is really OK
        val journal: Journal = visitor.journals.head
        journal.foldLeft(VertexTraversal.empty[V, VV]) { (q, vv) => q + (vv._2 -> vv) }
      case Failure(exception) =>
        throw exception
    }
  }

  //  /**
  //   * Method to run breadth-first-search with a mutable queue on this Traversable.
  //   *
  //   * @param visitor the visitor, of type Visitor[V, J].
  //   * @param v       the starting vertex.
  //   * @tparam J the journal type.
  //   * @tparam Q the type of the mutable queue for navigating this Traversable.
  //   *           Requires implicit evidence of MutableQueueable[Q, V].
  //   * @return a new Visitor[V, J].
  //   */
  //  def bfsMutable[J, Q](visitor: Visitor[V, J])(v: V)(goal: V => Boolean)(implicit ev: MutableQueueable[Q, V]): Visitor[V, J]
}

/**
 * A trait representing a traversable structure that encompasses both vertices and edges in a graph.
 * Extends the functionality of `Traversable` by including a method for iterating over the edges.
 *
 * This structure is useful for graph-like data where both vertex and edge traversal are required.
 *
 * @tparam V the type of the vertices in the traversable graph.
 * @tparam E the type of the attribute associated with an edge.
 */
trait EdgeTraversable[V, E] extends Traversable[V] {
  /**
   * Retrieves an iterator over all the edges in the graph.
   * The returned iterator provides sequential access to each edge, allowing traversal
   * of all connections between vertices in the graph. Each edge contains an attribute
   * of type `E` and connects vertices of type `V`.
   *
   * @return an `Iterator[Edge[E, V]]` that allows access to all edges in the graph.
   */
  def edges: Iterator[Edge[E, V]]
}

//
///**
// * Trait to define the behavior of a graph-like structure which can be traversed by BFS in search of a goal.
// *
// * @tparam V the underlying key (attribute) type for a vertex.
// * @tparam X the type of edge which connects two vertices. A sub-type of EdgeLike[V].
// * @tparam P the property type (a mutable property currently only supported by the Node type).
// */
//trait GoalTraversable[V, X <: EdgeLike[V], P] extends Traversable[V] {
//
//  /**
//   * Method to run breadth-first-search on this Traversable.
//   *
//   * @param v    the starting vertex.
//   * @param goal the goal function: None means "no decision;" Some(x) means the decision (win/lose) is true/false.
//   * @return a new Tree[V, E, X, Double] of shortest paths.
//   */
//  def bfs(v: V)(goal: V => Option[Boolean]): (Option[V], AcyclicNetwork[V, VertexPair[V], P])
//}
//
///**
// * Trait to define the behavior of a graph-like structure (with edge attributes) which can be traversed by BFS in search of a goal.
// *
// * @tparam V the underlying key (attribute) type for a vertex.
// * @tparam E the edge-attribute type.
// * @tparam X the type of edge which connects two vertices. A sub-type of Edge[V,E].
// * @tparam P the property type (a mutable property currently only supported by the Node type).
// */
//trait EdgeGoalTraversable[V, E, X <: Edge[V, E], P] extends Traversable[V] {
//
//  /**
//   * Method to run breadth-first-search on this Traversable.
//   *
//   * NOTE in this method name, the F comes before the S. Important ;)
//   *
//   * @param v    the starting vertex.
//   * @param goal the goal function: None means "no decision;" Some(x) means the decision (win/lose) is true/false.
//   * @return a new Tree[V, E, X, Double] of shortest paths.
//   */
//  def bfse(v: V)(goal: V => Option[Boolean]): AcyclicNetwork[V, VertexPair[V], P]
//}
