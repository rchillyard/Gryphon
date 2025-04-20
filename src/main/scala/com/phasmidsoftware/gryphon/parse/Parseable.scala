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
   * A trait that provides an implementation of the `Parseable` typeclass for the `String` type.
   *
   * This trait allows parsing a string input into an optional value of type `String`.
   * It is mainly used in contexts where type-safe and consistent string parsing is required.
   */
  trait ParseableString extends Parseable[String] {
    /**
     * Parses a string input and attempts to convert it into an optional value of type `String`.
     *
     * @param input The string to be parsed into an object of type `String`.
     * @return An `Option` containing the parsed object of type `String` if successful, or `None` if the parsing fails.
     */
    def parse(input: String): Option[String] = Option(input)
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