/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.applications.bfs

import com.phasmidsoftware.gryphon.oldcore.{DirectedOrderedEdge, Graph}

case class Dijkstra[V, E, X <: DirectedOrderedEdge[V, E]](graph: Graph[V, E, X, Double])(start: V) extends BaseSP[V, E, X](graph)(start: V) {

  def isReachable(v: V): Boolean = reachable.contains(v)

  def shortestPath(v: V): Seq[X] = ??? // TODO implement me!
}
