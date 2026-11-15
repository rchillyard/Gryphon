/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.adjunct

import com.phasmidsoftware.gryphon.core.*
import scala.util.{Failure, Success, Try}

/**
 * Common base for the concrete edge graphs, [[DirectedGraph]] and [[UndirectedGraph]].
 *
 * `G` is the self type — the kind of graph this is. It exists so that `unit`, the single point
 * at which a graph of this kind is built, is typed as *that* kind rather than as the base
 * `Graph[V]`.
 *
 * That matters for more than tidiness. Until 1.8.1 `UndirectedGraph.unit` returned a
 * `DirectedGraph`, and the compiler could not see it, because the declared return type erased
 * the very distinction the two classes exist to carry. With `G` in place, the same mistake is
 * a type error.
 *
 * @tparam V the vertex attribute type.
 * @tparam E the edge attribute type.
 * @tparam G the concrete graph type — `DirectedGraph[V, E]` or `UndirectedGraph[V, E]`.
 * @param vertexMap the `VertexMap` backing this graph.
 */
abstract class AbstractAdjunctGraph[V, E, G <: AbstractAdjunctGraph[V, E, G]](vertexMap: VertexMap[V])
        extends AbstractGraph[V](vertexMap) with EdgeGraph[V, E]:

  /**
   * Builds a graph of this same kind from `vertexMap`.
   *
   * This is the only place a graph of kind `G` is constructed from another, so it is the only
   * place the kind could be got wrong — and now it cannot be, because the return type says so.
   *
   * @param vertexMap the vertex map for the new graph.
   * @return a graph of the same kind as this one.
   */
  def unit(vertexMap: VertexMap[V]): G

  /**
   * Adds an edge of the kind this graph admits, and rebuilds through `unit`.
   *
   * Identical in both subclasses: the only difference between them is `EdgeT`, which each
   * fixes to its own edge kind, so that a graph cannot absorb an edge of the other kind.
   *
   * @param edge the edge to add.
   * @return a graph of the same kind, including `edge`.
   */
  def addEdge(edge: EdgeT): G = unit(vertexMap + edge)

  /**
   * The *size* of the graph: the number of edges. Identical in both subclasses.
   *
   * This is where `size` is first given a body. `AbstractGraph` leaves it abstract on purpose,
   * because counting edges needs to know what an edge is — which is this level's business, not
   * that one's. Until 1.9.0 `AbstractGraph` did define it, as `vertexMap.M`, reading a mutable
   * counter that was wrong; both subclasses happened to override it, so nothing user-facing
   * broke. See `docs/stocktake-2026-11-15.md` §2.
   *
   * @return the number of edges in this graph.
   */
  def size: Int = edges.size

/**
 * Shared construction for the concrete edge graphs.
 */
object AbstractAdjunctGraph:

  /**
   * The skeleton shared by `DirectedGraph.triplesToTryGraph` and
   * `UndirectedGraph.triplesToTryGraph`, which differed only in three places. Both previously
   * carried their own copy, one of them labelled `NOTE duplicated code in UndirectedGraph`.
   *
   * Note the return type: `Try[G]`, not `Try[Graph[V]]`. The callers used to widen to the base
   * type here and `GraphBuilder` cast it straight back again.
   *
   * @param f         creates a vertex from an attribute.
   * @param warn      the orientation this graph does not want, as a message; see the note below.
   * @param adjacency builds the adjacency for a pair of vertices and an optional attribute.
   * @param bothWays  whether the triplet's adjacency should also be added in the reverse direction.
   * @param build     wraps the finished `VertexMap` as a graph of kind `G`.
   * @param triples   the triplets to build from.
   * @return `Success(G)`, or `Failure(GraphException)` if the triplets could not be parsed.
   */
  def fromTriplets[V, E, G <: AbstractGraph[V]](
                                               f: V => Vertex[V],
                                               warn: Triplet[V, E, EdgeType] => Option[String],
                                               adjacency: (Vertex[V], Vertex[V], Option[E]) => Adjacency[V],
                                               bothWays: Triplet[V, E, EdgeType] => Boolean,
                                               build: VertexMap[V] => G
                                       )(triples: Seq[Triplet[V, E, EdgeType]]): Try[G] =
    SerializableGraph.createFromTriplets[V, E, EdgeType](triples) match
      case triplets: Triplets[V, E, EdgeType] =>
        val vertexMap: VertexMap[V] =
          triplets.triplets.foldLeft(VertexMap[V]) { (z, t) =>
            // TODO see issue #57: a triplet whose EdgeType disagrees with the graph being built
            //  is reported to stderr and then used anyway. It should be a Failure, but doing that
            //  needs the parser to stop producing them.
            warn(t).foreach(System.err.println)
            z.createVerticesFromTriplet[E, EdgeType](f)(adjacency)(bothWays(t))(t)
          }
        val graph: G = build(vertexMap)
        checkAdjacencies(graph, triplets)
        Success(graph)
      case z =>
        Failure(GraphException(s"parse failed: $z"))

  /**
   * Reports a graph whose adjacency count does not match its triplets: one adjacency per
   * one-way triplet, two for each of the others.
   *
   * TODO see issue #57 — like the warning above, this notices a real inconsistency and then
   *  carries on regardless.
   */
  private def checkAdjacencies[V, E](graph: AbstractGraph[V], triplets: Triplets[V, E, EdgeType]): Unit =
    val expected = triplets.triplets.map(t => if t._4.oneWay then 1 else 2).sum
    val actual = graph.adjacencies.size
    if actual != expected then System.err.println(s"WARNING: $actual != $expected")
