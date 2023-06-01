/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.applications.mst

import com.phasmidsoftware.gryphon.core.Queueable.QueueableQueue
import com.phasmidsoftware.gryphon.core._
import scala.collection.immutable.{HashMap, Queue}

/**
 * Trait to model the behavior of a minimum spanning tree.
 * This works only for undirected graphs.
 *
 * @tparam V the vertex (key) attribute type.
 * @tparam E the edge type.
 */
trait DFS[V, E, X <: Edge[V, E]] {

    /**
     * Method to yield the DFS tree for the starting vertex <code>v</code>.
     *
     * @return a Tree.
     */
    val tree: Tree[V, E, X, Unit]
}

/**
 * Abstract class to implement DFS[V, E].
 *
 * @tparam V the vertex (key) attribute type.
 * @tparam E the edge type.
 */
abstract class BaseDFS[V, E, X <: Edge[V, E]](_tree: Tree[V, E, X, Unit]) extends DFS[V, E, X] {

    def isCyclic: Boolean = _tree.isCyclic

    def isBipartite: Boolean = _tree.isBipartite
}

case class TreeDFS[V, E, X <: Edge[V, E]](tree: Tree[V, E, X, Unit]) extends BaseDFS[V, E, X](tree)

class DFSHelper[V, X <: Edge[V, E], E]() {

    /**
     * Method to yield the DFS tree for given <code>graph</code> starting at the given vertex <code>v</code>.
     *
     * @param g the graph to be traversed (Graph[V, E, Y, Unit]).
     * @param v the starting vertex (V).
     * @param f a function of type (String,VertexMap[V,X,Unit]) => Tree[V,E,X,Unit] which will form a tree from a String and a VertexMap.
     * @tparam Y the edge type of the given graph.
     * @return a TreeDFS[V, E, X].
     */
    def dfsTree[Y <: Edge[V, E]](g: Graph[V, E, Y, Unit], v: V)(f: (String, VertexMap[V, X, Unit]) => Tree[V, E, X, Unit]): TreeDFS[V, E, X] = {
        implicit val vj: IterableJournalQueue[V] = new IterableJournalQueue[V] {}
        val visitor: PostVisitor[V, Queue[V]] = Visitor.createPost[V]
        val z: Visitor[V, Queue[V]] = g.dfs(visitor)(v)
        val i: Iterator[V] = z.journal.iterator
        i foreach println
        val map: HashMap[V, Vertex[V, X, Unit]] = HashMap()
        val mv: UnorderedVertexMap[V, X, Unit] = UnorderedVertexMapCase[V, X, Unit](map)
        val t: Tree[V, E, X, Unit] = f("DFS Tree", mv)
        TreeDFS[V, E, X](t)
    }
}