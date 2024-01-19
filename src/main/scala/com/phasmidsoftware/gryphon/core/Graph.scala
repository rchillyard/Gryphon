/*
 * Copyright (c) 2024. Phasmid Software
 */

package com.phasmidsoftware.gryphon.core

import com.phasmidsoftware.gryphon.visit.{MutableQueueable, Visitor}

/**
 * Case class to represent a graph of nodes and edges.
 * For a network of connections, only a NodeMap is required.
 *
 * @param nodeMap the node map, i.e. the adjacency model.
 * @param edges   the edges of the graph (edges are redundantly available via the NodeMap).
 * @tparam V the node (vertex) attribute type.
 * @tparam E the edge attribute type.
 */
case class Graph[V, E](nodeMap: NodeMap[V, Node], edges: Seq[Edge[V, E]]) extends Network[V, Node] with Traversable[V, Node] {

  /**
   * Method to add a connexion to this NodeMap.
   *
   * @param connexion a Connexion[V, Node].
   * @return the updated Connectible[V].
   */
  def +(connexion: Connexion[V, Node])(implicit hasConnexions: HasConnexions[V, Node]): NodeMap[V, Node] = nodeMap.+(connexion)

  /**
   * Method to add a V->Node key-value pair.
   *
   * @param vx a tuple of V1, Node[V1]
   * @tparam V1 the underlying type of vx: must be super-type of V.
   * @return NodeMap[V1].
   */
  def +[V1 >: V](vx: (V1, Node[V1])): NodeMap[V1, Node] = nodeMap.+(vx)

  /**
   * Method to run depth-first-search on this NodeMap.
   *
   * @param visitor      the visitor, of type Visitor[V, J].
   * @param v            the starting vertex.
   * @param discoverable (implicit) instance of Discoverable of Node[V].
   * @tparam J the journal type.
   * @return a new Visitor[V, J].
   */
  def dfs[J](visitor: Visitor[V, J])(v: V)(implicit attributed: Attributed[Node[V], V], discoverable: Discoverable[Node[V]], hasConnexions: HasConnexions[V, Node]): Visitor[V, J] = nodeMap.dfs(visitor)(v)

  /**
   * Method to run depth-first-search on this Traversable, ensuring that every vertex is visited..
   *
   * @param visitor the visitor, of type Visitor[V, J].
   * @tparam J the journal type.
   * @return a new Visitor[V, J].
   */
  def dfsAll[J](visitor: Visitor[V, J]): Visitor[V, J] = nodeMap.dfsAll(visitor)

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
  def bfsMutable[J, Q](visitor: Visitor[V, J])(v: V)(goal: V => Boolean)(implicit ev: MutableQueueable[Q, V]): Visitor[V, J] = nodeMap.bfsMutable(visitor)(v)(goal)(ev)

  /**
   * Method to yield the (optional) Node[V] corresponding to the given key.
   *
   * @param key the attribute which we seek.
   * @return Option[Node of V].
   */
  def get(key: V): Option[Node[V]] = nodeMap.get(key)

}
