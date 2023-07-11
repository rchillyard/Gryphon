/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.core

import com.phasmidsoftware.gryphon.visit.{MutableQueueable, Visitor}

/**
 * Trait to define the behavior of a graph-like structure which can be traversed by dfs, dfsAll, and bfsMutable.
 *
 * @tparam V the underlying key (attribute) type for a vertex.
 */
trait Traversable[V] {

    /**
     * Method to run depth-first-search on this Traversable.
     * Vertices will not be visited if they are not reachable from v.
     *
     * @param visitor the visitor, of type Visitor[V, J].
     * @param v       the starting vertex.
     * @tparam J the journal type.
     * @return a new Visitor[V, J].
     */
    def dfs[J](visitor: Visitor[V, J])(v: V): Visitor[V, J]

    /**
     * Method to run depth-first-search on this Traversable, ensuring that every vertex is visited..
     *
     * @param visitor the visitor, of type Visitor[V, J].
     * @tparam J the journal type.
     * @return a new Visitor[V, J].
     */
    def dfsAll[J](visitor: Visitor[V, J]): Visitor[V, J]

    /**
     * Method to run breadth-first-search with a mutable queue on this Traversable.
     *
     * @param visitor the visitor, of type Visitor[V, J].
     * @param v       the starting vertex.
     * @tparam J the journal type.
     * @tparam Q the type of the mutable queue for navigating this Traversable.
     *           Requires implicit evidence of MutableQueueable[Q, V].
     * @return a new Visitor[V, J].
     */
    def bfsMutable[J, Q](visitor: Visitor[V, J])(v: V)(goal: V => Boolean)(implicit ev: MutableQueueable[Q, V]): Visitor[V, J]
}

/**
 * Trait to define the behavior of a graph-like structure which can be traversed by BFS in search of a goal.
 *
 * @tparam V the underlying key (attribute) type for a vertex.
 * @tparam X the type of edge which connects two vertices. A sub-type of EdgeLike[V].
 * @tparam P the property type (a mutable property currently only supported by the Vertex type).
 */
trait GoalTraversable[V, X <: EdgeLike[V], P] extends Traversable[V] {

    /**
     * Method to run breadth-first-search on this Traversable.
     *
     * @param v    the starting vertex.
     * @param goal the goal function: None means "no decision;" Some(x) means the decision (win/lose) is true/false.
     * @return a new Tree[V, E, X, Double] of shortest paths.
     */
    def bfs(v: V)(goal: V => Option[Boolean]): (Option[Boolean], AcyclicNetwork[V, VertexPair[V], P])
}

/**
 * Trait to define the behavior of a graph-like structure (with edge attributes) which can be traversed by BFS in search of a goal.
 *
 * @tparam V the underlying key (attribute) type for a vertex.
 * @tparam E the edge-attribute type.
 * @tparam X the type of edge which connects two vertices. A sub-type of Edge[V,E].
 * @tparam P the property type (a mutable property currently only supported by the Vertex type).
 */
trait EdgeGoalTraversable[V, E, X <: Edge[V, E], P] extends Traversable[V] {

    /**
     * Method to run breadth-first-search on this Traversable.
     *
     * NOTE in this method name, the F comes before the S. Important ;)
     *
     * @param v    the starting vertex.
     * @param goal the goal function: None means "no decision;" Some(x) means the decision (win/lose) is true/false.
     * @return a new Tree[V, E, X, Double] of shortest paths.
     */
    def bfse(v: V)(goal: V => Option[Boolean]): AcyclicNetwork[V, VertexPair[V], P]
}
