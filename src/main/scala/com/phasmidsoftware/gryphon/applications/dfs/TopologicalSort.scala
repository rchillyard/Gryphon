/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.applications.dfs

import com.phasmidsoftware.gryphon.oldcore.{DirectedEdge, EdgeLike, Graph}
import com.phasmidsoftware.gryphon.visit.{IterableJournalStack, Visitor}

object TopologicalSort {

  /**
   * Find the topologically sorted sequence of vertices for a directed graph.
   *
   * @param graph a directed graph.
   * @tparam V the (key) vertex-attribute type.
   * @tparam E the edge-attribute type.
   * @tparam X the type of edge which connects two vertices. A sub-type of Edge[V,E].
   * @tparam P the property type (a mutable property currently only supported by the Vertex type).
   * @return An IndexedSeq[V] which is a List of vertices in topological order.
   */
  def sort[V, E, X <: DirectedEdge[V, E], P](graph: Graph[V, E, X, P]): IndexedSeq[V] = {
    implicit val vj: IterableJournalStack[V] = new IterableJournalStack[V] {}
    val visited: Visitor[V, List[V]] = graph.dfsAll(Visitor.reversePostList[V])
    val topologicalSort = visited.journal.toIndexedSeq
    topologicalSort
  }

  def acyclic[V, X <: EdgeLike[V]](edges: Iterable[X], topologicalSort: IndexedSeq[V]): Boolean = {
    val zs = for {
      edge <- edges
      (v1, v2) = edge.vertices
      pos1 = topologicalSort.indexOf(v1)
      pos2 = topologicalSort.indexOf(v2)
    } yield pos1 <= pos2
    zs forall (z => z)
  }
}
