package com.phasmidsoftware.gryphon.core

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class Unordered_BagSpec extends AnyFlatSpec with Matchers {

  behavior of "Unordered_Bag"

  it should "iterator" in {
    val target: Unordered[Int] = Unordered_Bag(Seq(1))
    val iterator = target.iterator
    iterator.hasNext shouldBe true
    iterator.next() shouldBe 1
    iterator.hasNext shouldBe false
  }

  it should "copy" in {
    val target: Unordered_Bag[Int] = Unordered_Bag.create(1)
    target.copy(elements = Bag.create(1, 2))
  }

  it should "isEmpty 1" in {
    val target: Unordered[Int] = Unordered_Bag(Seq(1))
    target.isEmpty shouldBe false
  }

  it should "isEmpty 2" in {
    val target: Unordered[Int] = Unordered_Bag.empty
    target.isEmpty shouldBe true
  }

  it should "$plus" in {
    val target: Unordered[Int] = Unordered_Bag(Seq(1))
    target + 2 should matchPattern { case Unordered_Bag(ListBag(Seq(1, 2))) => }
  }

  it should "contains" in {
    val target: Unordered[Int] = Unordered_Bag(Seq(1))
    target.contains(1) shouldBe true
  }

  it should "size" in {
    val target: Unordered[Int] = Unordered_Bag(Seq(1))
    target.size shouldBe 1
  }

  it should "apply" in {
    val target: Unordered[Int] = Unordered_Bag[Int](Seq(1))
    target should matchPattern { case Unordered_Bag(ListBag(Seq(1))) => }

  }

  it should "create" in {
    val target: Unordered[Int] = Unordered_Bag.create[Int](1)
    target should matchPattern { case Unordered_Bag(ListBag(Seq(1))) => }
  }

}
