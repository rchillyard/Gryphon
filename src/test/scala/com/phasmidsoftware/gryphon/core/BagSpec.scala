package com.phasmidsoftware.gryphon.core

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scala.util.Random

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

  behavior of "Bag randomisation (#67)"

  private val tenValues: Bag[Int] = Bag.create((0 to 9)*)

  // Up to 1.10.0 iterator shuffled, using a process-global generator nobody could reach.
  it should "iterate in insertion order" in {
    tenValues.iterator.toSeq shouldBe (0 to 9)
  }

  // The property that was missing: the same seed must give the same order. Two separate
  // Random(42) instances, not one reused, so this pins the seed and not merely the stream.
  it should "give the same random order for the same seed" in {
    val first = tenValues.randomIterator(using Random(42)).toSeq
    val second = tenValues.randomIterator(using Random(42)).toSeq
    first shouldBe second
  }

  it should "give different orders for different seeds" in {
    val a = tenValues.randomIterator(using Random(1)).toSeq
    val b = tenValues.randomIterator(using Random(2)).toSeq
    a should not be b
  }

  // Confirms the shuffling is live: a randomIterator that quietly returned insertion order
  // would satisfy both tests above as long as it did so consistently.
  it should "actually shuffle" in {
    val orders = (1 to 50).map(seed => tenValues.randomIterator(using Random(seed)).toSeq)
    orders.count(_ != (0 to 9)) should be > 45
    orders.foreach(_.sorted shouldBe (0 to 9))
  }

  // A given in scope must be the one consulted. Before the fix the ambient Bag.random won,
  // and the compiler reported a local `given Random` as unused.
  it should "take the Random from the caller's given" in {
    given Random = Random(42)

    tenValues.randomIterator.toSeq shouldBe tenValues.randomIterator(using Random(42)).toSeq
  }

}
