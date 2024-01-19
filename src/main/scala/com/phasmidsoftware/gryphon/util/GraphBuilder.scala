/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.util

import com.phasmidsoftware.gryphon.oldcore._
import com.phasmidsoftware.gryphon.parse.Parseable
import com.phasmidsoftware.gryphon.util.Util.sequence
import java.net.URL
import scala.io.{BufferedSource, Source}
import scala.util.Try

/**
 * Base class for building graphs.
 *
 * CONSIDER: the entire parsing mechanism could be replaced by using TableParser.
 *
 * @tparam V the vertex type (must have evidence of Parseable).
 * @tparam E the edge type (must have evidence of Parseable).
 */
abstract class AbstractGraphBuilder[V: Parseable, E: Parseable] extends StringsParser {

  /**
   * Method to create an edge list (edge from VVE triples).
   *
   * @param uy a Try[URL] yielding the resource containing the text document.
   * @param f  function which takes two Vs and an E and returns an X (edge).
   * @return a Try of Iterable of X.
   */
  def createEdgeListTriple[X <: Edge[V, E]](uy: Try[URL])(f: (V, V, E) => X): Try[Iterable[X]] = for {
    es <- createTripleList(uy)
  } yield for {
    (v1, v2, e) <- es
  } yield f(v1, v2, e)

  /**
   * Method to create an edge list (edge from vertex pairs).
   *
   * @param uy a Try[URL] yielding the resource containing the text document.
   * @param f  function which takes two vertices and returns an X (edge).
   * @return a Try of Iterable of X.
   */
  def createEdgeListPair[X <: Edge[V, Unit]](uy: Try[URL])(f: (V, V) => X): Try[Iterable[X]] = for {
    es <- createPairList(uy)
  } yield for {
    (v1, v2) <- es
  } yield f(v1, v2)

  /**
   * Method to create a list of triples (V, V, E) from a source document.
   *
   * @param uy a Try[URL] yielding the resource containing the text document.
   * @return a Try of Iterator of Try of (V,V,E).
   */
  private def createTripleList(uy: Try[URL]): Try[Iterable[(V, V, E)]] = for {
    u <- uy
    vVEs <- sequence(processTripleSource(Source.fromURL(u)))
  } yield vVEs

  private def processTripleSource(s: BufferedSource) = for {
    string <- s.getLines()
    t3 <- parse3(string).toOption
  } yield for {
    v1 <- implicitly[Parseable[V]].parse(t3._1)
    v2 <- implicitly[Parseable[V]].parse(t3._2)
    e <- implicitly[Parseable[E]].parse(t3._3)
  } yield (v1, v2, e)

  /**
   * Method to create a list of tuples (V, V) from a source document.
   *
   * @param uy a Try[URL] yielding the resource containing the text document.
   * @return a Try of Iterator of Try of (V,V,E).
   */
  private def createPairList(uy: Try[URL]): Try[Iterable[(V, V)]] = for {
    u <- uy
    vVs <- sequence(processPairSource(Source.fromURL(u)))
  } yield vVs

  private def processPairSource(s: BufferedSource): Iterator[Try[(V, V)]] = for {
    string <- s.getLines()
    t2 <- parse2(string).toOption
  } yield for {
    v1 <- implicitly[Parseable[V]].parse(t2._1)
    v2 <- implicitly[Parseable[V]].parse(t2._2)
  } yield (v1, v2)
}

/**
 * Utility class to help create an undirected graph from an edge list, etc.
 * The edges of this class support Ordering.
 */
case class GraphBuilder[V: Ordering : Parseable, E: Parseable, P]() extends AbstractGraphBuilder[V, E] {

  /**
   * Method to create a graph from a list of edges.
   *
   * @param graph an empty graph of the appropriate type.
   * @param esy   a Try of Iterable[X].
   * @tparam X the edge type.
   * @return a Try of Graph[V, E, X, P].
   */
  def createGraphFromEdges[X <: Edge[V, E]](graph: Graph[V, E, X, P])(esy: Try[Iterable[X]]): Try[Graph[V, E, X, P]] =
    esy map {
      es => es.foldLeft(graph)((g, e) => g.addEdge(e))
    }
}
