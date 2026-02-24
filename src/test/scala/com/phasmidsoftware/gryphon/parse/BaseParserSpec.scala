package com.phasmidsoftware.gryphon.parse

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class BaseParserSpec extends AnyFlatSpec with should.Matchers:

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
    // A comment line starts with // and the rest is returned as the content
    p.parseAll(p.comment, "// hello world") should matchPattern {
      case p.Success("hello world", _) =>
    }
  }

  it should "vertex — parsed as part of a pair" in {
    // vertex is protected; exercise it via the public parsePair interface
    val q = new GraphParser[Int, String, Unit]
    q.parsePair("42 99") should matchPattern { case scala.util.Success((42, 99, None)) => }
  }

  it should "maybeEdge — parsed as part of a triple" in {
    // maybeEdge is protected; exercise it via the public parseTriple interface
    val q = new GraphParser[Int, String, Unit]
    q.parseTriple("1 2 hello") should matchPattern {
      case scala.util.Success(com.phasmidsoftware.gryphon.core.Triplet(1, 2, Some("hello"), _)) =>
    }
  }

  it should "maybeZ — parsed as part of a triple with Unit edge type" in {
    // maybeZ is protected; a pair with no edge type parses successfully
    val q = new GraphParser[Int, Double, Unit]
    q.parsePair("3 7") should matchPattern { case scala.util.Success((3, 7, None)) => }
  }