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
   * Counts the elements in the collection that satisfy the given predicate.
   *
   * This method evaluates each element in the unordered collection using the
   * provided predicate function `p`. It returns the total number of elements
   * for which the predicate evaluates to `true`.
   *
   * @param p a predicate function that takes an element of type `T` and returns a boolean,
   *          indicating whether the element satisfies the condition
   *
   * @return the number of elements in the collection that satisfy the predicate
   */
  def count(p: T => Boolean): Int = iterator.count(p)

  /**
   * Returns an iterator over the elements contained in the collection.
   * NOTE that the order of the elements in the iterator is unpredictable.
   *
   * @return an iterator that provides sequential access to each element of type `T`
   *         in the collection.
   */
  def iterator: Iterator[T]

  /**
   * Filters the elements of the collection using the provided predicate function.
   *
   * This method returns a new collection that contains only those elements
   * for which the predicate function `p` evaluates to `true`.
   *
   * @param p a predicate function that takes an element of type `T` and returns a boolean,
   *          indicating whether the element satisfies the condition or not.
   *
   * @return an instance of `Unordered[T]` containing the elements that satisfy the predicate.
   */
  def filter(p: T => Boolean): Unordered[T]

  /**
   * Finds the first element in the collection that satisfies the given predicate function.
   *
   * This method traverses the unordered collection to identify the first element for which
   * the provided predicate function `p` evaluates to `true`. If such an element is found,
   * it is returned wrapped in an `Option`. If no such element exists, `None` is returned.
   *
   * @param p a predicate function that takes an element of type `T` and returns a boolean,
   *          indicating whether the element satisfies the condition.
   *
   * @return an `Option` containing the first element that satisfies the predicate, or `None`
   *         if no such element exists in the collection.
   */
  def find(p: T => Boolean): Option[T]

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
 * Abstract base class providing common implementations for unordered collections.
 * Concrete subclasses (`Unordered_Bag` and `Unordered_Set`) differ in their
 * underlying storage and duplicate-handling behaviour.
 *
 * @tparam T the type of elements contained in the collection (covariant).
 * @param elements the underlying storage, accessed via `IterableOnce`.
 */
abstract class AbstractUnordered[+T](elements: IterableOnce[T]) extends Unordered[T]:
  /**
   * Checks if the unordered collection of elements is empty.
   *
   * @return `true` if the collection contains no elements, `false` otherwise.
   */
  def isEmpty: Boolean =
    iterator.isEmpty

  /**
   * Retrieves the number of elements contained in this collection.
   *
   * @return the total count of elements as an integer.
   */
  def size: Int =
    iterator.size

  /**
   * Returns an iterator over the elements in the adjacency set.
   * NOTE that the order of the elements in the iterator is unpredictable.
   *
   * @return an `Iterator[T]` over the elements of type `T` contained in the set.
   */
  def iterator: Iterator[T] =
    elements.iterator

  /**
   * Filters the elements of the collection using the provided predicate function.
   *
   * This method returns a new collection that contains only those elements
   * for which the predicate function `p` evaluates to `true`.
   *
   * @param p a predicate function that takes an element of type `T` and returns a boolean,
   *          indicating whether the element satisfies the condition or not.
   *
   * @return an instance of `Unordered[T]` containing the elements that satisfy the predicate.
   */
  def filter(p: T => Boolean): Unordered[T] =
    unit(iterator.filter(p).toSeq)

  /**
   * Finds the first element in the collection that satisfies the given predicate function.
   *
   * This method traverses the unordered collection to identify the first element for which
   * the provided predicate function `p` evaluates to `true`. If such an element is found,
   * it is returned wrapped in an `Option`. If no such element exists, `None` is returned.
   *
   * @param p a predicate function that takes an element of type `T` and returns a boolean,
   *          indicating whether the element satisfies the condition.
   *
   * @return an `Option` containing the first element that satisfies the predicate, or `None`
   *         if no such element exists in the collection.
   */
  def find(p: T => Boolean): Option[T] = iterator.find(p)

  /**
   * Adds the specified element to the unordered collection.
   *
   * @param u the element to be added to the collection
   * @return a new instance of `Unordered` with the specified element added
   * @tparam U a supertype of T
   */
  def +[U >: T](u: U): Unordered[U] =
    unit(elements.iterator.toSeq :+ u)

  /**
   * Creates a new instance of `Unordered` containing the specified sequence of elements.
   *
   * This method allows the initialization of an unordered collection with a given sequence
   * of elements. The order of elements in the input sequence is not preserved or relevant
   * in the resulting unordered collection.
   *
   * @param elements a sequence of elements of type `U` to be added to the unordered collection
   * @tparam U a supertype of `T`, representing the type of elements in the input sequence
   * @return a new instance of `Unordered[U]` containing the specified elements
   */
  def unit[U >: T](elements: Seq[U]): Unordered[U]

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
case class Unordered_Bag[+T](elements: Bag[T]) extends AbstractUnordered[T](elements):

  /**
   * Checks if the specified element is present in the set.
   *
   * @param u the element to be checked for presence in the set.
   * @tparam U the type of `u` and a supertype of `T`.
   * @return `true` if the element is present in the set, `false` otherwise.
   */
  def contains[U >: T](u: U): Boolean =
    elements.contains(u)

  /**
   * Creates a new instance of `Unordered` with the specified sequence of elements.
   *
   * This method allows creating a new unordered collection by copying the current
   * instance and replacing its underlying element set with a newly created `Bag`
   * containing the provided sequence of elements.
   *
   * @param elements the sequence of elements to populate the new unordered collection.
   *                 These elements will be used to construct a new `Bag`, which will
   *                 replace the current one in the copied instance.
   *
   * @tparam U the type of the elements in the provided sequence, which must be a
   *           supertype of the current element type `T`.
   *
   * @return a new instance of `Unordered[U]` containing the specified elements.
   */
  def unit[U >: T](elements: Seq[U]): Unordered[U] =
    copy(elements = Bag.create(elements *))

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
  def empty[X]: Unordered_Bag[X] =
    Unordered_Bag(Bag.empty)

  /**
   * Creates an instance of `Unordered` containing elements from the provided sequence.
   *
   * @param xs the sequence of elements to include in the `Unordered` collection
   * @return an `Unordered` collection containing the elements from the input sequence
   */
  def apply[X](xs: Seq[X]): Unordered_Bag[X] =
    Unordered_Bag(ListBag[X](xs))

  /**
   * Creates an instance of `Unordered` containing the provided elements.
   *
   * This method accepts a variable number of arguments and constructs an 
   * unordered collection from those arguments.
   *
   * @param xs the elements to include in the unordered collection
   * @return an `Unordered` instance containing the provided elements
   */
  def create[X](xs: X*): Unordered_Bag[X] =
    apply(xs)
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
case class Unordered_Set[T](elements: Set[T]) extends AbstractUnordered[T](elements):

  /**
   * Checks whether the specified element is contained within the set.
   *
   * @param u the element to search for within the set. Its type must conform to or extend
   *          the type `T` of the elements in the set or be a supertype of `T`.
   *
   * @return `true` if the element exists within the set, otherwise `false`.
   */
  def contains[U >: T](u: U): Boolean =
    elements.asInstanceOf[Set[U]].contains(u)

  /**
   * Creates a new unordered collection containing the provided elements.
   *
   * This method accepts a sequence of elements, converts them into a set
   * (ensuring uniqueness), and returns a new instance of `Unordered` with
   * these elements.
   *
   * @param elements a sequence of elements to be included in the new unordered collection.
   *                 The type of elements must conform to or be a supertype of the existing
   *                 elements in the collection.
   *
   * @return a new `Unordered` instance containing the specified elements.
   */
  def unit[U >: T](elements: Seq[U]): Unordered[U] = copy(elements = Set(elements *))

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
  def empty[X]: Unordered[X] =
    Unordered_Set(Set.empty)

  /**
   * Creates an instance of `Unordered` from a sequence of elements.
   *
   * The elements in the provided sequence are converted into a `Set`, ensuring the resulting
   * unordered collection contains unique elements with no particular order.
   *
   * @param xs the sequence of elements to be converted into an unordered collection
   * @tparam X the type of elements contained in the input sequence and resulting `Unordered`
   *           collection
   *
   * @return an instance of `Unordered` containing the unique elements from the input sequence
   */
  def apply[X](xs: Seq[X]): Unordered[X] =
    new Unordered_Set(xs.toSet)

  /**
   * Creates a new instance of `Unordered[X]` populated with the provided elements.
   *
   * @param xs the elements to include in the unordered collection.
   *           These elements are provided as a variable-length argument list, 
   *           allowing the creation of an unordered collection containing one or more elements.
   *
   * @tparam X the type of the elements contained within the unordered collection.
   * @return an instance of `Unordered[X]` containing the provided elements.
   */
  def create[X](xs: X*): Unordered[X] =
    apply(xs)
}
