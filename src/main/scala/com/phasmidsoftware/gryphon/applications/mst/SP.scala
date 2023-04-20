package com.phasmidsoftware.gryphon.applications.mst

import com.phasmidsoftware.gryphon.core.Edge

trait SP[V, E, X <: Edge[V, E]] {

    def isReachable(v: V): Boolean

    def shortestPath(v: V): Seq[X]
}

abstract class BaseSP[V, E, X <: Edge[V, E]]() extends SP[V, E, X] {


}