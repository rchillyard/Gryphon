/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.util

import com.phasmidsoftware.gryphon.core.{GraphException, Parseable}
import java.net.URL
import scala.io.Source
import scala.util.{Failure, Success, Try}

abstract class GraphBuilder[V: Parseable, E: Parseable] {

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