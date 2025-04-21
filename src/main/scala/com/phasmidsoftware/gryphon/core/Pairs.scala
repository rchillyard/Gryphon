package com.phasmidsoftware.gryphon.core

import com.phasmidsoftware.gryphon.parse.Parseable

/**
 * A trait representing a collection of vertex pairs within a graph structure.
 *
 * The `Pairs` trait provides an abstraction for working with pairs of vertices,
 * commonly used in undirected graphs or situations where the relationship between
 * two vertices needs to be represented as a pair.
 *
 * @tparam V the type of attributes associated with the vertices in the graph.
 */
trait Pairs[V] {

  /**
   * Retrieves the sequence of vertex pairs contained within the graph structure.
   *
   * Each pair represents a connection or relationship between two vertices in the graph,
   * commonly used in undirected graph representations or scenarios requiring bidirectional
   * adjacency between vertices.
   *
   * @return a sequence of vertex pairs, where each pair consists of two `Vertex[V]` objects.
   */
  def pairs: Seq[(Vertex[V], Vertex[V])]
}


/**
 * Represents a list of vertex pairs that defines edges in a graph.
 * Each pair in the list corresponds to a directed edge where the first vertex
 * in the pair is the source, and the second vertex is the target.
 *
 * This class extends the `SerializableGraph` trait, providing a way
 * to serialize and traverse the graph structure through the triplets it generates.
 *
 * @tparam V the type of attributes associated with the vertices in the graph.
 * @param pairs a sequence of vertex pairs representing the connections in the graph.
 *              Each pair consists of a source vertex and a target vertex.
 */
case class VertexPairList[V](pairs: Seq[(Vertex[V], Vertex[V])]) extends SerializableGraph[V, Unit] with Pairs[V]:
  /**
   * Transforms the sequence of vertex pairs into a sequence of triplets.
   * Each triplet consists of the attributes of the source vertex, the target vertex,
   * and a unit value (`()`), indicating the absence of an explicit edge attribute.
   *
   * @return a sequence of triplets where each triplet contains the attribute of the source vertex,
   *         the attribute of the target vertex, and a unit value.
   */
  def triplets: Seq[(V, V, Unit)] = pairs.map(p => (p._1.attribute, p._2.attribute, ()))

/**
 * A case class representing a collection of vertex pairs, which can be interpreted as
 * the edges of a graph with unit-edge attributes (i.e., no meaningful data attached to edges).
 *
 * This class extends `SerializableGraph` with vertices of type `V` and edges with a unit (`Unit`) attribute.
 * It provides the ability to retrieve graph edges in the form of triplets, where each triplet
 * contains a source vertex, a target vertex, and the corresponding edge attribute (always `()`).
 *
 * @constructor Creates a new instance of `Connexions` with a sequence of vertex pairs representing edges.
 * @param pairs a sequence of tuples `(V, V)` where each tuple represents a directed edge from a source
 *              vertex (first element) to a target vertex (second element).
 * @tparam V the type of vertices in the graph, which is also the type of the elements in the vertex pairs.
 */
case class Connexions[V](pairs: Seq[(V, V)]) extends SerializableGraph[V, Unit]:
  def triplets: Seq[(V, V, Unit)] = pairs.map(p => (p._1, p._2, ()))

/**
 * Provides a utility for constructing a `Connexions` instance from a sequence of string ts.
 *
 * This object contains methods to parse input data and generate graph-like structures
 * where vertices are connected by edges derived from parsed values.
 */
object Connexions {
  def parse[V: Parseable](pairs: Seq[(String, String)]): Connexions[V] =
    Connexions(for {
      (x, y) <- pairs
      vx <- implicitly[Parseable[V]].parse(x)
      vy <- implicitly[Parseable[V]].parse(y)
    } yield (vx, vy)
    )
}
