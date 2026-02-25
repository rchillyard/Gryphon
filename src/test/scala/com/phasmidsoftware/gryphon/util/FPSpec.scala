package com.phasmidsoftware.gryphon.util

import com.phasmidsoftware.gryphon.util.FP.sequence
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class FPSpec extends AnyFlatSpec with Matchers {

  behavior of "FP"

  it should "sequence 0" in {
    val target: Seq[Option[Int]] = Nil
    sequence(target) shouldBe None
  }

  it should "sequence 1" in {
    val target: Seq[Option[Int]] = Seq(Some(1), None, Some(2))
    sequence(target) shouldBe Some(Seq(1, 2))
  }

  it should "sequence 2" in {
    val target: Seq[Option[Int]] = Seq(None, Some(2), Some(1))
    sequence(target) shouldBe Some(Seq(2, 1))
  }

  it should "sequence 3" in {
    val target: Seq[Option[Int]] = Seq(None)
    sequence(target) shouldBe None
  }

}
