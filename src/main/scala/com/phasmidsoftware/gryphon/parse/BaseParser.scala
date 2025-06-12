package com.phasmidsoftware.gryphon.parse

import com.phasmidsoftware.gryphon.util.FP.lift

import scala.util.Try
import scala.util.parsing.combinator.JavaTokenParsers

/**
 * A base class for parsing operations that extends the Scala `JavaTokenParsers`.
 * This class provides utility methods and predefined parsers for parsing input strings
 * into specific types `V` and `E` using the `Parseable` typeclass.
 *
 * @tparam V The type of vertex-attribute values to be parsed from the input, requiring an implicit `Parseable[V]`.
 * @tparam E The type of edge-attribute values to be parsed from the input, requiring an implicit `Parseable[E]`.
 * @tparam Z The type of edge-type values to be parsed from the input, requiring an implicit `Parseable[Z]`.
 */
class BaseParser[V: Parseable, E: Parseable, Z: Parseable] extends JavaTokenParsers {
  /**
   * Attempts to parse the provided input string `s` using the given optional parser `parser`
   * followed by an optional `comment`.
   * If `parser` succeeds, then any subsequent comment is ignored, and the result is a `Success`.
   * The method handles cases where parsing is successful, where parsing yields no result,
   * or where an error occurs during parsing, and returns the corresponding `Try[T]`.
   *
   * @param parser The parser of type `Parser[T]` used to parse the input string.
   * @param s      The input string to be parsed that may (or may not) end with a comment.
   * @return A `Try` instance containing the parsed result of type `T` if parsing succeeds,
   *         or a `Failure` containing an exception if parsing fails or the input is empty.
   *         In particular, a comment line, or a blank line will result in a `Failure`
   *         containing an `EmptyStringException`.
   */
  def tryParseAll[T](parser: Parser[T])(s: String): Try[T] =
    parseAll(opt(parser) ~ opt(comment), s) match {
      case this.Success(Some(result) ~ _, _) =>
        scala.util.Success(result)
      case this.Success(None ~ Some(w), _) =>
        scala.util.Failure(EmptyStringException(w))
      case this.Success(None ~ _, _) =>
        scala.util.Failure(EmptyStringException(s))
      case this.NoSuccess.I(msg, _) =>
        scala.util.Failure(ParseException(msg))
    }

  private val vp = implicitly[Parseable[V]]
  private val ep = implicitly[Parseable[E]]
  private val zp = implicitly[Parseable[Z]]

  /**
   * Parses an input string using the regular expression provided by `vp.regex`
   * and converts the matched result into a value of type `V` using the `Parseable.parser` method.
   * The input must match the regular expression for the parsing to succeed.
   *
   * @return A `Parser` that produces a value of type `V`. If the input matches the regular expression
   *         and is successfully parsed into a value of type `V`, the result is returned by the parser.
   *         Otherwise, parsing fails.
   */
  protected def vertex: Parser[V] =
    vp.regex ^^ Parseable.parser

  /**
   * Parses an optional element of type `E` using the regular expression provided by `ep.regex`
   * and converts the matched result into a value of type `E` using the `Parseable.parser` method.
   * The parsing process matches an optional occurrence (`opt`) of the pattern.
   *
   * @return A `Parser` that evaluates to an `Option` containing a value of type `E` if the input matches
   *         the regular expression and is successfully parsed. Returns `None` if no match is found.
   */
  protected def maybeEdge: Parser[Option[E]] =
    opt(ep.regex) ^^ lift(Parseable.parser)

  /**
   * Parses an optional element of type `Z` using the regular expression provided by `zp.regex`
   * and converts the matched result into a value of type `Z` using the `Parseable.parser` method.
   * The parsing process matches an optional occurrence (`opt`) of the pattern.
   *
   * @return A `Parser` that evaluates to an `Option` containing a value of type `Z` if the input matches
   *         the regular expression and is successfully parsed. Returns `None` if no match is found.
   */
  protected def maybeZ: Parser[Option[Z]] =
    opt(zp.regex) ^^ lift(Parseable.parser)

  /**
   * NOTE: only used by unit tests.
   * Combines two optional parsers: one for a vertex (`vertex`) and another for a comment (`comment`).
   * The resulting parser produces a tuple, where the first element is an `Option[V]`
   * representing the parsed vertex (if present), and the second element is an `Option[String]`
   * representing the parsed comment (if present).
   *
   * @return A parser that evaluates to a tuple consisting of an optional vertex and an optional comment.
   */
  def optional: Parser[Option[V] ~ Option[String]] = opt(vertex) ~ opt(comment)

  /**
   * Parses a comment line from the input string.
   * Matches any characters following the `commentToken` until the end of the line.
   *
   * @return A `Parser` that evaluates to a `String` containing the content of the comment line.
   */
  def comment: Parser[String] = commentToken ~> """.*$""".r

  private val commentToken = "//"
}
