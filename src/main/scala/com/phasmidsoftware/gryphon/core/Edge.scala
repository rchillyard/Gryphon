package com.phasmidsoftware.gryphon.core

import com.phasmidsoftware.gryphon.parse.Parseable

import scala.util.matching.Regex
import scala.util.{Success, Try}

/**
 * Defines an abstraction for an edge in a graph structure that connects two vertices.
 * An `Edge` may be directed or undirected, and it carries an attribute of type `E`.
 *
 * The connection is established between two vertices of type `V`, referred to as `white` and `black`.
 * The type (directionality) of the edge, as well as whether it allows traversal in one or both directions,
 * is given by the `edgeType` field.
 *
 * @tparam E the type of the attribute associated with the edge.
 * @tparam V the type of the vertices connected by the edge.
 */
trait Edge[E, V] extends Attribute[E] with Connexion[V] {

  /**
   * Retrieves the type of the edge (e.g., directed or undirected).
   *
   * @return an instance of `EdgeType` that describes whether the edge is directed (one-way) or undirected (two-way).
   */
  def edgeType: EdgeType
}

/**
 * A trait representing the type of an edge, indicating whether it is directed or undirected.
 * Or something else?
 * It provides a method to determine if the edge is one-way, and a `toString` method
 * for human-readable representation of the edge type.
 */
trait EdgeType:
  /**
   * Indicates whether the edge type is one-way.
   *
   * @return true if the edge is directed (one-way), false if the edge is undirected.
   */
  def oneWay: Boolean

/**
 * The `Directed` object represents a directed edge type in a graph,
 * where the edge has a specific direction (one-way).
 * It extends the `EdgeType` trait and implements the `oneWay` method,
 * which always returns true to indicate that the edge is directed.
 */
object Directed extends EdgeType {
  /**
   * Indicates whether the edge is one-way (directed).
   *
   * @return true, as this method always returns true to signify that the edge is directed.
   */
  def oneWay: Boolean = true

  override def toString: String = "Directed"
}

/**
 * The `Undirected` object represents an undirected edge type in a graph.
 * It extends the `EdgeType` trait and overrides the `oneWay` method
 * to indicate that edges of this type are not one-way.
 */
object Undirected extends EdgeType {
  /**
   * Indicates whether the edge type is directional (one-way).
   *
   * @return false, as the edge type is not one-way.
   */
  def oneWay: Boolean = false

  override def toString: String = "Undirected"
}

/**
 * Represents the "Undefined" edge type in a graph.
 *
 * The `Undefined` edge type is considered undirected, as determined by the `oneWay` method,
 * which always returns `false`. This implies that there is no specific directionality
 * for edges of this type.
 *
 * This object provides a concrete implementation of the `EdgeType` trait, overriding the `oneWay`
 * method to indicate the lack of directionality.
 */
object Undefined extends EdgeType {
  /**
   * Determines if the edge type is one-way.
   *
   * This method always returns `false`, indicating that the edge type is undirected.
   *
   * @return false, indicating the edge type is not one-way.
   */
  def oneWay: Boolean = false

  override def toString: String = "Undefined"
}

/**
 * The `EdgeType` object provides utilities and definitions for working with edge types in a graph,
 * including traits and implicit parsers. Edge types include `Directed`, `Undirected`, and `Undefined`.
 *
 * It includes a companion object for parsing strings into `EdgeType` instances using the `ParseableEdgeType` trait.
 * This allows for flexible and structured parsing of textual representations of edge types into their corresponding instances.
 */
object EdgeType {

  /**
   * A trait that extends `Parseable` for parsing strings into instances of `EdgeType`.
   *
   * This trait provides specific parsing functionality and default values for `EdgeType`.
   * It includes methods to define the regex pattern for parsing, the default `EdgeType`
   * value, and a descriptive message about the type. The `parse` method attempts to
   * convert a given string into a valid `EdgeType` instance, returning failure if
   * the input does not conform to the expected format.
   */
  trait ParseableEdgeType extends Parseable[EdgeType] {
    /**
     * The regular expression used for parsing a `Parseable[T]`.
     *
     * @return A compiled regular expression.
     *         By default, the result matches one or more word characters (`\w+`).
     */
    def regex: Regex = """[=<->.]+""".r

    /**
     * Returns a message providing details about of type `T`.
     *
     * @return The message.
     */
    def message: String = "EdgeType"

    /**
     * Attempts to parse a string into an `EdgeType`.
     *
     * This method maps specific string patterns to predefined `EdgeType` instances:
     * - "." corresponds to `Directed`.
     * - "=" corresponds to `Undirected`.
     * If the string does not match these patterns, a failure is returned with an exception.
     *
     * @param s the input string to be parsed into an `EdgeType`
     * @return a `Try[EdgeType]` representing success with a parsed `EdgeType` or failure with an exception
     */
    def parse(s: String): Try[EdgeType] = s match {
      case ">" => Success(Directed)
      case "=" => Success(Undirected)
      case _ => Success(Undefined)
    }

    /**
     * Returns a default value of type `T`.
     *
     * @return the default value of type `T`, typically representing an "empty" or "none" state.
     */
    def none: EdgeType = Directed
  }

  /**
   * Companion object for the `ParseableEdgeType` trait, providing an implicit instance of it.
   *
   * This object enables the parsing of strings into instances of `EdgeType` using the functionality
   * defined in the `ParseableEdgeType` trait. It allows users to implicitly convert strings into
   * `EdgeType` objects by defining a default implicit instance of the parser.
   *
   * The `ParseableEdgeType` provides specific parsing rules for string representations of `EdgeType` and
   * ensures the parsing process adheres to defined formats and constraints. It includes methods for
   * matching specific patterns, returning descriptive messages, and handling invalid inputs gracefully.
   */
  implicit object ParseableEdgeType extends ParseableEdgeType
}