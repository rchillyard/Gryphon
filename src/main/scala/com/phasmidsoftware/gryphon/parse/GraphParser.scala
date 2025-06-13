package com.phasmidsoftware.gryphon.parse

import com.phasmidsoftware.gryphon.core.Triplet
import com.phasmidsoftware.gryphon.util.FP
import com.phasmidsoftware.gryphon.util.FP.sequence

import scala.io.Source
import scala.util.Try

/**
 * A class for parsing graph-related data structures, including pairs of vertices and triplets (vertices connected by an edge).
 * This class extends `BaseParser` and leverages its parsing utilities to handle graph-specific parsing tasks.
 *
 * @tparam V The type of vertex-attribute values to be parsed, requiring an implicit `Parseable[V]`.
 * @tparam E The type of edge-attribute values to be parsed, requiring an implicit `Parseable[E]`.
 * @tparam Z The type of edge-type values to be parsed, requiring an implicit `Parseable[Z]`.
 */
class GraphParser[V: Parseable, E: Parseable, Z: Parseable] extends BaseParser[V, E, Z] {

  /**
   * Parses the given input string into a tuple `(V, V)` using the `pair` parser.
   * The parsing is performed via the `tryParseAll` method and depends on the success of the `pair` parser.
   * If parsing succeeds, a `Success` containing the tuple `(V, V)` is returned.
   * If parsing fails, a `Failure` containing a `ParseException` is returned.
   *
   * @param s The input string to be parsed.
   * @return A `Try` containing a tuple `(V, V)` if parsing is successful, or a `Failure` with a `ParseException` if parsing fails.
   */
  def parsePair(s: String): Try[(V, V, Option[Z])] =
    tryParseAll(pair)(s)

  /**
   * Parses the given input string into an `Option[Triplet[V, E]]` using the `triplet` parser.
   * The parsing is performed via the `tryParseAll` method. If the `triplet` parser succeeds,
   * the result is an `Option[Triplet[V, E]]` wrapped in a `Success`. If parsing fails,
   * a `Failure` containing a `ParseException` is returned.
   *
   * @param s The input string to be parsed.
   * @return A `Try` containing an `Option[Triplet[V, E]]` if parsing succeeds, or a `Failure`
   *         with a `ParseException` if parsing fails.
   */
  def parseTriple(s: String): Try[Triplet[V, E, Z]] =
    tryParseAll(triplet)(s)

  /**
   * Parses an iterator of strings into an iterator of `Try[Token]` using the provided string parser.
   * Each string in the input iterator is processed by the `stringParser` function, which attempts to convert it into a `Token`.
   *
   * @param stringParser A function that takes a string as input and returns a `Try` containing a parsed token of type `Token`.
   * @param ws           An iterator of strings to be parsed.
   * @return An iterator of `Try[Token]`, where each element represents the result of parsing the corresponding input string.
   */
  def parseIterator[Token](stringParser: String => Try[Token])(ws: Iterator[String]): Iterator[Try[Token]] =
    ws map stringParser

  /**
   * Parses a sequence of strings using the provided string parser and returns a `Try` of a sequence of parsed tokens.
   *
   * @param stringParser A function that takes a string as input and returns a `Try` containing a parsed token of type `Token`.
   * @param ws           An iterator of strings to be parsed.
   * @return A `Try` containing a sequence of successfully parsed tokens if all inputs are valid,
   *         or a `Failure` containing a `ParseException` if there are invalid inputs.
   */
  def parseStrings[Token](stringParser: String => Try[Token])(ws: Iterator[String]): Try[Seq[Token]] = {
    val (good, bad) = FP.partition(parseIterator(stringParser)(ws) to Seq)

    val ok = bad.isEmpty || bad.forall {
      case scala.util.Failure(_: EmptyStringException) => true
      case _ => false
    }

    if (ok)
      sequence(good)
    else
      scala.util.Failure(ParseException(s"Bad input: $bad"))
  }

  def parseSource[Token](stringParser: String => Try[Token])(source: Source): Try[Seq[Token]] =
    parseStrings(stringParser)(source.getLines())

  /**
   * Parses two consecutive `vertex` elements from the input and combines their results into an optional tuple.
   * If both `vertex` elements are successfully parsed and yield values, the result will contain a tuple with those values.
   * If either `vertex` fails to yield a value, the result will be `None`.
   *
   * @return A parser that evaluates to an `Option` containing a tuple of two elements `(V, V)` if successful, or `None` otherwise.
   */
  def pair: Parser[(V, V, Option[Z])] =
    vertex ~ maybeZ ~ vertex ^^ { case x ~ zo ~ y => (x, y, zo) }

  /**
   * Parses a sequence consisting of two `V` elements followed by an `E` element and combines them into an `Option` tuple.
   * Each element is parsed using the `vertex` and `maybeEdge` parsers, which are responsible for parsing parts of the input.
   * If all elements are successfully parsed, they are combined into the tuple `Triplet[V, E]`; otherwise, the result is `None`.
   *
   * @return A `Parser` that produces an `Option` containing a tuple `Triplet[V, E]` if all parts are successfully parsed, or `None` if any part fails.
   */
  def triplet: Parser[Triplet[V, E, Z]] =
    vertex ~ maybeZ ~ vertex ~ maybeEdge ^^ { case x ~ zo ~ y ~ eo => zo match {
      case Some(z) =>
        Triplet(x, y, eo, z)
      case None =>
        Triplet(x, y, eo, implicitly[Parseable[Z]].none)
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
  def apply(message: String): ParseException =
    ParseException(message, null)
}

/**
 * Exception representing an error when an empty string is encountered where it is not allowed.
 *
 * @param message The error message that provides additional information about the exception.
 */
case class EmptyStringException(message: String) extends Exception(message)
