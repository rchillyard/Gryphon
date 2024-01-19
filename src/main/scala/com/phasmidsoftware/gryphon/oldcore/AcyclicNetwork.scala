/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.oldcore

import com.phasmidsoftware.gryphon.visit.{MutableQueueable, Visitor}

/**
 * Trait to model the behavior of the most basic tree, which is an acyclic network.
 *
 * @tparam V the (key) vertex-attribute type.
 * @tparam X the type of edge which connects two vertices. A sub-type of EdgeLike[V].
 * @tparam P the property type (a mutable property currently only supported by the Vertex type).
 */
trait AcyclicNetwork[V, X <: EdgeLike[V], P] extends Network[V, X, P] {

  /**
   * (abstract) Method to create a new Network which includes the given edge.
   *
   * @param x the edge to add.
   * @return AcyclicNetwork[V, X, P].
   */
  def addEdge(x: X): AcyclicNetwork[V, X, P]

  /**
   * Method to add a vertex of (key) type V to this network.
   * The vertex will have degree of zero.
   *
   * @param v the (key) attribute of the result.
   * @return a new AcyclicNetwork[V, X, P].
   */
  def addVertex(v: V): AcyclicNetwork[V, X, P]

  /**
   * Method to run breadth-first-search with a mutable queue on this Traversable.
   *
   * @param visitor the visitor, of type Visitor[V, J].
   * @param v       the starting vertex.
   * @tparam J the journal type.
   * @tparam Q the type of the mutable queue for navigating this Traversable.
   *           Requires implicit evidence of MutableQueueable[Q, V].
   * @return a new Visitor[V, J].
   */
  def bfsMutable[J, Q](visitor: Visitor[V, J])(v: V)(goal: V => Boolean)(implicit ev: MutableQueueable[Q, V]): Visitor[V, J]


  override def isCyclic: Boolean = false // TODO we should be able to assert this

  override def isBipartite: Boolean = true

  def unit(vertexMap: VertexMap[V, X, P]): AcyclicNetwork[V, X, P]
}

abstract class AbstractAcyclicNetwork[V, X <: EdgeLike[V], P](vertexMap: VertexMap[V, X, P], undirected: Boolean) extends AcyclicNetwork[V, X, P] {
  /**
   * (abstract) Yield an iterable of edges, of type X.
   *
   * @return an Iterable[X].
   */
  def edges: Iterable[X] =
    vertexMap.edges

  /**
   * (abstract) Method to create a new Network which includes the given edge.
   *
   * @param x the edge to add.
   * @return Network[V, X, P].
   */
  def addEdge(x: X): AcyclicNetwork[V, X, P] = {
    val (v, w) = x.vertices
    val edgeAdded = vertexMap.addEdge(v, x)
    val edgesAdded = if (undirected) edgeAdded.addEdge(w, x) else edgeAdded
    unit(edgesAdded.addVertex(w))
  }

  /**
   * Method to add a vertex of (key) type V to this network.
   * The vertex will have degree of zero.
   *
   * @param v the (key) attribute of the result.
   * @return a new Network[V, X, P].
   */
  def addVertex(v: V): AcyclicNetwork[V, X, P] = unit(vertexMap.addVertex(v))

  /**
   * An attribute.
   *
   * @return the value of the attribute, for example, a weight.
   */
  val attribute: String = "" // TODO do this properly

  /**
   * Method to run breadth-first-search with a mutable queue on this Traversable.
   *
   * @param visitor the visitor, of type Visitor[V, J].
   * @param v       the starting vertex.
   * @tparam J the journal type.
   * @tparam Q the type of the mutable queue for navigating this Traversable.
   *           Requires implicit evidence of MutableQueueable[Q, V].
   * @return a new Visitor[V, J].
   */
  def bfsMutable[J, Q](visitor: Visitor[V, J])(v: V)(goal: V => Boolean)(implicit ev: MutableQueueable[Q, V]): Visitor[V, J] =
    ??? // TODO implement me!
}

case class AcyclicNetworkCase[V, X <: EdgeLike[V], P](vertexMap: VertexMap[V, X, P], undirected: Boolean) extends AbstractAcyclicNetwork[V, X, P](vertexMap, undirected) {

  def unit(vertexMap: VertexMap[V, X, P]): AcyclicNetwork[V, X, P] = AcyclicNetworkCase(vertexMap, undirected)
}

object AcyclicNetworkCase {
  def apply[V, P](vertexMap: PairVertexMap[V, P]): AcyclicNetwork[V, VertexPair[V], P] = new AcyclicNetworkCase[V, VertexPair[V], P](vertexMap, false)

  def empty[V, P]: AcyclicNetwork[V, VertexPair[V], P] = AcyclicNetworkCase(PairVertexMap.empty[V, P])
}