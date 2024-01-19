/*
 * Copyright (c) 2024. Phasmid Software
 */

package com.phasmidsoftware.gryphon.core

import com.phasmidsoftware.gryphon.core.VertexMap.addVertexPair
import littlegryphon.visit.Visitor
import scala.util.Using

sealed trait Connectible[V] {

  /**
   * Method to add a connexion to this VertexMap.
   *
   * @param connexion a Connexion[V, Vertex].
   * @return the updated Connectible[V].
   */
  def +(connexion: Connexion[V, Vertex]): Connectible[V]
}

/**
 * Class to represent a VertexMap and thus all of the adjacencies of a graph.
 *
 * @param map a Map of V -> Vertex[V].
 * @tparam V the underlying Vertex attribute type.
 */
case class VertexMap[V](map: Map[V, Vertex[V]]) extends Connectible[V] {

  /**
   * Method to add a connexion to this VertexMap.
   *
   * @param connexion a Connexion[V, Vertex].
   * @return the updated VertexMap[V].
   */
  def +(connexion: Connexion[V, Vertex]): VertexMap[V] = connexion match {
    case pair: Pair[V] =>
      val x2: Vertex[V] = pair.v2
      val m = map.get(pair.v1) match {
        case Some(x1) =>
          addVertexPair(map, x1, x2)
        case None =>
          val x1: Vertex[V] = Vertex.create[V](pair.v1)
          addVertexPair(map, x1 + Pair(x1.attribute, x2), x2)
      }
      copy(map = m)
    case _ => throw GraphException("VertexMap.+: not a Pair")
  }

  /**
   * Method to add a V->Vertex key-value pair.
   *
   * @param vx a tuple of V1, Vertex[V1]
   * @tparam V1 the underlying type of vx: must be super-type of V.
   * @return VertexMap[V1].
   */
  def +[V1 >: V](vx: (V1, Vertex[V1])): VertexMap[V1] = VertexMap[V1](map.asInstanceOf[Map[V1, Vertex[V1]]] + vx)

  /**
   * Method to run depth-first-search on this VertexMap.
   *
   * @param visitor the visitor, of type Visitor[V, J].
   * @param v       the starting vertex.
   * @tparam J the journal type.
   * @return a new Visitor[V, J].
   */
  def dfs[J](visitor: Visitor[V, J])(v: V)(implicit discoverable: Discoverable[Vertex[V]]): Visitor[V, J] = {
    initializeVisits(Some(v)) match {
      case None => throw GraphException(s"dfs: starting vertex $v unknown")
      case Some(x) => Using.resource(recursiveDFS(visitor, x))(identity)
    }
  }

  /**
   * Method to yield the (optional) Vertex[V] corresponding to the given key.
   *
   * @param key the attribute which we seek.
   * @return Option[Vertex of V].
   */
  def get(key: V): Option[Vertex[V]] = map.get(key)

  private def initializeVisits(vo: Option[V])(implicit discoverable: Discoverable[Vertex[V]]): Option[Vertex[V]] = {
    val f: Boolean => Vertex[V] => Unit = b => v => discoverable.setDiscovered(v, b)
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
  private def recursiveDFS[J](visitor: Visitor[V, J], v: Vertex[V]): Visitor[V, J] =
    recurseOnVertex(v, visitor.visitPre(v.attribute)).visitPost(v.attribute)

  private def recurseOnVertex[J](v: Vertex[V], visitor: Visitor[V, J]) =
    v.connexions.foldLeft(visitor)((q, x) => recurseOnConnexion(x, q))

  private def recurseOnConnexion[J](x: Connexion[V, Vertex], visitor: Visitor[V, J]): Visitor[V, J] =
    markUndiscoveredVertex({ w => w.discovered = true }, x.v2) match {
      case Some(z) => recursiveDFS(visitor, z)
      case None => visitor
    }

  private def markUndiscoveredVertex(f: Vertex[V] => Unit, vx: Vertex[V]) = {
    val vwo: Option[Vertex[V]] = Option.when(!vx.discovered)(vx)
    vwo foreach f
    vwo
  }

}

object VertexMap {
  /**
   * Method to yield an empty VertexMap.
   *
   * @tparam V the underlying node attribute type.
   * @return an empty VertexMap[V].
   */
  def empty[V]: VertexMap[V] = VertexMap[V](Map.empty)

  /**
   * Method to add a VertexPair to the given map.
   *
   * @param map a Map[V, Vertex of V].
   * @param x1  the first Vertex.
   * @param x2  the second Vertex.
   * @tparam V the underlying type.
   * @return an updated version of map.
   */
  def addVertexPair[V](map: Map[V, Vertex[V]], x1: Vertex[V], x2: Vertex[V]): Map[V, Vertex[V]] =
    map + (x1 + Pair[V](x1.attribute, x2)).keyValue + x2.keyValue

}
