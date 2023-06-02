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
    }
}

class DFSHelper[V, Xin <: Edge[V, Unit], Xout <: DirectedEdge[V, E], E](f: (V, V) => E) {

    /**
     * Method to yield the DFS tree for given <code>graph</code> starting at the given vertex <code>v</code>.
     *
     * @param g the graph to be traversed (Graph[V, E, Y, Unit]).
     * @param v the starting vertex (V).
     * @return a TreeDFS[V, E, X].
     */
    def dfsTree(g: Graph[V, Unit, Xin, Unit], v: V): TreeDFS[V, E, Xout, VertexProp] = {
        implicit val vj: IterableJournalQueue[V] = new IterableJournalQueue[V] {}
        val visitor: Visitor[V, Queue[V]] = Visitor.createPostQueue[V]
        val z: Visitor[V, Queue[V]] = g.dfs(visitor)(v)
        val map: HashMap[V, Vertex[V, Xout, VertexProp]] = HashMap()
        val mv: UnorderedVertexMap[V, Xout, VertexProp] = UnorderedVertexMapCase[V, Xout, VertexProp](map)
        val t: Tree[V, E, Xout, VertexProp] = treeGenerator("DFS Tree", mv)
        // TODO why are we getting nothing from the iterator on the journal?
        val es: Iterator[DirectedEdgeCase[V, E]] = for {
            v <- z.journal.iterator
            vv <- mv.asInstanceOf[AbstractVertexMap[V, Xout, VertexProp]].vertexMap.get(v).toSeq
            p = vv.getProperty
            z <- p.maybeEdge.toSeq
            x = z.asInstanceOf[Xout]
            _ = t.addEdge(x)
        } yield DirectedEdgeCase[V, E](v, v, f(v, v))

        TreeDFS[V, E, Xout, VertexProp](t)
    }

    def treeGenerator(label: String, vertexMap: VertexMap[V, Xout, VertexProp]): Tree[V, E, Xout, VertexProp] = DirectedTreeCase[V, E, Xout, VertexProp](label, vertexMap)
}