/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.adjunct

import com.phasmidsoftware.gryphon.util.GraphException

/**
 * Concrete disjoint-set implementation using "Weighted Quick Union."
 *
 * Each object maps to a `ParentSize[V]` carrying both its optional parent and the
 * size of its component tree. When merging, the smaller tree is attached under the
 * larger, keeping tree height O(log n) and all operations O(log n).
 *
 * This implementation obeys the Arbitrary Substitution Principle (ASP): the
 * choice of which root becomes the child is justified by tree size, not arbitrary.
 *
 * `unit` and `put` return `Connectivity[V]` directly,
 * with no overrides required, courtesy of the F-bounded type parameter.
 *
 * @param map the component map.
 * @tparam V the underlying object type.
 */
case class Connectivity[V](map: Map[V, ParentSize[V]])
        extends ADS[V, ParentSize[V], Connectivity[V]](map)(_.parent):

  def unit(map: Map[V, ParentSize[V]]): Connectivity[V] = Connectivity(map)

  def put(key: V): Connectivity[V] = unit(map + (key -> ParentSize[V]))

  protected def union(v1: V, v2: V): Map[V, ParentSize[V]] =
    WeightedUnion(map, v1, v2, "Connectivity")

/**
 * Companion object for `Connectivity`.
 */
object Connectivity:

  def apply[V](entries: Seq[(V, ParentSize[V])]): Connectivity[V] =
    new Connectivity(entries.toMap)

  def empty[V]: Connectivity[V] = apply(Nil)

  def create[V](vs: V*): Connectivity[V] =
    Connectivity(vs.map(v => v -> ParentSize[V]))

/**
 * Shared weighted-union logic for all `ParentSize`-based disjoint-set implementations.
 *
 * Extracted here to avoid duplication between `Connectivity` and
 * `ConnectivityOptimized`, both of which use identical merge strategies.
 * A single point of change ensures the two implementations cannot silently diverge.
 */
private[adjunct] object WeightedUnion:

  /**
   * Merges the components rooted at `v1` and `v2` in `map` by attaching the
   * smaller tree under the larger, updating the root's size accordingly.
   * Both `v1` and `v2` must be distinct roots present in `map`.
   *
   * @param label a class name used in exception messages.
   */
  def apply[V](map: Map[V, ParentSize[V]], v1: V, v2: V, label: String): Map[V, ParentSize[V]] =
    if v1 == v2 then throw GraphException(s"$label: union: objects are the same: $v1")
    else
      def join(child: V, parent: V, size: Int): Map[V, ParentSize[V]] =
        map
                .updatedWith(child)(_.map(_.reparent(Some(parent))))
                .updatedWith(parent)(_.map(_.resize(size)))

      (map.get(v1), map.get(v2)) match
        case (Some(ParentSize(_, s1)), Some(ParentSize(_, s2))) if s1 < s2 =>
          join(v1, v2, s1 + s2)
        case (Some(ParentSize(_, s1)), Some(ParentSize(_, s2))) =>
          join(v2, v1, s1 + s2)
        case _ => throw GraphException(s"$label: union: logic error for $v1, $v2")

/**
 * Concrete disjoint-set implementation using "Weighted Quick Union with Path Compression."
 *
 * Extends `Connectivity` with path compression: during every `findAndCompress`
 * traversal to the root, all nodes along the path are rewired to point directly
 * to the root. This flattens the tree lazily, giving amortized near-O(1) performance
 * (formally O(α(n)), where α is the inverse Ackermann function).
 *
 * Path compression is implemented purely functionally: the rewired nodes are
 * accumulated into a new immutable `Map` which is returned alongside the root
 * as a fresh `ConnectivityOptimized` instance. No mutation occurs.
 *
 * Note that after path compression, `rank` (tracked via `ParentSize.size`) becomes
 * an upper bound on depth rather than the true depth — this is the standard
 * behaviour of union-by-rank with path compression, and is why the field is
 * called `size` (a weaker proxy) rather than `depth`.
 *
 * `unit` and `put` return `ConnectivityOptimized[V]` directly,
 * with no overrides required, courtesy of the F-bounded type parameter.
 *
 * @param map the component map.
 * @tparam V the underlying object type.
 */
case class ConnectivityOptimized[V](map: Map[V, ParentSize[V]])
        extends ADSWC[V, ParentSize[V], ConnectivityOptimized[V]](map)(_.parent):

  def unit(map: Map[V, ParentSize[V]]): ConnectivityOptimized[V] = ConnectivityOptimized(map)

  def put(key: V): ConnectivityOptimized[V] = unit(map + (key -> ParentSize[V]))

  /**
   * Finds the root of `key`'s component and simultaneously compresses the path,
   * rewiring every node visited to point directly to the root.
   *
   * The traversal collects the path from `key` to the root into a `List[V]`,
   * then folds over it to produce an updated `Map` in which every node on the
   * path (except the root itself) has its parent set directly to the root.
   * A fresh `ConnectivityOptimized` wrapping the updated map is returned
   * alongside the root — no mutation occurs at any point.
   *
   * @param key the element whose root is sought.
   * @return a pair `(root, compressed)` where `root` is the representative of
   *         `key`'s component and `compressed` is the path-compressed instance.
   */
  def findAndCompress(key: V): (V, ConnectivityOptimized[V]) =
    // Collect the path from key to the root.
    @scala.annotation.tailrec
    def collectPath(current: V, acc: List[V]): List[V] =
      parent(current) match
        case None => current :: acc  // current is the root
        case Some(p) => collectPath(p, current :: acc)

    // Path is built with root at head: List(root, ..., key)
    val path = collectPath(key, Nil)
    val root = path.head

    // Rewire every non-root node on the path to point directly to the root.
    val compressedMap = path.tail.foldLeft(map): (m, node) =>
      m.updatedWith(node)(_.map(_.reparent(Some(root))))

    (root, unit(compressedMap))

  protected def union(v1: V, v2: V): Map[V, ParentSize[V]] =
    WeightedUnion(map, v1, v2, "ConnectivityOptimized")

/**
 * Companion object for `ConnectivityOptimized`.
 */
object ConnectivityOptimized:

  def apply[V](entries: Seq[(V, ParentSize[V])]): ConnectivityOptimized[V] =
    new ConnectivityOptimized(entries.toMap)

  def empty[V]: ConnectivityOptimized[V] = apply(Nil)

  def create[V](vs: V*): ConnectivityOptimized[V] =
    ConnectivityOptimized(vs.map(v => v -> ParentSize[V]))

/**
 * Concrete disjoint-set implementation using "Quick Union" (lazy union),
 * with the ASP (Arbitrary Substitution Principle) violation noted in the code.
 *
 * Each object maps to `Option[V]`: `None` means the object is a root;
 * `Some(parent)` means the object's parent in the component tree.
 *
 * This is O(n) in the worst case due to potentially unbalanced trees.
 * Prefer `Connectivity` for production use.
 *
 * `unit` and `put` return `ConnectivityASP[V]` directly,
 * with no overrides required, courtesy of the F-bounded type parameter.
 *
 * @param map the component map.
 * @tparam V the underlying object type.
 */
case class ConnectivityASP[V](map: Map[V, Option[V]])
        extends ADS[V, Option[V], ConnectivityASP[V]](map)(identity):

  def unit(map: Map[V, Option[V]]): ConnectivityASP[V] = ConnectivityASP(map)

  def put(key: V): ConnectivityASP[V] = unit(map + (key -> None))

  protected def union(v1: V, v2: V): Map[V, Option[V]] =
    if v1 != v2 then updated(v1, Some(v2)) // ASP violation: no justification for v1 under v2 vs. v2 under v1.
    else throw GraphException(s"ConnectivityASP: union: objects are the same: $v1")

/**
 * Companion object for `ConnectivityASP`.
 */
object ConnectivityASP:

  def apply[V](entries: Seq[(V, Option[V])]): ConnectivityASP[V] = new ConnectivityASP(entries.toMap)

  def empty[V]: ConnectivityASP[V] = apply(Nil)

  def create[V](vs: V*): ConnectivityASP[V] = ConnectivityASP(vs.map(v => v -> None))

/**
 * Carries an optional parent reference and the size of the component tree.
 *
 * @param parent `None` if this object is a root; `Some(p)` otherwise.
 * @param size   the number of objects in this component tree (meaningful only at the root).
 *               With path compression active, this becomes an upper bound on depth
 *               (rank) rather than the precise tree size.
 *
 * @tparam V     the underlying object type.
 */
case class ParentSize[V](parent: Option[V], size: Int):

  def reparent(vo: Option[V]): ParentSize[V] = copy(parent = vo)

  def resize(s: Int): ParentSize[V] = copy(size = s)

/**
 * Companion object for `ParentSize`.
 */
object ParentSize:

  /** Creates a `ParentSize` with `parent = Some(v)` and `size = 1`. */
  def apply[V](v: V): ParentSize[V] = apply(Some(v), 1)

  /** Creates a `ParentSize` with `parent = None` and `size = 1` (singleton root). */
  def apply[V]: ParentSize[V] = apply(None, 1)