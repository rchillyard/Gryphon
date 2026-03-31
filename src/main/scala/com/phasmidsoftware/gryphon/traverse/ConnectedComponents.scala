/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.traverse

import com.phasmidsoftware.gryphon.core.{Graph, Traversable}
import com.phasmidsoftware.gryphon.util.GraphException
import scala.annotation.tailrec
import scala.util.Random

/**
 * Type alias for the result of a connected-components analysis.
 * The first element is a `Connexions[V, E]` tracking, for each non-root vertex,
 * the directed edge via which it was discovered.
 * The second element maps every vertex to its integer component ID (0, 1, 2, ...).
 */
type ConnectedResult[V, E] = (Connexions[V, E], Map[V, Int])

/**
 * Computes connected components of an undirected graph.
 *
 * Uses `getConnexions` (the manual parent-tracking DFS on `Traversable`) to explore
 * each component in turn, labelling vertices with a monotonically increasing component ID.
 *
 * NOTE: this implementation is intended for undirected graphs only.
 * For strongly connected components of a directed graph,
 * see `StronglyConnectedComponents` (Kosaraju's algorithm) — to be implemented.
 */
object ConnectedComponents:

  /**
   * Identifies all connected components of `graph`, returning both the DFS discovery
   * tree (as `Connexions`) and a component-ID map for every vertex.
   *
   * @param graph  the graph to analyse; must be a `Graph[V]` to allow key enumeration.
   * @param random controls adjacency ordering during traversal.
   * @tparam V the vertex type.
   * @tparam E the edge-attribute type.
   * @return a `ConnectedResult[V, E]` — a pair of (Connexions, componentMap).
   * @throws GraphException if `graph` is not a `Graph[V]`.
   */
  def components[V, E](graph: Traversable[V])(using random: Random = Random()): ConnectedResult[V, E] =
    graph match
      case g: Graph[V] =>
        val allVertices = g.vertexMap.keySet

        @tailrec
        def loop(unvisited: Set[V], connexions: Connexions[V, E], componentMap: Map[V, Int], componentId: Int): ConnectedResult[V, E] =
          if unvisited.isEmpty then (connexions, componentMap)
          else
            val root = unvisited.head
            val newConnexions = graph.getConnexions[E](root)
            val visited = newConnexions.connexions.keySet + root
            val newComponentMap = componentMap ++ visited.map(_ -> componentId)
            loop(
              unvisited -- visited,
              Connexions(connexions.connexions ++ newConnexions.connexions),
              newComponentMap,
              componentId + 1
            )

        loop(allVertices, Connexions.empty[V, E], Map.empty, 0)

      case _ => throw GraphException(s"ConnectedComponents.components: graph must be a Graph[V]")