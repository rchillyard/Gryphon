package littlegryphon.core

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class AttributeSpec extends AnyFlatSpec with should.Matchers {

  case class IntAttribute(attribute: Int) extends Attribute[Int]

  behavior of "Attribute"
  it should "work for IntAttribute(1)" in {
    IntAttribute(1).attribute shouldBe 1
  }

}
