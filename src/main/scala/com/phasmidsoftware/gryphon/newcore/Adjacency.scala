/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.newcore

/**
 * Trait to model the concept of adjacency.
 *
 * @tparam V the attribute type of a vertex (node).
 * @tparam X the edge (connection) type.
 */
trait Adjacency[V, X <: Connection[V]] {
  /**
   * Method to yield all the relations from vertex (node) identified by v.
   *
   * @param v a V.
   * @return a Seq[X].
   */
  def adjacent(v: V): Seq[X]
}
