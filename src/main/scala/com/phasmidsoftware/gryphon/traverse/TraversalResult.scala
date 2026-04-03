package com.phasmidsoftware.gryphon.traverse

import com.phasmidsoftware.gryphon.adjunct.{AttributedDirectedEdge, DirectedEdge, UndirectedEdge}
import com.phasmidsoftware.gryphon.core
import com.phasmidsoftware.gryphon.core.{Connexion, Edge}
import com.phasmidsoftware.gryphon.util.GraphException

/**
 * A trait that defines the behavior of traversal over vertices and edges within a graph-like structure.
 *
 * @tparam V the type representing a vertex in the graph.
 * @tparam T the resulting type after traversing a vertex or an edge.
 */
trait TraversalResult[V, T] {
  /**
   * Returns the number of key-value pairs in the map.
   *
   * @return the size of the map as an integer
   */
  def size: Int

  /**
   * Retrieves the set of keys from the underlying map that represents the traversal result.
   *
   * @return a set of keys of type `V` contained in the underlying map.
   */
  def keySet: Set[V]

  /**
   * Traverses a given vertex in the graph and returns the result of the traversal.
   *
   * @param v the vertex to be traversed.
   * @return the result of the traversal for the provided vertex.
   */
  def vertexTraverse(v: V): Option[T]

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
object TraversalResult {
  /**
   * Performs a traversal over the edges of a graph and applies a function to each edge, returning the results
   * as a `TraversalResult` object.
   *
   * @param f a function applied to each edge, transforming each `Edge[V, E]` into a value of type `T`
   * @param traversable the graph or data structure implementing the `core.Traversable` interface over edge type `E`
   * @tparam V the type of vertices connected by the edges in the graph
   * @tparam E the type of the attributes associated with the edges in the graph
   * @tparam T the resulting type after applying the function `f` to each edge
   * @return a `TraversalResult[V, T]` containing the results of applying the function `f` to each edge of the graph
   * @throws GraphException if the provided `traversable` does not represent an edge graph
   */
  def edgeTraversal[V, E, T](f: Edge[V, E] => T)(traversable: core.EdgeTraversable[V, E]): TraversalResult[Int, T] =
    EdgeTraversalResult(traversable.edges.foldLeft(List[T]())((list, edge) => f(edge) :: list))
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
abstract class AbstractEdgeTraversalResult[T](ts: List[T]) extends TraversalResult[Int, T] {
  /**
   * Returns the number of key-value pairs in the map.
   *
   * @return the size of the map as an integer
   */
  def size: Int = ts.size

  /**
   * Retrieves the set of keys from the underlying map that represents the traversal result.
   *
   * @return a set of keys of type `V` contained in the underlying map.
   */
  def keySet: Set[Int] = Range(0, ts.size).toSet

  /**
   * Traverses a given vertex in the graph and returns the result of the traversal.
   *
   * @param v the vertex to be traversed.
   * @return the result of the traversal for the provided vertex.
   */
  def vertexTraverse(v: Int): Option[T] = None

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
abstract class AbstractVertexTraversalResult[V, T](map: Map[V, T]) extends TraversalResult[V, T] {
  /**
   * Returns the number of key-value pairs in the map.
   *
   * @return the size of the map as an integer
   */
  def size: Int = map.size

  /**
   * Retrieves the set of keys from the underlying map that represents the traversal result.
   *
   * @return a set of keys of type `V` contained in the underlying map.
   */
  def keySet: Set[V] = map.keySet

  /**
   * Traverses the specified vertex in the graph and retrieves the associated traversal result
   * from the underlying map. If the vertex does not exist, a `NoSuchElementException` is thrown.
   *
   * @param v the vertex to be traversed and looked up in the map.
   * @return the traversal result of type `T` associated with the provided vertex.
   * @throws NoSuchElementException if the vertex does not exist in the map.
   */
  def vertexTraverse(v: V): Option[T] =
    map.get(v)

  /**
   * Traverses a specified edge within a graph-like structure and produces a result of type T.
   *
   * @param x the index of the edge to be traversed
   * @return the result of the traversal
   */
  def edgeTraverse(x: Int): T =
    throw GraphException(s"edgeTraverse called on AbstractVertexTraversalResult: $x")

  /**
   * Constructs a traversal instance using the provided map of vertices and their associated traversal results.
   *
   * @param map a mapping where keys represent vertices of type `V`, and values represent traversal results of type `T`.
   * @return a new `TraversalResult[V, T]` instance built from the specified map.
   */
  def unit(map: Map[V, T]): TraversalResult[V, T]

  /**
   * Adds a new mapping of a vertex to its corresponding traversal result to the existing map,
   * returning a new `VertexTraversalResult` instance with the updated mapping.
   *
   * @param t a tuple where the first element is a vertex of type `V`, and the second element is its associated
   *          traversal result of type `T`.
   * @return a new `VertexTraversalResult` instance of type `VertexTraversalResult[V, E, T]` containing the updated map.
   */
  def +(t: (V, T)): TraversalResult[V, T] =
    unit(map + t)
}

/**
 * A case class representing the concrete implementation of edge traversal for a graph-like structure.
 *
 * This class enables the traversal of edges by providing a list of precomputed results of type `T`
 * associated with each edge.
 *
 * Edge traversal uses the defined behavior from the `AbstractEdgeTraversalResult` base class,
 * where `vertexTraverse` throws an exception, and `edgeTraverse` retrieves the traversal result
 * from the precomputed list based on the edge index.
 *
 * @tparam V the type representing a vertex in the graph.
 * @tparam E the type representing an edge in the graph.
 * @tparam T the type of results for the edge traversal.
 * @param ts a list of elements of type `T` representing the computed results of edge traversal.
 */
case class EdgeTraversalResult[V, E, T](ts: List[T]) extends AbstractEdgeTraversalResult[T](ts)

/**
 * A concrete implementation of the `TraversalResult` trait that uses a map to represent
 * vertex traversals. Each vertex in the graph is associated with a traversal result.
 *
 * CONSIDER this class and its companion object are only used for unit testing. They could be eliminated.
 *
 * @constructor Creates a `VertexTraversalResult` with the given map of vertices and their associated traversal outputs.
 * @param map A map where each key is a vertex of type `V`, and the value is the corresponding traversal result of type `T`.
 * @tparam V The type representing a vertex in the graph.
 * @tparam T The resulting type after traversing a vertex or an edge.
 */
case class VertexTraversalResult[V, T](map: Map[V, T]) extends AbstractVertexTraversalResult[V, T](map) {
  /**
   * Adds a new mapping of a vertex to its corresponding traversal result to the existing map,
   * returning a new `VertexTraversalResult` instance with the updated mapping.
   *
   * @param t a tuple where the first element is a vertex of type `V`, and the second element is its associated
   *          traversal result of type `T`.
   * @return a new `VertexTraversalResult` instance of type `VertexTraversalResult[V, E, T]` containing the updated map.
   */
  override def +(t: (V, T)): VertexTraversalResult[V, T] =
    super.+(t).asInstanceOf[VertexTraversalResult[V, T]]

  /**
   * Creates a new traversal instance based on the provided map of vertices and their associated traversal results.
   *
   * @param map a map where the keys are vertices of type `V` and the values are the associated traversal results of type `T`.
   * @return a new traversal instance of type `TraversalResult[V, T]` constructed with the given map.
   */
  def unit(map: Map[V, T]): TraversalResult[V, T] =
    VertexTraversalResult(map)
}

/**
 * Provides utility methods for creating and working with instances of `VertexTraversalResult`.
 *
 * A `VertexTraversalResult` is a concrete implementation of the `TraversalResult` trait
 * that uses a map to represent the traversal results for graph vertices.
 * This object contains a factory method for creating an empty `VertexTraversalResult` instance.
 */
object VertexTraversalResult {
  /**
   * Creates and returns an empty `VertexTraversalResult` instance.
   *
   * An empty `VertexTraversalResult` contains no mappings of vertices to their traversal results.
   * This method is useful as a starting point for building a traversal object incrementally.
   *
   * @tparam V The type representing a vertex in the graph.
   * @tparam T The resulting type after traversing a vertex or an edge.
   * @return A new `VertexTraversalResult` instance with an empty map.
   */
  def empty[V, T]: VertexTraversalResult[V, T] =
    VertexTraversalResult(Map.empty[V, T])
}

/**
 * Represents a particular traversal of vertices within a graph resulting from a depth-first-search
 * starting at a particular vertex.
 * Each vertex that is reached (not including the start vertex) is represented in the `connexions` `Map`
 * by a `DirectedEdge` where the "white" (first) vertex is the start of a followed edge and the "black" (second)
 * vertex will be the same as the key vertex.
 * NOTE that not all edges will be followed and different random seeds will result in different results.
 *
 * CONSIDER returning a directed, rooted tree of DirectedEdge.
 *
 * @param connexions a map where each key is a vertex of type `V` and its value is a tuple `(V, V)`
 *                   representing the result associated with traversal from this vertex.
 * @tparam V the type representing a vertex in the graph.
 */
case class Connexions[V, E](connexions: Map[V, DirectedEdge[V, E]]) extends AbstractVertexTraversalResult[V, DirectedEdge[V, E]](connexions) {
  /**
   * Creates a new `TraversalResult` instance with the specified map of vertices and their associated traversal results.
   *
   * @param map a map where each key is a vertex of type `V` and its value is a tuple `(V, V)` representing
   *            the result associated with traversal from this vertex.
   * @return a new `TraversalResult` instance of type `TraversalResult[V, (V, V)]` initialized with the provided map.
   */
  def unit(map: Map[V, DirectedEdge[V, E]]): TraversalResult[V, DirectedEdge[V, E]] =
    copy(connexions = map)

  /**
   * Adds a connexion between the specified vertex and another vertex defined in the given `Connexion` instance.
   * If the `Connexion` is directed, it is added as-is. If the `Connexion` is undirected, it is converted into a directed connexion
   * pointing from the specified vertex to the other vertex before being added.
   *
   * @param v         the vertex to which the connexion is to be added.
   * @param connexion the `Connexion` instance representing the connexion details (directed or undirected) between the vertices.
   * @return a new `Connexions[V, E]` instance with the updated connexion added.
   * @throws GraphException if the provided connexion type is unexpected.
   */
  def addConnexion(v: V, connexion: Connexion[V]): Connexions[V, E] = connexion match {
    case d@AttributedDirectedEdge[V, E] (_, _, _) =>
  copy (connexions = connexions + (v -> d) )
    case u@UndirectedEdge[V, E] (q, _, _) =>
  copy (connexions = connexions + (v -> AttributedDirectedEdge (q, u.other (v), v) ) )
    case _ =>
      throw GraphException(s"getConnexions: unexpected connexion: $connexion")
  }

  /**
   * Adds a new mapping of a vertex to its corresponding traversal result to the existing map,
   * returning a new `VertexTraversalResult` instance with the updated mapping.
   *
   * @param t a tuple where the first element is a vertex of type `V`, and the second element is its associated
   *          traversal result of type `T`.
   * @return a new `VertexTraversalResult` instance of type `VertexTraversalResult[V, E, T]` containing the updated map.
   */
  override def +(t: (V, DirectedEdge[V, E])): Connexions[V, E] =
    super.+(t).asInstanceOf[Connexions[V, E]]
}

/**
 * Provides utility methods for creating and managing instances of the `ProtoConnexions` class,
 * which represents a specific traversal of vertices within a graph resulting from
 * a depth-first search starting at a particular vertex.
 */
object Connexions {
  /**
   * Creates a `ProtoConnexions` instance representing a traversal of a graph using depth-first search (DFS).
   * The traversal starts from the specified vertex, processes each vertex using the provided function,
   * and returns the resulting connexions.
   *
   * @param graph the graph-like structure to be traversed, of type `core.Traversable[V]`.
   * @param start the starting vertex for the DFS traversal, of type `V`.
   * @tparam V the type representing a vertex in the graph.
   * @tparam E the type representing the edge attribute in the traversal.
   * @return a `ProtoConnexions` object that encapsulates the result of the traversal,
   *         mapping vertices to their respective directed edges.
   */
  def create[V, E](graph: core.Traversable[V])(start: V): Connexions[V, E] = {
    graph.getConnexions(start)
  }

  /**
   * Creates an empty instance of the `ProtoConnexions` class, representing a traversal with no vertices or edges.
   *
   * @tparam V the type representing a vertex in the graph.
   * @return an empty `ProtoConnexions` instance.
   */
  def empty[V, E]: Connexions[V, E] =
    Connexions(Map.empty[V, DirectedEdge[V, E]])
}
