/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.core

/**
 * Trait to model the behavior of a Tree.
 *

/**
 * Trait to define a Tree.
 *
 * @tparam V the (key) vertex-attribute type.
 * @tparam E the edge-attribute type.
 * @tparam X the type of edge which connects two vertices. A sub-type of Edge[V,E].
 * @tparam P the property type (a mutable property currently only supported by the Vertex type).
 */
 */
trait Tree[V, E, X <: Edge[V, E], P] extends Graph[V, E, X, P] {
    override def isCyclic: Boolean = false // TODO we should be able to assert this

    override def isBipartite: Boolean = true
}


/**
 * Trait to define an undirected Tree.
 *
 * @tparam V the (key) vertex-attribute type.
 * @tparam E the edge-attribute type.
 * @tparam X the type of edge which connects two vertices. A sub-type of UndirectedEdge[V,E].
 * @tparam P the property type (a mutable property currently only supported by the Vertex type).
 */
trait UndirectedTree[V, E, X <: UndirectedEdge[V, E], P] extends Tree[V, E, X, P]

/**
 * Trait to define a directed Tree.
 *
 * @tparam V the (key) vertex-attribute type.
 * @tparam E the edge-attribute type.
 * @tparam X the type of edge which connects two vertices. A sub-type of DirectedEdge[V,E].
 * @tparam P the property type (a mutable property currently only supported by the Vertex type).
 */
trait DirectedTree[V, E, X <: DirectedEdge[V, E], P] extends Tree[V, E, X, P]

/**
 * Case class to represent an undirected tree.
 *
 * @param description the description of this tree.
 * @param vertexMap   the vertex map to define this tree.
 * @tparam V the (key) vertex-attribute type.
 * @tparam E the edge-attribute type.
 *
 */
case class UndirectedTreeCase[V, E, X <: UndirectedEdge[V, E], P](description: String, vertexMap: VertexMap[V, X, P]) extends AbstractUndirectedGraph[V, E, X, P](description, vertexMap) with UndirectedTree[V, E, X, P] {

    /**
     * Method to createUndirectedOrderedGraph a new AbstractGraph from a given vertex map.
     *
     * CONSIDER add an attribute parameter.
     *
     * @param vertexMap the vertex map.
     * @return a new AbstractGraph[V, E].
     */
    def unit(vertexMap: VertexMap[V, X, P]): AbstractGraph[V, E, X, P] = UndirectedTreeCase("no description", vertexMap)

}

/**
 * Case class to represent a directed tree.
 *
 * @param description the description of this tree.
 * @param vertexMap   the vertex map to define this tree.
 * @tparam V the (key) vertex-attribute type.
 * @tparam E the edge-attribute type.
 *
 */
case class DirectedTreeCase[V, E, X <: DirectedEdge[V, E], P](description: String, vertexMap: VertexMap[V, X, P]) extends AbstractDirectedGraph[V, E, X, P](description, vertexMap) with Tree[V, E, X, P] {

    /**
     * Method to createUndirectedOrderedGraph a new AbstractGraph from a given vertex map.
     *
     * CONSIDER add an attribute parameter.
     *
     * @param vertexMap the vertex map.
     * @return a new AbstractGraph[V, E].
     */
    def unit(vertexMap: VertexMap[V, X, P]): AbstractGraph[V, E, X, P] = DirectedTreeCase("no description", vertexMap)

}