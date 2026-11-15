/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.adjunct

import com.phasmidsoftware.gryphon.core.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

/**
 * `unit` is what every derived graph is rebuilt through, so it must preserve the
 * kind of graph it was called on. `addVertex` is the public route to it.
 */
class AddVertexUnitSpec extends AnyFlatSpec with should.Matchers:

  behavior of "unit, via addVertex"

  it should "keep an UndirectedGraph undirected" in :
    val g = UndirectedGraph[Int, Double](VertexMap[Int])
    val h = g.addVertex(Vertex.createWithSet[Int](1))
    h shouldBe a[UndirectedGraph[?, ?]]

  it should "keep a DirectedGraph directed" in :
    val g = DirectedGraph[Int, Double](VertexMap[Int])
    val h = g.addVertex(Vertex.createWithSet[Int](1))
    h shouldBe a[DirectedGraph[?, ?]]
