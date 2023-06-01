/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.util

import com.phasmidsoftware.gryphon.core._
import com.phasmidsoftware.util.FP.resource
import java.net.URL
import scala.util.{Failure, Success, Try}

/**
 * Utility class to help create graphs from edge lists, etc.
 * The edges of this class support Ordering.
 */
class OrderedGraphBuilder[V: Ordering : Parseable, E: Ordering : Parseable, P: HasZero]() extends GraphBuilder[V, E] {

    private type G = Graph[V, E, UndirectedOrderedEdge[V, E], P]

    def createEdgeList[X <: Edge[V, E]](uy: Try[URL])(f: (V, V, E) => X): Try[Iterable[X]] = for {
        eys <- createTripleList(uy)
        es <- GraphBuilder.sequence(eys)
    } yield for {
        (v1, v2, e) <- es
    } yield f(v1, v2, e)

    def createGraphFromUndirectedOrderedEdges(esy: Try[Iterable[UndirectedOrderedEdge[V, E]]]): Try[Graph[V, E, UndirectedOrderedEdge[V, E], P]] =
        esy map {
            es =>
                // CONSIDER avoiding the two asInstanceOf calls
                val graph: G = UndirectedGraph[V, E, P]("no title").asInstanceOf[G]
                es.foldLeft(graph)((g, e) => g.addEdge(e))
        }
}

object PrimDemo extends App {

    private val resourceName = "/prim.graph"
    private val uy = resource(resourceName)
    private val gy = new OrderedGraphBuilder[Int, Double, Unit]().createEdgeList(uy)(UndirectedOrderedEdgeCase(_, _, _))
    gy match {
        case Success(g) =>
            println(s"read ${g.size} edges from $resourceName")
            println(g)
        case Failure(x) => throw x
    }
}