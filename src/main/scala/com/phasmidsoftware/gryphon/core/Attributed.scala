/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.core

trait Attributed[+A] {

    /**
     * An attribute.
     *
     * @return the value of the attribute, for example, a weight.
     */
    val attribute: A
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
