package com.phasmidsoftware.gryphon.core

import com.phasmidsoftware.gryphon.parse.Parseable

/**
 * A trait representing a graph structure defined by its edges.
 * The `Edges` trait provides an abstraction for accessing the
 * sequence of graph edges without requiring direct knowledge of
 * the underlying graph implementation.
 *
 * @tparam V the type of attributes associated with the vertices in the graph.
 * @tparam E the type of attributes associated with the edges in the graph.
 */
trait Edges[V, E] extends Pairs[V] {

  /**
   * Retrieves a sequence of edges in the graph.
   *
   * @return a `Seq` containing all edges in the graph. 
   *         Each edge connects two vertices and may carry an associated attribute.
   */
  def edges: Seq[Edge[E, V]]

  /**
   * Retrieves all the vertex pairs that represent the connections (edges) in the graph.
   * Each pair consists of a starting vertex (`from`) and an ending vertex (`to`)
   * corresponding to the `from` and `to` vertices of an edge.
   *
   * @return a `Seq` of tuples, where each tuple consists of two `Vertex[V]` elements.
   *         The first element in the tuple represents the starting vertex of the edge,
   *         and the second element represents the ending vertex.
   */
  def pairs: Seq[(Vertex[V], Vertex[V])] = edges.map(e => (e.from, e.to))
}

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
case class EdgeList[V, E](edges: Seq[Edge[E, V]]) extends SerializableGraph[V, E] with Edges[V, E]:
  /**
   * Constructs a sequence of triplets from the edges in the edge list.
   * Each triplet consists of the attribute of the source vertex, the attribute of the target vertex,
   * and the attribute of the edge connecting them.
   *
   * @return a sequence of triplets where each triplet is represented as `Triplet[V, E]`,
   *         with the first element being the source vertex's attribute, the second element
   *         being the target vertex's attribute, and the third element being the edge's attribute.
   */
  def triplets: Seq[Triplet[V, E]] = edges.map(e => (e.from.attribute, e.to.attribute, e.attribute))

case class Triple[V, E](v1: V, v2: V, e: E)

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
case class Triplets[V, E](triplets: Seq[Triplet[V, E]]) extends SerializableGraph[V, E]

/**
 * Provides utility methods for creating and working with `Triplets` by parsing string representations
 * of vertices and edge attributes into their respective types.
 *
 * This object defines methods to construct `Triplets` instances from sequences of string tuples,
 * where each tuple represents a triplet containing the source vertex, target vertex, 
 * and the edge attribute, all initially represented as strings.
 *
 * The parsing mechanism relies on the `Parseable` typeclass to convert these strings into
 * strongly typed objects. The `Parseable` instances must be available in the implicit scope
 * for the provided types. If any conversion fails, the corresponding triplet is discarded.
 */
object Triplets {
  def parse[V: Parseable, E: Parseable](ts: Seq[(String, String, String)]): Triplets[V, E] =
    Triplets(for {
      (x, y, z) <- ts
      vx <- implicitly[Parseable[V]].parse(x)
      vy <- implicitly[Parseable[V]].parse(y)
      ez <- implicitly[Parseable[E]].parse(z)
    } yield (vx, vy, ez)
    )

}
