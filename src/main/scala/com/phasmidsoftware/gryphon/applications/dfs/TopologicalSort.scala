/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.applications.dfs

import com.phasmidsoftware.gryphon.core.{DirectedEdge, Graph}
import com.phasmidsoftware.gryphon.visit.{IterableJournalStack, Visitor}

object TopologicalSort {

    /**
     * Find the topologically sorted sequence of vertices for a DAG (directed acyclic graph).
     *
     * @param graph a directed graph.
     * @tparam V the (key) vertex-attribute type.
     * @tparam E the edge-attribute type.
     * @tparam X the type of edge which connects two vertices. A sub-type of Edge[V,E].
     * @tparam P the property type (a mutable property currently only supported by the Vertex type).
     * @return a List of vertices in topological order.
     */
    def sort[V, E, X <: DirectedEdge[V, E], P](graph: Graph[V, E, X, P]): Seq[V] = {
        implicit val vj: IterableJournalStack[V] = new IterableJournalStack[V] {}
        val visited: Visitor[V, List[V]] = graph.dfsAll(Visitor.reversePostList[V])
        visited.journal
    }
}
