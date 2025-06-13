/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.core

import com.phasmidsoftware.gryphon.traverse.{Traversal, VertexTraversal}
import com.phasmidsoftware.gryphon.visit.{Journal, PostVisitor, Visitor}

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
   * Traverses the vertices of a graph depth-first starting from the specified starting vertex
   * and applies a transformation function to each vertex.
   *
   * TODO not currently used but we should copy the code from Traversal to here and use it.
   *
   * @param f     a function that takes a vertex of type `V` and transforms it into a value of type `T`.
   * @param start the vertex from which the depth-first traversal begins.
   * @param ev    an implicit evidence parameter of type `Journal[mutable.Map[V, T], V]`
   *              representing the journal used during the traversal process.
   * @tparam E the type of the edges in the graph.
   * @tparam T the type resulting from applying the function `f` to the vertices.
   * @return a `Traversal[V, T]` representing the result of the depth-first traversal and transformation.
   */
  def traverseVertexDfs[E, T](f: V => T)(start: V)(implicit ev: Journal[mutable.Map[V, T], V]): Traversal[V, T] = {
    val result: Try[Visitor[V, mutable.Map[V, T]]] =
      Using(PostVisitor[V, mutable.Map[V, T]]()) {
        (visitor: Visitor[V, mutable.Map[V, T]]) =>
          dfs[mutable.Map[V, T]](visitor)(start)
      }
    result match {
      case Success(visitor: Visitor[V, mutable.Map[V, T]]) =>
        val map: mutable.Map[V, T] = visitor.journal
        map.keys.foldLeft(VertexTraversal.empty[V, E, T]) { (m, v) => m + (v -> map(v)) }
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
