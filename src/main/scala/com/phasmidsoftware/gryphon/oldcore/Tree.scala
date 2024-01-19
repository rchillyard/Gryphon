/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.oldcore

/**
 * Trait to model the behavior of a Tree.
 *
 * @tparam V the (key) vertex-attribute type.
 * @tparam E the edge-attribute type.
 * @tparam X the type of edge which connects two vertices. A sub-type of Edge[V,E].
 * @tparam P the property type (a mutable property currently only supported by the Vertex type).
 */
trait Tree[V, E, X <: Edge[V, E], P] extends AcyclicNetwork[V, X, P]// with Graph[V, E, X, P]


/**
 * Abstract class to represent a graph.
 *
 * The attribute type for a Graph is always String. CONSIDER relaxing this.
 * The edge and vertex attributes are whatever you like (E and V respectively -- see below).
 *
 * @tparam V the (key) vertex-attribute type.
 * @tparam E the edge-attribute type.
 * @tparam X the type of edge which connects two vertices. A sub-type of Edge[V,E].
 * @tparam P the property type (a mutable property currently only supported by the Vertex type).
 *
 */
abstract class AbstractTree[V, E, X <: Edge[V, E], P](val __description: String, val __vertexMap: VertexMap[V, X, P], undirected: Boolean) extends AbstractAcyclicNetwork(__vertexMap, undirected) with Tree[V, E, X, P] {

  /**
   * Method to yield the concatenation of the all the adjacency lists.
   *
   * @return AdjacencyList[X]
   */
  def allAdjacencies: AdjacencyList[X] = __vertexMap.values.foldLeft(AdjacencyList.empty[X])(_ ++ _.adjacent)

  /**
   * Method to determine if there is a connection between v1 and v2.
   *
   * @param v1 the start of the possible path.
   * @param v2 the end of the possible path.
   * @return true if there is a connection between v1 and v2.
   */
  def isPathConnected(v1: V, v2: V): Boolean = __vertexMap.isPathConnected(v1, v2)

  /**
   * Method to get a path between v1 and v2.
   * There is no implication that this is the shortest path.
   *
   * @param v1 a node in a network.
   * @param v2 another node in a network.
   * @return the path from v1 to v2.
   *         By convention, the path consists of v1, any intermediate nodes, and v2.
   */
  def path(v1: V, v2: V): Seq[V] = __vertexMap.path(v1, v2)

  /**
   * Method to make a connection between v1 and v2.
   *
   * @param v1 a node in a network.
   * @param v2 another node in a network.
   * @return a new Connected object on which isConnected(v1, v2) will be true.
   */
  def connect(v1: V, v2: V): Tree[V, E, X, P] = unit(vertexMap.connect(v1, v2))

  /**
   * (abstract) Method to create a new AbstractGraph from a given vertex map.
   *
   * @param vertexMap the vertex map.
   * @return a new AbstractGraph[V, E].
   */
  def unit(vertexMap: VertexMap[V, X, P]): Tree[V, E, X, P]
}


/**
 * Abstract class to represent an undirected graph.
 *
 * The attribute type for a Graph is always String. CONSIDER relaxing this.
 * The edge and vertex attributes are whatever you like (E and V respectively -- see below).
 *
 * @tparam V the (key) vertex-attribute type.
 * @tparam E the edge-attribute type.
 * @tparam X the type of edge which connects two vertices. A sub-type of Edge[V,E].
 * @tparam P the property type (a mutable property currently only supported by the Vertex type).
 */
abstract class AbstractUndirectedTree[V, E, X <: UndirectedEdge[V, E], P](val _description: String, val _vertexMap: VertexMap[V, X, P]) extends AbstractTree[V, E, X, P](_description, _vertexMap, true) with UndirectedTree[V, E, X, P] {

  /**
   * (abstract) Method to create a new AbstractGraph from a given vertex map.
   *
   * @param vertexMap the vertex map.
   * @return a new UndirectedGraph[V, E, X, P].
   */
  def unit(vertexMap: VertexMap[V, X, P]): UndirectedTree[V, E, X, P]
}

/**
 * Abstract class to represent an undirected graph.
 *
 * The attribute type for a Graph is always String. CONSIDER relaxing this.
 * The edge and vertex attributes are whatever you like (E and V respectively -- see below).
 *
 * @tparam V the (key) vertex-attribute type.
 * @tparam E the edge-attribute type.
 * @tparam X the type of edge which connects two vertices. A sub-type of Edge[V,E].
 * @tparam P the property type (a mutable property currently only supported by the Vertex type).
 */
abstract class AbstractDirectedTree[V, E, X <: DirectedEdge[V, E], P](val _description: String, val _vertexMap: VertexMap[V, X, P]) extends AbstractTree[V, E, X, P](_description, _vertexMap, false) with DirectedTree[V, E, X, P] {

  /**
   * (abstract) Method to create a new AbstractGraph from a given vertex map.
   *
   * @param vertexMap the vertex map.
   * @return a new UndirectedGraph[V, E, X, P].
   */
  def unit(vertexMap: VertexMap[V, X, P]): DirectedTree[V, E, X, P]
}

/**
 * Trait to define an undirected Tree.
 *
 * @tparam V the (key) vertex-attribute type.
 * @tparam E the edge-attribute type.
 * @tparam X the type of edge which connects two vertices. A sub-type of UndirectedEdge[V,E].
 * @tparam P the property type (a mutable property currently only supported by the Vertex type).
 */
trait UndirectedTree[V, E, X <: UndirectedEdge[V, E], P] extends Tree[V, E, X, P]// with UndirectedGraph[V, E, X, P]

/**
 * Trait to define a directed Tree.
 *
 * @tparam V the (key) vertex-attribute type.
 * @tparam E the edge-attribute type.
 * @tparam X the type of edge which connects two vertices. A sub-type of DirectedEdge[V,E].
 * @tparam P the property type (a mutable property currently only supported by the Vertex type).
 */
trait DirectedTree[V, E, X <: DirectedEdge[V, E], P] extends Tree[V, E, X, P]// with DirectedGraph[V, E, X, P]

/**
 * Case class to represent an undirected tree.
 *
 * @param description the description of this tree.
 * @param vertexMap   the vertex map to define this tree.
 * @tparam V the (key) vertex-attribute type.
 * @tparam E the edge-attribute type.
 *
 */
case class UndirectedTreeCase[V, E, X <: UndirectedEdge[V, E], P](description: String, vertexMap: VertexMap[V, X, P]) extends AbstractUndirectedTree[V, E, X, P](description, vertexMap) with UndirectedTree[V, E, X, P] {

  /**
   * Method to createUndirectedOrderedGraph a new AbstractGraph from a given vertex map.
   *
   * CONSIDER add an attribute parameter.
   *
   * @param vertexMap the vertex map.
   * @return a new AbstractGraph[V, E].
   */
  def unit(vertexMap: VertexMap[V, X, P]): UndirectedTree[V, E, X, P] = UndirectedTreeCase("no description", vertexMap)
}

/**
 * Case class to represent a directed tree.
 *
 * @param description the description of this tree.
 * @param vertexMap   the vertex map to define this tree.
 * @tparam V the (key) vertex-attribute type.
 * @tparam E the edge-attribute type.
 *
 */
case class DirectedTreeCase[V, E, X <: DirectedEdge[V, E], P](description: String, vertexMap: VertexMap[V, X, P]) extends AbstractDirectedTree[V, E, X, P](description, vertexMap) with DirectedTree[V, E, X, P] {

  /**
   * Method to createUndirectedOrderedGraph a new AbstractGraph from a given vertex map.
   *
   * CONSIDER add an attribute parameter.
   *
   * @param vertexMap the vertex map.
   * @return a new AbstractGraph[V, E].
   */
  def unit(vertexMap: VertexMap[V, X, P]): DirectedTree[V, E, X, P] = DirectedTreeCase("no description", vertexMap)

}