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

    // Each vertex starts as its own component using optimized Connectivity
    val wc0: Connectivity[V] = Connectivity.createOptimized(vertices *)

    @scala.annotation.tailrec
    def loop(wc: Connectivity[V], mstEdges: Seq[Edge[V, E]]): Seq[Edge[V, E]] =
      // getDisjointSet walks the parent chain to the true root.
      if vertices.map(wc.getDisjointSet).toSet.size == 1 then mstEdges
      else
        val cheapest = cheapestCrossingEdges(graph, vertices, wc)
        val uniqueEdges = uniqueCrossingEdges(cheapest, wc).values
        val (wc1, mstEdges1) = mergeUniqueEdges(wc, mstEdges, uniqueEdges)
        loop(wc1, mstEdges1)

    loop(wc0, Seq.empty)

  /**
   * Adds unique crossing edges to the current Minimum Spanning Tree (MST) and updates the
   * connectivity structure to reflect the new connections.
   *
   * For each edge in `uniqueEdges`, the method checks if the edge's endpoints belong to
   * different components. If they do, the edge is added to the MST and the components are merged.
   *
   * @param wc          the current connectivity state, represented as a disjoint-set structure.
   *                    This is used to determine whether two vertices are in the same connected component.
   * @param mstEdges    the sequence of edges that currently form the MST.
   * @param uniqueEdges the collection of unique edges to be considered. Each edge connects two vertices
   *                    in different components and contributes to forming the MST.
   * @tparam V the type of the vertices in the graph.
   * @tparam E the type of the edge weight, which must have a `Monoid` and `Ordering` instance.
   * @return a tuple containing the updated connectivity state and the updated sequence of MST edges.
   */
  private def mergeUniqueEdges[V, E: {Monoid, Ordering}](wc: Connectivity[V], mstEdges: Seq[Edge[V, E]], uniqueEdges: Iterable[Edge[V, E]]) =
    val (wc1, mstEdges1) = uniqueEdges.foldLeft((wc, mstEdges)) { case ((wcAcc, edges), edge) =>
      val u = edge.white
      val v = edge.black
      if wcAcc.isConnected(u, v) then (wcAcc, edges)
      else (wcAcc.connect(u, v), edges :+ edge)
    }
    (wc1, mstEdges1)

  /**
   * Creates a mapping of unique crossing edges between disjoint components in a graph.
   *
   * Each entry in the resulting map associates a pair of component roots (represented as a `Set[V]`)
   * with the cheapest edge connecting the corresponding components. A crossing edge is an edge that
   * links two vertices residing in different components as determined by the disjoint-set structure.
   *
   * This method requires the input map `cheapest` where each component's root vertex is mapped
   * to its cheapest crossing edge. It uses the `Connectivity` structure `wc` to evaluate the
   * disjoint components of vertices connected by an edge.
   *
   * @param cheapest a map where each key is a root vertex of a component, and the value is the
   *                 cheapest edge crossing out of that component.
   * @param wc       the current connectivity information represented as a disjoint-set structure,
   *                 which provides the root component for any given vertex.
   * @tparam V       the vertex type in the graph.
   * @tparam E       the edge type, which must support `Monoid` and `Ordering` instances.
   * @return         a map whose keys are sets containing two component roots, and values are the cheapest
   *         edges connecting those components.
   */
  private def uniqueCrossingEdges[V, E: {Monoid, Ordering}](cheapest: Map[V, Edge[V, E]], wc: Connectivity[V]) =
    cheapest.map {
      case (root, edge) =>
        val otherRoot = if wc.getDisjointSet(edge.white) == root
        then wc.getDisjointSet(edge.black)
        else wc.getDisjointSet(edge.white)
        Set(root, otherRoot) -> edge
    }

  /**
   * For each component (identified by its root), find the minimum-weight edge crossing to a different component.
   *
   * This is the per-round embarrassingly parallel step: each vertex independently
   * inspects its crossing edges with no inter-vertex dependencies.
   *
   * CONSIDER Replacing the sequential `foldLeft` with a parallel fold over `vertices.par` to exploit it.
   * However, note that we would not then be able to use `ConnectivityOptimized` and the trade-off
   * versus running `ConnectivityLazy` with parallelism would likely not be worth it.
   *
   * @param graph    the undirected weighted graph.
   * @param vertices all vertex keys.
   * @param wc       current connectivity state.
   * @return a map from component root to the cheapest crossing edge out of that component.
   */
  private def cheapestCrossingEdges[V, E: {Monoid, Ordering}](
                                                                     graph: UndirectedGraph[V, E],
                                                                     vertices: Seq[V],
                                                                     wc: Connectivity[V]
                                                             ): Map[V, Edge[V, E]] =
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