package com.phasmidsoftware.gryphon.core

/**
 * Hierarchical trait defining a (single) attribute of type A.
 *
 * @tparam A an attribute
 */
trait Attribute[+A] {
  /**
   * Retrieves the attribute of type `A` associated with this instance.
   *
   * @return the attribute of type `A` representing the defining characteristic
   *         or property of this instance.
   */
  def attribute: A
}

/**
 * A type-class trait defining a mechanism to associate an attribute of type `A` with an entity of type `T`.
 *
 * The `Attributed` trait provides an abstraction to retrieve an attribute from a given object.
 * It is intended to be mixed into types that need to support mapping an item to its associated attribute.
 *
 * @tparam T the type of the entity with which the attribute is associated.
 * @tparam A the type of the attribute that can be retrieved from the entity.
 */
trait Attributed[T, A] {
  /**
   * Retrieves the attribute of type `A` associated with the provided entity of type `T`.
   *
   * @param t the entity of type `T` for which the associated attribute is to be retrieved.
   * @return the attribute of type `A` associated with the given entity.
   */
  def attribute(t: T): A
}


