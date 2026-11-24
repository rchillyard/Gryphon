/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.adjunct

import com.phasmidsoftware.gryphon.core.*
import org.slf4j.{Logger, LoggerFactory}
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
   * `VertexMap.+[E]` calls `ensure` for both endpoints, so the destination vertex is present in
   * the map even when it has no outgoing edges of its own — Issue #16.
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
   * @param absent    decides whether a triplet carrying no attribute is admissible (implicit):
   *                  it is where `E` has a value meaning "no attribute", which is `Unit` and
   *                  nothing else. Otherwise such a triplet yields a `Failure` naming it, rather
   *                  than the mistyped edge both flavours used to build. See #56.
   *
   * @tparam V the vertex attribute type.
   * @tparam E the edge attribute type.
   * @tparam G the concrete graph kind being built.
   * @return `Success(G)`, or `Failure(GraphException)` if the triplets could not be parsed, if one
   *         carries no attribute where `E` cannot represent that, or if the finished graph does not
   *         hold the adjacencies the triplets described.
   */
  def fromTriplets[V, E, G <: AbstractGraph[V]](
                                                       f: V => Vertex[V],
                                                       warn: Triplet[V, E, EdgeType] => Option[String],
                                                       adjacency: (Vertex[V], Vertex[V], Option[E]) => Adjacency[V],
                                                       bothWays: Triplet[V, E, EdgeType] => Boolean,
                                                       build: VertexMap[V] => G
                                               )(triples: Seq[Triplet[V, E, EdgeType]])(using absent: AbsentAttribute[E]): Try[G] =
    SerializableGraph.createFromTriplets[V, E, EdgeType](triples) match
      case _: Triplets[V, E, EdgeType] if absent.maybeValue.isEmpty && triples.exists(_.maybeAttribute.isEmpty) =>
        // A triplet with no attribute, in a graph whose E has no value meaning "no attribute".
        // Both flavours used to admit it and lie about the type — see AbsentAttribute and #56 —
        // which handed the caller an edge whose `attribute` threw ClassCastException when read.
        // Refusing here is the difference between a failure at construction, naming the triplet,
        // and a failure much later at a use site that did nothing wrong.
        val offenders = triples.filter(_.maybeAttribute.isEmpty)
        Failure(GraphException(
          s"triplesToTryGraph: ${offenders.size} triplet(s) have no attribute, but this graph's " +
                  s"edge type cannot represent an absent attribute. First: ${offenders.head}. " +
                  s"Either give every edge an attribute, or build a graph whose edge type is Unit."))
      case triplets: Triplets[V, E, EdgeType] =>
        val vertexMap: VertexMap[V] =
          triplets.triplets.foldLeft(VertexMap[V]) { (z, t) =>
            // TODO see issue #57: a triplet whose EdgeType disagrees with the graph being built
            //  is reported and then used anyway. It should be a Failure, but doing that needs the
            //  parser to stop producing them. Logged rather than printed in the meantime, so that
            //  an application can route or silence it; it fires on no tested path.
            warn(t).foreach(logger.warn)
            z.createVerticesFromTriplet[E, EdgeType](f)(adjacency)(bothWays(t))(t)
          }
        val graph: G = build(vertexMap)
        checkAdjacencies(graph, triplets).map(_ => graph)
      case z =>
        Failure(GraphException(s"parse failed: $z"))

  /**
   * Checks that the graph contains the expected number of adjacencies for the supplied triplets.
   *
   * A one-way triplet contributes one adjacency. A two-way triplet contributes two, one at each
   * end — unless both ends are the same vertex, because `createVerticesFromTriplet` gives a
   * self-loop a single adjacency on a single vertex, deliberately. Before 1.11.1 the count did not
   * know that, so every self-loop in an undirected graph produced a spurious warning: two of the
   * three `numberOfSelfLoops` tests reported one on every run. Issue #57.
   *
   * This is an internal invariant, not a statement about the caller's data: a mismatch means the
   * graph does not hold what the triplets described, which is this library's fault and not
   * something a caller can act on from a line on stderr. Hence a `Failure` rather than a warning.
   *
   * @param graph    the graph whose adjacencies are counted.
   * @param triplets the triplets used to determine the expected adjacency count.
   * @tparam V the vertex attribute type.
   * @tparam E the edge attribute type.
   * @return `Success(())` if the counts agree, otherwise `Failure(GraphException)`.
   */
  private def checkAdjacencies[V, E](graph: AbstractGraph[V], triplets: Triplets[V, E, EdgeType]): Try[Unit] =
    val expected = triplets.triplets.map(t => if t.edgeType.oneWay || t.from == t.to then 1 else 2).sum
    val actual = graph.adjacencies.size
    if actual == expected then Success(())
    else Failure(GraphException(
      s"fromTriplets: the graph holds $actual adjacencies where ${triplets.triplets.size} triplet(s) " +
              s"describe $expected. This is an internal inconsistency in Gryphon, not a problem with " +
              s"the input; please report it with the graph which produced it (#57)."))

  /**
   * Where this object's diagnostics go. Until 1.11.1 they went to `System.err` directly, which an
   * application could neither route nor silence — and which, being nobody's job to read, is how
   * the adjacency-count mismatch of #57 went unnoticed for so long.
   */
  private val logger: Logger = LoggerFactory.getLogger(getClass)
