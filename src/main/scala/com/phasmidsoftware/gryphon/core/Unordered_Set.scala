package com.phasmidsoftware.gryphon.core

/**
 * A case class representing an unordered collection of elements in the form of a Set.
 *
 * @tparam T the type of the elements contained in the set.
 * @param elements a set of elements of type `T` forming the basis of the adjacency set.
 *
 *                 This class extends the `Unordered` trait, providing a concrete 
 *                 implementation for unordered data structures.
 *
 *                 The `Unordered_Set` is immutable, meaning that any operation resulting
 *                 in modification (such as adding a new element) will return a new instance 
 *                 of `Unordered_Set` without changing the original instance.
 */
case class Unordered_Set[T](elements: Set[T]) extends Unordered[T]:
  /**
   * Checks if the set of elements is empty.
   *
   * @return `true` if the set contains no elements, otherwise `false`.
   */
  def isEmpty: Boolean = elements.isEmpty

  /**
   * Returns the number of elements in the set.
   *
   * @return the size of the set as an integer.
   */
  def size: Int = elements.size

  /**
   * Checks whether the specified element is contained within the set.
   *
   * @param u the element to search for within the set. Its type must conform to or extend
   *          the type `T` of the elements in the set, or be a supertype of `T`.
   * @return `true` if the element exists within the set, otherwise `false`.
   */
  def contains[U >: T](u: U): Boolean = elements.asInstanceOf[Set[U]].contains(u)

  /**
   * Returns an iterator over the elements in the adjacency set.
   * NOTE that the order of the elements in the iterator is unpredictable.
   *
   * @return an `Iterator[T]` over the elements of type `T` contained in the set.
   */
  def iterator: Iterator[T] = elements.iterator

  /**
   * Adds the specified element to the unordered collection, returning a new instance 
   * of the collection with the element included. The original collection remains unchanged.
   *
   * @param u the element to be added to the collection. Its type must conform to or extend
   *          the type `T` of the elements in the collection, or be a supertype of `T`.
   * @return a new `Unordered` instance containing the specified element along with the
   *         elements of the original collection.
   * @tparam U a supertype of `T` that represents the type of the element being added.
   */
  def +[U >: T](u: U): Unordered[U] = copy(elements = elements.asInstanceOf[Set[U]] + u)

/**
 * A companion object for `Unordered_Set`, providing factory methods to create instances of
 * unordered collections.
 */
object Unordered_Set {
  /**
   * Creates an empty unordered collection.
   *
   * The method initializes and returns an instance of `Unordered[X]` with no elements.
   * This serves as a starting point for creating immutable, unordered collections without any content.
   *
   * @return an empty instance of `Unordered[X]`.
   */
  def empty[X]: Unordered[X] = Unordered_Set(Set.empty)

  /**
   * Creates an instance of `Unordered` from a sequence of elements.
   *
   * The elements in the provided sequence are converted into a `Set`, ensuring the resulting
   * unordered collection contains unique elements with no particular order.
   *
   * @param xs the sequence of elements to be converted into an unordered collection
   * @tparam X the type of elements contained in the input sequence and resulting `Unordered`
   *           collection
   * @return an instance of `Unordered` containing the unique elements from the input sequence
   */
  def apply[X](xs: Seq[X]): Unordered[X] = new Unordered_Set(xs.toSet)

  /**
   * Creates a new instance of `Unordered[X]` populated with the provided elements.
   *
   * @param xs the elements to include in the unordered collection.
   *           These elements are provided as a variable-length argument list, 
   *           allowing the creation of an unordered collection containing one or more elements.
   * @tparam X the type of the elements contained within the unordered collection.
   * @return an instance of `Unordered[X]` containing the provided elements.
   */
  def create[X](xs: X*): Unordered[X] = apply(xs)
}
