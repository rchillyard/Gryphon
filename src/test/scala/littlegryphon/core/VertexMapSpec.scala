/*
 * Copyright (c) 2024. Phasmid Software
 */

package littlegryphon.core

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class VertexMapSpec extends AnyFlatSpec with should.Matchers {

  behavior of "VertexMap"

  private val red = "R"
  private val blue = "B"
  private val vRed = Vertex.create(red)
  private val vBlue = Vertex.create(blue)

  it should "+" in {
    val m_ = VertexMap.empty[String]
    val mRB = m_ + Pair(red, blue)
    mRB.get(red) map (_.attribute) shouldBe Some(red)
    mRB.get(blue) map (_.attribute) shouldBe Some(blue)
  }

  it should "get" in {
    val m_ = VertexMap.empty[String]
    m_.get(red) shouldBe None
    val m0 = m_ + (red, vRed)
    m0.get(red) shouldBe Some(vRed)
  }

  it should "apply" in {

  }

  it should "dfs" in {

  }

  it should "empty" in {
    VertexMap.empty[String].map shouldBe Map.empty
  }

}
