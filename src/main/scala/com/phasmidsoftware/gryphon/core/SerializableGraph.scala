package com.phasmidsoftware.gryphon.core

import com.phasmidsoftware.gryphon.parse.Parseable

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
   * Renders the triplets in the format `GraphParser` reads, one per line.
   *
   *   0 > 1 5.0
   *
   * That is: source, the edge type's symbol, target, and the attribute where there is one. The
   * attribute is omitted entirely when absent, which is what `maybeEdge` in the grammar expects.
   *
   * Until #135 this wrote `source,target,Some(5.0)` — comma-separated, with Scala's
   * `Option.toString`, and **no edge type at all**. Nothing could read it back, and the missing
   * edge type meant directedness was lost, so no amount of separator fixing would have given a
   * round trip. `SerializableGraphSpec` now asserts that parse ∘ serialize is the identity over
   * a real graph, which is the test that settles the format by construction.
   *
   * @param pz renders `Z`. `Parseable` carries `render` as the inverse of `parse`, defaulting to
   *           `toString`, which is correct for every type here except `EdgeType` — whose
   *           `toString` is "Directed" where the parser reads ">".
   * @return the graph in `GraphParser`'s own format.
   */
  def serialize(using pz: Parseable[Z]): String =
    triplets.map { t =>
      val attribute = t.maybeAttribute.map(a => s" $a").getOrElse("")
      s"${t.from} ${pz.render(t.edgeType)} ${t.to}$attribute"
    }.mkString("\n")

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
   * @return a `SerializableGraph[V, Unit, Z]` representing the graph constructed from the given
   *         vertex pairs — concretely a `ProtoConnexions`, but callers see the declared type.
   */
  def createFromPairs[V, Z](pairs: Seq[(V, V, Z)]): SerializableGraph[V, Unit, Z] =
    ProtoConnexions(pairs)
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
