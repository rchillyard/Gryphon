package com.phasmidsoftware.gryphon.adjunct

import com.phasmidsoftware.gryphon.core.{Directed, Edge, EdgeType}

/**
 * An abstract trait representing a directed edge in a graph.
 *
 * This trait extends the `Edge` trait, providing a specific implementation for the `edgeType` method,
 * which always returns `Directed`. The directed edge represents a one-way connection between two vertices.
 *
 * @tparam E the type of the attribute associated with the edge.
 * @tparam V the type of the vertices connected by the edge.
 */
trait DirectedEdge[E, V] extends Edge[E, V]:
  /**
   * Returns the type of the edge represented by this object.
   *
   * @return the edge type, which is always `Directed` for this implementation.
   */
  def edgeType: EdgeType = Directed

/**
 * Represents a directed edge in a graph structure, connecting a starting vertex (`from`)
 * to an ending vertex (`to`) while carrying an associated attribute.
 *
 * @tparam E the type of the attribute associated with the edge.
 * @tparam V the type of the vertex attributes connected by the edge.
 * @param attribute a value representing additional information or weight associated with the edge.
 * @param from      the vertex where the directed edge originates.
 * @param to        the vertex where the directed edge terminates.
 */
case class AttributedDirectedEdge[E, V](attribute: E, from: V, to: V) extends DirectedEdge[E, V]:

  /**
   * Retrieves the starting `Vertex` of this `Edge`.
   * If this is an `UndirectedEdge`, white and black represent the nominal direction.
   * Individual adjacencies can be based on a flipped version of an `UndirectedEdge`.
   *
   * @return the vertex where the edge originates (the "white" vertex).
   */
  def white: V = from

  /**
   * Retrieves the ending vertex of the edge.
   * See NOTE above for `white`.
   *
   * @return the vertex where the edge terminates (the "black" vertex).
   */
  def black: V = to

/**
 * Companion object for the `AttributedDirectedEdge` class.
 *
 * Provides factory methods for creating instances of `AttributedDirectedEdge`.
 */
object AttributedDirectedEdge:
  /**
   * Creates an instance of `AttributedDirectedEdge` with the specified attribute and vertex pair.
   *
   * @param attribute the attribute associated with the edge.
   * @param vv        a tuple representing the starting and ending vertices of the edge.
   * @return an instance of `AttributedDirectedEdge` with the specified attribute and vertices.
   */
  def create[E, V](attribute: E, vv: (V, V)): DirectedEdge[E, V] = AttributedDirectedEdge(attribute, vv._1, vv._2)

/**
 * Represents a directed edge in a graph connecting two vertices of type `V`.
 * This implementation specifically models directed edges for graph structures,
 * where the direction is indicated from a `from` vertex to a `to` vertex.
 * The edge carries no additional attributes.
 *
 * @param from the originating vertex of the edge.
 * @param to   the terminating vertex of the edge.
 * @tparam V the type of the vertices connected by the edge.
 */
case class OrderedEdge[V](from: V, to: V) extends DirectedEdge[Unit, V]:
  /**
   * Retrieves the attribute of type `A` associated with this instance.
   *
   * @return the attribute of type `A` representing the defining characteristic
   *         or property of this instance.
   */
  def attribute: Unit = ()

  /**
   * Retrieves the starting `Vertex` of this `Edge`.
   * If this is an `UndirectedEdge`, white and black represent the nominal direction.
   * Individual adjacencies can be based on a flipped version of an `UndirectedEdge`.
   *
   * @return the vertex where the edge originates (the "white" vertex).
   */
  def white: V = from

  /**
   * Retrieves the ending vertex of the edge.
   * See NOTE above for `white`.
   *
   * @return the vertex where the edge terminates (the "black" vertex).
   */
  def black: V = to
