package com.phasmidsoftware.gryphon.core

import com.phasmidsoftware.gryphon.util.FP.mkStringLimitIterator

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
case class Vertex[V](attribute: V, adjacencies: Adjacencies[V])(var discovered: Boolean = false) extends Attribute[V]:
  /**
   * Adds a new adjacency to the current vertex and returns a new vertex instance with the
   * added adjacency included in the collection of adjacencies.
   *
   * @param a the adjacency to be added to the vertex.
   *          This adjacency represents a connection to another vertex in the graph structure.
   * @return a new `Vertex[V]` instance with the same attribute and discovered state as the
   *         current vertex, but with the specified adjacency added to the adjacencies.
   */
  def +(a: Adjacency[V]): Vertex[V] =
    Vertex(attribute, adjacencies + a)(discovered)

  /**
   * Mutating method that resets the `discovered` state of this `Vertex` to `false`.
   *
   * @return Unit
   */
  def reset(): Vertex[V] = {
    discovered = false
    this
  }

  /**
   * Marks the current vertex as discovered by setting its internal `discovered` state to `true`.
   * This operates by side-effect.
   *
   * @return the current vertex (`Vertex[V]`) instance with its `discovered` state updated.
   */
  def discover(): Vertex[V] = {
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

/**
 * Companion object for the `Vertex` class, providing a factory method for creating
 * instances of `Vertex`.
 * Designed to construct a vertex with an associated attribute
 * and a dynamically initialized collection of adjacencies.
 */
object Vertex:
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
  def create[V](attribute: V, vau: Adjacencies[V] = emptyAdjacenciesBag[V]): Vertex[V] =
    Vertex[V](attribute, vau)()

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
  def createWithBag[V](attribute: V): Vertex[V] =
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
  def createWithSet[V](attribute: V): Vertex[V] =
    create(attribute, emptyAdjacenciesSet[V])
