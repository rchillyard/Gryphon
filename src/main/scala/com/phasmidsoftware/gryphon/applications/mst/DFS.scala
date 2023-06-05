/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.applications.mst

import com.phasmidsoftware.gryphon.core._
import scala.collection.immutable.Queue

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

case class TreeDFS[V, E, X <: Edge[V, E], P](tree: Tree[V, E, X, P]) extends BaseDFS[V, E, X, P](tree)

class DFSHelper[V, Xin <: Edge[V, Unit], Xout <: DirectedEdge[V, Unit]] {


    /**
     * Method to yield the DFS tree for given <code>graph</code> starting at the given vertex <code>v</code>.
     *
     * @param g the graph to be traversed (Graph[V, E, Y, Unit]).
     * @param v the starting vertex (V).
     * @return a TreeDFS[V, E, X].
     */
    def dfsTree(g: Graph[V, Unit, Xin, VertexPair[V]], v: V)(createEdge: VertexPair[V] => Xout): TreeDFS[V, Unit, Xout, Unit] = {
        implicit val vj: IterableJournalQueue[V] = new IterableJournalQueue[V] {}
        val visited: Visitor[V, Queue[V]] = g.dfs(Visitor.createPostQueue[V])(v)
        val mv1: VertexMap[V, Xin, VertexPair[V]] = g.vertexMap
        val mv2: VertexMap[V, Xout, Unit] =
            g.vertices.foldLeft[VertexMap[V, Xout, Unit]](UnorderedVertexMap.empty[V, Xout, Unit]) {
                (mv, v) => mv.addVertex(v)
            }
        val vvos: Iterator[Option[VertexPair[V]]] = visited.journal.iterator map {
            v =>
                for {
                    vertex <- mv1.asInstanceOf[AbstractVertexMap[V, Xin, VertexPair[V]]].vertexMap.get(v)
                    p <- vertex.getProperty
                    vv = if (p.vertices._2 == v) p else p.invert
                } yield vv
        }

        val result = vvos.flatten.foldLeft(treeGenerator("DFS Tree", mv2)) {
            case (u, pair) => u.addEdge(createEdge(pair)).asInstanceOf[Tree[V, Unit, Xout, Unit]]
        }

        TreeDFS[V, Unit, Xout, Unit](result)
    }

    private def treeGenerator(label: String, vertexMap: VertexMap[V, Xout, Unit]): Tree[V, Unit, Xout, Unit] = DirectedTreeCase[V, Unit, Xout, Unit](label, vertexMap)
}