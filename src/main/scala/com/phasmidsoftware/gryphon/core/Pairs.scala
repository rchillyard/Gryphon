package com.phasmidsoftware.gryphon.core

import com.phasmidsoftware.gryphon.parse.Parseable
import com.phasmidsoftware.gryphon.util.FP

import scala.util.Try

/**
 * A trait that defines a structure for managing pairs of vertices with an associated value.
 *
 * The purpose of `Pairs` is to represent relationships or connections between pairs of
 * vertices of type `V`, with an additional associated value of type `Z` for each pair.
 *
 * @tparam V the type of the vertices in the pairs.
 * @tparam Z the type of the additional value associated with each pair of vertices.
 */
trait Pairs[V, Z]:

  /**
   * Returns a sequence of vertex pairs.
   * Each tuple in the sequence represents a relationship or connection
   * between two vertices of type `V` in the graph.
   *
   * @return a sequence of tuples, where each tuple contains two vertices of type `V`.
   */
  def pairs: Seq[(V, V, Z)]

/**
 * Represents a collection of directed connections between vertices, where each connection
 * is associated with an additional attribute of type `Z`. The connections are stored as a sequence
 * of tuples `(V, V, Z)`, where the first element is the source vertex, the second is the target vertex,
 * and the third is the associated attribute.
 *
 * CONSIDER renaming this as ProtoConnexions (or something like that).
 *
 * @tparam V the type of the vertices in the connections.
 * @tparam Z the type of the additional attribute associated with each connection.
 * @constructor Creates an instance of `Connexions` initialized with a sequence of `(V, V, Z)` tuples.
 * @param pairs a sequence of tuples representing the connections. Each tuple consists of a source vertex,
 *              a target vertex, and an additional attribute of type `Z`.
 */
case class Connexions[V, Z](pairs: Seq[(V, V, Z)]) extends SerializableGraph[V, Unit, Z]:
  /**
   * Converts the current collection of directed connections represented as pairs
   * of vertices into a sequence of triplets. Each triplet consists of the source vertex,
   * the target vertex, a unit value, and an additional attribute of type `Z`.
   *
   * @return a sequence of triplets where each triplet is of the form `(V, V, Unit, Z)`.
   *         The first element represents the source vertex, the second represents the target vertex,
   *         the third is a unit value, and the fourth corresponds to the additional attribute.
   */
  def triplets: Seq[Triplet[V, Unit, Z]] =
    pairs.map(p => Triplet(p._1, p._2, None, p._3))

/**
 * Provides a utility for constructing a `Connexions` instance from a sequence of string ts.
 *
 * This object contains methods to parse input data and generate graph-like structures
 * where vertices are connected by edges derived from parsed values.
 */
object Connexions {
  /**
   * Parses a sequence of tuples containing string representations of vertices and edges into
   * a `Connexions` object, which models the graph-like relationships between the parsed vertices
   * and edges.
   *
   */
  def parse[V: Parseable, Z: Parseable](pairs: Seq[(String, String, String)]): Try[Connexions[V, Z]] = {
    val tys: Seq[Try[(V, V, Z)]] = for {
      (x, y, z) <- pairs
    } yield for {
      vx <- implicitly[Parseable[V]].parse(x)
      vy <- implicitly[Parseable[V]].parse(y)
      q <- implicitly[Parseable[Z]].parse(z)
    } yield (vx, vy, q)
    FP.sequence(tys) map (Connexions(_))
  }
}
