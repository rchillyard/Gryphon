package com.phasmidsoftware.gryphon.util

import com.phasmidsoftware.gryphon.core._
import com.phasmidsoftware.util.FP.resource
import java.net.URL
import scala.io.Source
import scala.util.{Failure, Success, Try}

/**
 * Utility class to help create graphs from edge lists, etc.
 */
class GraphBuilder[V: Ordering : Parseable, E: Ordering : Parseable, P: HasZero]() {

    private type G = Graph[V, E, UndirectedOrderedEdge[V, E], P]

    def createUndirectedEdgeList(uy: Try[URL]): Try[Iterable[UndirectedOrderedEdge[V, E]]] = {
        val eysy: Try[Iterator[Try[(V, V, E)]]] = for {
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

        for {
            eys <- eysy
            es <- sequence(eys)
        } yield for {
            (v1, v2, e) <- es
            edge = UndirectedOrderedEdgeCase(v1, v2, e)
        } yield edge
    }

    def createGraphFromUndirectedOrderedEdges(esy: Try[Iterable[UndirectedOrderedEdge[V, E]]]): Try[Graph[V, E, UndirectedOrderedEdge[V, E], P]] =
        esy map {
            es =>
                // CONSIDER avoiding the two asInstanceOf calls
                val graph: G = UndirectedGraph[V, E, P]("no title").asInstanceOf[G]
                es.foldLeft(graph)((g, e) => g.addEdge(e))
        }

    private def sequence(eys: Iterator[Try[(V, V, E)]]): Try[List[(V, V, E)]] =
        eys.foldLeft(Try(List[(V, V, E)]())) { (xsy, ey) =>
            (xsy, ey) match {
                case (Success(xs), Success(e)) => Success(xs :+ e)
                case _ => Failure(GraphException("GraphBuilder: sequence error"))
            }
        }
}

object GraphBuilder {

}

object PrimDemo extends App {

    private val resourceName = "/prim.graph"
    private val uy = resource(resourceName)
    private val gy = new GraphBuilder[Int, Double, Unit]().createUndirectedEdgeList(uy)
    gy match {
        case Success(g) =>
            println(s"read ${g.size} edges from $resourceName")
            println(g)
        case Failure(x) => throw x
    }
}