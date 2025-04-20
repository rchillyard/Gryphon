package littlegryphon.core

import littlegryphon.parse.Parseable

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
case class VertexPairList[V](pairs: Seq[(Vertex[V], Vertex[V])]) extends SerializableGraph[V, Unit]:
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
 * the edges of a graph with unit edge attributes (i.e., no meaningful data attached to edges).
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

case class Edges[V, E](triplets: Seq[(V, V, E)]) extends SerializableGraph[V, E]

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
