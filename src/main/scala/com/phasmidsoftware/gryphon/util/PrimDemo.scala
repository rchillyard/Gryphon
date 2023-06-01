/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.util

import com.phasmidsoftware.gryphon.core._
import com.phasmidsoftware.util.FP.resource
import scala.util.{Failure, Success}

object PrimDemo extends App {

    private val resourceName = "/prim.graph"
    private val uy = resource(resourceName)
    private val gy = new UndirectedGraphBuilder[Int, Double, Unit]().createEdgeList(uy)(UndirectedOrderedEdgeCase(_, _, _))
    gy match {
        case Success(g) =>
            println(s"read ${g.size} edges from $resourceName")
            println(g)
        case Failure(x) => throw x
    }
}