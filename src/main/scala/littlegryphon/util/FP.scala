package littlegryphon.util

import java.net.URL
import scala.collection.{AbstractIterator, mutable}
import scala.reflect.ClassTag
import scala.util.Using.Releasable
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try, Using}

/**
 * General functional programming utilities.
 *
 */
object FP {

  /**
   * Combines a sequence of optional values into a single optional sequence, preserving
   * the order of elements and excluding any `None` values.
   * If the input sequence contains only `None` values, the result will be `None`.
   * `Some(Nil)` will never be returned.
   *
   * @param tos the sequence of optional values to be combined.
   * @tparam T the type of the elements inside the `Option`.
   * @return an `Option` containing a sequence of all values extracted from the input sequence,
   *         or `None` if there are no such values present.
   */
  def sequence[T](tos: Seq[Option[T]]): Option[Seq[T]] =
    tos.foldLeft[Option[Seq[T]]](None) {
      (tso, to) =>
        (tso, to) match {
          case (Some(ts), Some(t)) =>
            Some(ts :+ t)
          case (_, None) =>
            tso
          case (None, Some(t)) =>
            Some(Seq(t))
        }
    }

  def mutableQueueIterator[T](queue: mutable.Queue[T]): Iterator[T] = new AbstractIterator[T] {
    def hasNext: Boolean = queue.nonEmpty

    def next(): T = queue.dequeue()
  }

  /**
   * Sequence method to combine elements of Try.
   *
   * @param xys an Iterator of Try[X]
   * @tparam X the underlying type
   * @return a Try of Iterator[X]
   */
  def sequence[X](xys: Iterator[Try[X]]): Try[Iterator[X]] = sequence(xys.to(List)).map(_.iterator)

  /**
   * Sequence method to combine elements of Try.
   *
   * @param xos an Iterator of Try[X]
   * @tparam X the underlying type
   * @return a Try of Iterator[X]
   */
  def sequence[X](xos: Iterator[Option[X]]): Option[Iterator[X]] = sequence(xos.to(List)).map(_.iterator)

  /**
   * Sequence method to combine elements of Try.
   *
   * @param xys an Iterable of Try[X]
   * @tparam X the underlying type
   * @return a Try of Seq[X]
   *         NOTE: that the output collection type will be Seq, regardless of the input type
   */
  def sequence[X](xys: Iterable[Try[X]]): Try[Seq[X]] =
    xys.foldLeft(Try(Seq[X]())) {
      (xsy, xy) => for (xs <- xsy; x <- xy) yield xs :+ x
    }

  /**
   * Sequence method to combine elements of Try.
   *
   * @param xos an Iterable of Option[X]
   * @tparam X the underlying type
   * @return an Option of Seq[X]
   *         NOTE: that the output collection type will be Seq, regardless of the input type
   */
  def sequence[X](xos: Iterable[Option[X]]): Option[Seq[X]] =
    xos.foldLeft(Option(Seq[X]())) {
      (xso, xo) => for (xs <- xso; x <- xo) yield xs :+ x
    }

  /**
   * Method to partition an  method to combine elements of Try.
   *
   * @param xys an Iterator of Try[X]
   * @tparam X the underlying type
   * @return a tuple of two iterators of Try[X], the first one being successes, the second one being failures.
   */
  def partition[X](xys: Iterator[Try[X]]): (Iterator[Try[X]], Iterator[Try[X]]) = xys.partition(_.isSuccess)

  /**
   * Method to partition an  method to combine elements of Try.
   *
   * TEST
   *
   * @param xys a Seq of Try[X]
   * @tparam X the underlying type
   * @return a tuple of two Seqs of Try[X], the first one being successes, the second one being failures.
   */
  def partition[X](xys: Seq[Try[X]]): (Seq[Try[X]], Seq[Try[X]]) = xys.partition(_.isSuccess)

  /**
   * Method to yield a URL for a given resourceForClass in the classpath for C.
   *
   * @param resourceName the name of the resourceForClass.
   * @tparam C a class of the package containing the resourceForClass.
   * @return a Try[URL].
   */
  def resource[C: ClassTag](resourceName: String): Try[URL] = resourceForClass(resourceName, implicitly[ClassTag[C]].runtimeClass)

  /**
   * Method to yield a Try[URL] for a resource name and a given class.
   *
   * @param resourceName the name of the resource.
   * @param clazz        the class, relative to which, the resource can be found (defaults to the caller's class).
   * @return a Try[URL]
   */
  def resourceForClass(resourceName: String, clazz: Class[_] = getClass): Try[URL] = Option(clazz.getResource(resourceName)) match {
    case Some(u) => Success(u)
    case None => Failure(FPException(s"$resourceName is not a valid resource for $clazz"))
  }

  /**
   * Method to determine if the String w was found at a valid index (i).
   *
   * @param w the String (ignored unless there's an exception).
   * @param i the index found.
   * @return Success(i) if all well, else Failure(exception).
   */
  def indexFound(w: String, i: Int): Try[Int] = i match {
    case x if x >= 0 => Success(x)
    case _ => Failure(FPException(s"Header column '$w' not found"))
  }

  /**
   * Method to transform a a Try[X] into an Option[X].
   * But, unlike "toOption," a Failure can be logged.
   *
   * @param f  a function to process any Exception (typically a logging function).
   * @param xy the input Try[X}.
   * @tparam X the underlying type.
   * @return an Option[X].
   */
  def tryToOption[X](f: Throwable => Unit)(xy: Try[X]): Option[X] = xy match {
    case Success(x) => Some(x)
    case Failure(NonFatal(x)) => f(x); None
    case Failure(x) => throw x
  }
}

object TryUsing {
  /**
   * This method is to Using.apply as flatMap is to Map.
   *
   * @param resource a resource which is used by f and will be managed via Using.apply
   * @param f        a function of R => Try[A].
   * @tparam R the resource type.
   * @tparam A the underlying type of the result.
   * @return a Try[A]
   */
  def apply[R: Releasable, A](resource: => R)(f: R => Try[A]): Try[A] = Using(resource)(f).flatten

  /**
   * Executes the provided function `f` with a resource managed via `Using` while handling the resource lifecycle.
   *
   * This method takes a `Try` of resource type `R`, and applies the function `f` to the successfully
   * acquired resource if `ry`. If `ry` is a `Failure`, the failure is propagated.
   *
   * @param ry a `Try[R]` representing a resource to be used. If `Success`, the resource is passed to the function `f`.
   * @param f  a function that operates on the resource of type `R` and produces a result of type `A`.
   * @tparam R the type of the resource being managed.
   * @tparam A the type of the result produced by the function `f`.
   * @return a `Try[A]` containing the result of applying `f` to the resource if `ry` is `Success`,
   *         or propagating the `Failure` if `ry` is `Failure`.
   */
  def trial[R: Releasable, A](ry: => Try[R])(f: R => A): Try[A] = ry match {
    case Success(r) => Using(r)(f)
    case Failure(x) => Failure(x)
  }

  /**
   * This method is similar to apply(r) but it takes a Try[R] as its parameter.
   * The definition of f is the same as in the other apply, however.
   *
   * TEST
   *
   * @param ry a Try[R] which is passed into f and will be managed via Using.apply
   * @param f  a function of R => Try[A].
   * @tparam R the resource type.
   * @tparam A the underlying type of the result.
   * @return a Try[A]
   */
  def tryIt[R: Releasable, A](ry: => Try[R])(f: R => Try[A]): Try[A] = for (r <- ry; a <- apply(r)(f)) yield a
}

case class FPException(msg: String, eo: Option[Throwable] = None) extends Exception(msg, eo.orNull)
