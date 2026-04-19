/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.builder

import com.phasmidsoftware.gryphon.adjunct.{DirectedGraph, UndirectedGraph}
import com.phasmidsoftware.gryphon.core.{EdgeType, Graph, Triplet, Vertex}
import com.phasmidsoftware.gryphon.parse.{GraphParser, Parseable}
import com.phasmidsoftware.gryphon.util.TryUsing
import scala.io.Source
import scala.util.Try

/**
 * A purely functional builder that constructs a typed graph from a resource file or
 * file-system path, using the `GraphParser` / `Parseable` infrastructure.
 *
 * `GraphBuilder` is parameterised on the vertex type `V`, edge type `E`, and the
 * concrete graph type `G`. Callers obtain an instance via the companion-object
 * factory methods `GraphBuilder.undirected` and `GraphBuilder.directed`, which
 * supply the appropriate `triplesToTryGraph` function for each graph flavour.
 *
 * The two `Parseable` context bounds are satisfied automatically for the built-in
 * types `Int`, `Double`, `String`, etc., and can be provided by the caller for
 * custom domain types (e.g. `Building`, `TunnelProperties`).
 *
 * @param build a function that converts a sequence of parsed triplets into a `Try[G]`.
 * @tparam V the vertex attribute type.
 * @tparam E the edge attribute type.
 * @tparam G the concrete graph type, must be a subtype of `Graph[V]`.
 */
class GraphBuilder[V: Parseable, E: Parseable, G <: Graph[V]](
                                                                     build: Seq[Triplet[V, E, EdgeType]] => Try[G]
                                                             ):

  /**
   * Builds a graph by reading from a classpath resource.
   *
   * @param resourceName the name of the resource file (e.g. `"prim.graph"`).
   * @return `Success(G)` if the file is found and parses cleanly; `Failure` otherwise.
   */
  def fromResource(resourceName: String): Try[G] =
    val parser = new GraphParser[V, E, EdgeType]
    TryUsing.tryIt(Try(Source.fromResource(resourceName))) { source =>
      parser.parseSource(parser.parseTriple)(source)
    }.flatMap(build)

  /**
   * Builds a graph by reading from a file-system path.
   *
   * @param path the absolute or relative path to the graph file.
   * @return `Success(G)` if the file is found and parses cleanly; `Failure` otherwise.
   */
  def fromFile(path: String): Try[G] =
    val parser = new GraphParser[V, E, EdgeType]
    TryUsing.tryIt(Try(Source.fromFile(path))) { source =>
      parser.parseSource(parser.parseTriple)(source)
    }.flatMap(build)

/**
 * Companion object providing factory methods for the two standard graph flavours.
 *
 * Usage (built-in types):
 * {{{
 * val g: Try[UndirectedGraph[Int, Double]] =
 *   GraphBuilder.undirected[Int, Double].fromResource("prim.graph")
 *
 * val g: Try[DirectedGraph[Int, Double]] =
 *   GraphBuilder.directed[Int, Double].fromResource("routes.graph")
 * }}}
 *
 * Usage (custom types — supply `Parseable` instances for `V` and `E`):
 * {{{
 * given Parseable[Building] = ...
 * given Parseable[TunnelProperties] = ...
 *
 * val g: Try[UndirectedGraph[Building, TunnelProperties]] =
 *   GraphBuilder.undirected[Building, TunnelProperties].fromResource("tunnels.graph")
 * }}}
 */
object GraphBuilder:

  /**
   * Returns a `GraphBuilder` that produces `UndirectedGraph[V, E]` instances.
   *
   * @tparam V the vertex attribute type; must have a `Parseable` instance.
   * @tparam E the edge attribute type; must have a `Parseable` instance.
   */
  def undirected[V: Parseable, E: Parseable]: GraphBuilder[V, E, UndirectedGraph[V, E]] =
    new GraphBuilder(
      UndirectedGraph.triplesToTryGraph[V, E](Vertex.createWithSet)(_).map(_.asInstanceOf[UndirectedGraph[V, E]])
    )

  /**
   * Returns a `GraphBuilder` that produces `DirectedGraph[V, E]` instances.
   *
   * @tparam V the vertex attribute type; must have a `Parseable` instance.
   * @tparam E the edge attribute type; must have a `Parseable` instance.
   */
  def directed[V: Parseable, E: Parseable]: GraphBuilder[V, E, DirectedGraph[V, E]] =
    new GraphBuilder(
      DirectedGraph.triplesToTryGraph[V, E](Vertex.createWithSet)(_).map(_.asInstanceOf[DirectedGraph[V, E]])
    )