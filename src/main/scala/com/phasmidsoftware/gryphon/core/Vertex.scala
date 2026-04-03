package com.phasmidsoftware.gryphon.core

import com.phasmidsoftware.gryphon.util.FP.mkStringLimitIterator
import com.phasmidsoftware.visitor.core.Monoid
import scala.math.Ordered.orderingToOrdered

/**
 * Represents a vertex in a graph, containing an attribute of type `V` and a collection
 * of adjacencies denoting its connections to other vertices.
 *
 * `Vertex` is immutable. The mutable `discovered` flag has been removed from all
 * implementations; visited-node tracking is now handled by the immutable `VisitedSet[V]`
 * typeclass inside the Visitor V1.2.0 traversal engine.
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
   * Creates a `RelaxableVertex` with an empty Set-backed adjacency collection.
   * Suitable for Dijkstra-style algorithms that need to relax edge weights.
   *
   * @param attribute the data associated with this vertex.
   * @tparam R the type of the relaxable property; must have both `Monoid` and `Ordering` instances.
   * @return a new `RelaxableVertex[V, R]`.
   */
  def createRelaxableWithSet[V, R: {Monoid, Ordering}](attribute: V): RelaxableVertex[V, R] =
    RelaxableVertex(attribute, emptyAdjacenciesSet[V])()

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

/**
 * A vertex that additionally carries an optional relaxable property `maybeR`, used in
 * Dijkstra-style shortest-path algorithms.
 *
 * The `discovered` flag has been removed; only the relaxable property `maybeR` is retained
 * as mutable state (Dijkstra relaxation requires in-place update semantics on the vertex).
 *
 * @tparam V the type of the vertex attribute.
 * @tparam R the type of the relaxable property; must have an `Ordering`.
 * @param attribute   the data associated with this vertex.
 * @param adjacencies the collection of adjacencies.
 * @param maybeR      the current best-known cost to reach this vertex, if set.
 */
case class RelaxableVertex[V, R: Ordering](
                                                  override val attribute: V,
                                                  override val adjacencies: Adjacencies[V]
                                          )(var maybeR: Option[R] = None)
        extends AbstractVertex[V](attribute, adjacencies)
                with Ordered[RelaxableVertex[V, R]]:

  def compare(that: RelaxableVertex[V, R]): Int =
    (for t <- maybeR; o <- that.maybeR
      yield implicitly[Ordering[R]].compare(t, o)).getOrElse(0)

  def unit(adjacencies: Adjacencies[V]): RelaxableVertex[V, R] =
    copy(adjacencies = adjacencies)(maybeR)

  /**
   * Updates `maybeR` to `r` if `r` is smaller than the current value (or if unset).
   *
   * @param r the candidate relaxation value.
   * @return this vertex (mutated in place) if relaxed, otherwise this vertex unchanged.
   */
  def relax(r: R): RelaxableVertex[V, R] =
    if maybeR.isEmpty || r < maybeR.get then
      maybeR = Some(r)
      this
    else this

  def render: String = s"v$attribute(maybeR=$maybeR)"

  override def toString: String =
    s"v$attribute with maybeR=$maybeR and adjacencies: ${mkStringLimitIterator(adjacencies.iterator)}"

/**
 * Companion object for `RelaxableVertex`, providing factory methods.
 */
object RelaxableVertex:
  /**
   * Creates a `RelaxableVertex` from an existing `SimpleVertex`, transferring its
   * attribute and adjacencies. `maybeR` is initialised to `None`.
   *
   * @param v the source `SimpleVertex`.
   * @return a new `RelaxableVertex[V, R]`.
   */
  def apply[V, R: {Monoid, Ordering}](v: SimpleVertex[V]): RelaxableVertex[V, R] =
    new RelaxableVertex(v.attribute, v.adjacencies)()

  /**
   * Creates a `RelaxableVertex` with the given attribute and adjacencies.
   * `maybeR` is initialised to `None`.
   */
  def apply[V, R: {Monoid, Ordering}](attribute: V, adjacencies: Adjacencies[V]): RelaxableVertex[V, R] =
    new RelaxableVertex(attribute, adjacencies)()

// ---------------------------------------------------------------------------
// Backward-compatibility type aliases
//
// The old `DiscoverableVertex` and `DiscoverableRelaxableVertex` names are
// preserved as aliases so that call sites outside this file continue to
// compile without changes during the migration.
// ---------------------------------------------------------------------------

/** @deprecated Use [[SimpleVertex]] instead. */
type DiscoverableVertex[V] = SimpleVertex[V]

/** @deprecated Use [[RelaxableVertex]] instead. */
type DiscoverableRelaxableVertex[V, R] = RelaxableVertex[V, R]