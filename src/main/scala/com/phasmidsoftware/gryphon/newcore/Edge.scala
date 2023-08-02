///*
// * Copyright (c) 2023. Phasmid Software
// */
//
//package com.phasmidsoftware.gryphon.newcore
//
//import com.phasmidsoftware.gryphon.util.Util
//
///**
// * Trait to model the behavior of an Edge.
// *
// * @tparam V the (covariant) BaseConnexions key type, i.e. the type of its attribute.
// * @tparam E the (covariant) Edge type, i.e. the type of its attribute.
// */
//trait Edge[+V, +E] extends Connexion[V] with Attributed[E]
//
///**
// * Trait to model the behavior of an edge-like object, that's to say something with two vertices of type <code>V</code>.
// *
// * @tparam V the (covariant) BaseConnexions key type.
// */
//trait EdgeLike[+V] {
//
//  /**
//   * The two vertices of this Edge in a (possibly) arbitrary but deterministic order.
//   *
//   * NOTE: the order used will be based on an instance of Ordering[V] provided.
//   * CONSIDER not requiring that V support Ordering[V].
//   *
//   */
//  val vertices: (V, V)
//
//  /**
//   * Method to return the other end of this undirected edge from the given vertex <code>v</code>.
//   * If the value of <code>v</code> is neither <code>_v1</code> nor <code>_v2</code>, then None will be returned.
//   *
//   * @param w (V) the given vertex key (attribute).
//   * @return an optional vertex key.
//   */
//  def other[W >: V](w: W): Option[W]
//
//  /**
//   * Method to determine if w is one of the vertices of this EdgeLike object.
//   *
//   * @param w (>: V) the given vertex key (attribute).
//   * @return true if w is a vertex else false.
//   */
//  def meets[W >: V](w: W): Boolean = vertices._1 == w || vertices._2 == w
//
//  /**
//   * Method to get the other vertex of this edge when we are reasonably sure that the other vertex is defined.
//   *
//   * @param w (>: V) the given vertex key (attribute).
//   * @return the vertex at the other end of this edge from v.
//   */
//  def otherVertex[W >: V](w: W): W = Util.getOrThrow(other(w), CoreException(s"Edge.otherVertex: $w does not belong to edge $this "))
//}
//
///**
// * Trait to define the behavior of an EdgeLike object which is directed.
// *
// * @tparam V the (covariant) BaseConnexions key type.
// */
//trait Directed[+V] extends EdgeLike[V] {
//  /**
//   * @return (V) start vertex attribute (key).
//   */
//  def from: V
//
//  /**
//   * @return (V) end vertex attribute (key).
//   */
//  def to: V
//}
//
///**
// * Trait to define the behavior of an EdgeLike object which is undirected.
// *
// * @tparam V the (covariant) BaseConnexions key type.
// */
//trait Undirected[+V] extends EdgeLike[V] {
//  /**
//   * Value of one of the vertices.
//   * This method is deterministic thus always gives the same result for a particular edge.
//   */
//  val vertex: V
//}
//
///**
// * Trait to represent an UndirectedEdge.
// *
// * @tparam V the (covariant) BaseConnexions key type, i.e. the type of its attribute.
// * @tparam E the (covariant) Edge type, i.e. the type of its attribute.
// */
//trait UndirectedEdge[+V, +E] extends Edge[V, E] with Undirected[V]
//
///**
// * Trait to represent a DirectedEdge.
// *
// * @tparam V the (covariant) BaseConnexions key type, i.e. the type of its attribute.
// * @tparam E the (covariant) Edge type, i.e. the type of its attribute.
// */
//trait DirectedEdge[+V, +E] extends Edge[V, E] with Directed[V]
//
///**
// * DirectedEdge object, companion to the trait.
// */
//object DirectedEdge {
//  /**
//   * Method to construct a DirectedEdge with no attribute type from two vertices.
//   *
//   * @param from (V) start vertex attribute (key).
//   * @param to   (V) the end vertex attribute (key).
//   * @tparam V the BaseConnexions key type, i.e. the type of its attribute.
//   * @return a DirectedEdge[V, Unit]
//   */
//  def apply[V](from: V, to: V): DirectedEdge[V, Unit] = DirectedEdgeCase(from, to, ())
//
//  /**
//   * Method to construct a DirectedEdge from two vertices and an attribute.
//   *
//   * @param from      (V) start vertex attribute (key).
//   * @param to        (V) the end vertex attribute (key).
//   * @param attribute (E) the edge attribute
//   * @tparam V the BaseConnexions key type, i.e. the type of its attribute.
//   * @tparam E the edge attribute type.
//   * @return a DirectedEdge[V, E]
//   */
//  def apply[V, E](from: V, to: V, attribute: E): DirectedEdge[V, E] = DirectedEdgeCase(from, to, attribute)
//}
//
///**
// * Trait to represent an undirected ordered edge.
// *
// * @tparam V the BaseConnexions key type, i.e. the type of its attribute.
// * @tparam E the (covariant) Edge type, i.e. the type of its attribute.
// */
//trait UndirectedOrderedEdge[+V, E] extends UndirectedEdge[V, E] with Ordering[E]
//
///**
// * Trait to represent an directed ordered edge.
// *
// * @tparam V the (covariant) BaseConnexions key type, i.e. the type of its attribute.
// * @tparam E the (covariant) Edge type, i.e. the type of its attribute.
// */
//trait DirectedOrderedEdge[+V, E] extends DirectedEdge[V, E] with Ordering[E]
//
///**
// * UndirectedEdge object, companion to the trait.
// */
//object UndirectedEdge {
//  /**
//   * Method to construct a UndirectedEdge with no attribute type from two vertices.
//   *
//   * @param from (V) start vertex attribute (key).
//   * @param to   (V) the end vertex attribute (key).
//   * @tparam V the BaseConnexions key type, i.e. the type of its attribute.
//   * @return a UndirectedEdge[V, Unit]
//   */
//  def apply[V: Ordering](from: V, to: V): UndirectedEdge[V, Unit] = UndirectedEdgeCase(from, to, ())
//
//  /**
//   * Method to construct a DirectedEdge from two vertices and an attribute.
//   *
//   * @param from      (V) start vertex attribute (key).
//   * @param to        (V) the end vertex attribute (key).
//   * @param attribute (E) the edge attribute
//   * @tparam V the BaseConnexions key type, i.e. the type of its attribute.
//   * @tparam E the edge attribute type.
//   * @return a DirectedEdge[V, E]
//   */
//  def apply[V: Ordering, E](from: V, to: V, attribute: E): UndirectedEdge[V, E] = UndirectedEdgeCase(from, to, attribute)
//}
//
///**
// * Class to represent a directed edge from <code>from</code> to <code>to</code>.
// *
// * @param from      (V) start vertex attribute (key).
// * @param to        (V) the end vertex attribute (key).
// * @param attribute (E) the edge attribute
// * @tparam V the BaseConnexions key type, i.e. the type of its attribute.
// * @tparam E the Edge type, i.e. the type of its attribute.
// */
//case class DirectedEdgeCase[V, E](from: V, to: V, attribute: E) extends BaseDirectedEdge[V, E](from, to, attribute) with DirectedEdge[V, E]
//
///**
// * Class to represent a directed, ordered edge from <code>from</code> to <code>to</code>.
// * For example, an edge with a weighting.
// *
// * @param from      (V) start vertex attribute (key).
// * @param to        (V) the end vertex attribute (key).
// * @param attribute (E) the edge attribute
// * @tparam V the BaseConnexions key type, i.e. the type of its attribute.
// * @tparam E the Edge type, i.e. the type of its attribute.
// *           Requires implicit evidence of type Ordering[E].
// */
//case class DirectedOrderedEdgeCase[V, E: Ordering](from: V, to: V, attribute: E) extends BaseDirectedOrderedEdge[V, E](from, to, attribute) with DirectedOrderedEdge[V, E]
//
///**
// * Class to represent an undirected edge between <code>v1</code> and <code>v2</code>.
// *
// * @param v1        (V) a vertex attribute (key).
// * @param v2        (V) the other vertex attribute (key).
// * @param attribute (E) the edge attribute
// * @tparam V the BaseConnexions key type, i.e. the type of its attribute.
// *           Requires implicit evidence of type Ordering[V].
// * @tparam E the Edge type, i.e. the type of its attribute.
// */
//case class UndirectedEdgeCase[V: Ordering, E](v1: V, v2: V, attribute: E) extends BaseUndirectedEdge[V, E](v1, v2, attribute) with UndirectedEdge[V, E]
//
///**
// * Class to represent an undirected, ordered edge between <code>v1</code> and <code>v2</code>.
// *
// * @param v1        (V) a vertex attribute (key).
// * @param v2        (V) the other vertex attribute (key).
// * @param attribute (E) the edge attribute
// * @tparam V the BaseConnexions key type, i.e. the type of its attribute.
// *           Requires implicit evidence of type Ordering[V].
// * @tparam E the Edge type, i.e. the type of its attribute.
// *           Requires implicit evidence of type Ordering[E].
// */
//case class UndirectedOrderedEdgeCase[V: Ordering, E: Ordering](v1: V, v2: V, attribute: E) extends BaseUndirectedOrderedEdge[V, E](v1, v2, attribute) with UndirectedOrderedEdge[V, E]
//
///**
// * Abstract base class to represent an undirected edge.
// *
// * @param _v1        (V) one vertex attribute (key).
// * @param _v2        (V) the other vertex attribute (key).
// * @param _attribute (E) the edge attribute.
// * @tparam V the (covariant) BaseConnexions key type, i.e. the type of its attribute.
// *           Requires implicit evidence of type Ordering[V].
// * @tparam E the (covariant) Edge type, i.e. the type of its attribute.
// */
//abstract class BaseUndirectedEdge[+V: Ordering, +E](_v1: V, _v2: V, val _attribute: E) extends UndirectedEdge[V, E] {
//  /**
//   * Value of _v1.
//   *
//   * NOTE could equally take the value of <code>_v2</code> but note that method vertices relies on this being deterministic.
//   */
//  val vertex: V = _v1
//
//  /**
//   * Method to return the other end of this edge from the given vertex <code>v</code>.
//   * If the value of <code>v</code> is neither <code>_v1</code> nor <code>_v2</code>, then None will be returned.
//   *
//   * @param w (V) the given vertex key (attribute).
//   * @return an optional vertex key.
//   */
//  def other[W >: V](w: W): Option[W] = Option.when(w == _v1)(_v2) orElse Option.when(w == _v2)(_v1)
//
//  /**
//   * Method to get the two vertices of this edge in some deterministic order, based on the implicit value of Ordering[V].
//   */
//  val vertices: (V, V) = {
//    val v = if (implicitly[Ordering[V]].compare(_v1, _v2) <= 0) _v1 else _v2
//    v -> otherVertex(v) // XXX this is guaranteed to have a defined value so will not throw an exception
//  }
//
//  /**
//   * Method to render this Edge as a String.
//   *
//   * @return a String of form: v1<--e-->v2
//   */
//  override def toString: String = {
//    val tuple = vertices
//    s"${tuple._1}<--(${_attribute})-->${tuple._2}"
//  }
//}
//
///**
// * Abstract base class for a directed edge.
// *
// * @param _from      (V) the start vertex attribute (key).
// * @param _to        (V) the end vertex attribute (key).
// * @param _attribute (E) the edge attribute.
// * @tparam V the (covariant) BaseConnexions key type, i.e. the type of its attribute.
// * @tparam E the (covariant) Edge type, i.e. the type of its attribute.
// */
//abstract class BaseDirectedEdge[+V, +E](val _from: V, val _to: V, val _attribute: E) extends DirectedEdge[V, E] {
//  /**
//   * The two vertices in the natural order: _from, _to.
//   */
//  val vertices: (V, V) = _from -> _to
//
//  /**
//   * Method to return the "to"" end of this directed edge provided that the given vertex
//   * is is the same as _from (in which case None) will be returned.
//   *
//   * @param w (V) the given vertex key (attribute).
//   * @return an optional vertex key.
//   */
//  def other[W >: V](w: W): Option[W] = Option.when(w == _from)(_to)
//
//  override def toString: String = s"${_from}--(${_attribute})-->${_to}"
//}
//
///**
// * Abstract base class for an undirected, ordered edge.
// * For example, an edge with a weighting.
// *
// * @param _v1        (V) one vertex attribute (key).
// * @param _v2        (V) the other vertex attribute (key).
// * @param _attribute (E) the edge attribute.
// * @tparam V the BaseConnexions key type, i.e. the type of its attribute.
// *           Requires implicit evidence of type Ordering[V].
// * @tparam E the Edge type, i.e. the type of its attribute.
// *           Requires implicit evidence of type Ordering[E].
// */
//abstract class BaseUndirectedOrderedEdge[V: Ordering, E: Ordering](_v1: V, _v2: V, override val _attribute: E) extends BaseUndirectedEdge[V, E](_v1, _v2, _attribute) with UndirectedOrderedEdge[V, E] with Ordered[Edge[V, E]] {
//
//  /**
//   * Method to compare x (E) attribute with y (E).
//   *
//   * CONSIDER do we actually need this?
//   *
//   * @param e1 an E value.
//   * @param e2 an E value.
//   * @return -1, 0, or 1 depending on the ordering of e1 and e2.
//   */
//  def compare(e1: E, e2: E): Int = implicitly[Ordering[E]].compare(e1, e2)
//
//  /**
//   * Method to compare this edge with that edge.
//   *
//   * CONSIDER do we actually need this?
//   *
//   * @param that another edge.
//   * @return -1, 0, or 1 depending on the ordering of this and that edges.
//   */
//  def compare(that: Edge[V, E]): Int = OrderedEdge.compare(this, that)
//}
//
///**
// * Abstract base class for an directed, ordered edge.
// * For example, an edge with a weighting.
// *
// * @param _from      (V) start vertex attribute (key).
// * @param _to        (V) the end vertex attribute (key).
// * @param _attribute (E) the edge attribute
// * @tparam V the BaseConnexions key type, i.e. the type of its attribute.
// * @tparam E the Edge type, i.e. the type of its attribute.
// *           Requires implicit evidence of type Ordering[E].
// */
//abstract class BaseDirectedOrderedEdge[V, E: Ordering](override val _from: V, override val _to: V, override val _attribute: E) extends BaseDirectedEdge[V, E](_from, _to, _attribute) with DirectedOrderedEdge[V, E] with Ordered[Edge[V, E]] {
//
//  /**
//   * Method to compare x (E) attribute with y (E).
//   *
//   * CONSIDER do we actually need this?
//   *
//   * @param e1 an E value.
//   * @param e2 an E value.
//   * @return -1, 0, or 1 depending on the ordering of e1 and e2.
//   */
//  def compare(e1: E, e2: E): Int = implicitly[Ordering[E]].compare(e1, e2)
//
//  /**
//   * Method to compare this edge with that edge.
//   *
//   * @param that another edge.
//   * @return -1, 0, or 1 depending on the ordering of this and that edges.
//   */
//  def compare(that: Edge[V, E]): Int = OrderedEdge.compare(this, that)
//}
//
///**
// * Trait to describe a vertex pair that has no explicit edge connecting them.
// *
// * CONSIDER redefining using EdgeLike.
// *
// * @tparam V the (covariant) BaseConnexions key type, i.e. the type of its attribute.
// */
//trait VertexPair[+V] extends Edge[V, Unit] {
//  def invert: VertexPair[V]
//
//  def unit[W >: V](v1: W, v2: W): VertexPair[W]
//}
//
///**
// * Class to represent a vertex pair, for example, as a connection in the Union-Find problem.
// *
// * @param v1 a vertex.
// * @param v2 another vertex.
// * @tparam V the BaseConnexions key type, i.e. the type of its attribute.
// */
//case class VertexPairCase[V](v1: V, v2: V) extends BaseVertexPair[V](v1, v2) with VertexPair[V] {
//  def unit[W >: V](v1: W, v2: W): VertexPair[W] = VertexPairCase(v1, v2)
//}
//
///**
// * Abstract base class to represent a connection between a pair of vertices that are not connected by an explicit edge object.
// * This class is intended for edge-less graphs such as are found in the Union-Find problem.
// *
// * @param _v1 one of the vertices.
// * @param _v2 the other vertex.
// * @tparam V the BaseConnexions key type, i.e. the type of its attribute.
// */
//abstract class BaseVertexPair[+V](_v1: V, _v2: V) extends VertexPair[V] {
//  /**
//   * @return ().
//   */
//  val attribute: Unit = ()
//
//  /**
//   * The two vertices of this Edge as a tuple: (_v1, _v2)
//   */
//  val vertices: (V, V) = _v1 -> _v2
//
//
//  def invert: VertexPair[V] = unit(_v2, _v1)
//
//  /**
//   * Method to return the other end of this edge from the given vertex <code>v</code>.
//   * If the value of <code>v</code> is neither <code>_v1</code> nor <code>_v2</code>, then None will be returned.
//   *
//   * CONSIDER merging this class with BaseUndirectedEdge.
//   *
//   * TESTME
//   *
//   * @param w (V) the given vertex key (attribute).
//   * @return an optional vertex key.
//   */
//  def other[W >: V](w: W): Option[W]
//  = Option.when(w == _v1)(_v2) orElse Option.when(w == _v2)(_v1)
//
//  override def toString: String = s"${_v1}:${_v2}"
//}
//
///**
// * Object to provide non-instance methods for an ordered edge.
// */
//object OrderedEdge {
//  /**
//   * Method to return an Int whose sign communicates how edge x compares to edge y.
//   *
//   * @param x an edge to be compared.
//   * @param y the other edge to be compared.
//   * @tparam V the vertex type
//   * @tparam E the edge-type.
//   *           Requires implicit evidence of type Ordering[E].
//   */
//  def compare[V, E: Ordering](x: Edge[V, E], y: Edge[V, E]): Int = implicitly[Ordering[E]].compare(x.attribute, y.attribute)
//}