/*
 * Copyright (c) 2024. Phasmid Software
 */

package littlegryphon.core

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class VertexMapSpec extends AnyFlatSpec with should.Matchers {

  behavior of "VertexMap"

  private val red = Vertex.create[Int](0)

  it should "get" in {
    val m_ = VertexMap.empty[Int]
    m_.get(0) shouldBe None
    val m0 = m_.+(0 -> red)
    m0.get(0) shouldBe Some(red)

  }

  it should "apply" in {

  }

  it should "dfs" in {

  }

  it should "empty" in {
    VertexMap.empty[Int].map shouldBe Map.empty
  }

}
