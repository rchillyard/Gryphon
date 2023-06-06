/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.util

import com.phasmidsoftware.gryphon.core._
import com.phasmidsoftware.gryphon.parse.Parseable
import java.net.URL
import scala.io.Source
import scala.util.{Failure, Success, Try}

abstract class GraphBuilder[V: Parseable, E: Parseable] {

    /**
     * Method to create an edge list (edge from VVE triples).
     *
     * @param uy a Try[URL] yielding the resource containing the text document.
     * @param f  function which takes two Vs and an E and returns an X (edge).
     * @return a Try of Iterable of X.
     */
    def createEdgeListTriple[X <: Edge[V, E]](uy: Try[URL])(f: (V, V, E) => X): Try[Iterable[X]] = for {
        eys <- createTripleList(uy)
        es <- Util.sequence(eys)
    } yield for {
        (v1, v2, e) <- es
    } yield f(v1, v2, e)

    /**
     * Method to create an edge list (edge from vertex pairs).
     *
     * @param uy a Try[URL] yielding the resource containing the text document.
     * @param f function which takes two vertices and returns an X (edge).
     * @return a Try of Iterable of X.
     */
    def createEdgeListPair[X <: Edge[V, Unit]](uy: Try[URL])(f: (V, V) => X): Try[Iterable[X]] = for {
        eys <- createPairList(uy)
        es <- Util.sequence(eys)
    } yield for {
        (v1, v2) <- es
    } yield f(v1, v2)

    /**
     * Method to create a list of triples (V, V, E) from a source document.
     *
     * @param uy a Try[URL] yielding the resource containing the text document.
     * @return a Try of Iterator of Try of (V,V,E).
     */
    def createTripleList(uy: Try[URL]): Try[Iterator[Try[(V, V, E)]]] = for {
        u <- uy
        s = Source.fromURL(u)
    } yield for {
        string <- s.getLines()
        Array(wV1, wV2, wE) = string.split(" ")
    } yield for {
        v1 <- implicitly[Parseable[V]].parse(wV1)
        v2 <- implicitly[Parseable[V]].parse(wV2)
        e <- implicitly[Parseable[E]].parse(wE)
    } yield (v1, v2, e)

    /**
     * Method to create a list of tuples (V, V) from a source document.
     *
     * @param uy a Try[URL] yielding the resource containing the text document.
     * @return a Try of Iterator of Try of (V,V,E).
     */
    def createPairList(uy: Try[URL]): Try[Iterator[Try[(V, V)]]] = for {
        u <- uy
        s = Source.fromURL(u)
    } yield for {
        string <- s.getLines()
        Array(wV1, wV2) = string.split(" ")
    } yield for {
        v1 <- implicitly[Parseable[V]].parse(wV1)
        v2 <- implicitly[Parseable[V]].parse(wV2)
    } yield (v1, v2)
}

/**
 * Utility class to help create graphs from edge lists, etc.
 * The edges of this class support Ordering.
 */
case class UndirectedGraphBuilder[V: Ordering : Parseable, E: Parseable, P]() extends GraphBuilder[V, E] {

    def createGraphFromEdges[X <: Edge[V, E]](graph: Graph[V, E, X, P])(esy: Try[Iterable[X]]): Try[Graph[V, E, X, P]] =
        esy map {
            es => es.foldLeft(graph)((g, e) => g.addEdge(e))
        }
}
