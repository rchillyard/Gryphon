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
  private val green = "G"
  private val vRed = Vertex.create(red)
  private val vBlue = Vertex.create(blue)
  private val vGreen = Vertex.create(green)

  it should "+" in {
    val m_ = VertexMap.empty[String]
    val mRB = m_ + Pair(red, blue)
    mRB.get(red) map (_.attribute) shouldBe Some(red)
    mRB.get(blue) map (_.attribute) shouldBe Some(blue)
  }

  it should "get" in {
    val m_ = VertexMap.empty[String]
    m_.get(red) shouldBe None
    val m0: VertexMap[String] = m_ + (red, vRed) + (blue, vBlue)
    m0.get(red) shouldBe Some(vRed)
  }

  it should "apply" in {
    val map = VertexMap(Map(red -> vRed))
    map.map.size shouldBe 1
  }

  it should "dfs1" in {
    val m_ = VertexMap.empty[String]
    val mRB = m_ + Pair(red, blue) + Pair(red, green) + Pair(blue, green)
    val bag1 = ListBag.create(VertexPair(vRed, vBlue), VertexPair(vRed, vGreen))
    val bag2 = ListBag.create(VertexPair(vBlue, vGreen))
    mRB.get(red) map (_.connexions) shouldBe Some(bag1)
    mRB.get(blue) map (_.connexions) shouldBe Some(bag2)
  }

  it should "empty" in {
    VertexMap.empty[String].map shouldBe Map.empty
  }

}
