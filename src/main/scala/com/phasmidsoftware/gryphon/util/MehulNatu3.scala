/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.util

object MehulNatu3 extends App {


  case class Rational(n: BigInt, d: BigInt) {
    require(d.compare(BigInt(0)) != 0, "denominator  cannot be zero")
    require(d.gcd(n).equals(BigInt(1)), "GCD is not 1")

    override def toString: String = n.toString() + "/" + d.toString()

    def +(that: Rational): Rational = {
      val calculatedN = (n * that.d) + (d * that.n)
      val calculatedD = d * that.d
      val gcd = calculatedN.gcd(calculatedD)
      Rational(calculatedN / gcd, calculatedD / gcd)
    }
  }

  object Rational {
    def apply(n: Long, d: Long): Rational = {
      val bigIntN = BigInt.apply(n)
      val bigIntD = BigInt.apply(d)
      val gcd = bigIntN.gcd(bigIntD)
      Rational(bigIntN / gcd, bigIntD / gcd)
    }

    def apply(n: Long, d: Int): Rational = apply(n, d.toLong)

    def apply(n: Int, d: Int): Rational = apply(n.toLong, d)

    def apply(n: Int): Rational = apply(n.toLong, 1L)

    def parse(s: String, maybeD: Option[String]): Option[Rational] = {
      for {
        n <- s.toLongOption
        d <- maybeD map (dString => dString.toLongOption) getOrElse Some(1L)
      } yield Rational(n, d)
    }

    implicit object RationalNumeric extends Numeric[Rational] {


      override def plus(x: Rational, y: Rational): Rational = x + y

      override def minus(x: Rational, y: Rational): Rational = ???

      override def times(x: Rational, y: Rational): Rational = ???

      override def negate(x: Rational): Rational = ???

      // Needed to provide starting point for folding using 'sum'
      override def fromInt(x: Int): Rational = {
        Rational(0, 1)
      }

      override def toInt(x: Rational): Int = ???

      override def toLong(x: Rational): Long = ???

      override def toFloat(x: Rational): Float = ???

      override def toDouble(x: Rational): Double = ???

      override def compare(x: Rational, y: Rational): Int = ???

      override def parseString(str: String): Option[Rational] = ???
    }

  }

  val val1 = Rational.parse("1",Some("2"))
  val val2 = Rational.parse("x",Some("2"))
  val val3 = Rational.parse("2",Some("x"))
  val val4 = Rational.parse("2",Some("1"))
  val val5 = Rational.parse("2",None)

  assert(val1.getOrElse(Rational(0, 1)) == Rational(1, 2))
  assert(val2.isEmpty)
  assert(val3.isEmpty)
  assert(val4 == val5)

  println(val1)
  println(val2)
  println(val3)
  println(val4)
  println(val5)

  //Rational(9, 0)

}
