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
 * @tparam Z the generic type for edge type.
 */
trait SerializableGraph[V, E, Z]:
  /**
   * Retrieves a sequence of triplets representing the graph structure.
   * Each triplet consists of two vertices (source and target) of type `V` and an edge attribute
   * of type `E` connecting them, if any.
   *
   * @return a sequence of triplets where each triplet is represented as a tuple `Triplet[V, E]`.
   */
  def triplets: Seq[Triplet[V, E, Z]]

  /**
   * Serializes the sequence of triplets into a string representation.
   * Each triplet is converted to a comma-separated string of its elements, and the
   * entire sequence is joined using newline characters.
   *
   * @return a string representation of the triplets, where each triplet is serialized as "source, target, attribute"
   *         and each serialized triplet is separated by a newline.
   */
  def serialize: String =
    triplets.map(t => s"${t._1},${t._2},${t._3}").mkString("\n")

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
   * @tparam Z the generic type for edge type.
   * @return a `SerializableGraph[V, E]` representing the graph described by the provided triplets.
   */
  def createFromTriplets[V, E, Z](triplets: Seq[Triplet[V, E, Z]]): SerializableGraph[V, E, Z] =
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
   * @tparam Z the generic type for edge type.
   * @return a `Connexions[V]` representing the graph constructed from the specified vertex pairs.
   */
  def createFromPairs[V, Z](pairs: Seq[(V, V, Z)]): SerializableGraph[V, Unit, Z] =
    Connexions(pairs)
}

/**
 * Represents a triplet structure consisting of two vertices, an optional attribute, and an edge type.
 *
 * @tparam V the type of the vertices (white and black).
 * @tparam E the type of the optional attribute associated with the edge.
 * @tparam Z the type representing the edge type.
 * @param from           the starting vertex of the triplet.
 * @param to             the ending vertex of the triplet.
 * @param maybeAttribute an optional attribute that provides additional metadata for the edge.
 * @param edgeType       the type of the edge, providing further classification or relationship information.
 */
case class Triplet[V, E, Z](from: V, to: V, maybeAttribute: Option[E], edgeType: Z)
