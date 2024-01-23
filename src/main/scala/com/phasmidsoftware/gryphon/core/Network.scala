/*
 * Copyright (c) 2024. Phasmid Software
 */

package com.phasmidsoftware.gryphon.core

import com.phasmidsoftware.gryphon.core.XMap.addVertexPair
import com.phasmidsoftware.gryphon.visit.{MutableQueueable, Visitor}
import scala.util.Using

/**
 * Hierarchical Trait that allows the addition of a Connexion[V, X].
 *
 * CONSIDER merging this into Network.
 *
 * @tparam V the underlying attribute type.
 * @tparam X the container type.
 */
sealed trait Connectible[V, X[_]] {

  /**
   * Method to add a connexion to this XMap.
   *
   * @param connexion     a Connexion[V, Node].
   * @param hasConnexions (implicit).
   * @return the updated Connectible[V].
   */
  def +(connexion: Connexion[V, X])(implicit hasConnexions: HasConnexions[V, Node]): XMap[V, X]
}

trait Network[V, X[_]] extends Connectible[V, X] {
  /**
   * Method to add a V->Node key-value pair.
   *
   * TESTME unused.
   *
   * @param vx a tuple of V1, Node[V1]
   * @tparam V1 the underlying type of vx: must be super-type of V.
   * @return XMap[V1].
   */
  def +[V1 >: V](vx: (V1, X[V1])): XMap[V1, X]

  /**
   * Method to yield the (optional) X[V] corresponding to the given key.
   *
   * @param key the attribute whose container we seek.
   * @return Option[Node of V].
   */
  def get(key: V): Option[X[V]]
}

/**
 * Class to represent a XMap and thus all of the adjacencies of a graph.
 *
 * @param map a Map of V -> Node[V].
 * @tparam V the underlying Node attribute type.
 */
abstract class XMap[V, X[_]](val map: Map[V, X[V]]) extends Network[V, X] with Traversable[V, X] {

  /**
   * Method to run depth-first-search on this XMap.
   *
   * @param visitor the visitor, of type Visitor[V, J].
   * @param v       the starting vertex.
   * @tparam J the journal type.
   * @return a new Visitor[V, J].
   */
  def dfs[J](visitor: Visitor[V, J])(v: V)(implicit attributed: Attributed[X[V], V], discoverable: Discoverable[X[V]], hasConnexions: HasConnexions[V, X]): Visitor[V, J] = {
    initializeVisits(Some(v)) match {
      case None => throw GraphException(s"dfs: starting vertex $v unknown")
      case Some(x) => Using.resource(recursiveDFS(visitor, x))(identity)
    }
  }

  def unit[W](map: Map[W, X[W]]): XMap[W, X]

  /**
   * Method to add a connexion to this XMap.
   *
   * @param connexion a Connexion[V, Node].
   * @return the updated XMap[V].
   */
  def +(connexion: Connexion[V, X])(implicit hasConnexions: HasConnexions[V, Node]): XMap[V, X] = connexion match {
    case c: Connexion[_, _] =>
      // CONSIDER Match on c (but it isn't going to be easy)
      if (c.isInstanceOf[Pair[_]]) {
        val pair = c.asInstanceOf[Pair[V]]
        val m = map.asInstanceOf[Map[V, Node[V]]]
        val x2: Node[V] = pair.v2
        val result = m.get(pair.v1) match {
          case Some(x1) =>
            addVertexPair(m, x1, x2)
          case None =>
            val x1: Node[V] = Node.create[V](pair.v1)
            addVertexPair(m, x1 + Pair(x1.attribute, x2), x2)
        }
        unit(result.asInstanceOf[Map[V, X[V]]])
      }
      else throw GraphException("NodeMap.+: not a Pair")
    case _ => throw GraphException("NodeMap.+: not a Connexion")
  }

  /**
   * Method to add a V->X[V] key-value pair.
   *
   * @param vx a tuple of V1, Node[V1]
   * @tparam V1 the underlying type of vx: must be super-type of V.
   * @return XMap[V1].
   */
  def +[V1 >: V](vx: (V1, X[V1])): XMap[V1, X] =
    unit(map.asInstanceOf[Map[V1, X[V1]]] + vx)

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
  def get(key: V): Option[X[V]] = map.get(key)

  private def initializeVisits(vo: Option[V])(implicit discoverable: Discoverable[X[V]]): Option[X[V]] = {
    val f: Boolean => X[V] => Unit = b => v => discoverable.setDiscovered(v, b)
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
  private def recursiveDFS[J](visitor: Visitor[V, J], v: X[V])(implicit attributed: Attributed[X[V], V], discoverable: Discoverable[X[V]], hasConnexions: HasConnexions[V, X]): Visitor[V, J] =
    recurseOnVertex(v, visitor.visitPre(attributed.attribute(v))).visitPost(attributed.attribute(v))

  private def recurseOnVertex[J](vx: X[V], visitor: Visitor[V, J])(implicit attributed: Attributed[X[V], V], discoverable: Discoverable[X[V]], hasConnexions: HasConnexions[V, X]): Visitor[V, J] =
    hasConnexions.connexions(vx).foldLeft(visitor)((q, x) => recurseOnConnexion(x, q))

  private def recurseOnConnexion[J](x: Connexion[V, X], visitor: Visitor[V, J])(implicit attributed: Attributed[X[V], V], discoverable: Discoverable[X[V]], hasConnexions: HasConnexions[V, X]): Visitor[V, J] =
    markUndiscoveredVertex({ w => discoverable.setDiscovered(w, b = true) }, x.v2) match {
      case Some(z) => recursiveDFS(visitor, z)
      case None => visitor
    }

  private def markUndiscoveredVertex(f: X[V] => Unit, vx: X[V])(implicit discoverable: Discoverable[X[V]]): Option[X[V]] = {
    val vwo: Option[X[V]] = Option.when(!discoverable.isDiscovered(vx))(vx)
    vwo foreach f
    vwo
  }

}

object XMap {

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

case class NodeMap[V](m: Map[V, Node[V]]) extends XMap[V, Node](m: Map[V, Node[V]]) {

  def unit[W](map: Map[W, Node[W]]): XMap[W, Node] = NodeMap(map)
}

object NodeMap {
  /**
   * Method to yield an empty XMap.
   *
   * @tparam V the underlying node attribute type.
   * @return an empty XMap[V].
   */
  def empty[V]: NodeMap[V] = NodeMap(Map.empty)

}