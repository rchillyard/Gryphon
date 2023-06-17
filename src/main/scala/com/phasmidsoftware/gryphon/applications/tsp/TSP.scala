/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.applications.tsp

import com.phasmidsoftware.gryphon.core._

/**
 * Object to help with the Traveling Salesman Problem.
 */
object TSP {

    /**
     * Method to create an edge from two vertices.
     *
     * @param v1 one vertex.
     * @param v2 the other vertex.
     * @param d  a function to get an E from a pair of Vs (usually this will be the distance).
     * @tparam V the Vertex attribute type.
     * @tparam E the Edge attribute type.
     * @return a Try of Iterable of UndirectedOrderedEdge.
     */
    def createEdgeFromVertices[V: Ordering, E: Ordering, X <: UndirectedOrderedEdge[V, E]](v1: V, v2: V)(implicit d: (V, V) => E): X =
        UndirectedOrderedEdgeCase(v1, v2, d(v1, v2)).asInstanceOf[X]
}