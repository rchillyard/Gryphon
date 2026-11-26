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
   * @param reject    given a triplet, `Some(reason)` if its orientation disagrees with the graph
   *                  being built, otherwise `None`. Such a triplet is refused; see #57.
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
                                                       reject: Triplet[V, E, EdgeType] => Option[String],
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
      case triplets: Triplets[V, E, EdgeType] if triplets.triplets.exists(reject(_).isDefined) =>
        // A triplet whose EdgeType disagrees with the graph being built. Up to 1.11.0 this was
        // reported — to stderr, then to a logger — and the triplet was used anyway, so the caller
        // got a Success containing a graph built partly from data the library had just called
        // wrong, and only learned of it if somebody was reading the log. It is the same kind of
        // thing as a parse failure and as the absent-attribute case above — the input does not
        // describe the graph being asked for — so it now takes the same exit. Issue #57.
        val offenders = triplets.triplets.filter(reject(_).isDefined)
        Failure(GraphException(
          s"triplesToTryGraph: ${offenders.size} triplet(s) have an orientation that disagrees " +
                  s"with the graph being built: ${reject(offenders.head).get} " +
                  s"First offender: ${offenders.head}. " +
                  s"Either correct the triplets, or build a graph of the other orientation."))
      case triplets: Triplets[V, E, EdgeType] =>
        val vertexMap: VertexMap[V] =
          triplets.triplets.foldLeft(VertexMap[V]) { (z, t) =>
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

  // The logger that lived here is gone. It had one call site, the orientation warning, and with
  // #57 finished that warning is a Failure instead: both of this object's diagnostics are now
  // returned to the caller rather than written anywhere. Note the consequence for #102 — this was
  // the project's first real logging call, so slf4j-api is once again declared and barely used.
