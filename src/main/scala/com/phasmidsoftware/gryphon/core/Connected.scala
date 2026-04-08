/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.core

/**
 * Trait to model the capability of answering connectivity queries over a set of objects.
 *
 * A type implementing `Connected[V]` can determine whether any two objects of type `V`
 * belong to the same connected component.
 *
 * Note the distinction from `Connexion[V]`, which models a specific edge between exactly
 * two vertices. `Connected[V]` is a higher-level oracle over the whole structure.
 *
 * Typical implementations: `Connectivity`, `WeightedUnionFind`, `ConnectedComponents`.
 *
 * @tparam V the underlying object type.
 */
trait Connected[V]:
  /**
   * Determines whether `v1` and `v2` belong to the same connected component.
   *
   * @param v1 an object.
   * @param v2 another object.
   * @return true if `v1` and `v2` are in the same component.
   */
  def isConnected(v1: V, v2: V): Boolean
