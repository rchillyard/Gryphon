package com.phasmidsoftware.gryphon.parse

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import org.scalatest.matchers.should.*

class BaseParserSpec extends AnyFlatSpec with should.Matchers {

  behavior of "BaseParser"

  val p = new GraphParser[Int, String, Unit]

  it should "parseComment" in {
    p.parseAll(p.comment, "//x") should matchPattern { case p.Success("x", _) => }
  }

  it should "parseOptional" in {
    p.parseAll(p.optional, "1") should matchPattern { case p.Success(p.~(Some(1), None), _) => }
    p.parseAll(p.optional, "//") should matchPattern { case p.Success(p.~(None, Some("")), _) => }
    p.parseAll(p.optional, "// 1") should matchPattern { case p.Success(p.~(None, Some("1")), _) => }
  }

  it should "tryParseAll 0" in {
    val z = p.tryParseAll(p.pair)("1 2")
    z.isSuccess shouldBe true
  }
  it should "tryParseAll 1" in {
    val z = p.tryParseAll(p.pair)("// 1 2")
    z.isFailure shouldBe true
    z.failed.get shouldBe a[EmptyStringException]
    z.failed.get.getMessage shouldBe "1 2"
  }

  it should "comment" in {

  }


  it should "vertex" in {

  }

  it should "maybeEdge" in {

  }

  it should "maybeZ" in {

  }

}
