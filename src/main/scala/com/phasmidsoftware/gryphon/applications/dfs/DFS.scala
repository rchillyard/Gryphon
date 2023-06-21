/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.applications.dfs

import com.phasmidsoftware.gryphon.core._
import com.phasmidsoftware.gryphon.visit.{IterableJournalQueue, Visitor}
import scala.collection.immutable.Queue

/**
 * Trait to model the behavior of a depth-first search.
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
 * @tparam E the edge attribute type.
 * @tparam X the edge type.
 * @tparam P the property type.
 */
abstract class BaseDFS[V, E, X <: Edge[V, E], P](_tree: Tree[V, E, X, P]) extends DFS[V, E, X, P] {

    def isCyclic: Boolean = _tree.isCyclic

    def isBipartite: Boolean = _tree.isBipartite
}

/**
 * Concrete implementation of DFS that results in a Tree of edges emanating from a particular start point.
 *
 * @param tree the directed-edge tree.
 * @tparam V the vertex (key) attribute type.
 * @tparam E the edge attribute type.
 * @tparam X the edge type.
 * @tparam P the property type.
 */
case class TreeDFS[V, E, X <: Edge[V, E], P](tree: Tree[V, E, X, P]) extends BaseDFS[V, E, X, P](tree)

class DFSHelper[V, Xin <: Edge[V, Unit], Xout <: DirectedEdge[V, Unit]] {

    /**
     * Method to yield the DFS tree for given <code>graph</code> starting at the given vertex <code>v</code>.
     *
     * @param gin the graph to be traversed (Graph[V, E, Y, Unit]).
     * @param v   the starting vertex (V).
     * @return a TreeDFS[V, E, X].
     */
    def dfsTree(gin: Graph[V, Unit, Xin, VertexPair[V]], v: V)(createEdge: VertexPair[V] => Xout): TreeDFS[V, Unit, Xout, Unit] = {
        implicit val vj: IterableJournalQueue[V] = new IterableJournalQueue[V] {}
        val visited: Visitor[V, Queue[V]] = gin.dfs(Visitor.createPostQueue[V])(v)
        val mv1: VertexMap[V, Xin, VertexPair[V]] = gin.vertexMap
        val mv2: VertexMap[V, Xout, Unit] = mv1.copyVertices(UnorderedVertexMap.empty[V, Xout, Unit])
        val function: V => Option[VertexPair[V]] = mv1.processVertexProperty[VertexPair[V]](vv => if (vv.vertices._2 == v) vv else vv.invert)
        val vvos: Iterator[Option[VertexPair[V]]] = visited.journal.iterator map function
        val gout: Graph[V, Unit, Xout, Unit] = vvos.flatten.foldLeft(treeGenerator("DFS Tree", mv2)) {
            case (u, pair) => u.addEdge(createEdge(pair))
        }
        gout match {
            case t: Tree[V, Unit, Xout, Unit] => TreeDFS[V, Unit, Xout, Unit](t)
            case _ => throw GraphException("DFSHelper.dfsTree: logic error")
        }
    }

    private def treeGenerator(label: String, vertexMap: VertexMap[V, Xout, Unit]): Graph[V, Unit, Xout, Unit] = DirectedTreeCase[V, Unit, Xout, Unit](label, vertexMap)
}