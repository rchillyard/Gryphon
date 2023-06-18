/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.applications.unionFind

import com.phasmidsoftware.gryphon.core.{AbstractDisjointSet, DisjointSet}

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
    def connect(v1: V, v2: V): DisjointSet[V] = doMerge(getDisjointSet(v1), getDisjointSet(v2))

    /**
     * Method to create a new Map such that v1 and v2 are unioned.
     *
     * @param v1 an object (site).
     * @param v2 another object (site).
     * @return a new Map
     */
    protected def union(v1: V, v2: V): Map[V, W]

    /**
     * TODO fix this violation of ASP by providing weighting.
     *
     * @param v1 an object/size tuple.
     * @param v2 another object/size tuple.
     * @return a new DisjointSet where the value for v1 is Some(v2).
     */
    private def doMerge(v1: V, v2: V): DisjointSet[V] = unit(union(v1, v2))
}

/**
 * Case class to represent the solution to the Union-Find problem which may be known as "Quick Union."
 * It is a lazy solution which can result in very unbalanced disjoint sets and so is O(n).
 *
 * @param map a Map of V -> Option[V] which represents the component map for each object.
 * @tparam V the underlying type of the objects.
 */
case class UnionFind[V](map: Map[V, Option[V]]) extends AbstractUnionFind[V, Option[V]](map)(identity) {

    /**
     * Method to implement basic union.
     *
     * NOTE this is a violation of the ASP because we arbitrarily join v1 to v2.
     *
     * @param v1 an object (site).
     * @param v2 another object (site).
     * @return a new Map
     */
    protected def union(v1: V, v2: V): Map[V, Option[V]] = updated(v1, Some(v2))

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
    def apply[V](entries: Seq[(V, Option[V])]): UnionFind[V] = new UnionFind(entries.toMap)

    def create[V](vs: V*): UnionFind[V] = UnionFind(vs.map(v => v -> None))
}


/**
 * Case class to represent the solution to the Union-Find problem which may be known as "Weighted Quick Union."
 * It is a lazy solution which results in a somewhat balanced disjoint sets and so is O(log n).
 *
 * @param map a Map of V -> Option[V] which represents the component map for each object.
 * @tparam V the underlying type of the objects.
 */
case class WeightedUnionFind[V](map: Map[V, (Option[V], Int)]) extends AbstractUnionFind[V, (Option[V], Int)](map)(_._1) {

    /**
     * Method to create a new DisjointSet from the given map.
     *
     * @param map See description of <code>map</code> field for this class.
     * @return
     */
    def unit(map: Map[V, (Option[V], Int)]): WeightedUnionFind[V] = WeightedUnionFind[V](map)


    /**
     * Method to create a new Map such that v1 and v2 are unioned.
     *
     * @param v1 an object (site).
     * @param v2 another object (site).
     * @return a new Map
     */
    protected def union(v1: V, v2: V): Map[V, (Option[V], Int)] = {
        def join(w1: V, w2: V, s: Int): Map[V, (Option[V], Int)] = updated(w1, (Some(w2), s))

        (get(v1), get(v2)) match {
            case (Some((_, s1)), Some((_, s2))) if s1 <= s2 => join(v1, v2, s1 + s2)
            case (Some((_, s1)), Some((_, s2))) => join(v2, v1, s1 + s2)
        }
    }
}

/**
 * Companion object to WeightedUnionFind.
 */
object WeightedUnionFind {
    def apply[V](entries: Seq[(V, (Option[V], Int))]): WeightedUnionFind[V] = new WeightedUnionFind(entries.toMap)

    def create[V](vs: V*): WeightedUnionFind[V] = WeightedUnionFind(vs.map(v => v -> (None -> 1)))
}