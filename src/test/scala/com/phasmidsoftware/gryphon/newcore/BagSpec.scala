/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.newcore

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import scala.util.Random

class BagSpec extends AnyFlatSpec with should.Matchers {

  behavior of "Bag"

  it should "iterator (1)" in {
    Bag.empty.iterator.hasNext shouldBe false
    val iterator = (Bag.empty + 1).iterator
    iterator.hasNext shouldBe true
    iterator.next() shouldBe 1
    iterator.hasNext shouldBe false
  }
  it should "iterator (2)" in {
    implicit val random: Random = new Random(0L)
    val bag = new Bag[Int](Seq(0, 1))
    val iterator = bag.iterator
    iterator.next() shouldBe 0
    iterator.next() shouldBe 1
    iterator.hasNext shouldBe false
  }

}
