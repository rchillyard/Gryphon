/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.adjunct

import com.phasmidsoftware.gryphon.core.Connected
import com.phasmidsoftware.gryphon.util.GraphException
import scala.annotation.tailrec

/**
 * Trait to model the behavior of a disjoint set.
 * Thus, we provide `put`, `connect`, `getDisjointSet`,
 * and `size` in addition to `isConnected` (from the super-trait).
 *
 * The purpose of having a disjoint set representation of a "graph" is
 * so that we can answer connectivity questions in close to `O(1)` time,
 * rather than the `O(n + m)` time required for a full traversal of an edge-connected graph.
 * The most typical application of this trait is `Kruskal`'s MST algorithm.
 *
 * The most typical concrete implementation of this trait is the `Connectivity` class
 * (usually known as `Union-Find`).
 *
 * The type parameter `This` is the F-bounded self-type, ensuring that `put` and
 * `connect` return the concrete subtype rather than `DisjointSet[V]`.
 *
 * @tparam V    the underlying object type.
 * @tparam This the concrete subtype (F-bounded polymorphism).
 */
trait DisjointSet[V, +This <: DisjointSet[V, This]]:

  /**
   * Adds the specified key to the disjoint set data structure and initializes
   * it as a new singleton set if it does not already exist.
   *
   * @param key the element to be added to the disjoint set.
   * @return the updated disjoint set containing the newly added key.
   */
  def put(key: V): This

  /**
   * Merges the sets containing the given elements `v1` and `v2`. After the
   * operation the two elements belong to the same set.
   *
   * @param v1 the first element to be connected.
   * @param v2 the second element to be connected.
   * @return the updated disjoint set with the connection established.
   */
  def connect(v1: V, v2: V): This

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
 * Abstract base class for disjoint-set implementations that relies on an immutable `Map`
 * to associate objects with their parent disjoint set.
 *
 * Each object `V` in `map` is associated with a value of type `W`.
 * The function `f` extracts an optional parent `V` from a `W` value.
 * If `f(w) == None` the object is the root of its own component.
 *
 * The F-bound `This <: AbstractDisjointSet[V, W, This]` ensures that `connect`,
 * `put`, and `remove` all return the concrete subtype without casting at the
 * call site. The only remaining `asInstanceOf` is in `doMerge`/`connect` for
 * the short-circuit case (`this` when already connected), which is safe but
 * unavoidable — the compiler cannot prove `this: This` without it.
 *
 * @param map the component map: each `V` maps to its associated `W`.
 * @param f   extracts the optional parent from a `W` value.
 * @tparam V    the underlying object type.
 * @tparam W    the map value type (may equal `Option[V]` or compound it with extra data).
 * @tparam This the concrete subtype (F-bounded polymorphism).
 */
abstract class AbstractDisjointSet[V, W, This <: AbstractDisjointSet[V, W, This]]
(map: Map[V, W])(f: W => Option[V]) extends Connected[V] with DisjointSet[V, This]:

  /**
   * True if `v1` and `v2` belong to the same disjoint set.
   */
  def isConnected(v1: V, v2: V): Boolean = getDisjointSet(v1) == getDisjointSet(v2)

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
  def size: Int = map.keys.count(isRoot)

  /**
   * Constructs a new instance of the concrete subtype from the given map.
   * Subclasses must implement this to return `This`.
   */
  def unit(map: Map[V, W]): This

  /**
   * Merges the components containing `v1` and `v2`.
   * Short-circuits (returning `this`) if they are already in the same component
   * or if `v1 == v2`. The `asInstanceOf` here is safe: `this` is always a `This`.
   */
  def connect(v1: V, v2: V): This =
    if v1 == v2 then this.asInstanceOf[This]
    else doMerge(getDisjointSet(v1), getDisjointSet(v2))

  /** The mean path length to root across all objects. */
  def meanDepth: Double = map.keys.map(depth).sum.toDouble / map.size

  /** The maximum path length to root across all objects. */
  def maxDepth: Double = map.keys.map(depth).max.toDouble

  /**
   * Subclass-specific map update that unions the components rooted at `v1` and `v2`.
   * Both `v1` and `v2` are guaranteed to be distinct roots when this is called.
   */
  protected def union(v1: V, v2: V): Map[V, W]

  /**
   * Removes `key` from this disjoint set.
   */
  def remove(key: V): This = unit(map.removed(key))

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
   * Returns the root of the component containing `key`.
   */
  def getDisjointSet(key: V): V = inner(key, 0)._1

  private def doMerge(v1: V, v2: V): This =
    if v1 == v2 then this.asInstanceOf[This] else unit(union(v1, v2))

  @tailrec
  private def inner(w: V, d: Int): (V, Int) = get(w) map f match
    case Some(None)             => (w, d)
    case Some(Some(x)) if x != w => inner(x, d + 1)
    case Some(Some(x))          =>
      throw GraphException(s"DisjointSet: corrupted structure: key $x points to itself")
    case None                   =>
      throw GraphException(s"DisjointSet: inner: key $w does not exist")