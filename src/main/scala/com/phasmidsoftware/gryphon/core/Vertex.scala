package com.phasmidsoftware.gryphon.core

import com.phasmidsoftware.gryphon.util.FP.mkStringLimitIterator

import scala.math.Ordered.orderingToOrdered

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
  def createRelaxableWithSet[V, R: Numeric](attribute: V): DiscoverableRelaxableVertex[V, R] =
    DiscoverableRelaxableVertex(attribute, emptyAdjacenciesSet[V], implicitly[Numeric[R]].zero)(false)
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
abstract class AbstractDiscoverableVertex[V](attribute: V, adjacencies: Adjacencies[V]) extends AbstractVertex[V](attribute, adjacencies) with Discoverable[V] {
  /**
   * Adds the specified adjacency to the current vertex, creating a new `DiscoverableVertex` instance
   * with the updated set of adjacencies.
   *
   * The adjacency defines a relationship between the current vertex and an adjacent vertex in the graph structure.
   *
   * @param a the adjacency to be added, defining a relationship with another vertex
   * @return a new instance of `DiscoverableVertex[V]` with the added adjacency
   */
  def +(a: Adjacency[V]): AbstractDiscoverableVertex[V] =
    unit(adjacencies + a)

  /**
   * Creates a new instance of `AbstractDiscoverableVertex` initialized with the specified adjacencies.
   *
   * @param adjacencies the collection of adjacencies representing the connections between this vertex
   *                    and other vertices in the graph.
   * @return a new `AbstractDiscoverableVertex` instance with the given adjacencies.
   */
  def unit(adjacencies: Adjacencies[V]): AbstractDiscoverableVertex[V]
}

/**
 * Represents a discoverable vertex in a graph with an attribute and adjacencies.
 * This vertex can be marked as discovered or undiscovered during graph traversal.
 *
 * @tparam V the type associated with the attribute of the vertex.
 * @param attribute   the data or value associated with this vertex.
 * @param adjacencies the collection representing connections of this vertex to others in the graph.
 * @param discovered  the initial discovered state of the vertex, defaults to `false`.
 */
case class DiscoverableVertex[V](attribute: V, adjacencies: Adjacencies[V])(var discovered: Boolean = false) extends AbstractDiscoverableVertex[V](attribute, adjacencies) {

  /**
   * Creates a new instance of `DiscoverableVertex` with the specified adjacencies.
   *
   * @param adjacencies the new set of adjacencies to associate with this vertex.
   * @return A new `DiscoverableVertex` instance with the updated adjacencies while preserving the current `discovered` state.
   */
  def unit(adjacencies: Adjacencies[V]): DiscoverableVertex[V] =
    copy(adjacencies = adjacencies)(discovered)

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

/**
 * Represents a vertex in a graph that combines discovery functionality and a relaxable property,
 * suitable for use in graph traversal or pathfinding algorithms.
 *
 * CONSIDER making r a var property.
 *
 * This vertex extends the capabilities of `AbstractDiscoverableVertex` to include a mutable
 * relaxable property `r` with an associated ordering. Additionally, it supports discovery state
 * management through the `discovered` property.
 *
 * @tparam V the type of the data attribute associated with the vertex.
 * @tparam R the type of the relaxable property, which must have an implicit `Ordering` in scope.
 * @param attribute   the data or value associated with this vertex.
 * @param adjacencies the collection representing connections of this vertex to others in the graph.
 * @param r           the initial value of the relaxable property.
 * @param discovered  the initial discovery state of the vertex (default is `false`).
 */
case class DiscoverableRelaxableVertex[V, R: Ordering](attribute: V, adjacencies: Adjacencies[V], r: R)(var discovered: Boolean = false) extends AbstractDiscoverableVertex[V](attribute, adjacencies) with Ordered[DiscoverableRelaxableVertex[V, R]] {
  def compare(that: DiscoverableRelaxableVertex[V, R]): Int = implicitly[Ordering[R]].compare(r, that.r)

  /**
   * Creates a new instance of `DiscoverableVertex` with the specified adjacencies.
   *
   * @param adjacencies the new set of adjacencies to associate with this vertex.
   * @return A new `DiscoverableVertex` instance with the updated adjacencies while preserving the current `discovered` state.
   */
  def unit(adjacencies: Adjacencies[V]): DiscoverableRelaxableVertex[V, R] =
    copy(adjacencies = adjacencies)(discovered)

  /**
   * Updates the relaxable property of the vertex to the given value if the specified
   * value is smaller than the current value. Otherwise, returns the vertex unchanged.
   *
   * @param r the new relaxable value to compare with the current value of the vertex.
   * @return a new instance of `DiscoverableRelaxableVertex[V, R]` with the updated relaxable value
   *         if the provided value is smaller; otherwise, the original vertex instance.
   */
  def relax(r: R): DiscoverableRelaxableVertex[V, R] =
    if (r < this.r)
      copy(r = r)(discovered)
    else
      this

  /**
   * Mutating method that resets the `discovered` state of this `DiscoverableVertex` to `false`.
   *
   * @return Unit
   */
  def reset(): DiscoverableRelaxableVertex[V, R] = {
    discovered = false
    this
  }

  /**
   * Marks the current DiscoverableVertex as discovered by setting its internal `discovered` state to `true`.
   * This operates by side effect.
   *
   * @return the current vertex (`Vertex[V]`) instance with its `discovered` state updated.
   */
  def discover(): DiscoverableRelaxableVertex[V, R] = {
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

/**
 * Companion object for the `DiscoverableRelaxableVertex` class, providing utility methods
 * to create and interact with instances of `DiscoverableRelaxableVertex`.
 */
object DiscoverableRelaxableVertex {
  /**
   * Creates a new instance of `DiscoverableRelaxableVertex` from a given `DiscoverableVertex`.
   * This method transfers the attribute and adjacencies of the provided vertex
   * to create a new `DiscoverableRelaxableVertex`. The relaxable property is initialized
   * to its default value, as determined by the implicit `Numeric` instance for the type `R`.
   *
   * @param v the `DiscoverableVertex` containing the attribute and adjacencies to use for creating
   *          the new `DiscoverableRelaxableVertex`.
   * @return A new `DiscoverableRelaxableVertex` instance with the same attribute and adjacencies
   *         as the input vertex, and a default relaxable value of zero.
   */
  def apply[V, R: Numeric](v: DiscoverableVertex[V]): DiscoverableRelaxableVertex[V, R] =
    apply(v.attribute, v.adjacencies)

  /**
   * Creates a new instance of `DiscoverableRelaxableVertex`, representing a vertex in a graph
   * that combines discovery functionality and a relaxable property.
   *
   * @param attribute   the data or value associated with this vertex.
   * @param adjacencies the collection representing the connections of this vertex to others in the graph.
   * @return A new instance of `DiscoverableRelaxableVertex` with the given attribute, adjacencies,
   *         and a default relaxable value of zero.
   */
  def apply[V, R: Numeric](attribute: V, adjacencies: Adjacencies[V]): DiscoverableRelaxableVertex[V, R] =
    DiscoverableRelaxableVertex(attribute, adjacencies, implicitly[Numeric[R]].zero)(false)
}