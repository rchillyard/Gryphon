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
        val visitor: Visitor[V, Queue[V]] = Visitor.createPostQueue[V]
        val z: Visitor[V, Queue[V]] = g.dfs(visitor)(v)
        val mv1: VertexMap[V, Xin, VertexPair[V]] = g.vertexMap
        val mv2: VertexMap[V, Xout, Unit] =
            g.vertices.foldLeft[VertexMap[V, Xout, Unit]](UnorderedVertexMap.empty[V, Xout, Unit]) {
                (mv, v) => constructVertexMapFromOriginal(mv1, mv, v)
            }
        val t: Tree[V, Unit, Xout, Unit] = treeGenerator("DFS Tree", mv2)
        // TODO what do we do with qq?
        val xos: Iterator[Option[VertexPair[V]]] = z.journal.iterator map {
            v =>
                for {
                    q <- mv1.asInstanceOf[AbstractVertexMap[V, Xin, VertexPair[V]]].vertexMap.get(v)
                    property = q.getProperty
                    x <- property
                } yield x
        }

        val result = xos.flatten.foldLeft(t) {
            case (u, pair) => u.addEdge(createEdge(pair)).asInstanceOf[Tree[V, Unit, Xout, Unit]]
        }

        TreeDFS[V, Unit, Xout, Unit](result)
    }

    private def constructVertexMapFromOriginal(original: VertexMap[V, Xin, VertexPair[V]], vertexMap: VertexMap[V, Xout, Unit], v: V) = {
        val ao: Option[Vertex[V, Xin, VertexPair[V]]] = original.get(v)
        val bo: Option[VertexPair[V]] = ao.flatMap {
            vertex => vertex.getProperty
        }
        val maybeProperty: Option[VertexPair[V]] = bo
        val result: VertexMap[V, Xout, Unit] = vertexMap.addVertex(v)
        val co: Option[Vertex[V, Xout, Unit]] = result.get(v)
        // TODO this is where we actually construct an edge of type Xout
//        co.foreach(_.setProperty(maybeProperty))
        result
    }

    private def treeGenerator(label: String, vertexMap: VertexMap[V, Xout, Unit]): Tree[V, Unit, Xout, Unit] = DirectedTreeCase[V, Unit, Xout, Unit](label, vertexMap)
}