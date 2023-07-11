/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.core

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import scala.collection.immutable.HashMap

class TreeSpec extends AnyFlatSpec with should.Matchers {

    behavior of "Tree"

    private val red: String = "red"
    private val blue: String = "blue"
    private val green: String = "green"

    it should "unit" in {
        // add edge42 (red <-> blue)
        // add edge17 (red <-> green)
        // TODO eliminate this asInstanceOf
        val vertexMap: BaseVertexMap[String, UndirectedEdge[String, Int], Unit] = UnorderedVertexMap.empty[String, UndirectedEdge[String, Int], Unit].asInstanceOf[BaseVertexMap[String, UndirectedEdge[String, Int], Unit]]
        val tree1: UndirectedTreeCase[String, Int, UndirectedEdge[String, Int], Unit] = UndirectedTreeCase("test1", vertexMap)
        val edge42: UndirectedEdge[String, Int] = UndirectedEdgeCase(red, blue, 42)
        val edge17: UndirectedEdge[String, Int] = UndirectedEdgeCase(red, green, 17)
        val vm2 = vertexMap.unit(buildUpVertexMap(vertexMap, edge42, edge17))
        val tree2: UndirectedTree[String, Int, UndirectedEdge[String, Int], Unit] = tree1.unit(vm2)
        tree2.vertexMap shouldBe vm2
        val edges = tree2.edges
        edges shouldBe Seq(edge42, edge17)
    }

    it should "isCyclic" in {
        // TODO eliminate this asInstanceOf
        val vertexMap: BaseVertexMap[String, UndirectedEdge[String, Int], Unit] = UnorderedVertexMap.empty[String, UndirectedEdge[String, Int], Unit].asInstanceOf[BaseVertexMap[String, UndirectedEdge[String, Int], Unit]]
        val edge42: UndirectedEdge[String, Int] = UndirectedEdgeCase(red, blue, 42)
        val edge17: UndirectedEdge[String, Int] = UndirectedEdgeCase(red, green, 17)
        val vm2 = vertexMap.unit(buildUpVertexMap(vertexMap, edge42, edge17))
        val tree: UndirectedTreeCase[String, Int, UndirectedEdge[String, Int], Unit] = UndirectedTreeCase("test", vm2)
        tree.vertexMap shouldBe vm2
        tree.edges shouldBe Seq(edge42, edge17)
        tree.isCyclic shouldBe false
    }

    private def buildUpVertexMap(vertexMap: BaseVertexMap[String, UndirectedEdge[String, Int], Unit], edge42: UndirectedEdge[String, Int], edge17: UndirectedEdge[String, Int]) = {
        val vRed: Vertex[String, UndirectedEdge[String, Int], Unit] = Vertex.empty(red)
        val vBlue: Vertex[String, UndirectedEdge[String, Int], Unit] = Vertex.empty(blue)
        val vGreen: Vertex[String, UndirectedEdge[String, Int], Unit] = Vertex.empty(green)
        val m1 = new HashMap[String, Vertex[String, UndirectedEdge[String, Int], Unit]]()
        val m2 = vertexMap.buildMap(m1, red, edge42, vRed)
        val m3 = vertexMap.buildMap(m2, blue, edge42, vBlue)
        val m4 = vertexMap.buildMap(m3 - red, red, edge17, m2(red))
        val m5 = vertexMap.buildMap(m4, green, edge17, vGreen)
        m5
    }
}
