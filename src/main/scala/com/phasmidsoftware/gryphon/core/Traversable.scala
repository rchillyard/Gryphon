/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.core

import com.phasmidsoftware.gryphon.traverse.{Connexions, Traversal, VertexTraversal}
import com.phasmidsoftware.gryphon.util.GraphException
import com.phasmidsoftware.gryphon.{core, traverse}
import com.phasmidsoftware.visitor
import com.phasmidsoftware.visitor.*
import com.phasmidsoftware.visitor.DfsVisitorMapped.createPostFunctionMapJournal

import java.util.Map.entry
import scala.collection.immutable.Queue
import scala.util.{Try, Using}

/**
 * Trait to define the behavior of a graph-like structure which can be traversed by dfs, dfsAll, and bfsMutable.
 *
 * @tparam V the underlying key (attribute) type for a vertex.
 */
trait Traversable[V] {

  /**
   * Retrieves the vertex associated with the given key.
   *
   * @param key the key of type V for which the associated vertex is to be retrieved
   * @return an Option containing the associated Vertex if found, or None if there is no vertex associated with the given key
   */
  def get(key: V): Option[Vertex[V]]

  /**
   * Retrieves the adjacent vertices connected to the specified vertex in the graph-like structure.
   *
   * @param v the vertex whose adjacent vertices are to be returned.
   * @return an iterator over the vertices adjacent to the specified vertex.
   */
  def adjacentVertices(v: V): Iterator[V]

  /**
   * Filters the adjacent vertices of a given vertex based on a specified predicate.
   *
   * @param predicate a function that evaluates each adjacent vertex and returns true
   *                  if the vertex satisfies the specified condition.
   * @param v         the vertex whose adjacent vertices are to be filtered.
   * @return an iterator over the vertices that are adjacent to the given vertex and
   *         satisfy the specified predicate.
   */
  def filteredAdjacentVertices(predicate: V => Boolean)(v: V): Iterator[V] =
    adjacentVertices(v).filter(predicate)

  /**
   * Filters the adjacencies of a given vertex based on a specified predicate.
   *
   * @param predicate a function that evaluates each adjacency of type `Discoverable[V]`
   *                  and returns true if the adjacency satisfies the specified condition.
   * @param v         the vertex whose adjacencies are to be filtered.
   * @return an iterator over the adjacencies of the given vertex that satisfy the predicate.
   */
  def filteredAdjacencies(predicate: Discoverable[V] => Boolean)(v: V): Iterator[Adjacency[V]]

  /**
   * Retrieves the undiscovered adjacent vertices connected to the specified vertex
   * in the graph-like structure.
   *
   * @param v the vertex whose undiscovered adjacent vertices are to be returned
   * @return an iterator over the vertices adjacent to the specified vertex that are undiscovered
   */
  def undiscoveredAdjacentVertices(v: V): Iterator[V]

  /**
   * Determines an undiscovered vertex adjacent to the specified vertex.
   *
   * @param v the vertex for which to find an adjacent undiscovered vertex
   * @return an Option containing the first undiscovered adjacent vertex if one exists,
   *         or None if all adjacent vertices are already discovered
   */
  def undiscoveredVertex(v: V): Option[Vertex[V]]

  /**
   * Converts the undiscovered adjacent vertices of a given vertex into a sequence.
   *
   * @param v the vertex for which the undiscovered adjacent vertices are to be retrieved.
   * @return a sequence of vertices that are adjacent to the given vertex and undiscovered.
   */
  private def undiscoveredVertexSequence(v: V): Seq[V] =
    undiscoveredAdjacentVertices(v).toSeq

  /**
   * Performs a depth-first search (DFS) traversal of a graph-like structure starting from the specified vertex.
   * This method uses a visitor to track the state and progress of traversal.
   *
   * @param visitor the DFS visitor of type `DfsVisitor[V]` responsible for tracking visited vertices and traversal state.
   * @param v       the starting vertex for the DFS traversal.
   * @return the updated `DfsVisitor[V]` containing the state after the traversal.
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
   * Performs a depth-first search (DFS) traversal for all vertices in the graph using the supplied visitor.
   * The traversal updates the visitor to reflect the state after processing all vertices.
   *
   * @param visitor the DFS visitor of type `DfsVisitor[V]` responsible for tracking traversal state and visited vertices.
   * @return the updated `DfsVisitor[V]` containing the state after traversing all vertices in the graph.
   */
  def dfsAll(visitor: DfsVisitor[V]): DfsVisitor[V]

  /**
   * Performs a breadth-first search (BFS) traversal of a graph-like structure starting from the specified vertex.
   * The traversal updates the BFS visitor state and may return a vertex discovered during the process.
   *
   * @param visitor the BFS visitor of type `BfsVisitor[V]` responsible for tracking the visited vertices and traversal state.
   * @param v       the starting vertex for the BFS traversal.
   * @return a tuple containing the updated `BfsVisitor[V]` after the traversal, and an `Option[V]` representing
   *         a vertex discovered during the traversal, if any.
   */
  def bfs(visitor: BfsVisitor[V])(v: V): (BfsVisitor[V], Option[V])

  /**
   * Executes a breadth-first search (BFS) traversal of a graph, visiting edges instead of vertices.
   * The traversal starts from the specified vertex `v` and continues until all reachable edges
   * are visited or the goal function returns true for a destination vertex.
   *
   * @param visitor the visitor responsible for processing edges during the traversal. It is of the type `Visitor[Edge[E, V], J]`.
   * @param v       the starting vertex from which the BFS traversal begins.
   * @param goal    a function that takes a vertex of type `V` and returns true if the vertex satisfies the goal condition, terminating the traversal.
   * @tparam E the type of the edge attribute in the graph.
   * @return the visitor after completing the BFS traversal, which may contain information about the visited edges.
   */
  def bfse[E](visitor: Visitor[Edge[E, V]])(v: V)(goal: V => Boolean): Visitor[Edge[E, V]]

  /**
   * Performs a depth-first search (DFS) traversal on a graph-like structure starting from the specified vertex.
   * Applies a provided transformation function to each visited vertex and generates a traversal result.
   *
   * @param f     a transformation function that maps a vertex of type `V` to a result of type `T`.
   *              This function is applied to each vertex during the DFS traversal.
   * @param start the starting vertex for the DFS traversal.
   * @tparam E the type of edges in the graph.
   * @tparam T the type of the result produced by the transformation function.
   * @return `Try` containing the resulting `Traversal[V, T]` if the traversal succeeds,
   *         or a failure if an error occurs during the traversal.
   */
  def vertexMappedTraversalDfs[E, T](f: V => T)(start: V): Try[Traversal[V, T]] = {

    def doDFS(visitor: DfsVisitorMapped[V, T]): VertexTraversal[V, T] = {
      // XXX We know there's exactly one MapJournal
      val mapJournal: AbstractMapJournal[V, T] = visitor.dfs(start).mapJournals.head
      mapJournal.keys.foldLeft(VertexTraversal.empty[V, T]) {
        case (m, vv) =>
          m + (vv, f(vv))
      }
    }

    val dfsVisitor = DfsVisitorMapped.createPostFunctionMapJournal[V, T](f, undiscoveredVertexSequence)
    Using(dfsVisitor)(doDFS)
  }

  /**
   * Performs a depth-first search (DFS) traversal on a graph-like structure starting from the specified vertex.
   * Applies a provided transformation function to each visited vertex and generates a traversal result.
   *
   * @param f     a transformation function that maps a vertex of type `V` to a result of type `T`.
   *              This function is applied to each vertex during the DFS traversal.
   * @param start the starting vertex for the DFS traversal.
   * @tparam E the type of edges in the graph.
   * @tparam T the type of the result produced by the transformation function.
   * @return `Try` containing the resulting `Traversal[V, T]` if the traversal succeeds,
   *         or a failure if an error occurs during the traversal.
   */
  def vertexMappedTraversalBfs[E, T](f: V => T)(start: V): Try[Traversal[V, T]] = {

    def doBFS(visitor: BfsQueueVisitorMapped[V, T]): VertexTraversal[V, T] = {
      val (v, go) = visitor.bfs(start)
      // XXX We know there's exactly one MapJournal
      val mapJournal: AbstractMapJournal[V, T] = v.mapJournals.head
      mapJournal.keys.foldLeft(VertexTraversal.empty[V, T]) {
        case (m, vv) =>
          m + (vv, f(vv))
      }
    }

    val dfsVisitor = BfsQueueVisitorMapped[V, T](Queue.empty, Map(Pre -> MapJournal.empty), f, undiscoveredVertexSequence, _ => false)
    Using(dfsVisitor)(doBFS)
  }

  /**
   * Retrieves the connections (edges) originating from the specified starting vertex in a graph.
   * The method performs a depth-first search (DFS) traversal to discover and construct these
   * connections while handling various edge types like directed and undirected edges.
   *
   * @param start the starting vertex for computing the connections in the graph.
   * @tparam E the type of the edges in the graph.
   * @return a `Connexions[V, E]` object representing the discovered connexions from the start vertex.
   *         This includes both directed and undirected edges, appropriately handled.
   */
  def getConnexions[E](start: V): Connexions[V, E] = {
    val f: V => Seq[Adjacency[V]] = v => filteredAdjacencies(a => !a.discovered)(v).toSeq
    val visitor = DfsOriginVisitor.createPostMapJournal[V, Adjacency[V]](f, _.vertex).dfs(start)
    visitor.mapJournals.head.entries.foldLeft[Connexions[V, E]](Connexions.empty[V, E]) {
      case (m, (v, Some(AdjacencyEdge[V, E] (connexion, _)) ) ) =>
        m addConnexion (v, connexion)
      case (m, (_, None)) =>
        m
      case _ =>
        throw GraphException(s"getConnexions: unexpected entry: $entry")
    }
  }
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
