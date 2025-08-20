package com.phasmidsoftware.gryphon.traverse

import com.phasmidsoftware.gryphon.adjunct.{AttributedDirectedEdge, DirectedEdge, UndirectedEdge}
import com.phasmidsoftware.gryphon.core
import com.phasmidsoftware.gryphon.core.{Adjacency, AdjacencyEdge, Edge, VertexPair}
import com.phasmidsoftware.visitor.{AbstractMapJournal, BfsPQVisitorMapped}

object ShortestPaths {

  def undiscoveredEdges[V, E: Numeric](traversable: core.Traversable[V])(vd: VertexAndDistance[V, E]): Seq[VertexAndDistance[V, E]] = {
    val en = implicitly[Numeric[E]]
    val vertex = vd.vertex
    val distance = vd.distance
    val iterator: Iterator[Adjacency[V]] = traversable.filteredAdjacencies(a => !a.discovered)(vertex)
    val result: Iterator[Option[Edge[_, V]]] = iterator map { a => a.maybeEdge }
    val xs: Iterator[Edge[E, V]] = result.flatten.asInstanceOf[Iterator[Edge[E, V]]]
    val ys: Iterator[VertexAndDistance[V, E]] = xs map {
      case d@AttributedDirectedEdge(e, from, to) =>
        traversable.get(from)
        VertexAndDistance(to, en.plus(e, distance), Some(d))
      case _ =>
        throw new IllegalArgumentException("ShortestPaths.undiscoveredEdges: unexpected Edge")

    }
    ys.toSeq
  }

  def dijkstra[V, E: Numeric](traversable: core.Traversable[V], start: V): Traversal[V, DirectedEdge[E, V]] = {
    // NOTE: in Dijkstra, we find the shortest paths to ALL vertices so the goal function always yields false
    val goal: VertexAndDistance[V, E] => Boolean = _ => false
    val makeEdge: VertexAndDistance[V, E] => DirectedEdge[E, V] = vd => AttributedDirectedEdge(vd.distance, vd.vertex, vd.vertex)
    val visitor: BfsPQVisitorMapped[VertexAndDistance[V, E], DirectedEdge[E, V]] = BfsPQVisitorMapped.createMin(makeEdge, undiscoveredEdges(traversable), goal)
    val (visited, vo): (BfsPQVisitorMapped[VertexAndDistance[V, E], DirectedEdge[E, V]], Option[VertexAndDistance[V, E]]) = visitor.bfs(VertexAndDistance(start, implicitly[Numeric[E]].zero, None))
    val journal: AbstractMapJournal[VertexAndDistance[V, E], DirectedEdge[E, V]] = visited.mapJournals.head
    val z: Iterable[(V, DirectedEdge[E, V])] = for {
      (vd, q) <- journal.entries
    } yield vd.vertex -> q
    VertexTraversal(z.toMap)
  }
}

case class VertexAndDistance[V, E: Numeric](vertex: V, distance: E, edgeFrom: Option[Edge[E, V]]) extends Ordered[VertexAndDistance[V, E]] {
  override def compare(that: VertexAndDistance[V, E]): Int = implicitly[Numeric[E]].compare(distance, that.distance)
}
