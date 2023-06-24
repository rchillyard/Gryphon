/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.applications.sp

import com.phasmidsoftware.gryphon.core.{Edge, Graph, VertexMap}
import com.phasmidsoftware.gryphon.visit.PriorityQueueable.QueueablePriorityQueue
import com.phasmidsoftware.gryphon.visit.{PreVisitorIterable, Visitor}
import scala.collection.immutable.Queue
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

    implicit object z extends QueueablePriorityQueue[V] {
        def compare(x: V, y: V): Int = ordering.compare(x, y)
    }

    implicit object q extends com.phasmidsoftware.gryphon.visit.IterableJournalQueue[V]

    val visitor: PreVisitorIterable[V, Queue[V]] = Visitor.createPreQueue[V]

    lazy val reachable: List[V] = graph.bfsMutable(visitor)(start).journal.iterator.toList

//    def relax(x: X)

}