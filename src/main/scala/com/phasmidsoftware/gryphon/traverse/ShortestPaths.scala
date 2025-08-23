package com.phasmidsoftware.gryphon.traverse

import com.phasmidsoftware.gryphon.adjunct.{AttributedDirectedEdge, DirectedEdge}
import com.phasmidsoftware.gryphon.core
import com.phasmidsoftware.gryphon.core.*
import com.phasmidsoftware.visitor.*

/**
 * Provides utilities for computing shortest paths within a graph structure. The objects and methods
 * in this singleton focus on graph traversal and shortest path calculation, leveraging Dijkstra's
 * algorithm and related concepts.
 *
 * CONSIDER re-writing this as a class that extends Traversal. Then we could create type aliases for the whole class (DRV, etc.)
 */
object ShortestPaths {

  /**
   * Computes the shortest paths from a starting vertex to all other vertices in the given traversable graph
   * using Dijkstra's algorithm.
   *
   * @param traversable the graph representation to be traversed, which must support operations related
   *                    to discoverable and relaxable vertices.
   * @param start       the starting vertex from which the shortest paths are computed.
   * @tparam V the type of the vertices in the graph.
   * @tparam E the type of the edge weights in the graph. This must support numeric operations.
   * @return a traversal containing the shortest paths from the starting vertex to all reachable vertices,
   *         represented as directed edges.
   * @throws IllegalArgumentException if the starting vertex is missing or invalid, or if the journal
   *                                  required for path traversal is not found.
   */
  def dijkstra[V, E: Numeric](traversable: core.Traversable[V], start: V): Traversal[V, DirectedEdge[E, V]] = {
    type DRV = DiscoverableRelaxableVertex[V, E]
    type Edge = DirectedEdge[E, V]
    // NOTE: in Dijkstra, we find the shortest paths to ALL vertices so the goal function always yields false
    val goal: DRV => Boolean = _ => false
    val message = Pre
    val visitor = BfsPQVisitorMapped(MinPQ.empty[DRV], Map(message -> MapJournal.empty[DRV, Option[Edge]]), makeEdge, undiscoveredEdges(traversable), goal)
    traversable.get(start) match {
      case Some(drv: DRV) =>
        val (visited, _): (BfsPQVisitorMapped[DRV, Edge], Option[DRV]) = visitor.bfs(drv)
        visited.mapAppendables.get(message) match {
          case Some(journal: AbstractMapJournal[DRV, Option[Edge]]) =>
            val z: Iterable[(V, Edge)] = for {
              (vd, q) <- journal.entries
              p <- q
            } yield vd.attribute -> p
            VertexTraversal(z.toMap)
          case None =>
            throw new IllegalArgumentException("ShortestPaths.dijkstra: missing journal")
        }
      case _ =>
        throw new IllegalArgumentException(s"ShortestPaths.dijkstra: missing or invalid start vertex: $start")
    }
  }

  /**
   * Identifies and returns edges in a traversable graph that are not yet discovered,
   * starting from a given discoverable and relaxable vertex.
   *
   * @param traversable the graph structure that provides access to vertices
   *                    and their adjacencies, enabling traversal and filtering operations.
   * @param vd          a `DiscoverableRelaxableVertex` representing the current vertex,
   *                    which contains the vertex's attribute, its relaxed distance, and discovery status.
   * @tparam V the type of the vertices in the graph.
   * @tparam E the type of the edge weights in the graph. Must support numeric operations.
   * @return a sequence of `DiscoverableRelaxableVertex` objects that represents the vertices
   *         reachable via undiscovered edges from the current vertex, with their states updated appropriately.
   * @throws IllegalArgumentException if the graph contains invalid or missing vertex data, or if an edge
   *                                  does not conform to the expected structure for the operation to succeed.
   */
  def undiscoveredEdges[V, E: Numeric](traversable: core.Traversable[V])(vd: DiscoverableRelaxableVertex[V, E]): Seq[DiscoverableRelaxableVertex[V, E]] = {
    val en = implicitly[Numeric[E]]
    val vertex = vd.attribute
    val distance = vd.r
    val iterator: Iterator[Adjacency[V]] = traversable.filteredAdjacencies(a => !a.discovered)(vertex)
    val result: Iterator[Option[Edge[_, V]]] = iterator map { a => a.maybeEdge }
    val xs: Iterator[Edge[E, V]] = result.flatten.asInstanceOf[Iterator[Edge[E, V]]]
    val ys: Iterator[DiscoverableRelaxableVertex[V, E]] = xs map {
      case AttributedDirectedEdge(e, _, to) =>
        traversable.get(to) match {
          case Some(d@DiscoverableVertex[V] (v1, _) ) =>
        DiscoverableRelaxableVertex (d)
          case Some(d@DiscoverableRelaxableVertex[V, E] (v1, _, r) ) =>
        d.relax (en.plus (e, distance) )
          case Some(x) =>
            throw new IllegalArgumentException(s"ShortestPaths.undiscoveredEdges: incorrect DiscoverableRelaxableVertex: ${x.getClass}")
          case None =>
            throw new IllegalArgumentException(s"ShortestPaths.undiscoveredEdges: missing DiscoverableRelaxableVertex")
        }
      case _ =>
        throw new IllegalArgumentException("ShortestPaths.undiscoveredEdges: unexpected Edge")
    }
    ys.toSeq
  }

  /**
   * Constructs a directed edge between the specified vertices if a valid adjacency and associated edge exist.
   *
   * @param vo an optional instance of `DiscoverableRelaxableVertex`, representing the source vertex.
   *           If `None`, the method returns `None`.
   * @param v  a `DiscoverableRelaxableVertex` representing the destination vertex.
   * @return an `Option[DirectedEdge[E, V]]` containing the constructed directed edge if a valid
   *         adjacency and edge are found; otherwise, `None`.
   */
  private def makeEdge[V, E](vo: Option[DiscoverableRelaxableVertex[V, E]])(v: DiscoverableRelaxableVertex[V, E]): Option[DirectedEdge[E, V]] = vo match {
    case Some(drv) =>
//      println(s"makeEdge: drv = $drv, v = $v, r = ${v.r}, attribute = ${v.attribute}, drv.r = ${drv.r}, drv.attribute = ${drv.attribute}")
      // CONSIDER this might not be unique in which case how do we know we got the correct one?
      val adj: Option[Adjacency[V]] = drv.adjacencies.find(a => a.vertex == v.attribute)
      for {
        a <- adj
        e <- a.maybeEdge[E]
      } yield AttributedDirectedEdge(e.attribute, drv.attribute, v.attribute)
    case None =>
      None
  }
}
