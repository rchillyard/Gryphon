package com.phasmidsoftware.gryphon.parse

import com.phasmidsoftware.gryphon.core.Triplet

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
   * Parses the given input string into an optional tuple of two elements `(V, V)`
   * using the `pair` parser.
   * The parsing result depends on the success of the `pair`
   * parser in matching the input string.
   *
   * @param s The input string to be parsed.
   * @return An `Option` containing a tuple `(V, V)` if the parsing is successful,
   *         or `None` if the parsing fails or encounters an error.
   */
  def parsePair(s: String): Option[(V, V)] = maybeParseAll(pair)(s)

  /**
   * Parses the given input string into an optional tuple `Triplet[V, E]` using the `triple` parser.
   * The parsing result depends on the success of the `triple` parser in matching the input string. 
   * If parsing is successful, a tuple `Triplet[V, E]` is returned as `Some`.
   * If parsing fails or encounters an error, `None` is returned, 
   * and the error message is printed to the standard error stream.
   *
   * @param s The input string to be parsed.
   * @return An `Option` containing a tuple `Triplet[V, E]` if parsing is successful,
   *         or `None` if parsing fails or encounters an error.
   */
  def parseTriple(s: String): Option[Triplet[V, E]] = maybeParseAll(triple)(s).flatten

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