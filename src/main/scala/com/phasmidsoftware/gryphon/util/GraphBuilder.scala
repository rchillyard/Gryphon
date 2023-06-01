/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.util

import com.phasmidsoftware.gryphon.core._
import java.net.URL
import scala.io.Source
import scala.util.{Failure, Success, Try}

abstract class GraphBuilder[V: Parseable, E: Parseable] {

    def createEdgeListTriple[X <: Edge[V, E]](uy: Try[URL])(f: (V, V, E) => X): Try[Iterable[X]] = for {
        eys <- createTripleList(uy)
        es <- GraphBuilder.sequence(eys)
    } yield for {
        (v1, v2, e) <- es
    } yield f(v1, v2, e)

    def createEdgeListPair[X <: Edge[V, Unit]](uy: Try[URL])(f: (V, V) => X): Try[Iterable[X]] = for {
        eys <- createPairList(uy)
        es <- GraphBuilder.sequence(eys)
    } yield for {
        (v1, v2) <- es
    } yield f(v1, v2)


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

object GraphBuilder {

    def sequence[X](eys: Iterator[Try[X]]): Try[List[X]] =
        eys.foldLeft(Try(List[X]())) { (xsy, ey) =>
            (xsy, ey) match {
                case (Success(xs), Success(e)) => Success(xs :+ e)
                case _ => Failure(GraphException("GraphBuilder: sequence error"))
            }
        }

}


/**
 * Utility class to help create graphs from edge lists, etc.
 * The edges of this class support Ordering.
 */
case class UndirectedGraphBuilder[V: Ordering : Parseable, E: Parseable, P: HasZero]() extends GraphBuilder[V, E] {

    def createGraphFromEdges[X <: Edge[V, E]](graph: Graph[V, E, X, P])(esy: Try[Iterable[X]]): Try[Graph[V, E, X, P]] =
        esy map {
            es => es.foldLeft(graph)((g, e) => g.addEdge(e))
        }
}
