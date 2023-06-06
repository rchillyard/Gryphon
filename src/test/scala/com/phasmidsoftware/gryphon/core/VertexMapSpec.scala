/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.core

import com.phasmidsoftware.gryphon.visit.{IterableVisitor, Visitor}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import scala.collection.immutable.{HashMap, Queue}

class VertexMapSpec extends AnyFlatSpec with should.Matchers {

    private val vertexA = "A"
    private val vertexB = "B"

    behavior of "VertexMap"

    it should "dfs" in {
        import com.phasmidsoftware.gryphon.visit.Journal._
        val vertexMap: VertexMap[String, DirectedEdgeCase[String, Int], Unit] = OrderedVertexMap.empty
        val target = vertexMap.addEdge("A", DirectedEdgeCase("A", "B", 1)).addEdge("B", DirectedEdgeCase("B", "C", 2)).addVertex("C")
        val visitor = Visitor.createPre[String]
        val result = target.dfs(visitor)("A")
        result.journal shouldBe Queue("A", "B", "C")
    }

    it should "bfs" in {
        import com.phasmidsoftware.gryphon.visit.Journal._
        val vertexMap: VertexMap[String, DirectedEdgeCase[String, Int], Unit] = OrderedVertexMap.empty
        val target = vertexMap.addEdge("A", DirectedEdgeCase("A", "B", 1)).addVertex("B").addEdge("A", DirectedEdgeCase("A", "D", 3)).addVertex("D").addEdge("A", DirectedEdgeCase("A", "C", 2)).addVertex("C")
        val visitor = Visitor.createPre[String]
        val result = target.bfs(visitor)("A")
        result.journal shouldBe Queue("A", "C", "D", "B")
    }

    it should "bfsMutable" in {
        import com.phasmidsoftware.gryphon.visit.Journal._
        val vertexMap: VertexMap[String, DirectedEdgeCase[String, Int], Unit] = OrderedVertexMap.empty
        val target = vertexMap.addEdge("A", DirectedEdgeCase("A", "B", 1)).addVertex("B").addEdge("A", DirectedEdgeCase("A", "D", 3)).addVertex("D").addEdge("A", DirectedEdgeCase("A", "C", 2)).addVertex("C")
        val visitor = Visitor.createPreQueue[String]
        val result = target.bfsMutable(visitor)("A")
        result match {
            case x: IterableVisitor[String, _] => x.iterator.toSeq shouldBe Seq("A", "C", "D", "B")
        }
    }

    behavior of "OrderedVertexMapCase"

    it should "keys" in {
        val target: VertexMap[String, DirectedEdgeCase[String, Int], Unit] = OrderedVertexMap.empty
        target.keys shouldBe Set.empty
    }

    it should "values" in {
        val target: VertexMap[String, DirectedEdgeCase[String, Int], Unit] = OrderedVertexMap.empty
        target.values.isEmpty shouldBe true
    }

    it should "addEdge" in {
        val target: VertexMap[String, DirectedEdgeCase[String, Int], Unit] = OrderedVertexMap.empty
        val edge: DirectedEdgeCase[String, Int] = DirectedEdgeCase(vertexA, vertexB, 42)
        val targetUpdated = target.addEdge(vertexA, edge)
        targetUpdated.keys shouldBe Set(vertexA)
        targetUpdated.values.toSeq shouldBe Seq(VertexCase[String, DirectedEdgeCase[String, Int], Unit]("A", AdjacencyList(List(edge))))
        targetUpdated.edges shouldBe Seq(edge)
    }

    it should "addVertex" in {
        val target: VertexMap[String, DirectedEdgeCase[String, Int], Unit] = OrderedVertexMap.empty
        val targetUpdated = target.addVertex(vertexA)
        targetUpdated.keys shouldBe Set(vertexA)
        targetUpdated.edges.isEmpty shouldBe true
    }

    behavior of "UnorderedVertexMapCase"

    private val red: Color = Color("red")
    private val blue: Color = Color("blue")
    private val green: Color = Color("green")

    it should "keys" in {
        val target: VertexMap[Color, DirectedEdgeCase[Color, Int], Unit] = UnorderedVertexMap.empty
        target.keys shouldBe Set.empty
    }

    it should "values" in {
        val target: VertexMap[Color, DirectedEdgeCase[Color, Int], Unit] = UnorderedVertexMap.empty
        target.values.isEmpty shouldBe true
    }

    it should "addEdge" in {
        val target: VertexMap[Color, DirectedEdgeCase[Color, Int], Unit] = UnorderedVertexMap.empty
        val edge: DirectedEdgeCase[Color, Int] = DirectedEdgeCase(red, blue, 42)
        val targetUpdated = target.addEdge(red, edge)
        targetUpdated.keys shouldBe Set(red)
        targetUpdated.edges shouldBe Seq(edge)
    }

    it should "addVertex" in {
        val target: VertexMap[Color, DirectedEdgeCase[Color, Int], Unit] = UnorderedVertexMap.empty
        val targetUpdated = target.addVertex(red)
        targetUpdated.keys shouldBe Set(red)
        targetUpdated.edges.isEmpty shouldBe true
    }

    it should "buildMap" in {
        // TODO eliminate this asInstanceOf
        val target: BaseVertexMap[Color, DirectedEdge[Color, Int], Unit] = UnorderedVertexMap.empty[Color, DirectedEdge[Color, Int], Unit].asInstanceOf[BaseVertexMap[Color, DirectedEdge[Color, Int], Unit]]
        val edge42: DirectedEdge[Color, Int] = DirectedEdgeCase(red, blue, 42)
        val edge17: DirectedEdge[Color, Int] = DirectedEdgeCase(red, green, 17)
        val m1 = new HashMap[Color, Vertex[Color, DirectedEdge[Color, Int], Unit]]()
        val vRed: Vertex[Color, DirectedEdge[Color, Int], Unit] = Vertex.empty(red)
        val m2: Map[Color, Vertex[Color, DirectedEdge[Color, Int], Unit]] = target.buildMap(m1, red, edge42, vRed)
        m2 shouldBe new HashMap[Color, Vertex[Color, DirectedEdge[Color, Int], Unit]] + (red -> VertexCase[Color, DirectedEdge[Color, Int], Unit](red, AdjacencyList(List(edge42))))
        val vBlue: Vertex[Color, DirectedEdge[Color, Int], Unit] = Vertex.empty(blue)
        val vGreen: Vertex[Color, DirectedEdge[Color, Int], Unit] = Vertex.empty(green)
        val m3: Map[Color, Vertex[Color, DirectedEdge[Color, Int], Unit]] = target.buildMap(m2, blue, edge42, vBlue)
        m3 shouldBe m2 + (blue -> VertexCase[Color, DirectedEdge[Color, Int], Unit](blue, AdjacencyList(List(edge42))))
        val m4: Map[Color, Vertex[Color, DirectedEdge[Color, Int], Unit]] = target.buildMap(m3, red, edge17, vRed)
        m4 shouldBe m3 + (red -> VertexCase[Color, DirectedEdge[Color, Int], Unit](red, AdjacencyList(List(edge17))))
        val m5: Map[Color, Vertex[Color, DirectedEdge[Color, Int], Unit]] = target.buildMap(m4, green, edge17, vGreen)
        m5 shouldBe m4 + (green -> VertexCase[Color, DirectedEdge[Color, Int], Unit](green, AdjacencyList(List(edge17))))
    }
}
