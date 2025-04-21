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
case class Unordered_Bag[+T](elements: Bag[T]) extends Unordered[T]:
  /**
   * Checks if the unordered collection of elements is empty.
   *
   * @return `true` if the collection contains no elements, `false` otherwise.
   */
  def isEmpty: Boolean = elements.isEmpty

  /**
   * Retrieves the number of elements contained in this collection.
   *
   * @return the total count of elements as an integer.
   */
  def size: Int = elements.size

  /**
   * Checks if the specified element is present in the set.
   *
   * @param u the element to be checked for presence in the set.
   * @tparam U the type of `u` and a supertype of `T`.
   * @return `true` if the element is present in the set, `false` otherwise.
   */
  def contains[U >: T](u: U): Boolean = elements.contains(u)

  /**
   * Returns an iterator over the elements in the adjacency set.
   * NOTE that the order of the elements in the iterator is unpredictable.
   *
   * @return an `Iterator[T]` over the elements of type `T` contained in the set.
   */
  def iterator: Iterator[T] = elements.iterator

  /**
   * Adds a specified element to the unordered collection, resulting in a new instance
   * of `Unordered` that includes the added element.
   *
   * @param u the element to be checked for presence in the set.
   * @tparam U the type of `u` and a supertype of `T`.
   * @return a new instance of `Unordered` containing the specified element in addition
   *         to the existing elements
   */
  def +[U >: T](u: U): Unordered[U] = copy(elements = elements + u)

/**
 * Companion object for the `Unordered_Bag` class providing factory methods to create instances of `Unordered`
 * with unordered collections of elements.
 *
 * This object includes utility methods for creating empty unordered bags, bags from sequences, and bags
 * initialized with varargs.
 */
object Unordered_Bag {
  /**
   * Creates an empty instance of `Unordered` containing no elements.
   *
   * @tparam X the type of elements that the unordered collection can hold
   * @return an empty unordered collection
   */
  def empty[X]: Unordered_Bag[X] = Unordered_Bag(Bag.empty)

  /**
   * Creates an instance of `Unordered` containing elements from the provided sequence.
   *
   * @param xs the sequence of elements to include in the `Unordered` collection
   * @return an `Unordered` collection containing the elements from the input sequence
   */
  def apply[X](xs: Seq[X]): Unordered_Bag[X] = Unordered_Bag(ListBag[X](xs))

  /**
   * Creates an instance of `Unordered` containing the provided elements.
   *
   * This method accepts a variable number of arguments and constructs an 
   * unordered collection from those arguments.
   *
   * @param xs the elements to include in the unordered collection
   * @return an `Unordered` instance containing the provided elements
   */
  def create[X](xs: X*): Unordered_Bag[X] = apply(xs)
}

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
