/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.applications.unionFind

import com.phasmidsoftware.gryphon.core.{AbstractDisjointSet, DisjointSet}

case class UnionFind[V](map: Map[V, Option[V]]) extends AbstractDisjointSet[V](map) {

    def connect(v1: V, v2: V): DisjointSet[V] = doMerge(getDisjointSet(v1), getDisjointSet(v2))

    def unit(map: Map[V, Option[V]]): DisjointSet[V] = UnionFind[V](map)

    /**
     * TODO fix this violation of ASP by providing weighting.
     *
     * @param v1 the first vertex.
     * @param v2 the second vertex.
     * @return a new DisjointSet where the value for v1 is Some(v2).
     */
    private def doMerge(v1: V, v2: V): DisjointSet[V] = unit(updated(v1, Some(v2)))
}

object UnionFind {
    def apply[V](entries: Seq[(V, Option[V])]): UnionFind[V] = new UnionFind(entries.toMap)

    def create[V](vs: V*): UnionFind[V] = UnionFind(vs.map(v => v -> None))
}