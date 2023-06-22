/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.applications.sp

import com.phasmidsoftware.gryphon.core.{Edge, Graph, VertexMap}
import scala.collection.mutable
import scala.collection.mutable.PriorityQueue

trait SP[V, E, X <: Edge[V, E]] {

    def isReachable(v: V): Boolean

    def shortestPath(v: V): Seq[X]
}

abstract class BaseSP[V, E, X <: Edge[V, E]](graph: Graph[V, E, X, Double])(start: V) extends SP[V, E, X] {

    val vertexMap: VertexMap[V, X, Double] = graph.vertexMap

    implicit val ordering: Ordering[V] = (x: V, y: V) => (for {
        xCost <- vertexMap.get(x).flatMap(vertex => vertex.getProperty)
        yCost <- vertexMap.get(y).flatMap(vertex => vertex.getProperty)
    } yield xCost.compareTo(yCost)).getOrElse(0)

    val pq: mutable.PriorityQueue[V] = new PriorityQueue[V]()

//    graph.bfsMutable()

//    def relax(x: X)

}