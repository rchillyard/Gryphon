/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.traverse

import com.phasmidsoftware.gryphon.adjunct.{UndirectedEdge, UndirectedGraph, WeightedUnionFind}
import com.phasmidsoftware.gryphon.core.*

/**
 * Computes the minimum spanning tree of an undirected weighted graph using Kruskal's algorithm.
 *
 * The algorithm:
 *   1. Sort all edges by weight (ascending).
 *   2. Iterate through edges; for each edge (u, v, w): if u and v are not yet connected,
 *      add the edge to the MST and union their components.
 *   3. Stop when N-1 edges have been added (MST is complete).
 *
 * Uses `WeightedUnionFind` for O(log n) connectivity queries and unions.
 *
 * NOTE: intended for undirected weighted graphs only.
 */
object Kruskal:

  /**
   * Computes the MST of `graph` using Kruskal's algorithm.
   *
   * @param graph the undirected weighted graph.
   * @tparam V the vertex attribute type.
   * @tparam E the edge weight type; must be `Numeric` and `Ordering`.
   * @return a `TraversalResult[V, Edge[V, E]]` mapping each vertex to its incident MST edge
   *         (`None` for isolated vertices or the arbitrary first vertex processed).
   */
  def mst[V, E: {Numeric, Ordering}](graph: UndirectedGraph[V, E]): Seq[Edge[V, E]] =
    val vertices = graph.vertexMap.keySet.toSeq

    // Seed WeightedUnionFind with all vertices as singleton components.
    val wuf0 = WeightedUnionFind.create(vertices *)

    // Collect and sort all edges by weight ascending.
    val sortedEdges: Seq[UndirectedEdge[V, E]] =
      graph.edges.collect { case e: UndirectedEdge[V, E] @unchecked => e }
              .toSeq
              .sortBy(_.attribute)

    // Fold over sorted edges, adding each to the MST if it connects two components.
    val (_, mstEdges) = sortedEdges.foldLeft((wuf0, Seq.empty[Edge[V, E]])) {
      case ((wuf, mst), edge) =>
        val u = edge.white
        val v = edge.black
        if wuf.isConnected(u, v) then (wuf, mst)
        else (wuf.connect(u, v), mst :+ edge)
    }

    mstEdges