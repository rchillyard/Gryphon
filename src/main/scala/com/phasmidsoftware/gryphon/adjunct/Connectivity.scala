/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.adjunct

import com.phasmidsoftware.gryphon.core.Connected
import com.phasmidsoftware.gryphon.util.GraphException

/**
 * A trait that combines the functionalities of a disjoint set and connectivity queries.
 *
 * `Connectivity` extends both `DisjointSet` and `Connected`, allowing for efficient
 * operations on disjoint sets as well as determining connectivity between elements.
 *
 * This trait is useful for scenarios where a combination of union-find operations and
 * connectivity checks is required, such as network connectivity, clustering, and graph
 * algorithms like Kruskal's Minimum Spanning Tree (MST).
 *
 * The type parameter `V` represents the type of elements in the disjoint set.
 *
 * @tparam V the type of elements managed by this connectivity structure.
 */
trait Connectivity[V] extends DisjointSet[V, Connectivity[V]] with Connected[V]

/**
 * The `Connectivity` object provides factory methods for creating instances of `Connectivity`
 * using different strategies for managing disjoint sets. These strategies include lazy and
 * optimized implementations, each employing the "Weighted Quick Union" algorithm for
 * efficient operations.
 */
object Connectivity {
  /**
   * Creates a new `Connectivity` instance using the lazy implementation strategy.
   * Each provided element will represent a separate component initially, optimized
   * using the "Weighted Quick Union" algorithm.
   *
   * @param vertices the elements to initialize as individual components in the `Connectivity` instance.
   * @return a new `Connectivity` instance containing the given elements as disjoint sets.
   */
  def createLazy[V](vertices: V*): Connectivity[V] = ConnectivityLazy.create[V](vertices *)

  /**
   * Creates a new `Connectivity` instance using the optimized implementation strategy.
   * Each provided element will represent a separate component initially, optimized
   * using the "Weighted Quick Union" algorithm.
   *
   * @param vertices the elements to initialize as individual components in the `Connectivity` instance.
   * @return a new `Connectivity` instance containing the given elements as disjoint sets.
   */
  def createOptimized[V](vertices: V*): Connectivity[V] = ConnectivityOptimized.create[V](vertices *)
}

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
 * `unit` and `put` return `ConnectivityLazy[V]` directly,
 * with no overrides required, courtesy of the F-bounded type parameter.
 *
 * @param map the component map.
 * @tparam V the underlying object type.
 */
case class ConnectivityLazy[V](map: Map[V, ParentSize[V]])
        extends ADS[V, ParentSize[V], ConnectivityLazy[V]](map)(_.parent) with Connectivity[V]:

  def unit(map: Map[V, ParentSize[V]]): ConnectivityLazy[V] = ConnectivityLazy(map)

  def put(key: V): ConnectivityLazy[V] = unit(map + (key -> ParentSize[V]))

  protected def union(v1: V, v2: V): Map[V, ParentSize[V]] =
    WeightedUnion(map, v1, v2, "ConnectivityLazy")

/**
 * Companion object for `ConnectivityLazy`.
 */
object ConnectivityLazy:

  /**
   * Constructs a new `ConnectivityLazy` instance from a sequence of value-to-`ParentSize`
   * pairs. Each pair associates a value with its corresponding parent and size information.
   *
   * @param entries a sequence of pairs where each pair consists of a value and a `ParentSize`
   *                object that defines the parent reference and the size of the component tree.
   *
   * @return a new `ConnectivityLazy` instance initialized with the provided entries.
   */
  def apply[V](entries: Seq[(V, ParentSize[V])]): ConnectivityLazy[V] =
    new ConnectivityLazy(entries.toMap)

  /**
   * Creates an empty `ConnectivityLazy` instance with no elements.
   *
   * @return an empty `ConnectivityLazy` instance.
   */
  def empty[V]: ConnectivityLazy[V] = apply(Nil)

  /**
   * Creates a new `ConnectivityLazy` instance from the given elements. Each provided element
   * will represent a separate component initially, using "Weighted Quick Union" for management.
   *
   * @param vs the elements to initialize as individual components in the `ConnectivityLazy` instance.
   * @return a new `ConnectivityLazy` instance containing the given elements as disjoint sets.
   */
  def create[V](vs: V*): ConnectivityLazy[V] =
    ConnectivityLazy(vs.map(v => v -> ParentSize[V]))

/**
 * Shared weighted-union logic for all `ParentSize`-based disjoint-set implementations.
 *
 * Extracted here to avoid duplication between `ConnectivityLazy` and
 * `ConnectivityOptimized`, both of which use identical merge strategies.
 * A single point of change ensures the two implementations cannot silently diverge.
 */
object WeightedUnion:

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
 * Extends `ConnectivityLazy` with path compression: during every `findAndCompress`
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
        extends ADSWC[V, ParentSize[V], ConnectivityOptimized[V]](map)(_.parent) with Connectivity[V]:

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

  /**
   * Creates a new instance of `ConnectivityOptimized` from a sequence of entries.
   *
   * @param entries a sequence of key-value pairs where each key is associated with
   *                a `ParentSize` representing the parent reference and size
   *                of the component tree for that key.
   * @tparam V the type of elements managed by the disjoint set.
   * @return a new `ConnectivityOptimized` instance containing the provided entries.
   */
  def apply[V](entries: Seq[(V, ParentSize[V])]): ConnectivityOptimized[V] =
    new ConnectivityOptimized(entries.toMap)

  /**
   * Creates an empty instance of `ConnectivityOptimized` with no entries.
   *
   * @tparam V the type of elements managed by the disjoint set.
   * @return an empty `ConnectivityOptimized` instance.
   */
  def empty[V]: ConnectivityOptimized[V] = apply(Nil)

  /**
   * Creates a new instance of `ConnectivityOptimized` initialized with the provided elements.
   *
   * Each provided element is mapped to a `ParentSize` representing it as
   * a singleton root with a size of 1.
   *
   * @param vs a variable argument list of elements to be included in the disjoint set.
   * @tparam V the type of elements managed by the disjoint set.
   * @return a new `ConnectivityOptimized` instance containing the provided elements.
   */
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
 * Prefer `ConnectivityLazy` for production use.
 *
 * `unit` and `put` return `ConnectivityASP[V]` directly,
 * with no overrides required, courtesy of the F-bounded type parameter.
 *
 * @param map the component map.
 * @tparam V the underlying object type.
 */
case class ConnectivityASP[V](map: Map[V, Option[V]])
        extends ADS[V, Option[V], ConnectivityASP[V]](map)(identity) with Connectivity[V]:

  def unit(map: Map[V, Option[V]]): ConnectivityASP[V] = ConnectivityASP(map)

  def put(key: V): ConnectivityASP[V] = unit(map + (key -> None))

  protected def union(v1: V, v2: V): Map[V, Option[V]] =
    if v1 != v2 then updated(v1, Some(v2)) // ASP violation: no justification for v1 under v2 vs. v2 under v1.
    else throw GraphException(s"ConnectivityASP: union: objects are the same: $v1")

/**
 * Companion object for `ConnectivityASP`.
 */
object ConnectivityASP:

  /**
   * Creates a new instance of `ConnectivityASP` from the given sequence of entries.
   * Each entry in the sequence consists of a value of type `V` and an optional parent,
   * where `None` indicates that the value is a root, and `Some(parent)` specifies the direct parent of the value.
   *
   * @param entries a sequence of pairs, where each pair contains a value of type `V` and an optional parent of type `Option[V]`.
   * @return a `ConnectivityASP[V]` instance initialized with the specified entries.
   */
  def apply[V](entries: Seq[(V, Option[V])]): ConnectivityASP[V] = new ConnectivityASP(entries.toMap)

  /**
   * Provides an empty instance of `ConnectivityASP` with no elements.
   *
   * @return a `ConnectivityASP[V]` instance that is empty, containing no entries.
   */
  def empty[V]: ConnectivityASP[V] = apply(Nil)

  /**
   * Creates a new instance of `ConnectivityASP` using the provided sequence of values.
   * Each value becomes a root node in the disjoint-set, with no parent initially assigned.
   *
   * @param vs a variable-length sequence of values of type `V` to be added as root nodes.
   * @return a `ConnectivityASP[V]` instance initialized with the specified values,
   *         where each value is mapped to `None` as its parent.
   */
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

  /**
   * Creates a new instance of `ParentSize` with an updated parent reference.
   *
   * @param vo The new parent reference to be set. Use `None` to indicate no parent
   *           (root node) or `Some(v)` to set a specific parent `v`.
   * @return A new `ParentSize[V]` instance with the updated parent reference.
   */
  def reparent(vo: Option[V]): ParentSize[V] = copy(parent = vo)

  /**
   * Creates a new instance of `ParentSize` with an updated size.
   *
   * @param s The new size of the component tree. This value represents either the
   *          number of objects in the tree (if at the root) or an upper bound on
   *          the tree's depth (rank) when path compression is active.
   * @return A new `ParentSize[V]` instance with the updated size.
   */
  def resize(s: Int): ParentSize[V] = copy(size = s)

/**
 * Companion object for `ParentSize`.
 */
object ParentSize:

  /** Creates a `ParentSize` with `parent = Some(v)` and `size = 1`. */
  def apply[V](v: V): ParentSize[V] = apply(Some(v), 1)

  /** Creates a `ParentSize` with `parent = None` and `size = 1` (singleton root). */
  def apply[V]: ParentSize[V] = apply(None, 1)