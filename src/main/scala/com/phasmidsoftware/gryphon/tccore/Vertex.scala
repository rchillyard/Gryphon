/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.tccore

/**
 * Trait to model the behavior of a Vertex.
 *
 * @tparam V the key (attribute) type of this Vertex.
 * @tparam X the "edge" type for the adjacent edges of this Vertex. A sub-type of EdgeLike[V].
 * @tparam P the (mutable) property of this Vertex.
 */
trait Vertex[V, +X <: EdgeLike[V], P] extends VertexLike[V] with Property[P] {
  /**
   * Method to add an edge (x) to this Vertex.
   *
   * @param y the EdgeLike[V] object to be added this Vertex.
   * @tparam Y a super-type of X.
   * @return a Vertex[V, Y]
   */
  def addEdge[Y >: X <: EdgeLike[V]](y: Y): Vertex[V, Y, P]

  /**
   * The adjacency list, an AdjacencyList[X], for this Vertex.
   */
  val adjacent: AdjacencyList[X]

  /**
   * The (out) degree of this Vertex.
   *
   * @return the size of adjacent.
   */
  def degree: Int = adjacent.size

  def discovered: Boolean

  def reset(): Unit
}

/**
 * Trait to model the behavior of a vertex-like object.
 *
 * @tparam V the (covariant) attribute type.
 */
trait VertexLike[+V] extends Attributed[VertexLike[V], V]

/**
 * Abstract base class to represent an vertex.
 *
 * @tparam V the key (attribute) type of this Vertex.
 * @tparam X the "edge" type for the adjacent edges of this Vertex. A sub-type of EdgeLike[V].
 */
abstract class AbstractVertex[V, X <: EdgeLike[V], P] extends Vertex[V, X, P] {

  private var visited: Boolean = false
  private var property: Option[P] = None

  override def toString: String = s"visited: $visited, property: $property"

  def discovered: Boolean = {
    val result = visited
    visited = true
    result
  }

  def reset(): Unit = {
    visited = false
  }

  /**
   * Method to yield the current value of this property.
   *
   * @return property.
   */
  def getProperty: Option[P] = property

  /**
   * Mutating method to set the property.
   *
   * @param p the new value of the property.
   */
  def setProperty(p: Option[P]): Unit = {
    property = p
  }

  /**
   * Method to add an edge to this AbstractVertex.
   *
   * @param y the edge to add.
   * @tparam Y a super-type of X.
   * @return a new AbstractVertex which includes the new edge in its adjacency list.
   */
  def addEdge[Y >: X <: EdgeLike[V]](y: Y): Vertex[V, Y, P] = unit(AdjacencyList(y +: adjacent.xs))

  /**
   * Method to construct a new AbstractVertex.
   *
   * @param adjacent an AdjacencyList[Y].
   * @tparam Y the edge-type of the resulting AbstractVertex
   * @return a new AbstractVertex[V, Y].
   */
  def unit[W >: V, Y <: EdgeLike[W]](adjacent: AdjacencyList[Y]): AbstractVertex[W, Y, P]
}

/**
 * Case class to represent a concrete Vertex.
 * NOTE: clients usually will not need to reference this class directly: instead use Vertex.empty.
 *
 * @param attribute (V) the attribute/key of the resulting Vertex.
 * @param adjacent  (X) the adjacency list of the resulting Vertex.
 * @tparam V the key (attribute) type of this Vertex.
 * @tparam X the "edge" type for the adjacent edges of this Vertex (a sub-type of EdgeLike[V]).
 */
case class VertexCase[V, X <: EdgeLike[V], P](attribute: V, adjacent: AdjacencyList[X]) extends AbstractVertex[V, X, P] {

  /**
   * Method to construct a new ConcreteVersion based on the types V and X.
   *
   * @param adjacent an AdjacencyList[Y].
   * @tparam W the vertex-type of the result (W must be a super-type of V).
   * @tparam Y the edge-type of the resulting AbstractVertex
   * @return a new VertexCase[W, Y].
   */
  def unit[W >: V, Y <: EdgeLike[W]](adjacent: AdjacencyList[Y]): AbstractVertex[W, Y, P] = VertexCase(attribute, adjacent)

  override def toString: String = s"VertexCase($attribute, $adjacent, ${super.toString}"
}

/**
 * Object to provide non-instance vertex properties.
 */
object Vertex {
  /**
   * Method to construct an empty Vertex.
   *
   * @param a (V) the (key) attribute of the result.
   * @tparam V the underlying vertex-type of the result.
   * @tparam X the "edge" type for the adjacent edges of this Vertex (a sub-type of EdgeLike[V]).
   * @return an empty VertexCase[V, X].
   */
  def empty[V, X <: EdgeLike[V], P](a: V): Vertex[V, X, P] = VertexCase(a, AdjacencyList.empty)
}
