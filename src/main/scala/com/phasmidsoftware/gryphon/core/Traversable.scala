/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.core

import com.phasmidsoftware.gryphon.traverse.{Traversal, VertexTraversal}
import com.phasmidsoftware.gryphon.util.GraphException
import com.phasmidsoftware.gryphon.{core, traverse}
import com.phasmidsoftware.visitor
import com.phasmidsoftware.visitor.*

import scala.util.{Try, Using}

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
   * CONSIDER writing this in terms of filteredAdjacencies2 and then renaming.
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
   * This is the more general form of filteredAdjacencies.
   *
   * TODO rename this method to drop the 2
   *
   * @param predicate a predicate that evaluates a Discoverable[V]
   * @param v         the attribute of a Vertex[V]
   * @return an Iterator of Adjacency[V]
   */
  def filteredAdjacencies2(predicate: Discoverable[V] => Boolean)(v: V): Iterator[Adjacency[V]]

  /**
   * Retrieves the adjacent vertices connected to the specified vertex in the graph-like structure.
   *
   * @param v the vertex whose adjacent vertices are to be returned.
   * @return an iterator over the vertices adjacent to the specified vertex.
   */
  def undiscoveredAdjacentVertices(v: V): Iterator[V]

  /**
   * Method to run depth-first-search on this Traversable.
   *
   * @param visitor the visitor, of type DfsVisitor[V].
   * @param v       the starting vertex.
   * @tparam J the journal type.
   * @return a new DfsVisitor[V].
   */
  def dfs(visitor: DfsVisitor[V])(v: V): DfsVisitor[V]

  /**
   * Performs a depth-first search mapping traversal on a graph-like structure.
   * This method applies a transformation function to the current vertex and continues
   * the traversal to its adjacent vertices based on the supplied visitor.
   *
   * @param visitor the visitor of type `DfsVisitorMapped[V, T]` that keeps track of the traversal state and visited vertices.
   * @param f       a transformation function that maps a vertex of type `V` to a result of type `T`.
   * @param v       the starting vertex of the traversal.
   * @tparam T the type of the result produced by the transformation function.
   * @return an updated `DfsVisitorMapped[V, T]` containing the traversal state and mapping results.
   */
  def dfsFunction[T](visitor: DfsVisitorMapped[V, T])(f: V => T)(v: V): DfsVisitorMapped[V, T]

  /**
   * Method to run depth-first-search on this Traversable, ensuring that every vertex is visited.
   *
   * @param visitor the visitor, of type Visitor[V, J].
   * @tparam J the journal type.
   * @return a new Visitor[V, J].
   */
  def dfsAll(visitor: DfsVisitor[V]): DfsVisitor[V]

  /**
   * Method to run goal-terminated breadth-first-search on this VertexMap.
   *
   * CONSIDER add relax method as in bfsMutable.
   *
   * @param visitor the visitor, of type Visitor[V, J].
   * @param v       the starting vertex.
   * @tparam J the journal type.
   * @return a new Visitor[V, J].
   */
  def bfs(visitor: BfsVisitor[V])(v: V): BfsVisitor[V]

  /**
   * Executes a breadth-first search (BFS) traversal of a graph, visiting edges instead of vertices.
   * The traversal starts from the specified vertex `v` and continues until all reachable edges
   * are visited or the goal function returns true for a destination vertex.
   *
   * @param visitor the visitor responsible for processing edges during the traversal. It is of type `Visitor[Edge[E, V], J]`.
   * @param v       the starting vertex from which the BFS traversal begins.
   * @param goal    a function that takes a vertex of type `V` and returns true if the vertex satisfies the goal condition, terminating the traversal.
   * @tparam E the type of the edge attribute in the graph.
   * @tparam J the journal type that facilitates recording traversal progress.
   * @return the visitor after completing the BFS traversal, which may contain information about the visited edges.
   */
  def bfse[E](visitor: Visitor[Edge[E, V]])(v: V)(goal: V => Boolean): Visitor[Edge[E, V]]

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
  def vertexMappedTraversalDfs[E, T](f: V => T)(start: V): Try[Traversal[V, T]] = {
    def undiscoveredVertexSequence(v: V): Seq[V] = undiscoveredAdjacentVertices(v).toSeq

    def doDFS(visitor: DfsVisitorMapped[V, T]): VertexTraversal[V, T] = {
      // XXX We know there's exactly one MapJournal
      val mapJournal: AbstractMapJournal[V, T] = visitor.dfs(start).mapJournals.head
      mapJournal.keys.foldLeft(VertexTraversal.empty[V, T]) {
        case (m, vv) =>
          m + (vv -> f(vv))
      }
    }

    val dfsVisitor = DfsVisitorMapped.createPostFunctionMapJournal[V, T](f, undiscoveredVertexSequence)
    Using(dfsVisitor)(doDFS)
  }

  /**
   * Performs a depth-first search (DFS) traversal in a graph-like structure, starting from the specified vertex.
   * It processes visited vertices with a given function and returns the traversal connections as a result.
   * CONSIDER do we really need to keep (V, V) in the queue? How about using a Map of V => V?
   *
   * @param start the starting vertex for the DFS traversal.
   * @param e     a function that maps a vertex to an edge type `E`.
   * @param ev    an implicit instance of `IterableJournalQueue` used for handling the journaling of traversal steps.
   * @tparam E the type representing the edge attribute in the traversal.
   * @return a `Connexions` object that encapsulates the mapping of vertices to their respective directed edges.
   * @throws exception if an error occurs during the DFS traversal.
   */
  def vertexVertexIterableTraversalDfs[E](start: V)(e: V => E): traverse.Connexions[V, E] = {
    traverse.Connexions.apply[V, E](Map()) // TODO flesh this out as below
    //    type VV = (V, V)
    //    type MyVisitor = Visitor[VV]
    //    val result: Try[MyVisitor] =
    //      Using(SimpleVisitor[VV](QueueJournal.empty[(V, V)], Pre)) {
    //        (visitor: MyVisitor) =>
    //          dfsTuple(visitor)(start)
    //      }
    //    result match {
    //      case Success(visitor: Visitor[VV]) =>
    //        visitor.journals.flatten.foldLeft(com.phasmidsoftware.gryphon.traverse.Connexions.empty[V, E]) {
    //          (q, vv) =>
    //            q + (vv._2 -> DirectedEdge.create(e(vv._2), vv))
    //
    //        }
    //      case Failure(exception) =>
    //        throw exception
    //    }
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
