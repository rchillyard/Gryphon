package com.phasmidsoftware.gryphon.parse

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
   * Parses a string input and attempts to convert it into an optional value of type `T`.
   *
   * @param input The string to be parsed into an object of type `T`.
   * @return An `Option` containing the parsed object of type `T` if successful, or `None` if the parsing fails.
   */
  def parse(input: String): Option[T]
}

/**
 * This object contains several implementations of the `Parseable` typeclass for specific types.
 *
 * It provides implicit objects for parsing strings into common types, such as `String`, `Int`, and `Double`.
 * These implementations can be used where implicit conversions are required for parsing operations.
 */
object Parseable {
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
    def parse(w: String): Option[Unit] = Some(())
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
    def parse(w: String): Option[Boolean] = w.toBooleanOption
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
    def parse(input: String): Option[String] = Option.when(input != null && input.nonEmpty)(input)
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
    def parse(input: String): Option[Int] = input.toIntOption
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
    def parse(input: String): Option[Double] = input.toDoubleOption
  }

  /**
   * An implicit object that provides an implementation of the `Parseable` typeclass for `Double`.
   *
   * This allows strings to be parsed into `Double` values wherever an implicit `Parseable[Double]` instance
   * is required, such as in parsing operations for graph edges or numerical data.
   */
  implicit object ParseableDouble extends ParseableDouble
}