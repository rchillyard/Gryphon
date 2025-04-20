package com.phasmidsoftware.gryphon.parse

import scala.util.matching.Regex
import scala.util.parsing.combinator.JavaTokenParsers

/**
 * A class that provides utilities for parsing graph-related data. It operates on vertices of type `V`
 * and edges of type `E`, where the parsing logic is defined through the `Parseable` typeclass.
 *
 * This parser supports extracting pairs of vertices, triples (two vertices and an edge), and custom
 * parsing of vertices and edges in a graph representation.
 *
 * @tparam V the type of the vertices in the graph, which must have an implicit `Parseable[V]` instance
 * @tparam E the type of the edges in the graph, which must have an implicit `Parseable[E]` instance
 */
class GraphParser[V: Parseable, E: Parseable] extends JavaTokenParsers {

  /**
   * Parses a given input string and attempts to extract a pair of elements of type `V`.
   *
   * @param s the input string to be parsed, which is expected to represent a pair of elements.
   * @return a `ParseResult` encapsulating an `Option` that contains the pair of parsed elements of type `V`
   *         if the input is successfully parsed, or `None` otherwise.
   */
  def parsePair(s: String): ParseResult[Option[(V, V)]] = parseAll(pair, s)

  /**
   * Parses the given string input into an optional triple of vertices and an edge of the format (V, V, E),
   * using the defined parsing logic within the `triple` parser.
   *
   * @param s The input string to be parsed.
   * @return A `ParseResult` containing an `Option` with the parsed triple (V, V, E) if successful, 
   *         or `None` if the parsing fails.
   */
  def parseTriple(s: String): ParseResult[Option[(V, V, E)]] = parseAll(triple, s)

  /**
   * Parses two consecutive `vop` elements from the input and combines their results into an optional tuple.
   * If both `vop` elements are successfully parsed and yield values, the result will contain a tuple with those values.
   * If either `vop` fails to yield a value, the result will be `None`.
   *
   * @return A parser that evaluates to an `Option` containing a tuple of two elements `(V, V)` if successful, or `None` otherwise.
   */
  def pair: Parser[Option[(V, V)]] =
    vop ~ vop ^^ { case xo ~ yo => for (x <- xo; y <- yo) yield (x, y) }

  /**
   * Parses a sequence consisting of two `V` elements followed by an `E` element and combines them into an `Option` tuple.
   * Each element is parsed using the `vop` and `eop` parsers, which are responsible for parsing parts of the input.
   * If all elements are successfully parsed, they are combined into the tuple `(V, V, E)`; otherwise, the result is `None`.
   *
   * @return A `Parser` that produces an `Option` containing a tuple `(V, V, E)` if all parts are successfully parsed, or `None` if any part fails.
   */
  def triple: Parser[Option[(V, V, E)]] =
    vop ~ vop ~ eop ^^ { case xo ~ yo ~ zo => for (x <- xo; y <- yo; z <- zo) yield (x, y, z) }

  /**
   * A regular expression used to match attributes of vertices in the graph.
   * This should be overridden when anything other than the default pattern is required.
   *
   * @return A `Regex` pattern that matches one or more word characters (`\w+`).
   */
  def vertexAttributeRegex: Regex = """\w+""".r

  /**
   * A regular expression used to match attributes of edges in the graph.
   * This should be overridden when anything other than the default pattern is required.
   *
   * @return A `Regex` pattern that matches one or more word characters (`\w+`).
   */
  def edgeAttributeRegex: Regex = """\w+""".r

  private val vp = implicitly[Parseable[V]]
  private val ep = implicitly[Parseable[E]]

  /**
   * Parses an optional vertex attribute from the input string using a regular expression 
   * defined by `vertexAttributeRegex` and attempts to convert it into a value of type `V`.
   * The conversion is performed by the `parse` method provided by the `Parseable` instance for type `V`.
   *
   * @return A `Parser` that produces an `Option[V]`. Returns `Some(value)` if the input matches 
   *         the regular expression and successfully parses into a value of type `V`. 
   *         Returns `None` if the input does not match or parsing fails.
   */
  private def vop: Parser[Option[V]] = opt(vertexAttributeRegex) ^^ (vo => vo flatMap vp.parse)

  /**
   * Parses an optional edge attribute from the input string using a regular expression 
   * defined by `edgeAttributeRegex` and attempts to convert it into a value of type `E`.
   * The conversion is performed by the `parse` method provided by the `Parseable` instance for type `E`.
   *
   * @return A `Parser` that produces an `Option[E]`. Returns `Some(value)` if the input matches 
   *         the regular expression and successfully parses into a value of type `E`. 
   *         Returns `None` if the input does not match or parsing fails.
   */
  private def eop: Parser[Option[E]] = opt(edgeAttributeRegex) ^^ (vo => vo flatMap ep.parse)
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
class DecimalGraphParser[V: Parseable, E: Parseable] extends GraphParser[V, E]:
  /**
   * A regular expression used to match attributes of edges in the graph. This overrides the default implementation
   * to match decimal numbers, including integers, floating-point numbers with an optional leading or trailing zero,
   * and numbers in scientific notation.
   *
   * @return A `Regex` pattern that matches decimal numbers, allowing both integers (e.g., "42") and floating-point
   *         numbers (e.g., "3.14" or ".5").
   */
  override def edgeAttributeRegex: Regex = """(\d+(\.\d*)?|\d*\.\d+)""".r