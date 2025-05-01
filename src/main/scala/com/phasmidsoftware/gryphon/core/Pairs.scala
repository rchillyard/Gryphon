package com.phasmidsoftware.gryphon.core

import com.phasmidsoftware.gryphon.parse.Parseable
import com.phasmidsoftware.gryphon.util.FP

import scala.util.Try

/**
 * A trait representing a collection of vertex pairs within a graph structure.
 *
 * The `Pairs` trait provides an abstraction for working with pairs of vertices,
 * commonly used in undirected graphs or situations where the relationship between
 * two vertices needs to be represented as a pair.
 *
 * @tparam V the type of attributes associated with the vertices in the graph.
 */
trait Pairs[V]:

  /**
   * Returns a sequence of vertex pairs.
   * Each tuple in the sequence represents a relationship or connection
   * between two vertices of type `V` in the graph.
   *
   * @return a sequence of tuples, where each tuple contains two vertices of type `V`.
   */
  def pairs: Seq[(V, V)]

/**
 * A case class representing a list of vertex pairs in a graph.
 *
 * This class provides a concrete implementation of `SerializableGraph` and `Pairs`, enabling functionalities
 * such as accessing the pairs of vertices and transforming these pairs into triplets. Each triplet includes
 * the source vertex, target vertex, and a unit value indicating the absence of an explicit edge attribute.
 *
 * @tparam V the type of attributes associated with the vertices in the graph.
 * @param pairs a sequence of tuples, where each tuple contains two vertices representing
 *              a connection or relationship in the graph.
 */
case class VertexPairList[V](pairs: Seq[(V, V)], oneWay: Boolean = false) extends SerializableGraph[V, Unit] with Pairs[V]:
  /**
   * Transforms the sequence of vertex pairs into a sequence of triplets.
   * Each triplet consists of the attributes of the source vertex, the target vertex,
   * and a unit value (`()`), indicating the absence of an explicit edge attribute.
   *
   * @return a sequence of triplets where each triplet contains the attribute of the source vertex,
   *         the attribute of the target vertex, and a unit value.
   */
  def triplets: Seq[(V, V, Unit)] = pairs.map(p => (p._1, p._2, ()))

/**
 * A case class representing a collection of directed connections (edges) between pairs of vertices.
 *
 * Each connection is represented as a pair of vertices of type `V`, where the first element in the pair
 * represents the source vertex and the second element represents the target vertex. This structure allows
 * modeling graph-like relationships between vertices without associated edge attributes (i.e., the edges carry no additional data).
 *
 * This class extends `SerializableGraph` and implements its `triplets` method, representing connections as triplets with
 * an empty unit attribute (`()`).
 *
 * @tparam V the type of vertices in the graph. Each vertex can represent any user-defined type.
 * @constructor Creates a new `Connexions` instance to store the specified sequence of vertex pairs.
 *              These pairs define the edges of the graph-like structure.
 * @param pairs a sequence of vertex pairs of type `(V, V)`, each representing a directed edge
 *              between the source (`_1`) and target (`_2`) vertices.
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
  /**
   * Parses a sequence of string pairs into a `Connexions` instance, where each pair represents
   * a directed connection between two vertices of type `V`.
   *
   * The method relies on the `Parseable` typeclass to parse each string into the target type `V`.
   * If all pairs are successfully parsed, a `Connexions` instance is created. If any parsing
   * operation fails, the method returns a failed `Try`.
   *
   * @param pairs a sequence of string pairs representing directed connections; each pair consists 
   *              of two strings that are parsed into vertices of type `V`.
   * @tparam V the type of the vertices in the `Connexions`, which must have an implicit `Parseable`
   *           instance for parsing strings into `V`.
   * @return a `Try` containing the resulting `Connexions[V]` instance if all pairs are successfully
   *         parsed, or a failed `Try` with the corresponding exception if any parsing fails.
   */
  def parse[V: Parseable](pairs: Seq[(String, String)]): Try[Connexions[V]] = {
    val tys: Seq[Try[(V, V)]] = for {
      (x, y) <- pairs
    } yield for {
      vx <- implicitly[Parseable[V]].parse(x)
      vy <- implicitly[Parseable[V]].parse(y)
    } yield (vx, vy)
    FP.sequence(tys) map (Connexions(_))

  }
}
