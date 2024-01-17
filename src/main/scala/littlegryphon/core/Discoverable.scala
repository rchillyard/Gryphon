/*
 * Copyright (c) 2024. Phasmid Software
 */

package littlegryphon.core

trait Discoverable[T] {
  def isDiscovered(t: T): Boolean

  def setDiscovered(t: T, b: Boolean): Unit
}
