package com.phasmidsoftware.gryphon.core

/**
 * Trait to model the behavior of a Tree.
 *
 * @tparam V the (key) vertex-attribute type.
 * @tparam E the edge-attribute type.
 */
trait Tree[V, E, X <: UndirectedEdge[V, E], P] extends UndirectedGraph[V, E, X, P] {
    override def isCyclic: Boolean = false // TODO we should be able to assert this

    override def isBipartite: Boolean = true
}

/**
 * Case class to represent a tree.
 *
 * @param description the description of this tree.
 * @param vertexMap   the vertex map to define this tree.
 * @tparam V the (key) vertex-attribute type.
 * @tparam E the edge-attribute type.
 *
 */
case class TreeCase[V, E, X <: UndirectedEdge[V, E], P: HasZero](description: String, vertexMap: VertexMap[V, X, P]) extends AbstractUndirectedGraph[V, E, X, P](description, vertexMap) with Tree[V, E, X, P] {

    /**
     * Method to create a new AbstractGraph from a given vertex map.
     *
     * CONSIDER add an attribute parameter.
     *
     * @param vertexMap the vertex map.
     * @return a new AbstractGraph[V, E].
     */
    def unit(vertexMap: VertexMap[V, X, P]): AbstractGraph[V, E, X, P] = TreeCase("no description", vertexMap)

}