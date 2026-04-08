/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.adjunct

import com.phasmidsoftware.gryphon.util.GraphException

/**
 * Concrete disjoint-set implementation using "Quick Union" (lazy union).
 *
 * Each object maps to `Option[V]`: `None` means the object is a root;
 * `Some(parent)` means the object's parent in the component tree.
 *
 * This is O(n) in the worst case due to potentially unbalanced trees.
 * Prefer `WeightedConnectivity` for production use.
 *
 * `connect`, `put`, and `remove` all return `Connectivity[V]` directly,
 * with no overrides required, courtesy of the F-bounded type parameter.
 *
 * @param map the component map.
 * @tparam V the underlying object type.
 */
case class Connectivity[V](map: Map[V, Option[V]])
        extends AbstractDisjointSet[V, Option[V], Connectivity[V]](map)(identity):

  def unit(map: Map[V, Option[V]]): Connectivity[V] = Connectivity(map)

  def put(key: V): Connectivity[V] = unit(map + (key -> None))

  protected def union(v1: V, v2: V): Map[V, Option[V]] =
    if v1 != v2 then updated(v1, Some(v2))
    else throw GraphException(s"Connectivity: union: objects are the same: $v1")

/**
 * Companion object for `Connectivity`.
 */
object Connectivity:

  def apply[V](entries: Seq[(V, Option[V])]): Connectivity[V] = new Connectivity(entries.toMap)

  def empty[V]: Connectivity[V] = apply(Nil)

  def create[V](vs: V*): Connectivity[V] = Connectivity(vs.map(v => v -> None))

/**
 * Concrete disjoint-set implementation using "Weighted Quick Union."
 *
 * Each object maps to a `ParentSize[V]` carrying both its optional parent and the
 * size of its component tree. When merging, the smaller tree is attached under the
 * larger, keeping tree height O(log n) and all operations O(log n).
 *
 * `connect`, `put`, and `remove` all return `WeightedConnectivity[V]` directly,
 * with no overrides required, courtesy of the F-bounded type parameter.
 *
 * @param map the component map.
 * @tparam V the underlying object type.
 */
case class WeightedConnectivity[V](map: Map[V, ParentSize[V]])
        extends AbstractDisjointSet[V, ParentSize[V], WeightedConnectivity[V]](map)(_.parent):

  def unit(map: Map[V, ParentSize[V]]): WeightedConnectivity[V] = WeightedConnectivity(map)

  def put(key: V): WeightedConnectivity[V] = unit(map + (key -> ParentSize[V]))

  protected def union(v1: V, v2: V): Map[V, ParentSize[V]] =
    if v1 == v2 then throw GraphException(s"WeightedConnectivity: union: objects are the same: $v1")
    else
      def join(child: V, parent: V, size: Int): Map[V, ParentSize[V]] =
        map
                .updatedWith(child)(_.map(_.reparent(Some(parent))))
                .updatedWith(parent)(_.map(_.resize(size)))

      (get(v1), get(v2)) match
        case (Some(ParentSize(_, s1)), Some(ParentSize(_, s2))) if s1 < s2 =>
          join(v1, v2, s1 + s2)
        case (Some(ParentSize(_, s1)), Some(ParentSize(_, s2))) =>
          join(v2, v1, s1 + s2)
        case _ => throw GraphException(s"WeightedConnectivity: union: logic error for $v1, $v2")

/**
 * Companion object for `WeightedConnectivity`.
 */
object WeightedConnectivity:

  def apply[V](entries: Seq[(V, ParentSize[V])]): WeightedConnectivity[V] =
    new WeightedConnectivity(entries.toMap)

  def empty[V]: WeightedConnectivity[V] = apply(Nil)

  def create[V](vs: V*): WeightedConnectivity[V] =
    WeightedConnectivity(vs.map(v => v -> ParentSize[V]))

/**
 * Carries an optional parent reference and the size of the component tree.
 *
 * @param parent `None` if this object is a root; `Some(p)` otherwise.
 * @param size   the number of objects in this component tree (meaningful only at the root).
 * @tparam V the underlying object type.
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