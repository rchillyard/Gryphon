/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.applications.unionFind

import com.phasmidsoftware.gryphon.core.{AbstractDisjointSet, DisjointSet, GraphException}

/**
 * An abstract class to represent a solution to the Union-Find problem.
 *
 * @param map a Map of V -> Option[V] which represents the component map for each object.
 * @tparam V the underlying type of the objects.
 */
abstract class AbstractUnionFind[V, W](map: Map[V, W])(f: W => Option[V]) extends AbstractDisjointSet[V, W](map)(f) {

    /**
     * Method to connect v1 and v2.
     *
     * @param v1 an object (site).
     * @param v2 another object (site).
     * @return a new Connected object on which isConnected(v1, v2) will be true.
     */
    def connect(v1: V, v2: V): DisjointSet[V] = if (v1 == v2) this else doMerge(getDisjointSet(v1), getDisjointSet(v2))

    /**
     * Method to create a new Map such that v1 and v2 are unioned.
     *
     * @param v1 an object (site).
     * @param v2 another object (site).
     * @return a new Map
     */
    protected def union(v1: V, v2: V): Map[V, W]

    def meanDepth: Double = map.keys.map(depth).sum * 1.0 / map.size

    def maxDepth: Double = map.keys.map(depth).max

    /**
     * Method to merge the disjoint sets whose roots are v1 and v2.
     *
     * @param v1 an object/size tuple.
     * @param v2 another object/size tuple.
     * @return a new DisjointSet formed from invoking union(v1, v2).
     */
    private def doMerge(v1: V, v2: V): DisjointSet[V] = if (v1 == v2) this else unit(union(v1, v2))
}

/**
 * Case class to represent the solution to the Union-Find problem which may be known as "Quick Union."
 * It is a lazy solution which can result in very unbalanced disjoint sets and so is O(n).
 *
 * @param map a Map of V -> Option[V] which represents the map of disjoint sets for each object.
 * @tparam V the underlying type of the objects.
 */
case class UnionFind[V](map: Map[V, Option[V]]) extends AbstractUnionFind[V, Option[V]](map)(identity) {

    /**
     * Method to connect two objects and return a new UnionFind.
     *
     * @param v1 an object (site).
     * @param v2 another object (site).
     * @return a new Connected object on which isConnected(v1, v2) will be true.
     */
    override def connect(v1: V, v2: V): UnionFind[V] = super.connect(v1, v2).asInstanceOf[UnionFind[V]]

    /**
     * Method to implement basic union.
     *
     * NOTE this is a violation of the ASP because we arbitrarily join v1 to v2.
     * However, it is fixed in WeightedUnionFind so we won't be fixing it here.
     *
     * v1 and v2 must be different.
     *
     * @param v1 an object (site).
     * @param v2 another object (site).
     * @return a new Map
     */
    protected def union(v1: V, v2: V): Map[V, Option[V]] =
        if (v1 != v2) updated(v1, Some(v2)) else throw GraphException(s"UnionFind: union: objects are the same: $v1 and $v2")

    /**
     * Method to create a new DisjointSet from <code>this</code> by adding a new object which will be its own component.
     *
     * @param key a V.
     * @return a new DisjointSet.
     */
    def put(key: V): UnionFind[V] = unit(map + (key -> None))

    /**
     * Method to create a new UnionFind object from the given map.
     *
     * @param map See description of <code>map</code> field for this class.
     * @return a new UnionFind object.
     */
    def unit(map: Map[V, Option[V]]): UnionFind[V] = UnionFind[V](map)
}

/**
 * Companion object to UnionFind.
 */
object UnionFind {
    /**
     * Factory method to construct a new UnionFind from a sequence of tuples.
     *
     * @param entries a sequence of (V, Option[V]) tuples, i.e. the nodes and their parents.
     * @tparam V the object type.
     * @return a new UnionFind object constructed from the entries.
     */
    def apply[V](entries: Seq[(V, Option[V])]): UnionFind[V] = new UnionFind(entries.toMap)

    /**
     * Factory method to construct an empty UnionFind structure.
     *
     * @tparam V the underlying object type.
     * @return an empty UnionFind object.
     */
    def empty[V]: UnionFind[V] = apply(Nil)

    /**
     * Factory method to create a new UnionFind from a list of objects.
     *
     * @param vs a list of objects.
     * @tparam V the object type.
     * @return a new UnionFind[V].
     */
    def create[V](vs: V*): UnionFind[V] = UnionFind(vs.map(v => v -> None))
}

/**
 * Case class to represent the solution to the Union-Find problem which may be known as "Weighted Quick Union."
 * It is a lazy solution which results in a somewhat balanced disjoint sets and so is O(log n).
 *
 * @param map a Map of V -> ParentSize[V] which represents the component map with sizes for each object.
 * @tparam V the underlying type of the objects.
 */
case class WeightedUnionFind[V](map: Map[V, ParentSize[V]]) extends AbstractUnionFind[V, ParentSize[V]](map)(_.parent) {

    override def connect(v1: V, v2: V): WeightedUnionFind[V] = super.connect(v1, v2).asInstanceOf[WeightedUnionFind[V]]

    /**
     * Method to create a new DisjointSet from the given map.
     *
     * @param map See description of <code>map</code> field for this class.
     * @return a new WeightedUnionFind[V].
     */
    def unit(map: Map[V, ParentSize[V]]): WeightedUnionFind[V] = WeightedUnionFind[V](map)

    /**
     * Method to create a new DisjointSet from this by adding a new object which will be its own component.
     *
     * @param key a V.
     * @return a new WeightedUnionFind[V].
     */
    def put(key: V): WeightedUnionFind[V] = unit(map + (key -> ParentSize[V]))

    /**
     * Method to create a new Map such that v1 and v2 are unioned.
     * v1 and v2 must be different.
     *
     * @param v1 an object (site).
     * @param v2 another object (site).
     * @return a new Map of V -> ParentSize[V].
     */
    protected def union(v1: V, v2: V): Map[V, ParentSize[V]] =
        if (v1 != v2) {
            def join(w1: V, w2: V, s: Int): Map[V, ParentSize[V]] = updated(w1, ParentSize(Some(w2), s)).updated(w1, ParentSize(Some(w2), s))

            (get(v1), get(v2)) match {
                case (Some(ParentSize(_, s1)), Some(ParentSize(_, s2))) if s1 < s2 =>
                    val joined = join(v1, v2, s1 + s2)
                    joined
                case (Some(ParentSize(_, s1)), Some(ParentSize(_, s2))) =>
                    val joined = join(v2, v1, s1 + s2)
                    joined
                case _ => throw GraphException(s"UnionFind: logic error")
            }
        }
        else throw GraphException(s"WeightedUnionFind: union: objects are the same: $v1 and $v2")
}

/**
 * Companion object to WeightedUnionFind.
 */
object WeightedUnionFind {
    /**
     * Factory method to construct a new WeightedUnionFind from a sequence of tuples.
     *
     * @param entries a sequence of (V, Option[V]) tuples, i.e. the nodes and their parents.
     * @tparam V the object type.
     * @return a new UnionFind object constructed from the entries.
     */
    def apply[V](entries: Seq[(V, ParentSize[V])]): WeightedUnionFind[V] = new WeightedUnionFind(entries.toMap)

    /**
     * Factory method to construct an empty WeightedUnionFind structure.
     *
     * @tparam V the underlying object type.
     * @return an empty UnionFind object.
     */
    def empty[V]: WeightedUnionFind[V] = apply(Nil)

    /**
     * Factory method to create a new WeightedUnionFind from a list of objects.
     *
     * @param vs a list of objects.
     * @tparam V the object type.
     * @return a new UnionFind[V].
     */
    def create[V](vs: V*): WeightedUnionFind[V] = WeightedUnionFind(vs.map(v => v -> ParentSize[V]))
}

case class ParentSize[V](parent: Option[V], size: Int) {
    def reparent(vo: Option[V]): ParentSize[V] = copy(parent = vo)

    def resize(s: Int): ParentSize[V] = copy(size = s)
}

object ParentSize {
    def apply[V](v: V): ParentSize[V] = apply(Some(v), 1)

    def apply[V]: ParentSize[V] = apply(None, 1)
}