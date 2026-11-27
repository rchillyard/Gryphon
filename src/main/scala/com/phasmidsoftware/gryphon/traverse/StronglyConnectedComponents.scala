/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.traverse

import com.phasmidsoftware.gryphon.adjunct.DirectedGraph
import com.phasmidsoftware.visitor.core.Tracer
import scala.util.Random

/**
 * The result of a strongly-connected-components analysis: every vertex against an integer
 * component id (0, 1, 2, ...), vertices sharing an id belonging to the same component.
 *
 * ==Read `partition`, not `map`==
 *
 * The ids are '''arbitrary''' and genuinely vary: they come from traversal order, which is shuffled.
 * Measured over forty seeds on a DAG of singleton components, where only the numbering can
 * differ: '''two distinct numberings, one grouping'''. So two correct runs can return the same
 * decomposition under different labels, and comparing `map` between runs will report a
 * difference that is not there.
 *
 * `partition` carries no labelling and is a property of the graph. Compare that.
 *
 * This is a sharper warning than the one on `ConnectedResult`, whose ids happen not to vary --
 * and the two now share a supertype precisely so that the difference is stated once rather than
 * half-remembered on each. See [[AbstractComponentResult]] and #168.
 *
 * ==It was a bare `Map` until 1.15.0==
 *
 * `type SCCResult[V] = Map[V, Int]`, which meant the one algorithm whose ids explicitly DO vary
 * handed back a raw map and left every caller to derive the trustworthy view, while
 * `ConnectedComponents` -- whose ids do not vary -- returned a rich result that offered it. The
 * asymmetry was exactly backwards. The Java façade has offered the grouping as
 * `StronglyConnectedComponents.components` all along; this brings the Scala side into line.
 *
 * @param map every vertex against its component id.
 * @tparam V the vertex type.
 */
case class SCCResult[V](map: Map[V, Int]) extends AbstractComponentResult[V](map):

  /**
   * @param m the replacement vertex-to-component-id map.
   * @return this result with `map` replaced.
   */
  def unit(m: Map[V, Int]): TraversalResult[V, Int] = copy(map = m)

/**
 * Strongly connected components: the problem, with the algorithm that solves it.
 *
 * Two vertices are in the same strongly connected component when each is reachable from the
 * other. For an undirected graph that is just connectivity, so this is a directed-graph
 * question; see `ConnectedComponents` for the undirected one.
 *
 * ==One algorithm, for now==
 *
 * Only Kosaraju's is implemented. Tarjan's is the usual alternative — one pass rather than two —
 * and would be `tarjan` here if it is ever wanted. That is the point of naming this object for
 * the problem: a second algorithm has an obvious home, whereas under the old arrangement it
 * would have become another top-level `Tarjan` object with a `stronglyConnectedComponents`
 * method, and nothing would have related the two.
 *
 * ==Why this object exists==
 *
 * Until 1.15.0 the Scala API offered only `Kosaraju.stronglyConnectedComponents` — an object
 * named for the algorithm with a method named for the problem, which is the wrong way round and
 * the opposite of `ShortestPaths.dijkstra` next door. The Java façade has had
 * `StronglyConnectedComponents.kosaraju` all along; this brings the Scala side into line with
 * the façade that wraps it. See #171.
 */
object StronglyConnectedComponents:

  /**
   * Runs Kosaraju's algorithm, labelling each vertex with its component id.
   *
   * Two passes: a depth-first traversal of the graph to fix a finishing order, then a traversal
   * of the reverse graph in that order, each tree of which is one component.
   *
   * ==Compare groupings, never ids==
   *
   * Which vertices share a component is a property of the graph and never varies. The integers
   * labelling those components come from traversal order, which is shuffled, so two calls can
   * return the same partition under different ids.
   *
   * Measured over forty seeds on a DAG of singleton components, where only the numbering can
   * differ: two distinct numberings, one grouping. So `result.groupBy((_, id) => id)` is the
   * thing to compare; the ids themselves are an implementation detail.
   *
   * (Issue #168 proposes giving this a result type with a `partition` member, so that the
   * trustworthy view is the one you are handed rather than one you must derive.)
   *
   * @param graph  the directed graph.
   * @param random settles the traversal order, and hence the numbering.
   * @param tracer optional tracing.
   * @tparam V the vertex type.
   * @tparam E the edge-attribute type.
   * @return each vertex against its component id.
   * @note Result: varies with adjacency order; pin a seed with `given Random` to fix it.
   *       Specifically: the component NUMBERING varies, the GROUPING does not.
   */
  def kosaraju[V, E](graph: DirectedGraph[V, E])(using random: Random = Random(), tracer: Tracer[V] = Tracer.silent): SCCResult[V] =
    SCCResult(Kosaraju.stronglyConnectedComponents(graph))
