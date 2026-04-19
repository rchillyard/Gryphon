/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.traverse

import com.phasmidsoftware.gryphon.adjunct.{Connectivity, UndirectedEdge, UndirectedGraph}
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
   * @return a `Seq[Edge[V, E]]` — the MST edges in order of addition, consistent with `Kruskal.mst`.
   */
  def mst[V, E: {Monoid, Ordering}](graph: UndirectedGraph[V, E]): Seq[Edge[V, E]] =
    val vertices = graph.vertexMap.keySet.toSeq

    // Each vertex starts as its own component.
    val wc0 = Connectivity.create(vertices *)

    @scala.annotation.tailrec
    def loop(wc: Connectivity[V], mstEdges: Seq[Edge[V, E]]): Seq[Edge[V, E]] =
      // getDisjointSet walks the parent chain to the true root.
      if vertices.map(wc.getDisjointSet).toSet.size == 1 then mstEdges
      else
        // For each component (identified by its root), find the minimum-weight
        // outgoing edge crossing to a different component.
        val cheapest: Map[V, Edge[V, E]] =
          vertices.foldLeft(Map.empty[V, Edge[V, E]]) { (acc, v) =>
            val vRoot: V = wc.getDisjointSet(v)
            val crossingEdges = edgesFrom(graph)(v).filter(e => wc.getDisjointSet(e.other(v)) != vRoot)
            if crossingEdges.isEmpty then acc
            else
              val minEdge = crossingEdges.minBy(_.attribute)
              acc.get(vRoot) match
                case Some(existing) if summon[Ordering[E]].lteq(existing.attribute, minEdge.attribute) => acc
                case _ => acc + (vRoot -> minEdge)
          }

        // Deduplicate by unordered component-root pair.
        // cheapest is keyed by component root, so for each entry we know exactly
        // which root it belongs to. The other root is the root of the far endpoint.
        // Set(root, otherRoot) correctly collapses A->B and B->A to one merge,
        // while leaving A->D, B->D, C->D as three distinct merges.
        val uniqueEdges = cheapest
                .map { case (root, edge) =>
                  val otherRoot = if wc.getDisjointSet(edge.white) == root
                  then wc.getDisjointSet(edge.black)
                  else wc.getDisjointSet(edge.white)
                  Set(root, otherRoot) -> edge
                }
                .toMap.values

        // Add each unique crossing edge to the MST and merge its components.
        val (wc1, mstEdges1) = uniqueEdges.foldLeft((wc, mstEdges)) { case ((wcAcc, edges), edge) =>
          val u = edge.white
          val v = edge.black
          if wcAcc.isConnected(u, v) then (wcAcc, edges)
          else (wcAcc.connect(u, v), edges :+ edge)
        }

        loop(wc1, mstEdges1)

    loop(wc0, Seq.empty)

  /**
   * Returns an iterator over all edges adjacent to vertex `v` in `graph`.
   * Uses `Vertex.adjacencies` directly — no traversal engine involvement.
   */
  private def edgesFrom[V, E](graph: EdgeGraph[V, E])(v: V): Iterator[UndirectedEdge[V, E]] =
    for
      vv <- graph.vertexMap.get(v).iterator
      a <- vv.adjacencies.iterator
      e <- a.maybeEdge[E]
      ue <- e match {
        case ee: UndirectedEdge[V, E] @unchecked => Some(ee);
        case _ => None
      }
    yield ue