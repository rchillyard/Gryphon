package com.phasmidsoftware.gryphon.core

import com.phasmidsoftware.gryphon.util.FP.mkStringLimitIterator

/**
 * Represents a vertex in a graph, containing an attribute of type `V` and a collection
 * of adjacencies denoting its connections to other vertices.
 *
 * `Vertex` is immutable. The mutable `discovered` flag has been removed from all
 * implementations; visited-node tracking is now handled by the immutable `VisitedSet[V]`
 * typeclass inside the Visitor 1.8.0 traversal engine.
 *
 * @tparam V the type of the attribute associated with the vertex.
 */
trait Vertex[V] extends Attribute[V]:
  /**
   * Retrieves the collection of adjacencies associated with the vertex.
   *
   * @return an instance of `Adjacencies[V]` representing the connections from this vertex.
   */
  def adjacencies: Adjacencies[V]

  /**
   * Adds a new adjacency to this vertex, returning a new vertex instance.
   *
   * @param a the adjacency to be added.
   * @return a new `Vertex[V]` with the adjacency included.
   */
  def +(a: Adjacency[V]): Vertex[V]

  /**
   * Renders the vertex as a human-readable string.
   *
   * @return a string representation of the vertex.
   */
  def render: String

/**
 * Companion object for `Vertex`, providing factory methods.
 */
object Vertex:
  /**
   * Creates a `SimpleVertex` with the given attribute and adjacencies.
   *
   * @param attribute the data associated with this vertex.
   * @param vau       the initial adjacency collection (defaults to an empty Bag).
   * @return a new `SimpleVertex[V]`.
   */
  def create[V](attribute: V, vau: Adjacencies[V] = emptyAdjacenciesBag[V]): SimpleVertex[V] =
    SimpleVertex(attribute, vau)

  /**
   * Creates a `SimpleVertex` with an empty Bag-backed adjacency collection.
   *
   * @param attribute the data associated with this vertex.
   * @return a new `SimpleVertex[V]`.
   */
  def createWithBag[V](attribute: V): SimpleVertex[V] =
    create(attribute, emptyAdjacenciesBag[V])

  /**
   * Creates a `SimpleVertex` with an empty Set-backed adjacency collection.
   * Appropriate when edges (rather than bare vertices) form the adjacencies.
   *
   * @param attribute the data associated with this vertex.
   * @return a new `SimpleVertex[V]`.
   */
  def createWithSet[V](attribute: V): SimpleVertex[V] =
    create(attribute, emptyAdjacenciesSet[V])

/**
 * Base class for all concrete vertex implementations.
 *
 * @tparam V the type of the vertex attribute.
 * @param attribute   the data associated with this vertex.
 * @param adjacencies the collection of adjacencies.
 */
abstract class AbstractVertex[V](val attribute: V, val adjacencies: Adjacencies[V]) extends Vertex[V]:
  /**
   * Adds an adjacency, delegating construction to `unit`.
   */
  def +(a: Adjacency[V]): AbstractVertex[V] = unit(adjacencies + a)

  /**
   * Constructs a new instance of this vertex type with the given adjacencies,
   * preserving all other fields.
   *
   * @param adjacencies the updated adjacency collection.
   * @return a new vertex of the same concrete type.
   */
  def unit(adjacencies: Adjacencies[V]): AbstractVertex[V]

/**
 * A plain vertex carrying only an attribute and adjacencies.
 * Replaces the old `DiscoverableVertex` now that the `discovered` flag has been removed.
 *
 * @tparam V the type of the vertex attribute.
 * @param attribute   the data associated with this vertex.
 * @param adjacencies the collection of adjacencies.
 */
case class SimpleVertex[V](override val attribute: V, override val adjacencies: Adjacencies[V])
        extends AbstractVertex[V](attribute, adjacencies):

  def unit(adjacencies: Adjacencies[V]): SimpleVertex[V] =
    copy(adjacencies = adjacencies)

  def render: String = s"v$attribute"

  override def toString: String =
    s"v$attribute with adjacencies: ${mkStringLimitIterator(adjacencies.iterator)}"

// ---------------------------------------------------------------------------
// The `DiscoverableVertex` alias that stood here is gone, in 1.14.0 (#160).
//
// It was kept so that call sites outside this file continued to compile when the
// `discovered` flag went and the type became `SimpleVertex`. Nothing in src/main ever
// named it afterwards, and the only two users were tests that each carried a comment
// saying it was an alias -- which is a name earning its keep by being explained rather
// than by being used. `DiscoverableRelaxableVertex` went with `RelaxableVertex` in 1.9.0.
//
// Unlike the other three removals in #160 this alias carried no `since` version, so it
// had given no formal notice. It goes anyway: it aliases a type declared in this same
// file, so the replacement is visible at the point of use.
// ---------------------------------------------------------------------------