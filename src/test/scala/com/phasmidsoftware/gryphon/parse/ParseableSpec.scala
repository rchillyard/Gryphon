package com.phasmidsoftware.gryphon.parse

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.util.{Failure, Success, Try}

class ParseableSpec extends AnyFlatSpec with Matchers {

  behavior of "Parseable"

  it should "parse Boolean" in {
    val parseable = implicitly[Parseable[Boolean]]
    parseable.parse("true") shouldBe Success(true)
  }
  it should "fail Boolean 1" in {
    val parseable = implicitly[Parseable[Boolean]]
    val triedBoolean = parseable.parse("X")
    triedBoolean should matchPattern { case Failure(_) => }
    a[IllegalArgumentException] should be thrownBy triedBoolean.get
    triedBoolean match {
      case Failure(e) => e.getLocalizedMessage shouldBe "For input string: \"X\""
      case _ => fail("parse succeeded when it should have failed")
    }
  }
  it should "fail Boolean 2" in {
    val booleanParser: String => Boolean = Parseable.parser[Boolean]
    a[ParseException] should be thrownBy booleanParser("X")
  }

  it should "fail Boolean 3" in {
    val booleanParser: String => Boolean = Parseable.parser[Boolean]
    Try(booleanParser("X")) match {
      case Failure(e) => e.getLocalizedMessage shouldBe "Failed to parse \"X\" as Boolean"
      case _ => fail("parse succeeded when it should have failed")
    }
  }

  it should "parse Int" in {
    val parseable = implicitly[Parseable[Int]]
    parseable.parse("1") shouldBe Success(1)
  }
  it should "fail Int" in {
    val parseable = implicitly[Parseable[Int]]
    parseable.parse("X") should matchPattern { case Failure(_) => }
    a[IllegalArgumentException] should be thrownBy parseable.parse("X").get
  }

  it should "parse Double" in {
    val parseable = implicitly[Parseable[Double]]
    parseable.parse("3.1415927") shouldBe Success(3.1415927)
  }
  it should "fail Double" in {
    val parseable = implicitly[Parseable[Double]]
    parseable.parse("X") should matchPattern { case Failure(_) => }
    a[IllegalArgumentException] should be thrownBy parseable.parse("X").get
  }

  it should "parse Unit" in {
    val parseable = implicitly[Parseable[Unit]]
    parseable.parse("()") shouldBe Success(())
  }
  it should "fail Unit" in {
    val parseable = implicitly[Parseable[Unit]]
    val triedUnit = parseable.parse("X")
    triedUnit should matchPattern { case Failure(_) => }
    a[AssertionError] should be thrownBy parseable.parse("X").get
  }

}
