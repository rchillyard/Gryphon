/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.expcore

trait Attributed[+A] {

  /**
   * An attribute.
   *
   * @return the value of the attribute, for example, a weight.
   */
  val attribute: A
}
