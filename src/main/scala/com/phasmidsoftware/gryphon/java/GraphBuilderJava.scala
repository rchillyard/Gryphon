package com.phasmidsoftware.gryphon.java

import com.phasmidsoftware.gryphon.core._
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
case class GraphBuilderJava[V: Ordering, E: Ordering, P: HasZero](gb: com.phasmidsoftware.gryphon.util.GraphBuilder[V, E, Unit]) {

    def createUndirectedEdgeList(u: String): Optional[java.util.List[UndirectedOrderedEdge[V, E]]] =
        tryToOption(x => x.printStackTrace(System.err))(gb.createUndirectedEdgeList(resource(u))).map(_.toSeq.asJava).asJava

    def createGraphFromUndirectedEdgeList(eso: Optional[java.util.List[UndirectedOrderedEdge[V, E]]]): Optional[Graph[V, E, UndirectedOrderedEdge[V, E], Unit]] = {
        val ely = optionToTry(eso.toScala, GraphException(s"GraphBuilderJava.createGraphFromUndirectedEdgeList: cannot get edge list: $eso"))
        val esy = ely map (el => el.asScala)
        gb.createGraphFromUndirectedOrderedEdges(esy).toOption.asJava
    }
}

object GraphBuilderJava {
    def create[V <: Comparable[V], E <: Comparable[E]](fV: java.util.function.Function[String, V], fE: java.util.function.Function[String, E]): GraphBuilderJava[V, E, Unit] = {
        implicit object ParseableV extends Parseable[V] {
            def parse(w: String): Try[V] = parseString(fV, "V")(w)
        }
        implicit object ParseableE extends Parseable[E] {
            def parse(w: String): Try[E] = parseString(fE, "E")(w)
        }
        GraphBuilderJava(new com.phasmidsoftware.gryphon.util.GraphBuilder[V, E, Unit])
    }

    private def parseString[T](f: function.Function[String, T], genericType: String)(w: String): Try[T] =
        tryNonNull(f(w), GraphException(s"Java GraphBuilder.apply: cannot parse $w as a " + genericType))
}