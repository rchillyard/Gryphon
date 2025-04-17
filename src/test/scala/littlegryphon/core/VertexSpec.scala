package littlegryphon.core

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class VertexSpec extends AnyFlatSpec with should.Matchers {

  behavior of "Vertex"
  val target: Vertex[Int] = Vertex.create(() => Unordered_Bag.create[Adjacency[Int]]())(1)

  it should "discovered" in {
    target.discovered shouldBe false
    target.discovered = true
    target.discovered shouldBe true
  }

  it should "adjacencies" in {
    target.adjacencies shouldBe Unordered_Bag(Bag.empty)
  }

  it should "attribute" in {
    target.attribute shouldBe 1
  }

  it should "create" in {
    val iv: Vertex[Int] = Vertex.create(() => Unordered_Bag.create(AdjacencyVertex(target)))(2)

  }

}
