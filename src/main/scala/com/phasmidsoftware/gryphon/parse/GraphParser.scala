package com.phasmidsoftware.gryphon.parse

import com.phasmidsoftware.gryphon.core.Triplet
import com.phasmidsoftware.gryphon.util.FP.lift

import scala.util.parsing.combinator.JavaTokenParsers

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
class GraphParser[V: Parseable, E: Parseable] extends JavaTokenParsers {

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

  /**
   * Attempts to parse the given input string using the provided parser.
   * If the parsing is successful, the method wraps the result in an `Option`.
   * In case of a parsing failure or error, an appropriate error message is printed to
   * the standard error stream, and the method returns `None`.
   *
   * CONSIDER returning Try[T]
   *
   * @param parser The parser to be used for parsing the input string.
   * @param s      The input string to be parsed.
   * @return An `Option` containing the result of type `T` from the parser if parsing
   *         is successful, or `None` if parsing fails or encounters an error.
   */
  private def maybeParseAll[T](parser: Parser[T])(s: String): Option[T] = parseAll(parser, s) match {
    case this.Success(result, _) => Some(result)
    case this.NoSuccess.I(msg, _) => System.err.println(msg); None
  }

  private val vp = implicitly[Parseable[V]]
  private val ep = implicitly[Parseable[E]]

  /**
   * Parses an input string using the regular expression provided by `vp.regex`
   * and converts the matched result into a value of type `V` using the `Parseable.parser` method.
   * The input must match the regular expression for the parsing to succeed.
   *
   * @return A `Parser` that produces a value of type `V`. If the input matches the regular expression 
   *         and is successfully parsed into a value of type `V`, the result is returned by the parser. 
   *         Otherwise, parsing fails.
   */
  private def vop: Parser[V] = vp.regex ^^ Parseable.parser(vp)

  /**
   * Parses an optional element of type `E` using the regular expression provided by `ep.regex`
   * and converts the matched result into a value of type `E` using the `Parseable.parser` method.
   * The parsing process matches an optional occurrence (`opt`) of the pattern.
   *
   * @return A `Parser` that evaluates to an `Option` containing a value of type `E` if the input matches
   *         the regular expression and is successfully parsed. Returns `None` if no match is found.
   */
  private def eop: Parser[Option[E]] = opt(ep.regex) ^^ lift(Parseable.parser(ep))
}

/**
 * A specialized implementation of the `GraphParser` class for parsing graphs with edge attributes
 * that are represented as decimal numbers. This parser supports both integer and floating-point representations.
 *
 * This class overrides the default `edgeAttributeRegex` defined in `GraphParser` to provide
 * custom parsing logic for edge attributes that are decimal numbers.
 *
 * @tparam V the type of the vertices in the graph, which must have an implicit `Parseable[V]` instance
 * @tparam E the type of the edges in the graph, which must have an implicit `Parseable[E]` instance
 */
class DecimalGraphParser[V: Parseable, E: Parseable] extends GraphParser[V, E]

/**
 * A specialized exception class representing errors encountered during parsing processes.
 *
 * @param message The detail message explaining the cause or context of the exception.
 * @param t       The underlying throwable cause of the exception, if available.
 */
case class ParseException(message: String, t: Throwable) extends Exception(message, t)