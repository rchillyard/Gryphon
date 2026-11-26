/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.traverse

import com.phasmidsoftware.gryphon.core.{Graph, GraphException, Traversable}
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
   * @throws com.phasmidsoftware.gryphon.core.GraphException if `graph` is not a `Graph[V]`.
   */
  // The generator reaches `getConnexions` from 1.13.0. Until then this parameter was annotated
  // `@unused` and was exactly that: the DFS inside each component built its own `Random` from
  // the default on `getConnexions`, so a seed pinned here could not reach it.
  //
  // The two halves of the result differ in this respect, which is the whole subtlety. The
  // component map does not vary: `visited` is the *set* a component's DFS reached, and ids are
  // handed out as `unvisited.head` walks a Set, neither of which is shuffled. The `Connexions`
  // tree does vary -- it records the edge by which each vertex was discovered, and within a
  // component that depends on adjacency order.
  //
  // So removing the parameter, on the grounds that no present caller reads the tree, would have
  // been wrong. That the tree is unobserved does not make it invariant; it would have carved
  // into the API a claim the value does not support, and left the variation in place, merely
  // beyond anyone's reach. See #143.
  def components[V, E](graph: Traversable[V])(using random: Random = Random()): ConnectedResult[V, E] =
    graph match
      case g: Graph[V] =>
        loop(g)(g.vertexMap.keySet, Connexions.empty[V, E], Map.empty, 0)
      case _ =>
        throw GraphException(s"ConnectedComponents.components: graph must be a Graph[V]")

  /**
   * Tail-recursive helper that discovers connected components by repeatedly selecting an
   * unvisited root vertex, collecting its connexions, assigning all vertices reached from
   * that root to the current component identifier, and continuing until no unvisited
   * vertices remain.
   *
   * @param g            the graph whose connected components are being identified.
   * @param unvisited    the vertices not yet assigned to a connected component.
   * @param connexions   the accumulated connexion data discovered so far.
   * @param componentMap the accumulated mapping from each visited vertex to its component identifier.
   * @param componentId  the identifier to assign to the next discovered connected component.
   * @tparam V the vertex type.
   * @tparam E the edge-attribute type.
   * @return the accumulated connected-component result, containing the combined connexions and component map.
   */
  @tailrec
  private def loop[V, E](g: Graph[V])(unvisited: Set[V], connexions: Connexions[V, E], componentMap: Map[V, Int], componentId: Int)(using random: Random): ConnectedResult[V, E] =
    if unvisited.isEmpty then (connexions, componentMap)
    else
      val root = unvisited.head
      val newConnexions = g.getConnexions[E](root)(using random)
      val visited = newConnexions.connexions.keySet + root
      val newComponentMap = componentMap ++ visited.map(_ -> componentId)
      loop(g)(
        unvisited -- visited,
        Connexions(connexions.connexions ++ newConnexions.connexions),
        newComponentMap,
        componentId + 1
      )
