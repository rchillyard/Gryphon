/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.applications.mst

import com.phasmidsoftware.gryphon.core._
import scala.collection.immutable.{HashMap, Queue}

/**
 * Trait to model the behavior of a minimum spanning tree.
 * This works only for undirected graphs.
 *
 * @tparam V the vertex (key) attribute type.
 * @tparam E the edge type.
 */
trait DFS[V, E, X <: Edge[V, E], P] {

    /**
     * Method to yield the DFS tree for the starting vertex <code>v</code>.
     *
     * @return a Tree.
     */
    val tree: Tree[V, E, X, P]
}

/**
 * Abstract class to implement DFS[V, E].
 *
 * @tparam V the vertex (key) attribute type.
 * @tparam E the edge type.
 */
abstract class BaseDFS[V, E, X <: Edge[V, E], P](_tree: Tree[V, E, X, P]) extends DFS[V, E, X, P] {

    def isCyclic: Boolean = _tree.isCyclic

    def isBipartite: Boolean = _tree.isBipartite
}

case class TreeDFS[V, E, X <: Edge[V, E], P: HasZero](tree: Tree[V, E, X, P]) extends BaseDFS[V, E, X, P](tree)


case class VertexProp(var maybeEdge: Option[Any])

object VertexProp {

    implicit object VertexPropHasZero extends HasZero[VertexProp] {
        def zero: VertexProp = VertexProp(None)

//        def set[X](t: VertexProp, x: X): Unit = {
//            t.maybeEdge = Some(x)
//        }
    }
}

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
    def dfsTree[Y <: Edge[V, E]](g: Graph[V, E, Y, Unit], v: V)(f: (String, VertexMap[V, X, VertexProp]) => Tree[V, E, X, VertexProp]): TreeDFS[V, E, X, VertexProp] = {
        implicit val vj: IterableJournalQueue[V] = new IterableJournalQueue[V] {}
        //        implicit val vj: IterableJournalStack[V] = new IterableJournalStack[V] {}
//        val visitor: Visitor[V, List[V]] = Visitor.preAndPost[V]
val visitor: Visitor[V, Queue[V]] = Visitor.createPostQueue[V]
        val z: Visitor[V, Queue[V]] = g.dfs(visitor)(v)
        val i: Iterator[V] = z.journal.iterator
        i foreach println
        val map: HashMap[V, Vertex[V, X, VertexProp]] = HashMap()
        val mv: UnorderedVertexMap[V, X, VertexProp] = UnorderedVertexMapCase[V, X, VertexProp](map)
        val t: Tree[V, E, X, VertexProp] = f("DFS Tree", mv)
        TreeDFS[V, E, X, VertexProp](t)
    }
}