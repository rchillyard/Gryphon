package littlegryphon.adjunct

import littlegryphon.core.*

/**
 * Represents a directed graph structure that supports operations on vertexMap and edges. 
 * This class is a concrete implementation of the `EdgeGraph` trait, and models directed 
 * relationships between vertices with edges.
 *
 * @tparam V the type of attributes for the vertexMap in the graph (invariant).
 * @tparam E the type of attributes for the edges in the graph (invariant).
 * @param vertexMap the `VertexMap` that represents this `Graph`.
 */
case class DirectedGraph[V, E](vertexMap: VertexMap[V]) extends BaseGraph[V](vertexMap) with EdgeGraph[V, E] {

  def addEdge(edge: Edge[E, V]): EdgeGraph[V, E] = {
    val from: Vertex[V] = edge.from
    val to: Vertex[V] = edge.to
    val z: Vertex[V] = from + AdjacencyEdge(edge)
    copy(vertexMap + z)
  }

  /**
   * Retrieves all edges in the directed graph as an iterable collection of edges.
   *
   * @return an iterable collection containing all edges of type `Edge[E, V]` in the graph.
   */
  def edges: Iterable[DirectedEdge[E, V]] =
    (adjacencies map {
      case AdjacencyEdge(e: DirectedEdge[E, V], false) => e
    }
      ).toSeq

  /**
   * Retrieves an iterator of all adjacencies from the vertexMap in the graph.
   * Each vertex's adjacencies are iterated over, yielding a sequence of adjacency objects.
   *
   * @return an iterator over adjacencies of type `Adjacency[V]` from the vertexMap in the graph.
   */
  def adjacencies: Iterator[Adjacency[V]] = for {
    vv <- vertexMap.vertices.iterator
    va <- vv.adjacencies.iterator
  } yield va

  def unit(vertexMap: VertexMap[V]): Graph[V] = DirectedGraph(vertexMap)
}

object DirectedGraph {
  def apply[V, E](edgeList: EdgeList[V, E]): DirectedGraph[V, E] = {
    val vvVm: Map[V, Vertex[V]] = edgeList.edges.foldLeft[Map[V, Vertex[V]]](Map.empty[V, Vertex[V]]) {
      (vm, e) => Graph.addVertexToMap(vm)(e.from)
    }
    DirectedGraph(VertexMap(vvVm))
  }

}
