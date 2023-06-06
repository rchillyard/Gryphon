/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.core

/**
 * Trait to define a type class which supports the zero (identity) method.
 *
 * It's rather disappointing that Scala doesn't define this.
 * I suppose we could get it from Cats but that seems like overkill.
 *
 * @tparam T the type of this trait.
 */
trait HasZero[T] {

    def zero: T
}

object HasZero {
    trait HasZeroUnit extends HasZero[Unit] {
        def zero: Unit = ()
    }

    implicit object HasZeroUnit extends HasZeroUnit

    trait HasZeroBoolean extends HasZero[Boolean] {
        def zero: Boolean = false
    }

    implicit object HasZeroBoolean extends HasZeroBoolean

    trait HasZeroInt extends HasZero[Int] {
        def zero: Int = 0
    }

    implicit object HasZeroInt extends HasZeroInt

    trait HasZeroDouble extends HasZero[Double] {
        def zero: Double = 0
    }

    implicit object HasZeroDouble extends HasZeroDouble
}