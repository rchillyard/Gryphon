/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.newcore

/**
 * A trait to model the concept of a connexion between two objects of type V.
 *
 * NOTE that I am a fan of the old-fashioned way to spell what is now more commonly referred to as a connection.
 * Furthermore, it makes more sense to use the parametric type X to stand for a connexion.
 *
 * @tparam V the underlying key (attribute) type of the element(s) to be connected, i.e. a vertex (node).
 */
trait Connexion[V] {
  /**
   * Method to determine what V value, if any, is connected via this Connexion.
   *
   * @param v the start (from) value of this Connexion.
   * @return an optional V which is the element connected to <code>v</code> via this Connexion.
   */
  def connexion(v: V): Option[V]
}

/**
 * Abstract class which implements Connexion[V].
 *
 * @param start  the V object at the start of the connexion.
 * @param end    the V object at the end of the connection.
 * @param twoWay true if it is a two-way (i.e. undirected) connexion.
 * @tparam V the underlying key (attribute) type of the element(s) to be connected, i.e. a vertex (node).
 */
abstract class AbstractConnexion[V](start: V, end: V, twoWay: Boolean) extends Connexion[V] {
  /**
   * Method to determine what V value, if any, is connected via this Connexion.
   *
   * @param v the start of this Connexion.
   * @return Some(end), providing that the given vertex <code>v</code> is <code>start</code>; or
   *         Some(start), providing that the given vertex <code>v</code> is <code>end</code> AND twoWay is true.
   */
  def connexion(v: V): Option[V] = v match {
    case `start` => Some(end)
    case `end` if twoWay => Some(start)
    case _ => None
  }
}

/**
 * Trait to define a directed connexion of type V.
 *
 * @tparam V the underlying key (attribute) type of the element(s) to be connected, i.e. a vertex (node).
 */
trait DirectedConnexion[V] extends Connexion[V] {
  def start: V

  def end: V
}

/**
 * Trait to define an undirected connexion of type V.
 *
 * @tparam V the underlying key (attribute) type of the element(s) to be connected, i.e. a vertex (node).
 */
trait UndirectedConnexion[V] extends Connexion[V] {
  /**
   * Method to yield one of the vertices (nodes) of this Connexion[V].
   *
   * @return one of the vertices of this Connexion (arbitrary).
   */
  def vertex: V

  /**
   * Method to yield the other vertex of this Connexion, given a vertex (v).
   *
   * @param v the given vertex.
   * @return an optional V.
   */
  def other(v: V): Option[V]
}

/**
 * Abstract class to represent a directed connexion.
 *
 * @param start the V object at the start of the connexion.
 * @param end   the V object at the end of the connection.
 * @tparam V the underlying key (attribute) type of the element(s) to be connected, i.e. a vertex (node).
 */
abstract class AbstractDirectedConnexion[V](val start: V, val end: V) extends AbstractConnexion[V](start, end, false) with DirectedConnexion[V]

/**
 *
 * @param start the V object at the start of the connexion.
 * @param end   the V object at the end of the connection.
 * @tparam V the underlying key (attribute) type of the element(s) to be connected, i.e. a vertex (node).
 */
abstract class AbstractUndirectedConnexion[V](start: V, end: V) extends AbstractConnexion[V](start, end, true) with UndirectedConnexion[V] {
  def vertex: V = start // NOTE: ASP: this is entirely arbitrary

  /**
   *
   * @param v the given vertex.
   * @return an optional V.
   */
  def other(v: V): Option[V] = v match {
    case `end` => Some(start)
    case `start` => Some(end)
    case _ => None
  }
}

/**
 * Case class to represent a Connexion from start to end.
 *
 * @param _start the V object at the origin of the connexion.
 * @param _end   the V object at the destination (target) of the connection.
 * @tparam V the underlying key (attribute) type of the element(s) to be connected, i.e. a vertex (node).
 */
case class DirectedConnexionCase[V](_start: V, _end: V) extends AbstractDirectedConnexion[V](_start, _end)

/**
 * Case class to represent a Connexion from start to end.
 *
 * @param start the V object at the start (origin) of the connexion.
 * @param end   the V object at the end (target) of the connection.
 * @tparam V the underlying key (attribute) type of the element(s) to be connected, i.e. a vertex (node).
 */
case class UndirectedConnexionCase[V](start: V, end: V) extends AbstractUndirectedConnexion[V](start, end)

/**
 * Trait to define the behavior of an Edge.
 *
 * @tparam V the underlying key (attribute) type of the element(s) to be connected, i.e. a vertex (node).
 * @tparam E the underlying key (attribute) type of the edge.
 */
trait Edge[V, +E] extends Attributed[E] {

  /**
   * Method to return the vertices of this Edge.
   *
   * @return a tuple of two vertices.
   */
  def vertices: (V, V)

  /**
   * Method to render this Edge as a String.
   *
   * @return a rendering of this Edge.
   */
  def render: String
}

/**
 * Abstract class which represents a directed edge.
 *
 * @param from      the V object at the origin of this directed edge.
 * @param to        the V object at the destination (target) of this directed edge.
 * @param attribute the attribute of this edge.
 * @tparam V the underlying key (attribute) type of the element(s) to be connected, i.e. a vertex (node).
 * @tparam E the underlying key (attribute) type of the edge.
 */
abstract class AbstractDirectedEdge[V, E](from: V, to: V, val attribute: E) extends AbstractDirectedConnexion[V](from: V, to: V) with Edge[V, E] {
  /**
   * Method to return the vertices of this Edge.
   *
   * @return a tuple of two vertices.
   */
  val vertices: (V, V) = from -> to

  /**
   * Method to return the "to"" end of this directed edge provided that the given vertex is the same as <code>from</code>.
   * Otherwise, <code>None</code> will be returned.
   *
   * @param w (V) the given vertex key (attribute).
   * @return an optional vertex key.
   */
  def other[W >: V](w: W): Option[W] = Option.when(w == from)(to)

  /**
   * Method to render this Edge as a String.
   *
   * @return a rendering of this Edge.
   */
  def render: String = s"$from--($attribute)-->$to"

  override def toString: String = render

}

/**
 * Abstract class which represents an undirected edge.
 *
 * @param v1        the V object at the origin of this undirected edge.
 * @param v2        the V object at the destination (target) of this undirected edge.
 * @param attribute the attribute of this edge.
 * @tparam V the underlying key (attribute) type of the element(s) to be connected, i.e. a vertex (node).
 * @tparam E the underlying key (attribute) type of the edge.
 */
abstract class AbstractUndirectedEdge[V, E](v1: V, v2: V, val attribute: E) extends AbstractUndirectedConnexion[V](v1: V, v2: V) with Edge[V, E] {
  /**
   * Method to return the vertices of this Edge.
   *
   * @return a tuple of two vertices.
   */
  def vertices: (V, V) = (v1, v2)

  /**
   * Method to render this Edge as a String.
   *
   * @return a rendering of this Edge.
   */
  def render: String = s"$v1<--($attribute)-->$v2"

  override def toString: String = render

}

/**
 * Case class to represent a Connexion from start to end.
 *
 * @param from the V object at the origin of the connexion.
 * @param to   the V object at the destination (target) of the connection.
 * @param e    the attribute of this edge.
 * @tparam V the underlying key (attribute) type of the element(s) to be connected, i.e. a vertex (node).
 * @tparam E the underlying key (attribute) type of the edge.
 */
case class DirectedEdgeCase[V, E](from: V, to: V, e: E) extends AbstractDirectedEdge[V, E](from, to, e)

/**
 * Case class to represent a Connexion from start to end.
 *
 * @param v1 the V object at the start (origin) of the connexion.
 * @param v2 the V object at the end (target) of the connection.
 * @param e  the attribute of this edge.
 * @tparam V the underlying key (attribute) type of the element(s) to be connected, i.e. a vertex (node).
 * @tparam E the underlying key (attribute) type of the edge.
 */
case class UndirectedEdgeCase[V, E](v1: V, v2: V, e: E) extends AbstractUndirectedEdge[V, E](v1, v2, e)

