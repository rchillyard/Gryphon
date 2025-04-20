package com.phasmidsoftware.gryphon.core

import com.phasmidsoftware.gryphon.adjunct.{EdgeList, VertexPairList}

/**
 * A trait representing a serializable graph structure.
 *
 * `SerializableGraph` provides the capability to serialize and traverse through its
 * triplets, where each triplet represents an edge or vertex pair in the graph.
 * Each triplet consists of two vertices (source and target) and an edge attribute
 * connecting them (if any).
 *
 * @tparam V the type associated with the vertices in the graph.
 * @tparam E the type associated with the edges in the graph.
 */
trait SerializableGraph[V, E]:
  /**
   * Retrieves a sequence of triplets representing the graph structure.
   * Each triplet consists of two vertices (source and target) of type `V` and an edge attribute
   * of type `E` connecting them, if any.
   *
   * @return a sequence of triplets where each triplet is represented as a tuple `(V, V, E)`.
   */
  def triplets: Seq[(V, V, E)]

/**
 * Object providing factory methods for creating instances of the `SerializableGraph` trait.
 *
 * The `SerializableGraph` represents a graph structure that can be serialized and traversed. 
 * This companion object includes methods to construct graphs from sequences of edges or 
 * vertex pairs.
 */
object SerializableGraph {
  /**
   * Creates a `SerializableGraph` instance from a sequence of edges.
   *
   * @param edges a sequence of edges of type `Edge[E, V]` representing the connections 
   *              between vertices in the graph. Each edge contains two vertices 
   *              (`from` and `to`) and an attribute of type `E`.
   * @return a `SerializableGraph[V, E]` representing the graph structure defined by the provided edges.
   */
  def createFromEdges[V, E](edges: Seq[Edge[E, V]]): SerializableGraph[V, E] = EdgeList(edges)

  /**
   * Creates a `SerializableGraph` instance from a sequence of vertex pairs.
   *
   * This method takes a sequence of pairs where each pair represents a connection
   * between two vertices in the graph. The resulting graph uses the pairs to define
   * its structure, with no additional attributes associated with the edges.
   *
   * @param pairs a sequence of tuples where each tuple consists of two vertices of type `Vertex[V]`.
   *              The first element in the tuple represents the source vertex, and the second 
   *              element represents the target vertex.
   * @tparam V the type associated with the vertex attributes within the graph.
   * @return a `SerializableGraph[V, Unit]` instance representing the graph
   *         structure based on the given vertex pairs.
   */
  def createFromVertexPairs[V](pairs: Seq[(Vertex[V], Vertex[V])]): SerializableGraph[V, Unit] = VertexPairList(pairs)
}
