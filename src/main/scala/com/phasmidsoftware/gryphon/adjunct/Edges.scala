package com.phasmidsoftware.gryphon.adjunct

import com.phasmidsoftware.gryphon.core.{Edge, SerializableGraph}
import com.phasmidsoftware.gryphon.parse.Parseable

case class Triple[V, E](v1: V, v2: V, e: E)

/**
 * A case class representing a collection of edges in the form of an edge list.
 *
 * Each edge in the edge list connects two vertices and may have an associated attribute.
 * The `EdgeList` class can transform these edges into triplets consisting of the
 * attribute values of the source vertex, target vertex, and the edge itself.
 *
 * @tparam V the type associated with the vertices in the edge list.
 * @tparam E the type associated with the edges in the edge list.
 * @param edges a sequence of edges representing connections between vertices.
 */
case class EdgeList[V, E](edges: Seq[Edge[E, V]]) extends SerializableGraph[V, E]:
  /**
   * Constructs a sequence of triplets from the edges in the edge list.
   * Each triplet consists of the attribute of the source vertex, the attribute of the target vertex,
   * and the attribute of the edge connecting them.
   *
   * @return a sequence of triplets where each triplet is represented as `(V, V, E)`,
   *         with the first element being the source vertex's attribute, the second element
   *         being the target vertex's attribute, and the third element being the edge's attribute.
   */
  def triplets: Seq[(V, V, E)] = edges.map(e => (e.from.attribute, e.to.attribute, e.attribute))

/**
 * A case class representing the edges of a graph, where each edge connects two vertices
 * and carries an associated attribute. The edges are represented as a sequence of triplets,
 * where each triplet contains the source vertex, the target vertex, and the edge attribute.
 *
 * This class extends the `SerializableGraph` trait, enabling serialization and traversal
 * of the graph structure through its triplets.
 *
 * @tparam V the type of attributes associated with the vertices in the graph.
 * @tparam E the type of attributes associated with the edges in the graph.
 * @param triplets a sequence of triplets representing the connections in the graph.
 *                 Each triplet contains a source vertex of type `V`, a target vertex of type `V`,
 *                 and an edge attribute of type `E`.
 */
case class Edges[V, E](triplets: Seq[(V, V, E)]) extends SerializableGraph[V, E]

/**
 * Provides utility methods for creating and working with `Edges` by parsing string representations
 * of vertices and edge attributes into their respective types.
 *
 * This object defines methods to construct `Edges` instances from sequences of string tuples,
 * where each tuple represents a triplet containing the source vertex, target vertex, 
 * and the edge attribute, all initially represented as strings.
 *
 * The parsing mechanism relies on the `Parseable` typeclass to convert these strings into
 * strongly typed objects. The `Parseable` instances must be available in the implicit scope
 * for the provided types. If any conversion fails, the corresponding triplet is discarded.
 */
object Edges {
  def parse[V: Parseable, E: Parseable](ts: Seq[(String, String, String)]): Edges[V, E] =
    Edges(for {
      (x, y, z) <- ts
      vx <- implicitly[Parseable[V]].parse(x)
      vy <- implicitly[Parseable[V]].parse(y)
      ez <- implicitly[Parseable[E]].parse(z)
    } yield (vx, vy, ez)
    )

}
