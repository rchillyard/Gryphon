/*
 * Copyright (c) 2024. Phasmid Software
 */

package com.phasmidsoftware.gryphon.core

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class BagSpec extends AnyFlatSpec with should.Matchers {

  behavior of "Bag"

  it should "empty" in {
    val bag = Bag.empty
    bag.isEmpty shouldBe true
  }

  it should "$plus and iterator" in {
    val bag = Bag.empty
    val bag1 = bag + 1
    bag1.size shouldBe 1
    val iterator = bag1.iterator
    iterator.hasNext shouldBe true
    iterator.next() shouldBe 1
    iterator.hasNext shouldBe false
  }

  it should "$plus and equals good" in {
    val bag1 = Bag.empty + 1 + 2
    bag1.size shouldBe 2
    val bag2 = Bag.empty + 2 + 1
    bag2.size shouldBe 2
    bag1 shouldBe bag2
  }

  it should "$plus and equals bad" in {
    val bag1 = Bag.empty + 1 + 2
    bag1.size shouldBe 2
    val bag2 = Bag.empty + 2 + 1 + 2
    bag2.size shouldBe 2
    bag1 shouldBe bag2
    val bag3 = Bag.empty + 2 + 1 + 3
    bag3.size shouldBe 3
  }
}
