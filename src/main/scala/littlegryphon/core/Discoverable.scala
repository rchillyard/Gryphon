/*
 * Copyright (c) 2024. Phasmid Software
 */

package littlegryphon.core

/**
 * Typeclass trait to model Discovery.
 *
 * @tparam T the underlying type.
 */
trait Discoverable[T] {
  /**
   * Method to determine if t of type T has been discovered.
   *
   * @param t an object of type T.
   * @return true if t has been discovered.
   */
  def isDiscovered(t: T): Boolean

  /**
   * Method to set discovered in t of type T.
   *
   * @param t an object of type T.
   * @param b the value to set.
   */
  def setDiscovered(t: T, b: Boolean): Unit
}
