/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.java

import com.phasmidsoftware.gryphon.oldcore._
import com.phasmidsoftware.gryphon.parse.Parseable
import com.phasmidsoftware.gryphon.util.Util.{optionToTry, tryNonNull}
import com.phasmidsoftware.util.FP.{resource, tryToOption}
import java.util.{Optional, function}
import scala.compat.java8.OptionConverters.RichOptionForJava8
import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters.RichOptional
import scala.util.Try

/**
 * This GraphBuilder is intended to be called from Java.
 */
case class OrderedGraphBuilderJava[V: Ordering, E: Ordering, P](gb: com.phasmidsoftware.gryphon.util.GraphBuilder[V, E, (V, V)]) {

    def createUndirectedEdgeList(u: String): Optional[java.util.List[UndirectedEdge[V, E]]] = {
        val z: Try[Iterable[UndirectedEdge[V, E]]] = gb.createEdgeListTriple(resource(u))(UndirectedEdgeCase(_, _, _))
        tryToOption(x => x.printStackTrace(System.err))(z).map(_.toSeq.asJava).asJava
    }

    def createGraphFromUndirectedEdgeList(eso: Optional[java.util.List[UndirectedEdge[V, E]]]): Optional[Graph[V, E, UndirectedEdge[V, E], (V, V)]] = {
        val ely = optionToTry(eso.toScala, GraphException(s"OrderedGraphBuilderJava.createGraphFromUndirectedEdgeList: cannot get edge list: $eso"))
        val esy = ely map (el => el.asScala.toSeq)
        val graph: Graph[V, E, UndirectedEdge[V, E], (V, V)] = UndirectedGraph.createUnordered[V, E, UndirectedEdge[V, E], (V, V)]("no title")
        gb.createGraphFromEdges[UndirectedEdge[V, E]](graph)(esy).toOption.asJava
    }
}

object OrderedGraphBuilderJava {
    def create[V <: Comparable[V], E <: Comparable[E]](fV: java.util.function.Function[String, V], fE: java.util.function.Function[String, E]): OrderedGraphBuilderJava[V, E, Unit] = {
        implicit object ParseableV extends Parseable[V] {
            def parse(w: String): Try[V] = parseString(fV, "V")(w)
        }
        implicit object ParseableE extends Parseable[E] {
            def parse(w: String): Try[E] = parseString(fE, "E")(w)
        }
        OrderedGraphBuilderJava(new com.phasmidsoftware.gryphon.util.GraphBuilder[V, E, (V, V)])
    }

    private def parseString[T](f: function.Function[String, T], genericType: String)(w: String): Try[T] =
        tryNonNull(f(w), GraphException(s"Java GraphBuilder.apply: cannot parse $w as a " + genericType))
}