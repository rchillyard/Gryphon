package littlegryphon.core

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
