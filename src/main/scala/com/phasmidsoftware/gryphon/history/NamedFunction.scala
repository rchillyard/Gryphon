package com.phasmidsoftware.gryphon.history

/**
 * A case class that represents a named function.
 *
 * This class extends the function trait `(T => R)` and associates 
 * a given name with the function. This can be useful for debugging,
 * logging, or providing human-readable context to the function's purpose.
 *
 * @param name The name of the function, represented as a string.
 * @param f    The actual function of type `T => R`.
 * @tparam T The input type of the function.
 * @tparam R The return type of the function.
 */
case class NamedFunction[T, R](name: String)(f: T => R) extends (T => R) {
  def apply(t: T): R = f(t)
}
