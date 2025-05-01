package com.phasmidsoftware.gryphon.parse

import com.phasmidsoftware.gryphon.util.FP

import scala.util.matching.Regex
import scala.util.{Success, Try}

/**
 * A trait that defines a typeclass for parsing strings into objects of type `T`.
 *
 * Classes or objects implementing this trait must provide an implementation for 
 * the `parse` method, which takes a string input and converts it into an instance 
 * of the type parameter `T`.
 *
 * @tparam T The type of the object that the input string will be parsed into.
 */
trait Parseable[T] {

  /**
   * Parses the input string into an instance of type `T`.
   * The parsing process utilizes the logic provided by the implementation of the `parse` method
   * in the corresponding `Parseable[T]` instance.
   *
   * @param input the string to be parsed into an instance of type `T`
   * @return a `Try` wrapping the resulting parsed object of type `T` if successful,
   *         or a `Failure` with the corresponding exception if parsing fails
   */
  def parse(input: String): Try[T]

  /**
   * The regular expression used for parsing a `Parseable[T]`.
   *
   * @return A compiled regular expression.
   *         By default, the result matches one or more word characters (`\w+`).
   */
  def regex: Regex

  /**
   * Returns a message providing details about of type `T`.
   *
   * @return The message.
   */
  def message: String
}

/**
 * This object contains several implementations of the `Parseable` typeclass for specific types.
 *
 * It provides implicit objects for parsing strings into common types, such as `String`, `Int`, and `Double`.
 * These implementations can be used where implicit conversions are required for parsing operations.
 */
object Parseable {

  /**
   * Parses a string input into an instance of type `T` using the implicitly provided `Parseable[T]` typeclass.
   * Utilizes the `Parseable[T]#parse` method to attempt parsing the input, throwing a `ParseException`
   * with detailed information if the parsing fails.
   *
   * @tparam T The target type to which the string is parsed.
   * @return A function that takes a `String` input and returns an instance of type `T`.
   *         If parsing fails, a `ParseException` is thrown.
   */
  def parser[T: Parseable]: String => T =
    w =>
      val parseable = implicitly[Parseable[T]]
      parseable.parse(w) match {
        case util.Failure(exception) => throw ParseException(s"Failed to parse \"$w\" as ${parseable.message}", exception)
        case util.Success(value) => value
      }

  /**
   * A trait that provides a specific implementation of the `Parseable` typeclass for the `Unit` type.
   *
   * This implementation of `Parseable` for `Unit` treats any input string as a valid parseable input and
   * always returns `Some(())`, regardless of the content of the input string.
   * Parsing inherently succeeds for this type since `Unit` carries no meaningful information.
   *
   * This trait can be used wherever `Parseable[Unit]` is required to signify that parsing is always successful
   * and does not depend on the input string.
   *
   * This typeclass is defined as a trait, allowing it to be mixed into other components or implemented as an object.
   *
   * Extends:
   * - `Parseable[Unit]`: Defines the generic contract for parsing a string into a `Unit` value.
   *
   * Methods:
   * - `parse(w: String): Option[Unit]`: Always returns `Some(())`, ignoring the input string.
   *
   * Use this implementation in contexts where parsing behavior is trivial and the result is guaranteed.
   */
  trait ParseableUnit extends Parseable[Unit] {
    /**
     * Ignores the given string and returns an Option containing Unit.
     *
     * @param w the input string to be parsed (ignored)
     * @return an Option containing Unit, which is always Some(())
     */
    def parse(w: String): Try[Unit] = FP.assert(w == "()")("not a valid Unit")(w)

    /**
     * The regular expression used for parsing a `Parseable[Unit]`.
     *
     * @return A compiled regular expression.
     *         The result matches one or more word characters (`()`).
     */
    override def regex: Regex = """\(\)""".r

    /**
     * Returns a constant string representation of the `Unit` type.
     *
     * @return the string "Unit"
     */
    def message: String = "Unit"
  }

  /**
   * An implicit object that provides a `Parseable` implementation for the `Unit` type.
   * This object enables parsing of strings into `Unit` values using the `Parseable` typeclass.
   *
   * The `parse` method always returns `Some(())` regardless of the input string,
   * since `Unit` does not encode any meaningful data.
   *
   * It is commonly used in scenarios where the parsing result itself is irrelevant,
   * and only the fact that parsing occurred successfully matters.
   */
  implicit object ParseableUnit extends ParseableUnit

  /**
   * A trait that provides an implementation of the `Parseable` typeclass for the `Boolean` type.
   *
   * This trait defines a mechanism for parsing string representations of boolean values
   * ("true" or "false") into corresponding `Boolean` objects.
   * If the input string can be successfully converted into a boolean,
   * the result is wrapped in `Some`.
   * Otherwise, the result is `None`.
   *
   * The parsing relies on the standard library method `toBooleanOption` to perform
   * the string conversion, which is case-sensitive and adheres strictly to "true" and "false".
   */
  trait ParseableBoolean extends Parseable[Boolean] {
    /**
     * Parses the input string into an `Option[Boolean]` based on its content.
     * The method interprets the string as a boolean value ("true" or "false")
     * and returns it as an `Option`.
     * If the string cannot be interpreted as a valid boolean, the method returns `None`.
     *
     * @param w The input string to parse into a boolean value.
     * @return An `Option[Boolean]` containing `Some(true)` or `Some(false)` if parsing is successful,
     *         or `None` if the string is not a valid representation of a boolean.
     */
    def parse(w: String): Try[Boolean] = Try(w.toBoolean)

    /**
     * Provides a message describing the type supported by this implementation.
     *
     * @return A string representation of the type, in this case "Boolean".
     */
    def message: String = "Boolean"

    /**
     * The regular expression used for parsing a `Parseable[T]`.
     *
     * @return A compiled regular expression.
     *         By default, the result matches one or more word characters (`\w+`).
     */
    override def regex: Regex = """yes|true|no|false/i""".r
  }

  /**
   * Provides an implicit implementation of the `Parseable` typeclass for the `Boolean` type.
   *
   * This object defines a method to parse a `String` into a `Boolean` value using the `toBooleanOption` method.
   * It is used as an implicit instance where the `Parseable[Boolean]` is required.
   *
   * Parsing accepts strings like "true" or "false" (case-insensitive), and returns
   * an `Option` containing the corresponding `Boolean` value if successful.
   *
   * Extends the `ParseableBoolean` trait, which provides the parsing logic for the `Boolean` type.
   */
  implicit object ParseableBoolean extends ParseableBoolean

  /**
   * A trait that provides an implementation of the `Parseable` typeclass for the `String` type.
   *
   * This trait allows parsing a string input into an optional value of type `String`.
   * It is mainly used in contexts where type-safe and consistent string parsing is required.
   */
  trait ParseableString extends Parseable[String] {
    /**
     * Parses the input string and returns an optional string based on the validity of the input.
     * If the input string is non-null and non-empty, it is wrapped in an `Option` and returned.
     * Otherwise, `None` is returned.
     *
     * @param input the input string to parse. It can be null or empty.
     * @return an `Option[String]` containing the input string if it is non-null and non-empty, or `None` otherwise.
     */
    def parse(input: String): Try[String] = FP.assert(input != null && input.nonEmpty)("not a valid String")(input)

    /**
     * Retrieves a predefined string message.
     *
     * @return A string message, "String".
     */
    def message: String = "String"

    /**
     * The regular expression used for parsing a `Parseable[String]`.
     *
     * @return A compiled regular expression.
     *         The result matches one or more word characters (`\w+`).
     */
    def regex: Regex = """\w+""".r
  }

  /**
   * Provides an implicit implementation of the `Parseable` typeclass for the `String` type.
   *
   * This object defines a mechanism for parsing a string input into an optional `String` value.
   * It enables type-safe and consistent string parsing in contexts where the `Parseable` typeclass is used.
   */
  implicit object ParseableString extends ParseableString

  /**
   * A trait that provides a specialized implementation of the `Parseable` typeclass for parsing
   * strings into integer (`Int`) values.
   *
   * Classes or objects implementing this trait can parse a string input and convert it into an `Option[Int]`,
   * where `Some(value)` represents a successfully parsed integer, and `None` indicates a failed parsing attempt.
   *
   * This trait extends `Parseable[Int]`, inheriting its generic parsing behavior for the `Int` type.
   */
  trait ParseableInt extends Parseable[Int] {
    /**
     * Parses a string input and attempts to convert it into an optional value of type `Int`.
     *
     * @param input The string to be parsed into an object of type `Int`.
     * @return An `Option` containing the parsed object of type `Int` if successful, or `None` if the parsing fails.
     */
    def parse(input: String): Try[Int] = Try(input.toInt)

    /**
     * Retrieves a string representation of the type `Int`.
     *
     * @return A string value `"Int"` that represents the type.
     */
    def message: String = "Int"

    /**
     * A regular expression pattern for matching integer values in a string.
     *
     * The regex matches optional negative signs followed by one or more digits.
     *
     * @return A `Regex` instance that represents the pattern for matching integers.
     */
    def regex: Regex = """-?\d+""".r
  }

  /**
   * An implicit object that provides a specific implementation of the `Parseable[Int]` typeclass.
   * This object allows for parsing string inputs into optional integer values using the `parse` method.
   *
   * It serves as an instance of the `ParseableInt` trait and automatically provides parsing capabilities
   * for `Int` type values wherever an implicit `Parseable[Int]` is required.
   *
   * The parsing functionality uses the `toIntOption` method for the conversion, which returns `Some[Int]`
   * if the input string represents a valid integer, or `None` otherwise.
   */
  implicit object ParseableInt extends ParseableInt

  /**
   * A trait that provides parsing functionality for strings into `Double` values.
   *
   * This trait extends the `Parseable` typeclass, specifically parameterized for `Double`.
   * It includes a method for parsing strings into optional `Double` values.
   */
  trait ParseableDouble extends Parseable[Double] {
    /**
     * Parses a string input and attempts to convert it into an optional value of type `Double`.
     *
     * @param input The string to be parsed into an object of type `Double`.
     * @return An `Option` containing the parsed object of type `Double` if successful, or `None` if the parsing fails.
     */
    def parse(input: String): Try[Double] = Try(input.toDouble)

    /**
     * A method that returns a string representation associated with this trait.
     *
     * @return The string "Double".
     */
    def message: String = "Double"

    /**
     * A regular expression used to match numeric values in a string, including integers, floating-point numbers,
     * and numbers with an optional leading or trailing zero.
     *
     * @return A `Regex` pattern that matches valid numeric representations, such as "42", "3.14", or ".5".
     */
    override def regex: Regex = """(\d+(\.\d*)?|\d*\.\d+)""".r
  }

  /**
   * An implicit object that provides an implementation of the `Parseable` typeclass for `Double`.
   *
   * This allows strings to be parsed into `Double` values wherever an implicit `Parseable[Double]` instance
   * is required, such as in parsing operations for graph edges or numerical data.
   */
  implicit object ParseableDouble extends ParseableDouble
}