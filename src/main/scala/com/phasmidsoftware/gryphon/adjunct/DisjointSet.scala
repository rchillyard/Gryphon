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
 * All operations are purely functional: every "mutation" returns a new instance
 * constructed via `unit`, leaving the original unchanged. This makes instances
 * persistent data structures — an old reference remains valid after any `connect`
 * or `put`.
 *
 * Each object `V` in `map` is associated with a value of type `W`.
 * The function `f` extracts an optional parent `V` from a `W` value.
 * If `f(w) == None` the object is the root of its own component.
 *
 * The F-bound `This <: AbstractDisjointSet[V, W, This]` ensures that `connect`,
 * `put`, and `remove` all return the concrete subtype without casting at the
 * call site. The only remaining `asInstanceOf` casts are in the short-circuit
 * cases where `this` is returned unchanged — these are safe but unavoidable,
 * since the compiler cannot prove `this: This` without them.
 *
 * Subclasses that implement path compression should override `findAndCompress`.
 * The default implementation simply delegates to `getDisjointSet` and returns
 * `this` unchanged, so non-compressing subclasses need not override it.
 * The `connect` method in this class calls `findAndCompress` for both arguments,
 * so compression is applied automatically during every merge in subclasses that
 * override it.
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
   * Returns the root of the component containing `key`, together with a
   * (potentially restructured) copy of this disjoint set.
   *
   * The default implementation performs no restructuring — it returns
   * `getDisjointSet(key)` paired with `this` unchanged. Subclasses that
   * implement path compression override this to rewire every node along
   * the path to the root, returning the compressed instance alongside the root.
   *
   * Making compression explicit here (rather than hiding it inside
   * `getDisjointSet`) keeps the design honest: a `DisjointSet` that does
   * not compress still satisfies the trait, and one that does compress
   * makes that fact visible in its API.
   *
   * @param key the element whose root is sought.
   * @return a pair `(root, updated)` where `root` is the representative of
   *         `key`'s component and `updated` is the (possibly compressed) instance.
   */
  def findAndCompress(key: V): (V, This) = (getDisjointSet(key), this.asInstanceOf[This])

  /**
   * Merges the components containing `v1` and `v2`.
   * Short-circuits (returning `this`) if `v1 == v2`.
   * Delegates to `findAndCompress` for both arguments so that subclasses
   * which override it get compression applied during every merge automatically.
   * The `asInstanceOf` here is safe: `this` is always a `This`.
   */
  def connect(v1: V, v2: V): This =
    if v1 == v2 then this.asInstanceOf[This]
    else
      val (root1, c1) = findAndCompress(v1)
      val (root2, c2) = c1.findAndCompress(v2)
      c2.doMerge(root1, root2)

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
   * Note: `Map.updated` is a purely functional operation on an immutable `Map`
   * — it returns a new `Map` and does not mutate the receiver.
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
    case Some(None) => (w, d)
    case Some(Some(x)) if x != w => inner(x, d + 1)
    case Some(Some(x)) =>
      throw GraphException(s"DisjointSet: corrupted structure: key $x points to itself")
    case None =>
      throw GraphException(s"DisjointSet: inner: key $w does not exist")