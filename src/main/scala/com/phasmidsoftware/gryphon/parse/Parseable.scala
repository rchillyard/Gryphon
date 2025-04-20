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
