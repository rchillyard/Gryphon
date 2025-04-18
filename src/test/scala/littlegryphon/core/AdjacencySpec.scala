package littlegryphon.core

import littlegryphon.adjunct.DirectedEdge
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class AdjacencySpec extends AnyFlatSpec with Matchers {

  behavior of "AdjacencyVertex"

  it should "vertex" in {
    val adjacency = AdjacencyVertex(Vertex.create(1))
    adjacency.vertex.attribute shouldBe 1
  }

  behavior of "AdjacencyEdge"

  it should "vertex" in {
    val vertex1 = Vertex.create(1)
    val vertex2 = Vertex.create(2)
    val adjacency1 = AdjacencyEdge(DirectedEdge[String, Int]("child", vertex1, vertex2))
    adjacency1.edge.attribute shouldBe "child"
    adjacency1.vertex.attribute shouldBe 2
    val adjacency2 = AdjacencyEdge(DirectedEdge[String, Int]("child", vertex1, vertex2), flipped = true)
    adjacency2.edge.attribute shouldBe "child"
    adjacency2.vertex.attribute shouldBe 1
  }

}
