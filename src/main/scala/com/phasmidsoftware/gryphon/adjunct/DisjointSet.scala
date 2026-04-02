/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.adjunct

import com.phasmidsoftware.gryphon.core.Connected
import com.phasmidsoftware.gryphon.util.GraphException
import scala.annotation.tailrec

/**
 * Trait to model the behavior of a disjoint set.
 * The most typical application of this trait is the Union-Find problem.
 *
 * @tparam V the underlying object type.
 */
trait DisjointSet[V] extends Connected[V]:

  /**
   * Returns the root (representative) of the component containing `key`.
   *
   * @param key a key.
   * @return the root of the component tree to which `key` belongs.
   */
  def getDisjointSet(key: V): V

  /**
   * The number of disjoint sets (components), not the number of objects.
   *
   * @return the number of disjoint sets.
   */
  def size: Int

/**
 * Abstract base class for disjoint-set implementations.
 *
 * Each object `V` in `map` is associated with a value of type `W`.
 * The function `f` extracts an optional parent `V` from a `W` value.
 * If `f(w) == None` the object is the root of its own component.
 *
 * @param map the component map: each `V` maps to its associated `W`.
 * @param f   extracts the optional parent from a `W` value.
 * @tparam V the underlying object type.
 * @tparam W the map value type (may equal `Option[V]` or compound it with extra data).
 */
abstract class AbstractDisjointSet[V, W](map: Map[V, W])(f: W => Option[V]) extends DisjointSet[V]:

  /**
   * True if `v` is the root of its own component (i.e. has no parent).
   */
  def isRoot(v: V): Boolean = parent(v).isEmpty

  /**
   * Returns the optional parent of `v`.
   */
  def parent(v: V): Option[V] = get(v) flatMap f

  /**
   * Returns the depth of `v` in the component tree (root has depth 1).
   */
  def depth(v: V): Int = inner(v, 1)._2

  /**
   * Returns the number of disjoint sets (roots).
   */
  override def size: Int = map.keys.count(isRoot)

  /**
   * Constructs a new instance of this disjoint set from the given map.
   */
  def unit(map: Map[V, W]): DisjointSet[V]

  /**
   * Adds `key` as a new singleton component.
   */
  def put(key: V): DisjointSet[V]

  /**
   * Merges the components containing `v1` and `v2`.
   * Short-circuits if they are already in the same component.
   */
  def connect(v1: V, v2: V): DisjointSet[V] =
    if v1 == v2 then this else doMerge(getDisjointSet(v1), getDisjointSet(v2))

  /** The mean path length to root across all objects. */
  def meanDepth: Double = map.keys.map(depth).sum.toDouble / map.size

  /** The maximum path length to root across all objects. */
  def maxDepth: Double = map.keys.map(depth).max.toDouble

  /**
   * Subclass-specific map update that unions the components rooted at `v1` and `v2`.
   * Both `v1` and `v2` are guaranteed to be distinct roots when this is called.
   */
  protected def union(v1: V, v2: V): Map[V, W]

  private def doMerge(v1: V, v2: V): DisjointSet[V] =
    if v1 == v2 then this else unit(union(v1, v2))

  /**
   * Removes `key` from this disjoint set.
   */
  def remove(key: V): DisjointSet[V] = unit(map.removed(key))

  /**
   * Returns an updated map with `key -> value`.
   */
  def updated[W1 >: W](key: V, value: W1): Map[V, W1] = map.updated(key, value)

  /**
   * Returns the `W` value associated with `key`, if present.
   */
  def get(key: V): Option[W] = map.get(key)

  /**
   * Returns an iterator over all `(V, W)` entries.
   */
  def iterator: Iterator[(V, W)] = map.iterator

  /**
   * True if `v1` and `v2` belong to the same component.
   */
  def isConnected(v1: V, v2: V): Boolean = getDisjointSet(v1) == getDisjointSet(v2)

  /**
   * Returns the root of the component containing `key`.
   */
  def getDisjointSet(key: V): V = inner(key, 0)._1

  @tailrec
  private def inner(w: V, d: Int): (V, Int) = get(w) map f match
    case Some(None) => (w, d)
    case Some(Some(x)) if x != w => inner(x, d + 1)
    case Some(Some(x)) =>
      throw GraphException(s"DisjointSet: corrupted structure: key $x points to itself")
    case None =>
      throw GraphException(s"DisjointSet: inner: key $w does not exist")