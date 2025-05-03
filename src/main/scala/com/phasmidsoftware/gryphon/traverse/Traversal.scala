package com.phasmidsoftware.gryphon.traverse

import com.phasmidsoftware.gryphon.core
import com.phasmidsoftware.gryphon.visit.{Journal, PostVisitor, Visitor}

import scala.collection.mutable
import scala.util.{Failure, Success, Try, Using}

/**
 * A trait that defines the behavior of traversal over vertices and edges within a graph-like structure.
 *
 * @tparam V the type representing a vertex in the graph.
 * @tparam E the type representing an edge in the graph.
 * @tparam T the resulting type after traversing a vertex or an edge.
 */
trait Traversal[V, E, T] {

  /**
   * Traverses a given vertex in the graph and returns the result of the traversal.
   *
   * @param v the vertex to be traversed.
   * @return the result of the traversal for the provided vertex.
   */
  def vertexTraverse(v: V): T

  /**
   * Traverses a specified edge within a graph-like structure and produces a result of type T.
   *
   * @param e the edge to be traversed
   * @return the result of the traversal
   */
  def edgeTraverse(e: E): T
}

/**
 * Provides graph traversal functionality using depth-first search (DFS) for vertex traversal.
 */
object Traversal {

  /**
   * Executes a depth-first search (DFS) traversal starting from a specified vertex and applies a transformation
   * function to each visited vertex. The result of the traversal is represented as a `Traversal`.
   *
   * @param f           a function that takes a vertex of type `V` and returns a transformed value of type `T`.
   * @param traversable the graph-like structure that supports DFS traversal, represented as `core.Traversable[V]`.
   * @param start       the starting vertex for the DFS traversal, of type `V`.
   * @return a `Traversal` object of type `Traversal[V, E, T]` containing the results of the traversal.
   */
  def vertexTraversalDfs[V, E, T](f: V => T)(traversable: core.Traversable[V])(start: V): Traversal[V, E, T] = {
    implicit object ZZ extends Journal[mutable.Map[V, T], V] {
      /**
       * An empty journal.
       */
      def empty: mutable.Map[V, T] = mutable.Map.empty[V, T]

      /**
       * Method to append a `V` value to this `Journal`.
       *
       * @param map the journal to be appended to.
       * @param v   an instance of `V` to be appended to `map`.
       * @return a new `Journal`.
       */
      def append(map: mutable.Map[V, T], v: V): mutable.Map[V, T] = {
        val _ = map.put(v, f(v))
        map
      }
    }
    val result: Try[Visitor[V, mutable.Map[V, T]]] =
      Using(PostVisitor[V, mutable.Map[V, T]]()) {
      (visitor: Visitor[V, mutable.Map[V, T]]) =>
        traversable.dfs[mutable.Map[V, T]](visitor)(start)
    }
    result match {
      case Success(visitor: Visitor[V, mutable.Map[V, T]]) =>
        val map: mutable.Map[V, T] = visitor.journal
        map.keys.foldLeft(MapTraversal.empty[V, E, T]) { (m, v) => m + (v -> map(v)) }
      case Failure(exception) => throw exception
    }
  }

}

/**
 * A concrete implementation of the `Traversal` trait that uses a map to represent
 * vertex traversals. Each vertex in the graph is associated with a traversal result.
 *
 * @constructor Creates a `MapTraversal` with the given map of vertices and their associated traversal outputs.
 * @param map A map where each key is a vertex of type `V`, and the value is the corresponding traversal result of type `T`.
 * @tparam V The type representing a vertex in the graph.
 * @tparam E The type representing an edge in the graph.
 * @tparam T The resulting type after traversing a vertex or an edge.
 */
case class MapTraversal[V, E, T](map: Map[V, T]) extends Traversal[V, E, T] {
  /**
   * Traverses the specified vertex in the graph and retrieves the associated traversal result
   * from the underlying map. If the vertex does not exist, a `NoSuchElementException` is thrown.
   *
   * @param v the vertex to be traversed and looked up in the map.
   * @return the traversal result of type `T` associated with the provided vertex.
   * @throws NoSuchElementException if the vertex does not exist in the map.
   */
  def vertexTraverse(v: V): T =
    map.getOrElse(v, throw new NoSuchElementException(s"no such element: $v"))

  /**
   * Traverses a specified edge within a graph-like structure and produces a result of type T.
   *
   * @param e the edge to be traversed
   * @return the result of the traversal for the provided edge
   */
  def edgeTraverse(e: E): T = ???

  /**
   * Adds a new mapping of a vertex to its corresponding traversal result to the existing map,
   * returning a new `MapTraversal` instance with the updated mapping.
   *
   * @param t a tuple where the first element is a vertex of type `V`, and the second element is its associated
   *          traversal result of type `T`.
   * @return a new `MapTraversal` instance of type `MapTraversal[V, E, T]` containing the updated map.
   */
  def +(t: (V, T)): MapTraversal[V, E, T] =
    MapTraversal(map + t)
}

/**
 * Provides utility methods for creating and working with instances of `MapTraversal`.
 *
 * A `MapTraversal` is a concrete implementation of the `Traversal` trait
 * that uses a map to represent the traversal results for graph vertices.
 * This object contains a factory method for creating an empty `MapTraversal` instance.
 */
object MapTraversal {
  /**
   * Creates and returns an empty `MapTraversal` instance.
   *
   * An empty `MapTraversal` contains no mappings of vertices to their traversal results.
   * This method is useful as a starting point for building a traversal object incrementally.
   *
   * @tparam V The type representing a vertex in the graph.
   * @tparam E The type representing an edge in the graph.
   * @tparam T The resulting type after traversing a vertex or an edge.
   * @return A new `MapTraversal` instance with an empty map.
   */
  def empty[V, E, T]: MapTraversal[V, E, T] =
    MapTraversal(Map.empty[V, T])
}