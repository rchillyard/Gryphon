/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.util

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class StringsParserSpec extends AnyFlatSpec with should.Matchers {

  behavior of "StringsParser"

  val p = new StringsParser()

  it should "comment" in {
    p.parse(p.comment, "// junk").successful shouldBe true
    p.parse(p.comment, " // junk").successful shouldBe true
  }

  it should "word" in {
    p.parse(p.word, "0").successful shouldBe true
    p.parse(p.word, "X").successful shouldBe true
    p.parse(p.word, "X ").successful shouldBe true
    p.parse(p.word, " ").successful shouldBe false
    p.parse(p.word, "/").successful shouldBe false
  }

  it should "triple" in {
    p.parse(p.triple, "X Y Z") should matchPattern { case p.Success(p.Strings3("X", "Y", "Z"), _) => }
    p.parse(p.triple, "// X Y Z").successful shouldBe false
  }

  it should "stringy" in {

  }

  it should "pair" in {
    p.parse(p.pair, "X Y") should matchPattern { case p.Success(p.Strings2("X", "Y"), _) => }
    p.parse(p.pair, "// X Y").successful shouldBe false
  }

  it should "parse2" in {
    p.parse2("X Y") should matchPattern { case Right(("X", "Y")) => }
  }

  it should "parse3" in {
    p.parse3("X Y Z") should matchPattern { case Right(("X", "Y", "Z")) => }
  }

  it should "tuple3" in {
    val value2 = p.parse(p.tuple3, "X Y Z")
    value2.successful shouldBe true
    value2.get shouldBe Right(("X", "Y", "Z"))
    val value1 = p.parse(p.tuple3, "// X Y Z")
    value1.successful shouldBe true
    value1.get shouldBe Left("// X Y Z")

  }

  it should "tuple2" in {
    val value2 = p.parse(p.tuple2, "X Y")
    value2.successful shouldBe true
    value2.get shouldBe Right(("X", "Y"))
    val value1 = p.parse(p.tuple2, "// X Y")
    value1.successful shouldBe true
    value1.get shouldBe Left("// X Y")

  }

}
