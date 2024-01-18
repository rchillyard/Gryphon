package littlegryphon.core

/**
 * Trait to define the behavior of a Bag (or MultiSet).
 *
 * NOTE: there are only two operations on Bag: iteration and insertion (+).
 *
 * @tparam X the underlying type of this Bag (covariant).
 */
trait Bag[+X] extends Iterable[X] {
  /**
   * Method to add an element to this Bag.
   *
   * @param y the element to add.
   * @tparam Y the underlying type of the result and of <code>y</code> (must be a super-type of X).
   * @return a Bag[Y].
   */
  def +[Y >: X](y: Y): Bag[Y]

}

/**
 * Companion object to Bag.
 */
object Bag {
  /**
   * Constant value: empty: Bag[Nothing].
   */
  val empty: Bag[Nothing] = ListBag.apply
}

/**
 * Concrete case class which implements Bag.
 *
 * @param xs a sequence of X. The order of this sequence is immaterial.
 * @tparam X the underlying type of this Bag (covariant).
 */
case class ListBag[+X](xs: Seq[X]) extends Bag[X] {

  /**
   * Method to insert a new value of type Y (a super-type of X) into this ListBag.
   *
   * @param y the element to add.
   * @tparam Y the underlying type of the result and of <code>y</code> (must be a super-type of X).
   * @return a Bag[Y].
   */
  def +[Y >: X](y: Y): Bag[Y] = ListBag[Y](xs :+ y)

  override def toString(): String = s"ListBag($xs)"

  override def equals(obj: Any): Boolean = obj match {
    case bag: ListBag[X] => xs.size == bag.xs.size && (xs diff bag.xs).isEmpty
    case _ => false
  }

  /**
   * Method to yield an Iterator[X] on the values of this Bag.
   * CONSIDER shuffling the order of the values.
   *
   * @return an iterator on xs.
   */
  def iterator: Iterator[X] = xs.iterator
}

/**
 * Companion object to ListBag.
 */
object ListBag {
  def create[X](xs: X*): ListBag[X] =
    new ListBag[X](xs)

  def apply: ListBag[Nothing] = apply(Nil)
}