/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.newcore

import scala.collection.immutable.{HashMap, TreeMap}

/**
 * Trait to model the concept of adjacency.
 *
 * @tparam V the attribute type of a vertex (node).
 * @tparam X the edge (connection) type.
 */
trait Adjacency[V, X <: Connection[V]] {
  /**
   * Method to yield all the connections from the vertex (node) identified by v.
   *
   * @param v a V.
   * @return a Bag[X].
   */
  def adjacent(v: V): Bag[X]

  def +(vx: (V, Bag[X])): Adjacency[V, X]

  def unit(map: Map[V, Bag[X]]): Adjacency[V, X]
}

abstract class AbstractAdjacency[V, X <: Connection[V]](map: Map[V, Bag[X]]) extends Adjacency[V, X] {
  /**
   * Method to yield all the connections from the vertex (node) identified by v.
   *
   * @param v a V.
   * @return a Bag[X].
   */
  def adjacent(v: V): Bag[X] = map(v)

  def +(vx: (V, Bag[X])): Adjacency[V, X] = unit(map + vx)

}

case class OrderedAdjacency[V: Ordering, X <: Connection[V]](map: TreeMap[V, Bag[X]]) extends AbstractAdjacency[V, X](map) {
  def unit(map: Map[V, Bag[X]]): Adjacency[V, X] = map match {
    case m: TreeMap[V, Bag[X]] => OrderedAdjacency(m)
    case _ => throw CoreException(s"OrderedAdjacency: unit: map must be a TreeMap")
  }
}

object OrderedAdjacency {
  def empty[V: Ordering, X <: Connection[V]]: OrderedAdjacency[V, X] = OrderedAdjacency(TreeMap.empty)
}

case class UnorderedAdjacency[V, X <: Connection[V]](map: HashMap[V, Bag[X]]) extends AbstractAdjacency[V, X](map) {
  def unit(map: Map[V, Bag[X]]): Adjacency[V, X] = map match {
    case m: HashMap[V, Bag[X]] => UnorderedAdjacency(m)
    case _ => throw CoreException(s"UnorderedAdjacency: unit: map must be a HashMap")
  }

}

object UnorderedAdjacency {
  def empty[V, X <: Connection[V]]: UnorderedAdjacency[V, X] = UnorderedAdjacency(HashMap.empty)
}
