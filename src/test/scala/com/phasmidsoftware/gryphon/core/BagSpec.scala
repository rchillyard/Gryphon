package com.phasmidsoftware.gryphon.core

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class BagSpec extends AnyFlatSpec with Matchers {

  behavior of "Bag"

  private val bag1: Bag[Int] = Bag.create(1)
  private val emptyBag: Bag[Nothing] = Bag.empty

  it should "isEmpty" in {
    emptyBag.iterator.isEmpty shouldBe true
    bag1.iterator.isEmpty shouldBe false
    emptyBag.iterator.size shouldBe 0
  }

  it should "nonEmpty" in {
    emptyBag.iterator.nonEmpty shouldBe false
    bag1.iterator.nonEmpty shouldBe true
    bag1.iterator.size shouldBe 1
  }

  it should "$plus" in {
    val bag = bag1 + 2
    bag.iterator.size shouldBe 2
    val list = bag.iterator.toSeq
    list.contains(1) shouldBe true
    list.contains(2) shouldBe true
    val iterator = bag.iterator
    iterator.hasNext shouldBe true
    iterator.next()
    iterator.hasNext shouldBe true
    iterator.next()
    iterator.hasNext shouldBe false
  }

  it should "iterator" in {
    val iterator = bag1.iterator
    iterator.hasNext shouldBe true
    iterator.next()
    iterator.hasNext shouldBe false
  }

  it should "contains" in {
    bag1.contains(1) shouldBe true
    emptyBag.contains(1) shouldBe false
  }

  it should "create" in {
    val bag: Bag[Int] = Bag.create(1, 42, 99)
    bag should matchPattern { case ListBag(Seq(1, 42, 99)) => }
  }

}
