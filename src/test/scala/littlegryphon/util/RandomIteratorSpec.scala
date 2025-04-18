package littlegryphon.util

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.util.Random

class RandomIteratorSpec extends AnyFlatSpec with Matchers {

  behavior of "RandomIterator"

  private val list = List(1, 2, 3, 4, 5)

  it should "iterator 0" in {
    implicit val random: Random = new scala.util.Random(0)
    val iterator = RandomIterator(list)
    iterator.hasNext shouldBe true
    iterator.next() shouldBe 5
    iterator.hasNext shouldBe true
    iterator.next() shouldBe 3
    iterator.hasNext shouldBe true
    iterator.next() shouldBe 4
    iterator.hasNext shouldBe true
    iterator.next() shouldBe 1
    iterator.hasNext shouldBe true
    iterator.next() shouldBe 2
    iterator.hasNext shouldBe false
  }

  it should "iterator 1" in {
    implicit val random: Random = new scala.util.Random(1)
    val iterator = RandomIterator(list)
    iterator.hasNext shouldBe true
    iterator.next() shouldBe 3
    iterator.hasNext shouldBe true
    iterator.next() shouldBe 2
    iterator.hasNext shouldBe true
    iterator.next() shouldBe 5
    iterator.hasNext shouldBe true
    iterator.next() shouldBe 1
    iterator.hasNext shouldBe true
    iterator.next() shouldBe 4
    iterator.hasNext shouldBe false
  }

  it should "iterator 2" in {
    implicit val random: Random = new scala.util.Random(2)
    val iterator = RandomIterator(list)
    iterator.hasNext shouldBe true
    iterator.next() shouldBe 1
    iterator.hasNext shouldBe true
    iterator.next() shouldBe 2
    iterator.hasNext shouldBe true
    iterator.next() shouldBe 3
    iterator.hasNext shouldBe true
    iterator.next() shouldBe 5
    iterator.hasNext shouldBe true
    iterator.next() shouldBe 4
    iterator.hasNext shouldBe false
  }

}
