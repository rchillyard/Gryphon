/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.newcore

import scala.collection.immutable.{HashMap, TreeMap}

/**
 * Trait to model the concept of adjacency.
 * Adjacency is central to the representation of graphs and trees.
 * By using adjacency lists (bags), we can traverse a graph in time O(|E|) where E is the number of edges.
 * In terms of the number (n) of vertices, traversal takes O(nx) where x is the mean degree.
 * By referring to the Erdös-Rényi model, we can show that x ~ log(n), thus graph traversal becomes O(n log(n)).
 * See [[https://en.wikipedia.org/wiki/Erdős–Rényi_model]].
 *
 * @tparam V the attribute type of a vertex (node).
 * @tparam X the edge (connexion) type.
 */
trait Adjacency[V, X <: Connexion[V]] {
  /**
   * Method to yield all the connexions (as a Bag) from the vertex (node) identified by v.
   *
   * @param v a V.
   * @return a Bag[X].
   */
  def adjacent(v: V): Bag[X]

  /**
   * Method to add a new key-value pair of V->Bag[X] to this Adjacency.
   *
   * @param vx a tuple of V -> Bag[X].
   * @return a new Adjacency which includes the new key-value pair (vBx).
   */
  def +(vx: (V, Bag[X])): Adjacency[V, X]

  /**
   * Method to add a new connexion X at V.
   *
   * @param v a node (vertex) identifier [V].
   * @param x a connexion [X].
   * @return a new Adjacency which includes the connexion x at v.
   */
  def connect(v: V, x: X): Adjacency[V, X]

  /**
   * Method to construct a new Adjacency based on this.
   *
   * @param map the map to be used.
   * @return an Adjacency[V, X].
   */
  def unit(map: Map[V, Bag[X]]): Adjacency[V, X]
}

abstract class AbstractAdjacency[V, X <: Connexion[V]](map: Map[V, Bag[X]]) extends Adjacency[V, X] {
  /**
   * Method to yield all the connexions from the vertex (node) identified by v.
   *
   * @param v a V.
   * @return a Bag[X].
   */
  def adjacent(v: V): Bag[X] = map(v)

  /**
   * Method to add a new key-value pair of V->Bag[X] to this Adjacency.
   * Note that if the key value exists in this, its current bag will not be present in the result.
   *
   * @param vBx a tuple of V -> Bag[X].
   * @return a new Adjacency which includes the new key-value pair (vBx).
   */
  def +(vBx: (V, Bag[X])): Adjacency[V, X] = unit(map + vBx)

  /**
   * Method to add a new key-value pair of V->Bag[X] to this Adjacency.
   * Note that if the key value exists in this, its current bag will not be present in the result.
   *
   * @param v a node (vertex) identifier [V].
   * @param x a connexion X.
   * @return a new Adjacency which includes the new key-value pair (vBx).
   */
  def connect(v: V, x: X): Adjacency[V, X] = this + (v -> (map.getOrElse(v, Bag.empty) + x))

}

case class OrderedAdjacency[V: Ordering, X <: Connexion[V]](map: TreeMap[V, Bag[X]]) extends AbstractAdjacency[V, X](map) {
  def unit(map: Map[V, Bag[X]]): OrderedAdjacency[V, X] = map match {
    case m: TreeMap[V, Bag[X]] => OrderedAdjacency(m)
    case _ => throw CoreException(s"OrderedAdjacency: unit: map must be a TreeMap")
  }
}

object OrderedAdjacency {
  def empty[V: Ordering, X <: Connexion[V]]: OrderedAdjacency[V, X] = OrderedAdjacency(TreeMap.empty)
}

case class UnorderedAdjacency[V, X <: Connexion[V]](map: HashMap[V, Bag[X]]) extends AbstractAdjacency[V, X](map) {
  def unit(map: Map[V, Bag[X]]): UnorderedAdjacency[V, X] = map match {
    case m: HashMap[V, Bag[X]] => UnorderedAdjacency(m)
    case _ => throw CoreException(s"UnorderedAdjacency: unit: map must be a HashMap")
  }

}

object UnorderedAdjacency {
  def empty[V, X <: Connexion[V]]: UnorderedAdjacency[V, X] = UnorderedAdjacency(HashMap.empty)
}
