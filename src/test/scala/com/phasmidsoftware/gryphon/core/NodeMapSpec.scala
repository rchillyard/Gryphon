/*
 * Copyright (c) 2024. Phasmid Software
 */

package com.phasmidsoftware.gryphon.core

import com.phasmidsoftware.gryphon.visit.Visitor
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import scala.collection.immutable.Queue

class NodeMapSpec extends AnyFlatSpec with should.Matchers {

  behavior of "NodeMap"

  private val red = "R"
  private val blue = "B"
  private val green = "G"
  private val vRed = Node.create(red)
  private val vBlue = Node.create(blue)
  private val vGreen = Node.create(green)

  private val rb: Pair[String] = Pair(red, vBlue)
  it should "+ Pair" in {
    val m_ = NodeMap.empty[String]
    val target = m_ + rb
    target.get(red) map (_.attribute) shouldBe Some(red)
    target.get(blue) map (_.attribute) shouldBe Some(blue)
  }

//  it should "+ VertexPair" in {
//    val m_ = XMap.empty[String]
//    val target = m_ + Pair(vRed, vBlue)
//    target.get(red) map (_.attribute) shouldBe Some(red)
//    target.get(blue) map (_.attribute) shouldBe Some(blue)
//    target.get(red) map (_.connexions) shouldBe Some(ListBag.create(VertexPair(vRed, vBlue)))
//    target.get(blue) map (_.connexions) shouldBe Some(Bag.empty)
//  }

  it should "get" in {
    val m_ = NodeMap.empty[String]
    m_.get(red) shouldBe None
    val m0: XMap[String, Node] = m_ + (red, vRed) + (blue, vBlue)
    m0.get(red) shouldBe Some(vRed)
  }

  it should "apply" in {
    val map = NodeMap(Map(red -> vRed))
    map.map.size shouldBe 1
  }

  it should "dfs1" in {
    val m_ = NodeMap.empty[String]
    val rg = Pair(red, vGreen)
    val bg = Pair(blue, vGreen)
    val target: XMap[String, Node] = m_ + rb + rg + bg
    val bag1 = ListBag.create(rb, rg)
    val bag2 = ListBag.create(bg)
    target.get(red) map (_.connexions) shouldBe Some(bag1)
    target.get(blue) map (_.connexions) shouldBe Some(bag2)
    val visitor = Visitor.createPre[String]
    val result = target.dfs(visitor)(red)
    result.journal shouldBe Queue(red, blue, green)

  }

  it should "empty" in {
    NodeMap.empty[String].map shouldBe Map.empty
  }

}
