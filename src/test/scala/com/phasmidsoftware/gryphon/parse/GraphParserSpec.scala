package com.phasmidsoftware.gryphon.parse

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class GraphParserSpec extends AnyFlatSpec with Matchers {

  behavior of "GraphParser"

  it should "parsePair 1" in {
    val p = new GraphParser[Int, Double]
    p.parsePair("1 2") should matchPattern { case p.Success(Some((1, 2)), _) => }
  }

  it should "parsePair 2" in {
    val p = new GraphParser[String, Double]
    p.parsePair("A B") should matchPattern { case p.Success(Some(("A", "B")), _) => }
  }

  it should "parseTriple" in {
    val p = new DecimalGraphParser[Int, Double]
    p.parseTriple("1 2 3.14") should matchPattern { case p.Success(Some((1, 2, 3.14)), _) => }

  }

}
