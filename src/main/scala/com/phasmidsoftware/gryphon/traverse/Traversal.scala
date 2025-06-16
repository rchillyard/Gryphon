package com.phasmidsoftware.gryphon.traverse

import com.phasmidsoftware.gryphon.adjunct.DirectedGraph
import com.phasmidsoftware.gryphon.core
import com.phasmidsoftware.gryphon.core.{Connexion, Edge}
import com.phasmidsoftware.gryphon.util.GraphException
import com.phasmidsoftware.gryphon.visit.*

import scala.util.{Failure, Success, Try, Using}

/**
 * A trait that defines the behavior of traversal over vertices and edges within a graph-like structure.
 *
 * @tparam V the type representing a vertex in the graph.
 * @tparam T the resulting type after traversing a vertex or an edge.
 */
trait Traversal[V, T] {

  /**
   * Traverses a given vertex in the graph and returns the result of the traversal.
   *
   * @param v the vertex to be traversed.
   * @return the result of the traversal for the provided vertex.
   */
  def vertexTraverse(v: V): T

  /**
   * Traverses an indexed edge within a graph-like structure and produces a result of type T.
   *
   * @param x the index of the edge traversed
   * @return the corresponding value of T
   */
  def edgeTraverse(x: Int): T
}

/**
 * Provides graph traversal functionality using depth-first search (DFS) for vertex traversal.
 */
object Traversal {
  /**
   * Performs a traversal over the edges of a graph and applies a function to each edge, returning the results
   * as a `Traversal` object.
   *
   * @param f           a function applied to each edge, transforming each `Edge[E, V]` into a value of type `T`
   * @param traversable the graph or data structure implementing the `core.Traversable` interface over edge type `E`
   * @tparam V the type of vertices connected by the edges in the graph
   * @tparam E the type of the attributes associated with the edges in the graph
   * @tparam T the resulting type after applying the function `f` to each edge
   * @return a `Traversal[V, T]` containing the results of applying the function `f` to each edge of the graph
   * @throws GraphException if the provided `traversable` does not represent an edge graph
   */
  def edgeTraversal[V, E, T](f: Edge[E, V] => T)(traversable: core.EdgeTraversable[V, E]): Traversal[V, T] =
    EdgeTraversal(traversable.edges.foldLeft(List[T]())((list, edge) => f(edge) :: list))
}

/**
 * Abstract base class for edge traversal within a graph.
 * This class supports traversing edges in a graph-like structure by providing
 * a specific implementation for edge traversal while delegating the vertex traversal
 * to throw an exception, as it is not applicable for edge-based traversal.
 *
 * @tparam V the type representing a vertex in the graph
 * @tparam T the type representing the result of the edge traversal
 * @param ts a list of elements of type T representing traversal data, typically associated with edges
 */
abstract class AbstractEdgeTraversal[V, T](ts: List[T]) extends Traversal[V, T] {
  /**
   * Traverses a given vertex in the graph and returns the result of the traversal.
   *
   * @param v the vertex to be traversed.
   * @return the result of the traversal for the provided vertex.
   */
  def vertexTraverse(v: V): T = throw GraphException(s"vertexTraverse called on EdgeTraversal: $v")

  /**
   * Traverses a specified edge within a graph-like structure and produces a result of type T.
   *
   * @param x the index of the edge to be traversed
   * @return the result of the traversal
   */
  def edgeTraverse(x: Int): T = ts(x)
}

/**
 * Abstract base class for edge traversal within a graph.
 * This class supports traversing edges in a graph-like structure by providing
 * a specific implementation for edge traversal while delegating the vertex traversal
 * to throw an exception, as it is not applicable for edge-based traversal.
 *
 * @tparam V the type representing a vertex in the graph
 * @tparam T the type representing the result of the edge traversal
 * @param map the underlying map of vertex -> T elements
 */
abstract class AbstractVertexTraversal[V, T](map: Map[V, T]) extends Traversal[V, T] {
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
   * @param x the index of the edge to be traversed
   * @return the result of the traversal
   */
  def edgeTraverse(x: Int): T = throw GraphException(s"edgeTraverse called on AbstractVertexTraversal: $x")

  def unit(map: Map[V, T]): Traversal[V, T]

  /**
   * Adds a new mapping of a vertex to its corresponding traversal result to the existing map,
   * returning a new `VertexTraversal` instance with the updated mapping.
   *
   * @param t a tuple where the first element is a vertex of type `V`, and the second element is its associated
   *          traversal result of type `T`.
   * @return a new `VertexTraversal` instance of type `VertexTraversal[V, E, T]` containing the updated map.
   */
  def +(t: (V, T)): Traversal[V, T] =
    unit(map + t)

}

/**
 * A case class representing the concrete implementation of edge traversal for a graph-like structure.
 *
 * This class enables the traversal of edges by providing a list of precomputed results of type `T`
 * associated with each edge.
 *
 * Edge traversal uses the defined behavior from the `AbstractEdgeTraversal` base class,
 * where `vertexTraverse` throws an exception, and `edgeTraverse` retrieves the traversal result
 * from the precomputed list based on the edge index.
 *
 * @tparam V the type representing a vertex in the graph.
 * @tparam E the type representing an edge in the graph.
 * @tparam T the type of results for the edge traversal.
 * @param ts a list of elements of type `T` representing the computed results of edge traversal.
 */
case class EdgeTraversal[V, E, T](ts: List[T]) extends AbstractEdgeTraversal[V, T](ts)

/**
 * A concrete implementation of the `Traversal` trait that uses a map to represent
 * vertex traversals. Each vertex in the graph is associated with a traversal result.
 *
 * @constructor Creates a `VertexTraversal` with the given map of vertices and their associated traversal outputs.
 * @param map A map where each key is a vertex of type `V`, and the value is the corresponding traversal result of type `T`.
 * @tparam V The type representing a vertex in the graph.
 * @tparam T The resulting type after traversing a vertex or an edge.
 */
case class VertexTraversal[V, T](map: Map[V, T]) extends AbstractVertexTraversal[V, T](map) {
  /**
   * Adds a new mapping of a vertex to its corresponding traversal result to the existing map,
   * returning a new `VertexTraversal` instance with the updated mapping.
   *
   * @param t a tuple where the first element is a vertex of type `V`, and the second element is its associated
   *          traversal result of type `T`.
   * @return a new `VertexTraversal` instance of type `VertexTraversal[V, E, T]` containing the updated map.
   */
  override def +(t: (V, T)): VertexTraversal[V, T] =
    super.+(t).asInstanceOf[VertexTraversal[V, T]]

  /**
   * Creates a new traversal instance based on the provided map of vertices and their associated traversal results.
   *
   * @param map a map where the keys are vertices of type `V` and the values are the associated traversal results of type `T`.
   * @return a new traversal instance of type `Traversal[V, T]` constructed with the given map.
   */
  def unit(map: Map[V, T]): Traversal[V, T] = VertexTraversal(map)
}

/**
 * Provides utility methods for creating and working with instances of `VertexTraversal`.
 *
 * A `VertexTraversal` is a concrete implementation of the `Traversal` trait
 * that uses a map to represent the traversal results for graph vertices.
 * This object contains a factory method for creating an empty `VertexTraversal` instance.
 */
object VertexTraversal {
  /**
   * Creates and returns an empty `VertexTraversal` instance.
   *
   * An empty `VertexTraversal` contains no mappings of vertices to their traversal results.
   * This method is useful as a starting point for building a traversal object incrementally.
   *
   * @tparam V The type representing a vertex in the graph.
   * @tparam T The resulting type after traversing a vertex or an edge.
   * @return A new `VertexTraversal` instance with an empty map.
   */
  def empty[V, T]: VertexTraversal[V, T] =
    VertexTraversal(Map.empty[V, T])
}

/**
 * Represents a particular traversal of vertices within a graph resulting from a depth-first-search
 * starting at a particular vertex.
 * Each vertex that is reached (not including the start vertex) is represented in the `connexions` `Map`
 * by a pair of vertices (as a tuple) where the first element is the start of a followed edge and the second
 * element will be the same as the key vertex.
 *
 * CONSIDER converting each pair into a Connexion
 * CONSIDER returning a directed, rooted tree of connexions.
 *
 * @param connexions a map where each key is a vertex of type `V` and its value is a tuple `(V, V)`
 *                   representing the result associated with traversal from this vertex.
 * @tparam V the type representing a vertex in the graph.
 */
case class Connexions[V](connexions: Map[V, (V, V)]) extends AbstractVertexTraversal[V, (V, V)](connexions) {
  /**
   * Creates a new `Traversal` instance with the specified map of vertices and their associated traversal results.
   *
   * @param map a map where each key is a vertex of type `V` and its value is a tuple `(V, V)` representing
   *            the result associated with traversal from this vertex.
   * @return a new `Traversal` instance of type `Traversal[V, (V, V)]` initialized with the provided map.
   */
  def unit(map: Map[V, (V, V)]): Traversal[V, (V, V)] = copy(connexions = map)

  /**
   * Adds a new mapping of a vertex to its corresponding traversal result to the existing map,
   * returning a new `VertexTraversal` instance with the updated mapping.
   *
   * @param t a tuple where the first element is a vertex of type `V`, and the second element is its associated
   *          traversal result of type `T`.
   * @return a new `VertexTraversal` instance of type `VertexTraversal[V, E, T]` containing the updated map.
   */
  override def +(t: (V, (V, V))): Connexions[V] = super.+(t).asInstanceOf[Connexions[V]]
}

/**
 * Provides utility methods for creating and managing instances of the `Connexions` class,
 * which represents a specific traversal of vertices within a graph resulting from
 * a depth-first search starting at a particular vertex.
 */
object Connexions {
  /**
   * Creates a `Connexions` instance by performing a depth-first traversal on the given graph
   * starting from the specified vertex.
   *
   * @param graph the traversable graph structure on which the traversal will be performed
   * @param start the starting vertex for the depth-first traversal
   * @return a `Connexions` instance representing the resulting traversal
   */
  def create[V](graph: core.Traversable[V])(start: V): Connexions[V] = {
    implicit object IterableJournalQueue extends IterableJournalQueue[(V, V)]
    graph.vertexVertexIterableTraversalDfs(start)
  }

  /**
   * Creates an empty instance of the `Connexions` class, representing a traversal with no vertices or edges.
   *
   * @tparam V the type representing a vertex in the graph.
   * @return an empty `Connexions` instance.
   */
  def empty[V]: Connexions[V] =
    Connexions(Map.empty[V, (V, V)])
}

/**
 * Represents a "topological sort" of a directed graph. This class provides functionality
 * to manage the traversal order of vertices in a graph and supports creating customized
 * traversal instances with updated vertex-to-result mappings.
 *
 * @param map a mapping of vertices of type `V` to their associated traversal results of type `Int`.
 *            This map is used to store the state of the traversal.
 * @tparam V the type representing a vertex in the graph.
 */
case class TopologicalSort[V](map: Map[V, Int]) extends AbstractVertexTraversal[V, Int](map) {
  /**
   * Creates a new instance of the traversal with the provided mapping of vertices to their respective traversal results.
   *
   * @param m a map where the keys represent vertices of type `V` and the values represent their associated
   *          traversal results of type `Int`.
   * @return a new `Traversal[V, Int]` instance with the updated mapping.
   */
  def unit(m: Map[V, Int]): Traversal[V, Int] = copy(map = m)
}

/**
 * Provides functionality for topological sorting of directed graphs.
 */
object TopologicalSort {

  /**
   * Performs a topological sort on the given directed graph.
   *
   * The method uses a depth-first search (DFS) traversal to visit all vertices of the graph.
   * It collects vertices in reverse postorder using a specialized `PostVisitor`, ensuring
   * that every vertex appears before its successors in the resulting sequence.
   *
   * @param graph the directed graph to be sorted. It contains vertices and directed edges
   *              representing dependencies or relations between vertices.
   *              Vertices are of type `V`, and edges have attributes of type `E`.
   * @param ev    an implicit `Journal` defining how to record and manage the visitation order
   *              of vertices in the graph during the traversal. The journal accumulates
   *              the vertices in a specific order.
   * @return a sequence of vertices (`Seq[V]`) representing the topologically sorted order
   *         of the graph's vertices.
   */
  def sort[V, E](graph: DirectedGraph[V, E])(implicit ev: Journal[List[V], V]): Seq[V] = {
    val visitor: PostVisitor[V, List[V]] = Visitor.reversePost[V]
    val result = graph.dfsAll(visitor)
    // CONSIDER checking that the edges are properly aligned
    result.journals.head
  }

  /**
   * Performs a topological sort on the given directed graph.
   *
   * The method uses a depth-first search (DFS) traversal to visit all vertices of the graph.
   * It collects vertices in reverse postorder using a specialized `PostVisitor`, ensuring
   * that every vertex appears before its successors in the resulting sequence.
   *
   * @param graph the directed graph to be sorted. It contains vertices and directed edges
   *              representing dependencies or relations between vertices.
   *              Vertices are of type `V`, and edges have attributes of type `E`.
   * @param ev    an implicit `Journal` defining how to record and manage the visitation order
   *              of vertices in the graph during the traversal. The journal accumulates
   *              the vertices in a specific order.
   * @return a sequence of vertices (`Seq[V]`) representing the topologically sorted order
   *         of the graph's vertices.
   */
  def traversal[V, E](graph: DirectedGraph[V, E])(implicit ev: Journal[List[V], V]): TopologicalSort[V] = {
    val result = sort(graph)
    val vertexOrder: Seq[(V, Int)] = for ((v, i) <- result.zipWithIndex) yield v -> i
    TopologicalSort(vertexOrder.toMap)
  }
}
