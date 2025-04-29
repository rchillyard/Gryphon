package com.phasmidsoftware.gryphon.core

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
   * @return a sequence of triplets where each triplet is represented as a tuple `Triplet[V, E]`.
   */
  def triplets: Seq[Triplet[V, E]]

  def serialize: String = triplets.map(t => s"${t._1},${t._2},${t._3}").mkString("\n")

/**
 * Object providing factory methods for creating instances of the `SerializableGraph` trait.
 *
 * The `SerializableGraph` represents a graph structure that can be serialized and traversed. 
 * This companion object includes methods to construct graphs from sequences of edges or 
 * vertex pairs.
 */
object SerializableGraph {
  /**
   * Creates a `SerializableGraph` instance from a sequence of triplets.
   *
   * Each triplet in the input sequence represents an edge in the graph, consisting of
   * two vertices (source and target) and an edge attribute.
   *
   * @param triplets a sequence of triplets `Triplet[V, E]`, where:
   *                 - the first element is the source vertex of type `V`.
   *                 - the second element is the target vertex of type `V`.
   *                 - the third element is the edge attribute of type `E`.
   * @tparam V the type associated with the vertices in the graph.
   * @tparam E the type associated with the edges in the graph.
   * @return a `SerializableGraph[V, E]` representing the graph described by the provided triplets.
   */
  def createFromTriplets[V, E](triplets: Seq[Triplet[V, E]]): SerializableGraph[V, E] =
    Triplets(triplets)

  /**
   * Creates a `SerializableGraph` instance from a sequence of vertex pairs.
   *
   * Each pair in the input sequence represents an edge in the graph, defined by two vertices.
   * The resulting graph uses the pairs to establish connections between vertices, with no additional
   * attributes associated with the edges.
   *
   * @param pairs a sequence of tuples `(V, V)` where each tuple represents a vertex pair in the graph.
   *              The first element of the tuple is the source vertex, and the second element is the target vertex.
   * @tparam V the type associated with the vertices in the graph.
   * @return a `Connexions[V]` representing the graph constructed from the specified vertex pairs.
   */
  def createFromPairs[V](pairs: Seq[(V, V)]): SerializableGraph[V, Unit] =
    Connexions(pairs)

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
