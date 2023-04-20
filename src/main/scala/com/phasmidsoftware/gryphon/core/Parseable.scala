package com.phasmidsoftware.gryphon.core

import scala.util.{Success, Try}

trait Parseable[T] {

    def parse(w: String): Try[T]
}

object Parseable {
    trait ParseableString extends Parseable[String] {
        def parse(w: String): Try[String] = Success(w)
    }

    implicit object ParseableString extends ParseableString

    trait ParseableBoolean extends Parseable[Boolean] {
        def parse(w: String): Try[Boolean] = Success(w.toBoolean)
    }

    implicit object ParseableBoolean extends ParseableBoolean

    trait ParseableInt extends Parseable[Int] {
        def parse(w: String): Try[Int] = Success(w.toInt)
    }

    implicit object ParseableInt extends ParseableInt

    trait ParseableDouble extends Parseable[Double] {
        def parse(w: String): Try[Double] = Success(w.toDouble)
    }

    implicit object ParseableDouble extends ParseableDouble
} 