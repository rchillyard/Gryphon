package com.phasmidsoftware.gryphon.adjunct

import com.phasmidsoftware.gryphon.core.*

/**
 * Represents a directed graph structure that supports operations on vertexMap and edges. 
 * This class is a concrete implementation of the `EdgeGraph` trait, and models directed 
 * relationships between vertices with edges.
 *
 * @tparam V the type of attributes for the vertexMap in the graph (invariant).
 * @tparam E the type of attributes for the edges in the graph (invariant).
 * @param vertexMap the `VertexMap` that represents this `Graph`.
 */
case class DirectedGraph[V, E](vertexMap: VertexMap[V]) extends AbstractGraph[V](vertexMap) with EdgeGraph[V, E] {

  /**
   * Adds an edge to the directed graph, creating or updating the vertices and their adjacencies
   * accordingly. The edge is connected between its `from` and `to` vertices.
   *
   * @param edge the edge to be added to the graph. It contains a starting vertex (`from`),
   *             an ending vertex (`to`), and an associated attribute that defines the edge.
   * @return a new instance of the graph that includes the specified edge and updated vertices.
   */
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

  /**
   * Creates a new directed graph using the provided vertex map.
   *
   * @param vertexMap the vertex map to be used for constructing the graph. It defines the vertices
   *                  and their adjacencies in the graph.
   * @return a new directed graph constructed with the given vertex map.
   */
  def unit(vertexMap: VertexMap[V]): Graph[V] = DirectedGraph(vertexMap)
}

/**
 * Factory object for creating instances of `DirectedGraph`.
 * This object provides utility methods to construct a directed graph
 * from an edge list representation.
 */
object DirectedGraph {
  /**
   * Constructs a `DirectedGraph` instance from the provided `EdgeList`.
   * This method converts an `EdgeList` representation into a `DirectedGraph`,
   * effectively mapping vertex attributes and their corresponding connections.
   *
   * @param edgeList the list of edges representing the graph structure, 
   *                 where each edge connects a source vertex to a target vertex.
   * @tparam V the type of the vertex attributes.
   * @tparam E the type of the edge attributes.
   * @return a newly constructed `DirectedGraph` containing the vertices
   *         and edges derived from the given `edgeList`.
   */
  def apply[V, E](edgeList: EdgeList[V, E]): DirectedGraph[V, E] =
    DirectedGraph(VertexMap.create(edgeList))

}
