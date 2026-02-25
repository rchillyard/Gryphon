/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.util

import scala.util.{Failure, Success, Try}

/**
 * Utilities: especially functional utilities.
 * CONSIDER renaming this FP for consistency with my other libraries.
 */
object Util {

  /**
   * Method to sequence an Iterator of Try[X] into a Try of List[X].
   *
   * @param eys an Iterator of Try[X].
   * @tparam X the underlying type.
   * @return Try of List[X].
   */
  def sequence[X](eys: Iterator[Try[X]]): Try[List[X]] =
    eys.foldLeft(Try(List[X]())) { (xsy, ey) =>
      (xsy, ey) match {
        case (Success(xs), Success(e)) => Success(xs :+ e)
        case (Failure(x), _) => Failure(x)
        case (_, Failure(x)) => Failure(x)
      }
    }

  /**
   * Method to sequence an Iterator of Try[X] into a Try of List[X].
   *
   * @param eys an Iterator of Try[X].
   * @tparam X the underlying type.
   * @return Try of List[X].
   */
  def sequence[X](eys: Iterable[Try[X]]): Try[List[X]] = sequence(eys.iterator)

  /**
   * Method to get the value of an Option[X] but throwing a given exception rather than the usual NoSuchElement.
   *
   * @param xo an optional value of X (called by name).
   * @param t  a throwable.
   * @tparam X the underlying type of xo and the type of the result.
   * @return the value of xo or throws t.
   * @throws Throwable t
   */
  def getOrThrow[X](xo: => Option[X], t: => Throwable): X = xo.getOrElse(throw t)

  /**
   * Method to get the value of an Option[X] wrapped in Try.
   * In the case of a None input, a Failure, of the given Throwable, is returned.
   *
   * @param xo an optional value of X (called by name).
   * @param t  a throwable.
   * @tparam X the underlying type of xo and the underlying type of the result.
   * @return the value of <code>Try(getOrThrow(xo, t))</code>.
   */
  def optionToTry[X](xo: => Option[X], t: => Throwable): Try[X] = Try(getOrThrow(xo, t))

  /**
   * Method to get the value of a X, ensuring that its value is not null. but returning a Try with the Failure case having a specific Throwable.
   *
   * @param x an optional value of X (called by name).
   * @param t a throwable.
   * @tparam X the type of x and the underlying type of the result.
   * @return the value of <code>optionToTry(Option(x), t)</code>.
   */
  def tryNonNull[X](x: => X, t: => Throwable): Try[X] = optionToTry(Option(x), t)

  /**
   * Method to yield an Option of T according to whether the predicate p yields true.
   *
   * @param p a predicate on T.
   * @param t an actual value of T.
   * @tparam T the type of t (and the underlying type of the result).
   * @return Some(t) if p(t) is true, otherwise None.
   */
  def optional[T](p: T => Boolean)(t: T): Option[T] = Some(t).filter(p)
}
