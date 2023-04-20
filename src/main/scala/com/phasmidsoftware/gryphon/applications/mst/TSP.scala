package com.phasmidsoftware.gryphon.applications.mst

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
    def createEdgeFromVertices[V: Ordering, E: Ordering](v1: V, v2: V)(implicit d: (V, V) => E): UndirectedOrderedEdge[V, E] =
        UndirectedOrderedEdgeCase(v1, v2, d(v1, v2))
}