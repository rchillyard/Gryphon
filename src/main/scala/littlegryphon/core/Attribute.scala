package littlegryphon.core

/**
 * Hierarchical trait defining a (single) attribute of type A.
 *
 * @tparam A an attribute
 */
trait Attribute[+A] {
  def attribute: A
}

trait Attributed[T, A] {
  def attribute(t: T): A
}


