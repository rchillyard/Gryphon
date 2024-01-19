package littlegryphon.core

/**
 * Hierarchical trait defining a (single) attribute of type A.
 *
 * @tparam A an attribute
 */
trait Attribute[+A] {
  def attribute: A
}

/**
 * Typeclass trait to define behavior of an object with an attribute.
 *
 * @tparam T the object type.
 * @tparam A the attribute type.
 */
trait Attributed[T, A] {
  def attribute(t: T): A
}


