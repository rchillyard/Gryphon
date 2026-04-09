package com.phasmidsoftware.gryphon.adjunct

import com.phasmidsoftware.gryphon.core.*
import com.phasmidsoftware.gryphon.traverse.{BellmanFord, TopologicalSort, VertexTraversalResult}
import com.phasmidsoftware.gryphon.util.GraphException
import com.phasmidsoftware.visitor.core.Monoid
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
   * Returns a new DirectedGraph with all edges reversed (every u→v becomes v→u).
   * The vertex set is preserved, including any vertices with no outgoing edges.
   *
   * This is the first step in Kosaraju's strongly-connected-components algorithm.
   *
   * @return a new `DirectedGraph[V, E]` with reversed edges.
   */
  def reverse: DirectedGraph[V, E] =
    edges.foldLeft(DirectedGraph[V, E](vertexMap.keysOnly)) { (g, e) =>
      val rev: DirectedEdge[V, E] = e match
        case AttributedDirectedEdge(attr, from, to) => AttributedDirectedEdge(attr, to, from)
        case OrderedEdge(from, to) => OrderedEdge(to, from)
        case other => throw GraphException(s"unexpected edge type in reverse: $other")
      g.addEdge(rev)
    }

  /**
   * Checks whether the directed graph contains a cycle.
   *
   * This method determines if the graph is cyclic by attempting to perform a
   * topological sort. A graph is cyclic if and only if a topological order
   * cannot be established.
   *
   * @return true if the graph is cyclic, false otherwise.
   */
  def isCyclic: Boolean = TopologicalSort.sort(this).isEmpty

  /**
   * Not yet implemented for directed graphs.
   * For directed graphs, connectivity has two distinct notions:
   * weak (treating edges as undirected) and strong (every vertex reachable
   * from every other via directed paths). Use ConnectedComponents for
   * undirected connectivity or Kosaraju for strongly connected components.
   */
  def isConnected: Boolean =
    throw UnsupportedOperationException("isConnected is not yet implemented for DirectedGraph — use ConnectedComponents or Kosaraju")

  /**
   * Not yet implemented for directed graphs.
   */
  def isBipartite: Boolean =
    throw UnsupportedOperationException("isBipartite is not yet implemented for DirectedGraph")

  /**
   * Computes shortest paths from `start` using Bellman-Ford-Moore.
   * Returns None if a negative cycle is reachable from start.
   */
  def shortestPaths(start: V)(using Monoid[E], Ordering[E]): Option[VertexTraversalResult[V, DirectedEdge[V, E]]] =
    BellmanFord.shortestPaths(this, start)

  /**
   * Adds an edge to the graph. The edge connects two vertices and may carry an attribute of type `E`.
   * The type of the edge (e.g., directed, undirected, or orderable) determines how it is added to the graph.
   *
   * @param edge the edge to be added, which is an instance of `Edge[V, E]`. The edge defines the connection
   *             between two vertices of type `V` and may have a direction and an associated attribute of type `E`.
   *
   * @return a new `EdgeGraph[V, E]` instance that includes the newly added edge. The returned graph preserves
   *         all existing edges and vertices.
   *
   * @throws GraphException if the provided edge type is unexpected or unsupported.
   */
  override def addEdge(edge: Edge[V, E]): DirectedGraph[V, E] = edge match {
    case edge: DirectedEdge[_, _] =>
      copy(vertexMap.modifyVertex(v => v + AdjacencyEdge(edge))(edge.white))
    case edge: OrderableEdge[_, _] =>
      copy(vertexMap.modifyVertex(v => v + AdjacencyEdge(edge))(edge.white))
    case edge@UndirectedEdge(_, white, _) =>
      copy(vertexMap.modifyVertex(v => v + AdjacencyEdge(edge))(white)) // TODO we need to add this edge twice (once in each direction)
    case _ =>
      throw GraphException(s"unexpected edge type: $edge")
  }

  /**
   * Retrieves all edges in the directed graph as an iterable collection of edges.
   *
   * @return an iterable collection containing all edges of type `Edge[V, E]` in the graph.
   */
  def edges: Iterator[DirectedEdge[V, E]] =
    adjacencies map DirectedGraph.getDirectedEdgeFromAdjacency[V, E]

  /**
   * Computes and returns the total number of edges in the directed graph.
   *
   * @return the number of directed edges present in the graph.
   */
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
   *
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
              if (!t.edgeType.oneWay)
                System.err.println(s"WARNING: edge ${t.maybeAttribute} is not directed.")
              z.createVerticesFromTriplet[E, EdgeType](f) {
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
        val expectedAdjacencies = triplets.triplets.map(t => if t._4.oneWay then 1 else 2).sum
        if (graph.adjacencies.size != expectedAdjacencies)
          System.err.println(s"WARNING: ${graph.adjacencies.size} != $expectedAdjacencies")
        Success(graph)
      case z =>
        Failure(GraphException(s"parse failed: $z"))
    }

  /**
   * A partial function that extracts a `DirectedEdge` from an `Adjacency`, specifically when the `Adjacency`
   * is an `AdjacencyEdge` containing a `DirectedEdge` and is not marked as `discovered`.
   *
   * This function will throw a `GraphException` if the input `Adjacency` does not match the expected pattern,
   * indicating an unsupported or an invalid-edge type.
   *
   * @throws GraphException if the input is not an `AdjacencyEdge` with a `DirectedEdge`.
   */
  private def getDirectedEdgeFromAdjacency[V, E](va: Adjacency[V]): DirectedEdge[V, E] = va match {
    case AdjacencyEdge(e: DirectedEdge[V, E] @unchecked, false) =>
      e
    case AdjacencyEdge(e: VertexPair[V], false) =>
      OrderedEdge(e.white, e.black).asInstanceOf[DirectedEdge[V, E]] // E should be Unit
    case x =>
      throw GraphException(s"unexpected edge type: $x")
  }
}