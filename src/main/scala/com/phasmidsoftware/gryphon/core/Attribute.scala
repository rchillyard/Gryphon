package com.phasmidsoftware.gryphon.core

/**
 * Hierarchical trait defining a (single) attribute of type A.
 *
 * Used as a base trait by `Vertex` and `Edge` to expose their defining attribute.
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
 * A trait that extends `Attribute` to enable comparison between attributes of type `A` and others of compatible types.
 *
 * @tparam A the type of the attribute that this trait represents. Must be comparable using Scala's `Ordering`.
 */
trait OrderedAttribute[A] extends Attribute[A] {
  /**
   * Compares this attribute with another attribute, returning an integer to indicate
   * their relative order. The comparison is performed using an implicit `Ordering`
   * for the type of the attributes.
   *
   * @param other the other `Attribute` instance to compare against.
   *              It must have an attribute of type `T`, where `T` is a supertype of `A`
   *              and has an implicit `Ordering` available.
   * @return an integer indicating the relative order of the two attributes:
   *         - a negative integer if this attribute is less than the `other` attribute,
   *         - zero if they are equal,
   *         - a positive integer if this attribute is greater than the `other` attribute.
   */
  def compare[T >: A : Ordering](other: Attribute[T]): Int =
    implicitly[Ordering[T]].compare(attribute, other.attribute)
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
