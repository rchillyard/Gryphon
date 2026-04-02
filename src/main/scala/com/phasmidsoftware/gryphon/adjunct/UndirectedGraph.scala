package com.phasmidsoftware.gryphon.adjunct

import com.phasmidsoftware.gryphon.core.*
import com.phasmidsoftware.gryphon.util.GraphException
import scala.util.{Failure, Success, Try}

/**
 * Represents a directed graph structure that supports operations on vertexMap and edges. 
 * This class is a concrete implementation of the `EdgeGraph` trait, and models directed 
 * relationships between vertices with edges.
 *
 * @tparam V the type of attributes for the vertexMap in the graph (invariant).
 * @tparam E the type of attributes for the edges in the graph (invariant).
 * @param vertexMap the `VertexMap` that represents this `Graph`.
 */
case class UndirectedGraph[V, E](vertexMap: VertexMap[V]) extends AbstractGraph[V](vertexMap) with EdgeGraph[V, E] {

  /**
   * Adds an edge to the directed graph, creating or updating the vertices and their adjacencies
   * accordingly. The edge is connected between its `from` and `black` vertices.
   *
   * CONSIDER eliminating this method.
   *
   * @param edge the edge to be added to the graph. It contains a starting vertex (`from`),
   *             an ending vertex (`black`), and an associated attribute that defines the edge.
   *
   * @return a new instance of the graph that includes the specified edge and updated vertices.
   */
  def addEdge(edge: Edge[E, V]): UndirectedGraph[V, E] =
    copy(vertexMap + edge)

  /**
   * Retrieves all edges in the directed graph as an iterable collection of edges.
   *
   * @return an iterable collection containing all edges of type `Edge[E, V]` in the graph.
   */
  def edges: Iterator[UndirectedEdge[E, V]] = {
    adjacencies.collect { case AdjacencyEdge(e: UndirectedEdge[E, V] @unchecked, false) => e }
  }

  override def M: Int = edges.size

  /**
   * Creates a new directed graph using the provided vertex map.
   *
   * @param vertexMap the vertex map to be used for constructing the graph. It defines the vertices
   *                  and their adjacencies in the graph.
   *
   * @return a new directed graph constructed with the given vertex map.
   */
  def unit(vertexMap: VertexMap[V]): Graph[V] =
    DirectedGraph(vertexMap)

  /**
   * A partial function that extracts a `DirectedEdge` from an `Adjacency`, specifically when the `Adjacency`
   * is an `AdjacencyEdge` containing a `DirectedEdge` and is not marked as `discovered`.
   *
   * This function will throw a `GraphException` if the input `Adjacency` does not match the expected pattern,
   * indicating an unsupported or an invalid edge type.
   *
   * @throws GraphException if the input is not an `AdjacencyEdge` with a `DirectedEdge`.
   */
  private val getUndirectedEdgeFromAdjacency: PartialFunction[Adjacency[V], UndirectedEdge[E, V]] = {
    case AdjacencyEdge(e: UndirectedEdge[E, V] @unchecked, false) =>
      e
    case x =>
      throw GraphException(s"unexpected edge type: $x")
  }
}

/**
 * Factory object for creating instances of `DirectedGraph`.
 * This object provides utility methods to construct a directed graph
 * from an edge list representation.
 */
object UndirectedGraph {

  /**
   * Converts a sequence of triplets representing graph edges into a `Try` of `Graph[V]`.
   *
   * Each triplet in the input sequence consists of two vertices (source and target)
   * and an attribute associated with the edge connecting the vertices.
   * This method attempts to construct a directed graph from the provided triplets, returning
   * a `Success` with the graph instance if the operation is successful, or a `Failure`
   * if the construction process encounters an error.
   *
   * @param triples a sequence of triplets `Triplet[V, E]` where:
   *                - the first element is the source vertex of type `V`.
   *                - the second element is the target vertex of type `V`.
   *                - the third element is the edge attribute of type `E`.
   *
   * @tparam V the type of the vertices in the graph.
   * @tparam E the type of the edges in the graph.
   * @return a `Try[Graph[V]]`, where:
   *         - `Success(Graph[V])` contains the constructed graph if the operation is successful.
   *         - `Failure` contains a `GraphException` if the graph construction fails.
   */
  def triplesToTryGraph[V, E](f: V => Vertex[V])(triples: Seq[Triplet[V, E, EdgeType]]): Try[Graph[V]] =
    SerializableGraph.createFromTriplets[V, E, EdgeType](triples) match {
      case triplets: Triplets[V, E, EdgeType] =>
        val vm: VertexMap[V] =
          triplets.triplets.foldLeft(VertexMap[V]) {
            (z, t) =>
              // TODO find another way to handle this anomaly
              if (t._4.oneWay) System.err.println(s"WARNING: edge ${t._3} is directed.")
              z.createVerticesFromTriplet[E, EdgeType](f) {
                        case (vv1, vv2, Some(e)) =>
                          AdjacencyEdge(UndirectedEdge(e, vv1.attribute, vv2.attribute))
                        case (vv1, vv2, None) => // TODO fix this case so that it doesn't use a directed edge.
                          AdjacencyEdge(AttributedDirectedEdge(None, vv1.attribute, vv2.attribute))
                      }(!t._4.oneWay)
                      (t)
          }
        val graph = UndirectedGraph(vm)
        // TODO find another way to handle this anomaly
        val expectedAdjacencies = triplets.triplets.map(t => if t._4.oneWay then 1 else 2).sum
        if graph.adjacencies.size != expectedAdjacencies then
          System.err.println(s"WARNING: ${graph.adjacencies.size} != $expectedAdjacencies")
        Success(graph)
      case z =>
        Failure(GraphException(s"parse failed: $z"))
    }
}
