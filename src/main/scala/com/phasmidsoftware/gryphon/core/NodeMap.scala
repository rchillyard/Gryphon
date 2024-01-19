/*
 * Copyright (c) 2024. Phasmid Software
 */

package com.phasmidsoftware.gryphon.core

import com.phasmidsoftware.gryphon.core.NodeMap.addVertexPair
import com.phasmidsoftware.gryphon.visit.{MutableQueueable, Visitor}
import scala.util.Using

sealed trait Connectible[V] {

  /**
   * Method to add a connexion to this NodeMap.
   *
   * @param connexion a Connexion[V, Node].
   * @return the updated Connectible[V].
   */
  def +(connexion: Connexion[V, Node]): Connectible[V]
}

trait Network[V] extends Connectible[V] {
  /**
   * Method to add a V->Node key-value pair.
   *
   * @param vx a tuple of V1, Node[V1]
   * @tparam V1 the underlying type of vx: must be super-type of V.
   * @return NodeMap[V1].
   */
  def +[V1 >: V](vx: (V1, Node[V1])): NodeMap[V1]

  /**
   * Method to yield the (optional) Node[V] corresponding to the given key.
   *
   * @param key the attribute which we seek.
   * @return Option[Node of V].
   */
  def get(key: V): Option[Node[V]]
}

/**
 * Class to represent a NodeMap and thus all of the adjacencies of a graph.
 *
 * @param map a Map of V -> Node[V].
 * @tparam V the underlying Node attribute type.
 */
case class NodeMap[V](map: Map[V, Node[V]]) extends Network[V] with Traversable[V] {

  /**
   * Method to add a connexion to this NodeMap.
   *
   * @param connexion a Connexion[V, Node].
   * @return the updated NodeMap[V].
   */
  def +(connexion: Connexion[V, Node]): NodeMap[V] = connexion match {
    case pair: Pair[V] =>
      val x2: Node[V] = pair.v2
      val m = map.get(pair.v1) match {
        case Some(x1) =>
          addVertexPair(map, x1, x2)
        case None =>
          val x1: Node[V] = Node.create[V](pair.v1)
          addVertexPair(map, x1 + Pair(x1.attribute, x2), x2)
      }
      copy(map = m)
    case _ => throw GraphException("NodeMap.+: not a Pair")
  }

  /**
   * Method to add a V->Node key-value pair.
   *
   * @param vx a tuple of V1, Node[V1]
   * @tparam V1 the underlying type of vx: must be super-type of V.
   * @return NodeMap[V1].
   */
  def +[V1 >: V](vx: (V1, Node[V1])): NodeMap[V1] = NodeMap[V1](map.asInstanceOf[Map[V1, Node[V1]]] + vx)

  /**
   * Method to run depth-first-search on this NodeMap.
   *
   * @param visitor the visitor, of type Visitor[V, J].
   * @param v       the starting vertex.
   * @tparam J the journal type.
   * @return a new Visitor[V, J].
   */
  def dfs[J](visitor: Visitor[V, J])(v: V)(implicit discoverable: Discoverable[Node[V]]): Visitor[V, J] = {
    initializeVisits(Some(v)) match {
      case None => throw GraphException(s"dfs: starting vertex $v unknown")
      case Some(x) => Using.resource(recursiveDFS(visitor, x))(identity)
    }
  }

  /**
   * Method to run depth-first-search on this Traversable, ensuring that every vertex is visited..
   *
   * @param visitor the visitor, of type Visitor[V, J].
   * @tparam J the journal type.
   * @return a new Visitor[V, J].
   */
  def dfsAll[J](visitor: Visitor[V, J]): Visitor[V, J] = ???

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
  def bfsMutable[J, Q](visitor: Visitor[V, J])(v: V)(goal: V => Boolean)(implicit ev: MutableQueueable[Q, V]): Visitor[V, J] = ???

  /**
   * Method to yield the (optional) Node[V] corresponding to the given key.
   *
   * @param key the attribute which we seek.
   * @return Option[Node of V].
   */
  def get(key: V): Option[Node[V]] = map.get(key)

  private def initializeVisits(vo: Option[V])(implicit discoverable: Discoverable[Node[V]]): Option[Node[V]] = {
    val f: Boolean => Node[V] => Unit = b => v => discoverable.setDiscovered(v, b)
    map.values foreach f(false)
    val result = for {v <- vo; vertex <- map.get(v)} yield vertex
    result foreach f(true)
    result
  }

  /**
   * Non-tail-recursive method to run DFS on the vertex V with the given Visitor.
   *
   * @param visitor the Visitor[V, J].
   * @param v       the vertex at which we run depth-first-search.
   * @tparam J the Journal type of the Visitor.
   * @return a new Visitor[V, J].
   */
  private def recursiveDFS[J](visitor: Visitor[V, J], v: Node[V]): Visitor[V, J] =
    recurseOnVertex(v, visitor.visitPre(v.attribute)).visitPost(v.attribute)

  private def recurseOnVertex[J](v: Node[V], visitor: Visitor[V, J]) =
    v.connexions.foldLeft(visitor)((q, x) => recurseOnConnexion(x, q))

  private def recurseOnConnexion[J](x: Connexion[V, Node], visitor: Visitor[V, J]): Visitor[V, J] =
    markUndiscoveredVertex({ w => w.discovered = true }, x.v2) match {
      case Some(z) => recursiveDFS(visitor, z)
      case None => visitor
    }

  private def markUndiscoveredVertex(f: Node[V] => Unit, vx: Node[V]) = {
    val vwo: Option[Node[V]] = Option.when(!vx.discovered)(vx)
    vwo foreach f
    vwo
  }

}

object NodeMap {
  /**
   * Method to yield an empty NodeMap.
   *
   * @tparam V the underlying node attribute type.
   * @return an empty NodeMap[V].
   */
  def empty[V]: NodeMap[V] = NodeMap[V](Map.empty)

  /**
   * Method to add a VertexPair to the given map.
   *
   * @param map a Map[V, Node of V].
   * @param x1  the first Node.
   * @param x2  the second Node.
   * @tparam V the underlying type.
   * @return an updated version of map.
   */
  def addVertexPair[V](map: Map[V, Node[V]], x1: Node[V], x2: Node[V]): Map[V, Node[V]] =
    map + (x1 + Pair[V](x1.attribute, x2)).keyValue + x2.keyValue

}
