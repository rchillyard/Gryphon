package littlegryphon.core

/**
 * A generic trait representing a collection of items with support for common operations.
 * The `Bag` provides functionalities such as iteration, membership testing, addition, and size retrieval.
 *
 * @tparam X the type of elements contained within the bag. It is covariant, meaning a `Bag[A]` can
 *           be used as a `Bag[B]` if `A` is a subtype of `B`.
 */
trait Bag[+X] {
  /**
   * Returns an iterator over the elements in this collection.
   *
   * @return an iterator of type `Iterator[X]` that provides sequential access to the elements in the collection.
   */
  def iterator: Iterator[X]

  /**
   * Checks if the collection is empty.
   * This method evaluates whether the `iterator` of the collection contains any elements.
   *
   * @return true if the collection contains no elements, false otherwise.
   */
  def isEmpty: Boolean = iterator.isEmpty

  /**
   * Checks if the collection contains at least one element.
   *
   * @return true if the collection is not empty, false otherwise.
   */
  def nonEmpty: Boolean = iterator.nonEmpty

  /**
   * Retrieves the number of elements in the collection.
   *
   * @return the total count of elements in the collection.
   */
  def size: Int = iterator.size

  /**
   * Checks if the specified element is present in the collection.
   *
   * @param y the element to check for presence; can be any type that is a supertype of `X`.
   * @return `true` if the element is found in the collection; `false` otherwise.
   */
  def contains[Y >: X](y: Y): Boolean = iterator.contains(y)

  /**
   * Adds a new element to the bag, creating a new bag instance that includes the added element.
   * The returned bag preserves all existing elements and includes the additional element.
   *
   * @param y the element to be added to the bag. This element can be of the same type as
   *          the elements currently in the bag or a supertype of the bag's element type.
   * @return a new instance of `Bag[Y]` containing all elements of the current bag and
   *         the added element `y`.
   */
  def +[Y >: X](y: Y): Bag[Y]
}

/**
 * A concrete implementation of the `Bag` trait that uses a `Seq` to store elements.
 * This class provides a collection-like structure where elements can be added,
 * and iteration over the stored elements is supported.
 *
 * @tparam X the type of elements contained within the `ListBag`.
 *           The type is covariant to allow `ListBag[A]` to be used as `ListBag[B]`
 *           if `A` is a subtype of `B`.
 * @param xs a sequence of elements of type `X`, representing the contents of this `ListBag`.
 */
case class ListBag[X](xs: Seq[X]) extends Bag[X] {

  /**
   * Returns an iterator over the elements of type `X` contained within the collection.
   * NOTE: ideally, this iterator should be random.
   *
   * @return an `Iterator[X]` providing sequential access to the elements in this collection.
   */
  def iterator: Iterator[X] = xs.iterator

  /**
   * Adds a new element to the bag, creating a new bag instance that includes the added element.
   *
   * @param y the element to be added to the bag. The element can be of type `Y`,
   *          which must be a supertype of the elements currently present in the bag.
   * @return a new instance of `Bag[Y]` that includes all elements of the current bag
   *         and the newly added element.
   */
  def +[Y >: X](y: Y): Bag[Y] = ListBag(xs :+ y)
}

/**
 * Provides factory methods for creating instances of `Bag`.
 *
 * A `Bag` is an unordered collection that allows duplicate elements.
 * This object serves as the companion object to the `Bag` type,
 * offering utility functions to create empty or populated `Bag` instances.
 */
object Bag {
  /**
   * Creates an empty `Bag` instance.
   *
   * This method provides a convenient way to obtain a `Bag` without any elements.
   * The returned `Bag` is immutable and can be used as a starting point for adding elements.
   *
   * @return an empty `Bag` of type `Bag[X]`.
   */
  def empty[X]: Bag[X] = ListBag(Seq.empty)

  /**
   * Creates a new `Bag` instance populated with the specified elements.
   *
   * @param xs the elements to populate the created `Bag` with. These elements
   *           are variable-length arguments that allow creating a `Bag` with one or more elements.
   * @return a new `Bag[X]` instance containing the provided elements.
   */
  def create[X](xs: X*): Bag[X] = ListBag(xs)
}
