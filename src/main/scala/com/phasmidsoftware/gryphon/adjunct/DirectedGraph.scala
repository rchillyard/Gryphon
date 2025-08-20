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
case class DirectedGraph[V, E](vertexMap: VertexMap[V]) extends AbstractGraph[V](vertexMap) with EdgeGraph[V, E] {

  /**
   * Adds an edge to the graph. The edge connects two vertices and may carry an attribute of type `E`.
   * The type of the edge (e.g., directed, undirected, or orderable) determines how it is added to the graph.
   *
   * @param edge the edge to be added, which is an instance of `Edge[E, V]`. The edge defines the connection
   *             between two vertices of type `V` and may have a direction and an associated attribute of type `E`.
   * @return a new `EdgeGraph[V, E]` instance that includes the newly added edge. The returned graph preserves
   *         all existing edges and vertices.
   * @throws GraphException if the provided edge type is unexpected or unsupported.
   */
  def addEdge(edge: Edge[E, V]): EdgeGraph[V, E] = edge match {
    case edge: DirectedEdge[_, _] => copy(vertexMap.modifyVertex(v => v + AdjacencyEdge(edge))(edge.white))
    case edge: OrderableEdge[_, _] => copy(vertexMap.modifyVertex(v => v + AdjacencyEdge(edge))(edge.white))
    case UndirectedEdge(attribute, white, black) =>
      copy(vertexMap.modifyVertex(v => v + AdjacencyEdge(edge))(edge.white)) // TODO we need to add this edge twice (once in each direction)
    case _ => throw GraphException(s"unexpected edge type: $edge")
  }

  /**
   * Retrieves all edges in the directed graph as an iterable collection of edges.
   *
   * @return an iterable collection containing all edges of type `Edge[E, V]` in the graph.
   */
  def edges: Iterator[DirectedEdge[E, V]] =
    adjacencies map DirectedGraph.getDirectedEdgeFromAdjacency[E, V]

  /**
   * Creates a new directed graph using the provided vertex map.
   *
   * @param vertexMap the vertex map to be used for constructing the graph. It defines the vertices
   *                  and their adjacencies in the graph.
   * @return a new directed graph constructed with the given vertex map.
   */
  def unit(vertexMap: VertexMap[V]): Graph[V] =
    DirectedGraph(vertexMap)

  /**
   * Retrieves the adjacent vertices connected to the specified vertex in the graph-like structure.
   *
   * @param v the vertex whose adjacent vertices are to be returned.
   * @return an iterator over the vertices adjacent to the specified vertex.
   */
  def adjacentVertices(v: V): Iterator[V] =
    vertexMap.adjacentVertices(v)

  /**
   * Retrieves the adjacent vertices connected to the specified vertex in the graph-like structure.
   * NOTE this method relies on the side-effect of setting the `discovered` flag on the vertex.
   *
   * @param v the vertex whose adjacent vertices are to be returned.
   * @return an iterator over the vertices adjacent to the specified vertex.
   */
  def undiscoveredAdjacentVertices(v: V): Iterator[V] =
    vertexMap.undiscoveredAdjacentVertices(v)
}

/**
 * Factory object for creating instances of `DirectedGraph`.
 * This object provides utility methods to construct a directed graph
 * from an edge list representation.
 */
object DirectedGraph {

  /**
   * Constructs a new, directed graph using the provided vertex map.
   *
   * @param vertexMap a map containing the vertices of type `V` as keys and their associated
   *                  adjacency lists or other vertex-specific data.
   * @tparam V the type of the vertices in the graph.
   * @tparam E the type of the edges in the graph.
   * @return an instance of `DirectedGraph[V, E]` initialized with the given vertex map.
   */
  def apply[V, E](vertexMap: VertexMap[V]): DirectedGraph[V, E] =
    new DirectedGraph(vertexMap)

  /**
   * Creates a new instance of a `DirectedGraph` with an empty vertex map.
   *
   * This method provides a convenient way to initialize an empty directed graph
   * without explicitly providing a `VertexMap`. The vertex map is initialized
   * using the default empty `VertexMap` implementation.
   *
   * @tparam V the type of the vertices in the graph.
   * @tparam E the type of the edges in the graph.
   * @return an empty instance of `DirectedGraph[V, E]`.
   */
  def apply[V, E]: DirectedGraph[V, E] =
    apply[V, E](VertexMap[V])

  /**
   * A partial function that extracts a `DirectedEdge` from an `Adjacency`, specifically when the `Adjacency`
   * is an `AdjacencyEdge` containing a `DirectedEdge` and is not marked as `discovered`.
   *
   * This function will throw a `GraphException` if the input `Adjacency` does not match the expected pattern,
   * indicating an unsupported or an invalid-edge type.
   *
   * @throws GraphException if the input is not an `AdjacencyEdge` with a `DirectedEdge`.
   */
  def getDirectedEdgeFromAdjacency[E, V](va: Adjacency[V]): DirectedEdge[E, V] = va match {
    case AdjacencyEdge(e: DirectedEdge[E, V], false) =>
      e
    case AdjacencyEdge(e: VertexPair[V], false) =>
      OrderedEdge(e.white, e.black).asInstanceOf[DirectedEdge[E, V]] // E should be Unit
    case x =>
      throw GraphException(s"unexpected edge type: $x")
  }

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
   * @tparam V the type of the vertices in the graph.
   * @tparam E the type of the edges in the graph.
   * @return a `Try[Graph[V]]`, where:
   *         - `Success(Graph[V])` contains the constructed graph if the operation is successful.
   *         - `Failure` contains a `GraphException` if the graph construction fails.
   */
  def triplesToTryGraph[V, E](triples: Seq[Triplet[V, E, EdgeType]]): Try[Graph[V]] =
    SerializableGraph.createFromTriplets[V, E, EdgeType](triples) match {
      case triplets: Triplets[V, E, EdgeType] =>
        val vm: VertexMap[V] =
          triplets.triplets.foldLeft(VertexMap[V]) {
            (z, t) =>
              // TODO find another way to handle this anomaly
              if (!t._4.oneWay) System.err.println(s"WARNING: edge ${t._3} is not directed.")
              z.createVerticesFromTriplet[E, EdgeType](Vertex.createWithSet) {
                  case (vv1, vv2, Some(e)) =>
                    AdjacencyEdge(AttributedDirectedEdge(e, vv1.attribute, vv2.attribute))
                  case (vv1, vv2, None) =>
                    AdjacencyEdge(VertexPair(vv1.attribute, vv2.attribute))
                }(false)
                (t)
          }
        val graph = DirectedGraph(vm)
        // TODO find another way to handle this anomaly
        // NOTE duplicated code in UndirectedGraph
        if (graph.adjacencies.size != triplets.triplets.size)
          System.err.println(s"WARNING: ${graph.adjacencies.size} != ${triplets.triplets.size}")
        println(s"graph = $graph")
        Success(graph)
      case z =>
        Failure(GraphException(s"parse failed: $z"))
    }
}
