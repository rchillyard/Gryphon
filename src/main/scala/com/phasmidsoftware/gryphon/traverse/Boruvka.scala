/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.traverse

import com.phasmidsoftware.gryphon.adjunct.{Connectivity, UndirectedGraph}
import com.phasmidsoftware.gryphon.core.*
import com.phasmidsoftware.visitor.core.Monoid

/**
 * Computes the minimum spanning tree of an undirected weighted graph using Borůvka's algorithm.
 *
 * The algorithm (historically the oldest MST algorithm, 1926):
 *   1. Start with each vertex as its own component.
 *   2. For each component, find the minimum-weight edge crossing to another component.
 *   3. Add all such edges to the MST (merging components).
 *   4. Repeat until only one component remains.
 *
 * Each round halves the number of components, giving O(E log V) overall complexity.
 *
 * NOTE: this is a direct iterative algorithm and does not use the Visitor traversal engine.
 * The per-component minimum-edge scan in each round is embarrassingly parallel
 * and could be parallelised using parallel collections or futures (TODO).
 *
 * NOTE: intended for undirected weighted graphs only.
 */
object Boruvka:

  /**
   * Computes the MST of `graph` using Borůvka's algorithm.
   *
   * @param graph the undirected weighted graph.
   * @tparam V the vertex attribute type.
   * @tparam E the edge weight type; must have `Monoid` and `Ordering`.
   * @return a `VertexTraversalResult[V, Edge[V, E]]` mapping each vertex to its incident MST edge.
   */
  def mst[V, E: {Monoid, Ordering}](graph: UndirectedGraph[V, E]): VertexTraversalResult[V, Edge[V, E]] =
    val vertices = graph.vertexMap.keySet.toSeq

    // Each vertex starts as its own component.
    val wc0 = Connectivity.create(vertices *)

    @scala.annotation.tailrec
    def loop(wc: Connectivity[V], predMap: Map[V, Edge[V, E]]): Map[V, Edge[V, E]] =
      // getDisjointSet walks the parent chain to the true root.
      if vertices.map(wc.getDisjointSet).toSet.size == 1 then predMap
      else
        // For each component (identified by its root), find the minimum-weight
        // outgoing edge crossing to a different component.
        val cheapest: Map[V, Edge[V, E]] =
          vertices.foldLeft(Map.empty[V, Edge[V, E]]) { (acc, v) =>
            val vRoot: V = wc.getDisjointSet(v)
            val other: Edge[V, E] => V = e => if e.white == v then e.black else e.white
            val crossingEdges = edgesFrom(graph)(v).filter(e => wc.getDisjointSet(other(e)) != vRoot)
            if crossingEdges.isEmpty then acc
            else
              val minEdge = crossingEdges.minBy(_.attribute)
              acc.get(vRoot) match
                case Some(existing) if summon[Ordering[E]].lteq(existing.attribute, minEdge.attribute) => acc
                case _ => acc + (vRoot -> minEdge)
          }

        // Deduplicate by unordered vertex pair — two component roots may independently
        // select the same physical undirected edge (since it appears in both endpoints'
        // adjacency lists). Without deduplication, the second attempt to add the edge
        // is silently skipped via isConnected, leaving a component unmerged.
        // TODO: remove once UndirectedEdge.equals handles flipped endpoints correctly.
        val uniqueEdges = cheapest.values.map(e => Set(e.white, e.black) -> e).toMap.values

        // Add the cheapest edges, merging components.
        // connect internally calls getDisjointSet on both arguments before merging,
        // so we can pass raw vertices without pre-finding roots.
        val (wc1, predMap1) = uniqueEdges.foldLeft((wc, predMap)) { case ((wcAcc, pmAcc), edge) =>
          val u = edge.white
          val v = edge.black
          if wcAcc.isConnected(u, v) then (wcAcc, pmAcc)
          else (wcAcc.connect(u, v), pmAcc + (u -> edge) + (v -> edge))
        }

        loop(wc1, predMap1)

    VertexTraversalResult(loop(wc0, Map.empty))

  /**
   * Returns an iterator over all edges adjacent to vertex `v` in `graph`.
   * Uses `Vertex.adjacencies` directly — no traversal engine involvement.
   */
  private def edgesFrom[V, E](graph: EdgeGraph[V, E])(v: V): Iterator[Edge[V, E]] =
    for
      vv <- graph.vertexMap.get(v).iterator
      a <- vv.adjacencies.iterator
      e <- a.maybeEdge[E]
    yield e