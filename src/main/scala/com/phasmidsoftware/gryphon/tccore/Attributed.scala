/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.tccore

trait Attributed[X, +A] {

  /**
   * An attribute.
   *
   * @return the value of the x's attribute, for example, a weight.
   */
  def attribute(x: X): A
}

trait Property[P] {
  /**
   * Method to yield the current value of this property.
   *
   * @return
   */
  def getProperty: Option[P]

  /**
   * Mutating method to set the property.
   *
   * @param p the new value of the property.
   */
  def setProperty(p: Option[P]): Unit
}
