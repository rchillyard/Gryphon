package com.phasmidsoftware.gryphon.parse

import com.phasmidsoftware.gryphon.util.FP.lift

import scala.util.Try
import scala.util.parsing.combinator.JavaTokenParsers

/**
 * A base class for parsing operations that extends the Scala `JavaTokenParsers`.
 * This class provides utility methods and predefined parsers for parsing input strings
 * into specific types `V` and `E` using the `Parseable` typeclass.
 *
 * @tparam V The type of values to be parsed from the input, requiring an implicit `Parseable[V]`.
 * @tparam E The type of optional elements to be parsed from the input, requiring an implicit `Parseable[E]`.
 */
class BaseParser[V: Parseable, E: Parseable] extends JavaTokenParsers {
  /**
   * Attempts to parse the given input string using the specified parser.
   * If parsing succeeds, the result is wrapped in a `Success`.
   * If parsing fails, a `Failure` containing a `ParseException` is returned.
   *
   * @param parser The parser to be used for parsing the input string.
   * @param s      The input string to parse.
   * @return A `Try` containing the parsed result as `Success` if parsing succeeds, or
   *         a `Failure` with a `ParseException` if parsing fails.
   */
  def maybeParseAll[T](parser: Parser[T])(s: String): Try[T] = parseAll(parser, s) match {
    case this.Success(result, _) => scala.util.Success(result)
    case this.NoSuccess.I(msg, _) => scala.util.Failure(ParseException(msg))
  }

  private val vp = implicitly[Parseable[V]]

  /**
   * Parses an input string using the regular expression provided by `vp.regex`
   * and converts the matched result into a value of type `V` using the `Parseable.parser` method.
   * The input must match the regular expression for the parsing to succeed.
   *
   * @return A `Parser` that produces a value of type `V`. If the input matches the regular expression
   *         and is successfully parsed into a value of type `V`, the result is returned by the parser.
   *         Otherwise, parsing fails.
   */
  protected def vop: Parser[V] = vp.regex ^^ Parseable.parser

  private val ep = implicitly[Parseable[E]]

  /**
   * Parses an optional element of type `E` using the regular expression provided by `ep.regex`
   * and converts the matched result into a value of type `E` using the `Parseable.parser` method.
   * The parsing process matches an optional occurrence (`opt`) of the pattern.
   *
   * @return A `Parser` that evaluates to an `Option` containing a value of type `E` if the input matches
   *         the regular expression and is successfully parsed. Returns `None` if no match is found.
   */
  protected def eop: Parser[Option[E]] = opt(ep.regex) ^^ lift(Parseable.parser)

}
