/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.traverse

import com.phasmidsoftware.gryphon.core.{Graph, GraphException, Traversable}
import scala.annotation.tailrec
import scala.util.Random

/**
 * The result of a connected-components analysis: every vertex against its component id, plus the
 * discovery tree the search built on the way.
 *
 * Until 1.14.0 this was a type alias for the tuple `(Connexions[V, E], Map[V, Int])` — half
 * first-class, since `Connexions` is itself a `TraversalResult`, and half a bare `Map`. The bare
 * half was the one every caller actually used, through `val (_, componentMap) = ...`, so
 * connected components was the one algorithm whose result could not be passed to a function
 * written over "a vertex-keyed traversal result". `TopologicalSort` is the same shape — a
 * vertex-to-`Int` map — and has been a `TraversalResult` all along. See #157.
 *
 * ==The three views, and which to trust==
 *
 * These differ in how strongly they are determined by the graph, and the difference is easy to
 * get wrong:
 *
 *  - `partition` is '''a property of the graph'''. Two runs that disagreed about which id each
 *    component received would still produce the identical `Set[Set[V]]`. Assert on this.
 *  - `map` does not vary either, but for a weaker reason: ids are handed out as `unvisited.head`
 *    walks a `Set`, which is not shuffled. That is an implementation detail, not a guarantee, so
 *    code that depends on a component being id `0` is relying on something this library does not
 *    promise.
 *  - `connexions` '''does vary'''. It records the edge each vertex was discovered by, which
 *    within a component depends on adjacency order, so it is settled by the generator passed to
 *    `ConnectedComponents.components` (#143).
 *
 * ==A trap worth naming==
 *
 * `size` is inherited from `AbstractVertexTraversalResult` and is therefore '''the number of
 * vertices''', consistent with every other `TraversalResult`. On a type called `ConnectedResult`
 * it is tempting to read it as the number of components. That is `partition.size`.
 *
 * @param map        every vertex against its component id (0, 1, 2, ...).
 * @param connexions for each non-root vertex, the directed edge it was discovered by.
 * @tparam V the vertex type.
 * @tparam E the edge-attribute type.
 */
case class ConnectedResult[V, E](map: Map[V, Int], connexions: Connexions[V, E])
        extends AbstractVertexTraversalResult[V, Int](map):

  /**
   * The components themselves, as a partition of the vertices.
   *
   * This is the strongest thing this type offers, and the one to compare between runs: unlike
   * `map` it carries no arbitrary labelling, so its invariance needs no footnote. The number of
   * components is `partition.size`.
   *
   * Derived from `map` rather than stored alongside it, so the two cannot drift apart.
   *
   * @return one `Set[V]` per connected component.
   */
  def partition: Set[Set[V]] =
    map.groupBy(_._2).values.map(_.keySet).toSet

  /**
   * @param m the replacement vertex-to-component-id map.
   * @return this result with `map` replaced, keeping the discovery tree.
   */
  def unit(m: Map[V, Int]): TraversalResult[V, Int] = copy(map = m)

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
   * @param random controls adjacency ordering during traversal, and so settles the `connexions`
   *               tree. It does not affect `map` or `partition`.
   * @tparam V the vertex type.
   * @tparam E the edge-attribute type.
   * @return a `ConnectedResult[V, E]`. Read `partition` for the components themselves, `map` for
   *         the vertex-to-id labelling, `connexions` for the discovery tree.
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
    if unvisited.isEmpty then ConnectedResult(componentMap, connexions)
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
