package littlegryphon.core

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class Unordered_SetSpec extends AnyFlatSpec with Matchers {

  behavior of "Unordered_Set"

  private val set1: Set[Int] = Set(1)

  it should "iterator" in {
    val target: Unordered[Int] = Unordered_Set(Seq(1))
    val iterator = target.iterator
    iterator.hasNext shouldBe true
    iterator.next() shouldBe 1
    iterator.hasNext shouldBe false
  }

  it should "copy" in {
    val target: Unordered_Set[Int] = Unordered_Set(Set(1))
    target.copy(elements = Set(2))
  }

  it should "isEmpty 1" in {
    val target: Unordered[Int] = Unordered_Set(Seq(1))
    target.isEmpty shouldBe false
  }

  it should "isEmpty 2" in {
    val target: Unordered[Int] = Unordered_Set.empty
    target.isEmpty shouldBe true
  }

  it should "$plus" in {
    val target: Unordered[Int] = Unordered_Set(Seq(1))
    val set2 = set1 + 2
    target + 2 should matchPattern { case Unordered_Set(`set2`) => }
  }

  it should "contains" in {
    val target: Unordered[Int] = Unordered_Set(Seq(1))
    target.contains(1) shouldBe true
  }

  it should "size" in {
    val target: Unordered[Int] = Unordered_Set(Seq(1))
    target.size shouldBe 1
  }

  it should "apply" in {
    val target: Unordered[Int] = Unordered_Set[Int](Seq(1))
    target should matchPattern { case Unordered_Set(`set1`) => }

  }

  it should "create" in {
    val target: Unordered[Int] = Unordered_Set.create[Int](1)
    target should matchPattern { case Unordered_Set(`set1`) => }
  }

}
