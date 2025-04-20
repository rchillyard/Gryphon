package com.phasmidsoftware.gryphon.core

/**
 * A trait representing an abstract data type for unordered collections of elements.
 *
 * @tparam T the type of elements contained within this collection (covariant).
 *
 *           This trait provides the foundation for implementing unordered data structures by 
 *           defining fundamental operations such as checking for containment, iterating over 
 *           elements, and adding new elements. Implementations of this trait must ensure that 
 *           the order of elements is not relevant or preserved.
 */
trait Unordered[+T] {
  /**
   * Determines whether the collection is empty.
   *
   * This method checks if the unordered collection contains any elements. 
   * It returns `true` if the collection has no elements, and `false` otherwise.
   *
   * @return true if the collection is empty, false otherwise
   */
  def isEmpty: Boolean

  /**
   * Returns the number of elements contained in the unordered collection.
   *
   * @return the total count of elements within the collection as an integer.
   */
  def size: Int

  /**
   * Checks if the specified element is contained within the collection.
   *
   * @param u the element to check for existence in the collection
   * @return true if the element exists in the collection, otherwise false
   */

  def contains[U >: T](u: U): Boolean

  /**
   * Returns an iterator over the elements contained in the collection.
   * NOTE that the order of the elements in the iterator is unpredictable.
   *
   * @return an iterator that provides sequential access to each element of type `T`
   *         in the collection.
   */

  def iterator: Iterator[T]

  /**
   * Adds the specified element to the unordered collection.
   *
   * @param u the element to be added to the collection
   * @return a new instance of `Unordered` with the specified element added
   * @tparam U a supertype of T
   */

  def +[U >: T](u: U): Unordered[U]
}
