/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.newcore

trait Discovered {
  def isDiscovered: Boolean

  def setDiscovered(b: Boolean): Unit

}

trait Properties[V, P] {
  def getProperties(v: V): P

  def setProperties(v: V)(p: P): Unit
}

trait Discoverable[P] {

  def isDiscovered(p: P): Boolean

  def setDiscovered(p: P, b: Boolean): Unit

}

object Discoverable {
  implicit object DiscoverableUnit extends Discoverable[Unit] {
    def isDiscovered(p: Unit): Boolean = false

    def setDiscovered(p: Unit, b: Boolean): Unit = {}
  }
}

trait Initializable[P] {
  def initialize: P
}

object Initializable {
  implicit object InitializableUnit extends Initializable[Unit] {
    def initialize: Unit = {}
  }
}