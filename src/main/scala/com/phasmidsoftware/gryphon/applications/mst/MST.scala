package com.phasmidsoftware.gryphon.applications.mst

import com.phasmidsoftware.gryphon.applications.mst.TSP.createEdgeFromVertices
import com.phasmidsoftware.gryphon.core._
import com.phasmidsoftware.gryphon.util.LazyPriorityQueue

/**
 * Trait to model the behavior of a minimum spanning tree.
 * This works only for undirected graphs.
 *
 * @tparam V the vertex (key) attribute type.
 * @tparam E the edge type.
 */
trait MST[V, E] {

    def mst: Tree[V, E, UndirectedOrderedEdge[V, E], Unit]

    def total(implicit en: Numeric[E]): E
}

/**
 * Abstract class to implement MST[V, E].
 *
 * @tparam V the vertex (key) attribute type.
 *           Requires implicit evidence of type Ordering[V].
 * @tparam E the edge type.
 *           Requires implicit evidence of type Ordering[E].
 */
abstract class BaseMST[V: Ordering, E: Ordering](_mst: Tree[V, E, UndirectedOrderedEdge[V, E], Unit]) extends MST[V, E] {

    def isCyclic: Boolean = _mst.isCyclic

    def isBipartite: Boolean = _mst.isBipartite
}

/**
 * Implementation class which evaluates the MST using Prim's (lazy) algorithm.
 *
 * @tparam V the vertex (key) attribute type.
 *           Requires implicit evidence of type Ordering[V].
 * @tparam E the edge type.
 *           Requires implicit evidence of type Ordering[E].
 */
case class LazyPrimCase[V: Ordering, E: Ordering](mst: Tree[V, E, UndirectedOrderedEdge[V, E], Unit]) extends BaseMST[V, E](mst) {

    /**
     * (abstract) Yield an iterable of edges, of type X.
     *
     * @return an Iterable[X].
     */
    val edges: Iterable[UndirectedEdge[V, E]] = mst.edges
    /**
     * (abstract) The vertex map.
     */
    val vertexMap: VertexMap[V, UndirectedEdge[V, E], Unit] = mst.vertexMap

    /**
     * An attribute.
     *
     * @return the value of the attribute, for example, a weight.
     */
    val attribute: String = "LazyPrim"

    def total(implicit en: Numeric[E]): E = edges.map(_.attribute).sum
}

class LazyPrimHelper[V: Ordering, E: Ordering]() {

    type X = UndirectedOrderedEdge[V, E]
    type M = OrderedVertexMap[V, X, Unit]

    /**
     * Method to calculate the Minimum Spanning Tree of a graph using the LazyPrim algorithm.
     *
     * @param graph the graph whose MST is required.
     * @return the MST for graph.
     */
    def createFromGraph(graph: UndirectedGraph[V, E, X, Unit]): LazyPrimCase[V, E] = {
        /**
         * Method to yield the candidate edges from the given set of vertices.
         * TODO merge this method with candidateEdges in createFromVertices.
         *
         * @param v the most recent vertex to have been added to Prim's tree (or the starting vertex).
         * @param m the current vertex map.
         * @return a set of edges which are candidates to be added to Prim's tree.
         */
        def candidateEdges(v: V, m: M): Iterable[X] =
            graph.vertexMap.adjacentEdgesWithFilter(v)(x => !m.containsOther(v, x))

        LazyPrimCase(TreeCase[V, E, X, Unit](s"MST for graph ${graph.attribute}", doLazyPrim(graph.vertices, candidateEdges)))
    }

    /**
     * Method to calculate the Minimum Spanning Tree of a graph using the LazyPrim algorithm.
     *
     * @param vertices the vertices from which the MST is required
     *                 (potentially all edges between pairs of vertices are considered).
     * @return the MST for graph.
     */
    def createFromVertices(vertices: Iterable[V])(implicit d: (V, V) => E): LazyPrimCase[V, E] = {
        /**
         * Method to yield the candidate edges from the given set of vertices.
         *
         * @param v the most recent vertex to have been added to Prim's tree (or the starting vertex).
         * @param m the current vertex map.
         * @return a set of edges which are candidates to be added to Prim's tree.
         */
        def candidateEdges(v: V, m: M): Iterable[X] =
            for {
                w <- vertices
                vo = implicitly[Ordering[V]]
                eo = implicitly[Ordering[E]]
                x = createEdgeFromVertices(v, w)(vo, eo, d) if !m.containsOther(v, x)
            } yield
                x

        LazyPrimCase(TreeCase[V, E, X, Unit](s"MST for graph from vertices", doLazyPrim(vertices, candidateEdges)))
    }

    private def doLazyPrim(vs: Iterable[V], candidateEdges: (V, M) => Iterable[X]): M = {
        def hasExactlyOneVertexInMap(m: M)(e: X) =
            !m.contains(e.vertices._1) ^ !m.contains(e.vertices._2)

        implicit object UndirectedEdgeOrdering extends Ordering[X] {
            // NOTE that we compare in reverse order.
            def compare(x: X, y: X): Int = OrderedEdge.compare(y, x)
        }

        // NOTE this is an ordinary PriorityQueue which does not support deletion or changing priority of queues.
        // Thus, this algorithm is the lazy version of Prim's algorithm.
        val pq = LazyPriorityQueue[X]

        /**
         * Method to grow the tree according to Prim's algorithm.
         * CONSIDER using bfs for this.
         *
         * @param t a tuple consisting of (the vertex most recently added to the tree, the current VertexMap).
         * @return a tuple of (most recently added vertex, new vertex map).
         */
        def grow(t: (Option[V], M)): (Option[V], M) =
            t match {
                case (Some(v), m) =>
                    candidateEdges(v, m) foreach (w => pq.addOne(w))
                    // NOTE: because this is the lazy version of Prim, we must still check that other vertex of e is not in the vertex map.
                    pq.conditionalDequeue(hasExactlyOneVertexInMap(m)) match {
                        case Some(e) => m.addEdgeWithVertex(e)
                        case None => None -> m
                    }
                case (None, _) => t
            }

        // Starting at an arbitrary vertex of the graph (we pick the head of the vertices list),
        // gradually build up the VertexMap of the MST by invoking grow V-1 times where V is the number of vertices.
        val (_, vertexMapResult) = vs.headOption match {
            case Some(v) =>
                val start: (Option[V], M) = Some(v) -> OrderedVertexMap[V, X, Unit](v).asInstanceOf[M]
                Range(0, vs.size).foldLeft(start) { (m, _) => grow(m) }
            case None => throw GraphException("doLazyPrim: empty vertex list")
        }

        vertexMapResult
    }
}