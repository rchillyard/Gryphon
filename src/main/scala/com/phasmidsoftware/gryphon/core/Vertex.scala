package com.phasmidsoftware.gryphon.core

import com.phasmidsoftware.gryphon.util.FP.mkStringLimitIterator

/**
 * Represents a vertex in a graph, which contains an attribute of type `V` and maintains a collection
 * of adjacencies that denote its connections to other vertices.
 *
 * A `Vertex` is immutable and can be extended to define specific behaviors, offering functional
 * constructs for creating and manipulating graph structures. It serves as one of the fundamental
 * units in the graph's representation, primarily defined by its attribute and adjacencies.
 *
 * @tparam V the type of the attribute associated with the vertex.
 */
trait Vertex[V] extends Attribute[V] {
  /**
   * Retrieves the collection of adjacencies associated with the vertex.
   *
   * Adjacencies represent the connections of the vertex to other vertices
   * in the graph structure. These connections can be accessed and manipulated 
   * to explore or update the graph's topology.
   *
   * @return an instance of `Adjacencies[V]` representing the set of connections
   *         from this vertex to other vertices.
   */
  def adjacencies: Adjacencies[V]

  /**
   * Adds a new adjacency to the current vertex and returns a new vertex instance with the
   * added adjacency included in the collection of adjacencies.
   *
   * @param a the adjacency to be added to the vertex.
   *          This adjacency represents a connection to another vertex in the graph structure.
   * @return a new `Vertex[V]` instance with the same attribute and discovered state as the
   *         current vertex, but with the specified adjacency added to the adjacencies.
   */
  def +(a: Adjacency[V]): Vertex[V]
}

/**
 * Companion object for the `Vertex` class, providing a factory method for creating
 * instances of `Vertex`.
 * Designed to construct a vertex with an associated attribute
 * and a dynamically initialized collection of adjacencies.
 */
object Vertex {
  /**
   * Creates a new instance of `Vertex` with the specified attribute and an optional
   * collection of adjacencies.
   *
   * @param attribute the data or value associated with this vertex of type `V`.
   * @param vau       an optional unordered collection of adjacencies representing connections
   *                  to other vertices.
   *                  Defaults to an empty `Unordered_Bag` of adjacencies.
   * @return a newly created `Vertex[V]` instance with the specified attribute and the
   *         provided or default adjacencies.
   */
  def create[V](attribute: V, vau: Adjacencies[V] = emptyAdjacenciesBag[V]): DiscoverableVertex[V] =
    DiscoverableVertex(attribute, vau)()

  /**
   * Creates a new `Vertex` instance with the specified attribute and an empty
   * collection of adjacencies initialized as an `Unordered_Bag`.
   *
   * This method is a simplified factory for creating a vertex where
   * the adjacency collection is designed to be empty initially.
   *
   * @param attribute the attribute associated with the vertex, representing its data or value.
   * @return a new `Vertex[V]` instance with the specified attribute and an empty
   *         collection of adjacencies.
   */
  def createWithBag[V](attribute: V): DiscoverableVertex[V] =
    create(attribute, emptyAdjacenciesBag[V])

  /**
   * Creates a new `Vertex` instance with the specified attribute and an empty
   * collection of adjacencies initialized as an `Unordered_Set`.
   *
   * This method serves as a factory for creating a vertex where the adjacency
   * collection is tailored for edges, allowing the representation of connections
   * between vertices in the form of edges.
   *
   * @param attribute the attribute associated with the vertex, representing its data or value.
   * @tparam V the type representing the attribute of the vertex.
   * @return a new `Vertex[V]` instance with the specified attribute and an empty
   *         collection of `AdjacencyEdge[V, E]`.
   */
  def createWithSet[V](attribute: V): DiscoverableVertex[V] =
    create(attribute, emptyAdjacenciesSet[V])
}

/**
 * Represents a vertex in a graph structure with an associated attribute and a collection
 * of adjacencies.
 * Each vertex can optionally be marked as discovered during traversal,
 * which is useful for graph algorithms.
 *
 * @tparam V the type representing the attribute of the vertex (invariant).
 * @param attribute   the data or value associated with this vertex.
 * @param adjacencies an unordered collection of adjacencies representing the connections
 *                    of this vertex to others in the graph.
 * @param discovered  an optional flag indicating whether this vertex has been discovered 
 *                    in a traversal.
 *                    Defaults to `false`.
 */
abstract class AbstractVertex[V](attribute: V, adjacencies: Adjacencies[V]) extends Vertex[V]

/**
 * Represents an abstract vertex in a graph, capable of being marked as discovered or undiscovered
 * during traversal. Combines the functionality of `AbstractVertex` and `Discoverable` traits.
 *
 * This class serves as a base for graph vertices that need discovery functionality, typically used in graph
 * traversal algorithms such as Depth-First Search or Breadth-First Search.
 *
 * @tparam V the type associated with the attribute of the vertex.
 * @param attribute   the data or value associated with this vertex.
 * @param adjacencies the collection representing connections of this vertex to others in the graph.
 */
abstract class AbstractDiscoverableVertex[V](attribute: V, adjacencies: Adjacencies[V]) extends AbstractVertex[V](attribute, adjacencies) with Discoverable[V]

/**
 * Represents a vertex that can be marked as discovered or undiscovered in a graph traversal.
 * Extends `AbstractDiscoverableVertex` to provide additional functionality for managing the discovery
 * state and adjacencies in a graph. The vertex contains an attribute of type `V` and a collection
 * of adjacencies representing its connections to other vertices.
 *
 * @tparam V the type associated with the vertex's attribute.
 * @param attribute   the data or value associated with this vertex.
 * @param adjacencies the collection of adjacencies representing connections to other vertices.
 * @param discovered  optional parameter, defaulting to `false`, indicating whether the vertex has been discovered.
 */
case class DiscoverableVertex[V](attribute: V, adjacencies: Adjacencies[V])(var discovered: Boolean = false) extends AbstractDiscoverableVertex[V](attribute, adjacencies) {
  /**
   * Adds the specified adjacency to the current vertex, creating a new `DiscoverableVertex` instance
   * with the updated set of adjacencies.
   *
   * The adjacency defines a relationship between the current vertex and an adjacent vertex in the graph structure.
   *
   * @param a the adjacency to be added, defining a relationship with another vertex
   * @return a new instance of `DiscoverableVertex[V]` with the added adjacency
   */
  def +(a: Adjacency[V]): DiscoverableVertex[V] =
    DiscoverableVertex(attribute, adjacencies + a)(discovered).asInstanceOf[DiscoverableVertex[V]]

  /**
   * Mutating method that resets the `discovered` state of this `DiscoverableVertex` to `false`.
   *
   * @return Unit
   */
  def reset(): DiscoverableVertex[V] = {
    discovered = false
    this
  }

  /**
   * Marks the current DiscoverableVertex as discovered by setting its internal `discovered` state to `true`.
   * This operates by side effect.
   *
   * @return the current vertex (`Vertex[V]`) instance with its `discovered` state updated.
   */
  def discover(): DiscoverableVertex[V] = {
    discovered = true
    this
  }

  /**
   * Checks if the vertex is not discovered.
   * NOTE that this must remain a def (not a lazy val) because the `discovered` property is mutable.
   *
   * This method inversely reflects the `discovered` property of a vertex.
   * If a vertex is marked as not discovered, this method returns `true`.
   *
   * @return `true` if the vertex has not been discovered, `false` otherwise.
   */
  def undiscovered: Boolean = !discovered

  override def toString: String =
    s"v$attribute with adjacencies: ${mkStringLimitIterator(adjacencies.iterator)} and discovered = $discovered"
}
