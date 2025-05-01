package com.phasmidsoftware.gryphon.parse

import com.phasmidsoftware.gryphon.core.Triplet

import scala.util.Try

/**
 * A class that provides utilities for parsing graph-related data. It operates on vertices of type `V`
 * and edges of type `E`, where the parsing logic is defined through the `Parseable` typeclass.
 *
 * CONSIDER renaming (or splitting) this class
 *
 * This parser supports extracting pairs of vertices, triples (two vertices and an edge), and custom
 * parsing of vertices and edges in a graph representation.
 *
 * @tparam V the type of the vertices in the graph, which must have an implicit `Parseable[V]` instance
 * @tparam E the type of the edges in the graph, which must have an implicit `Parseable[E]` instance
 */
class GraphParser[V: Parseable, E: Parseable] extends BaseParser[V, E] {

  /**
   * Parses the given input string into a tuple `(V, V)` using the `pair` parser.
   * The parsing is performed via the `maybeParseAll` method and depends on the success of the `pair` parser.
   * If parsing succeeds, a `Success` containing the tuple `(V, V)` is returned.
   * If parsing fails, a `Failure` containing a `ParseException` is returned.
   *
   * @param s The input string to be parsed.
   * @return A `Try` containing a tuple `(V, V)` if parsing is successful, or a `Failure` with a `ParseException` if parsing fails.
   */
  def parsePair(s: String): Try[(V, V)] = maybeParseAll(pair)(s)

  /**
   * Parses the given input string into an `Option[Triplet[V, E]]` using the `triple` parser.
   * The parsing is performed via the `maybeParseAll` method. If the `triple` parser succeeds,
   * the result is an `Option[Triplet[V, E]]` wrapped in a `Success`. If parsing fails,
   * a `Failure` containing a `ParseException` is returned.
   *
   * @param s The input string to be parsed.
   * @return A `Try` containing an `Option[Triplet[V, E]]` if parsing succeeds, or a `Failure`
   *         with a `ParseException` if parsing fails.
   */
  def parseTriple(s: String): Try[Option[Triplet[V, E]]] = maybeParseAll(triple)(s)

  /**
   * Parses two consecutive `vop` elements from the input and combines their results into an optional tuple.
   * If both `vop` elements are successfully parsed and yield values, the result will contain a tuple with those values.
   * If either `vop` fails to yield a value, the result will be `None`.
   *
   * @return A parser that evaluates to an `Option` containing a tuple of two elements `(V, V)` if successful, or `None` otherwise.
   */
  def pair: Parser[(V, V)] =
    vop ~ vop ^^ { case x ~ y => (x, y) }

  /**
   * Parses a sequence consisting of two `V` elements followed by an `E` element and combines them into an `Option` tuple.
   * Each element is parsed using the `vop` and `eop` parsers, which are responsible for parsing parts of the input.
   * If all elements are successfully parsed, they are combined into the tuple `Triplet[V, E]`; otherwise, the result is `None`.
   *
   * @return A `Parser` that produces an `Option` containing a tuple `Triplet[V, E]` if all parts are successfully parsed, or `None` if any part fails.
   */
  def triple: Parser[Option[Triplet[V, E]]] =
    vop ~ vop ~ eop ^^ { case x ~ y ~ zo => zo match {
      case None => None
      case Some(z) => Some((x, y, z))
    }
    }
}

/**
 * A specialized exception class representing errors encountered during parsing processes.
 *
 * @param message The detail message explaining the cause or context of the exception.
 * @param t       The underlying throwable cause of the exception, if available.
 */
case class ParseException(message: String, t: Throwable) extends Exception(message, t)

/**
 * Companion object for the ParseException class.
 *
 * Provides utility methods for creating instances of the ParseException class.
 */
object ParseException {
  /**
   * Creates a new instance of the `ParseException` class with the specified message and no cause.
   *
   * @param message The detail message explaining the context or cause of the parsing error.
   * @return A new instance of `ParseException` initialized with the provided message.
   */
  def apply(message: String): ParseException = ParseException(message, null)
}