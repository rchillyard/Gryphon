/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.newcore.unready

import com.phasmidsoftware.gryphon.newcore.Vertex
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class VertexSpec extends AnyFlatSpec with should.Matchers {

  behavior of "Vertex"

  it should "attribute" in {
    Vertex.empty[String, Nothing, Unit]("A").attribute shouldBe "A"
  }

//    it should "getProperty Unit" in {
//        val vertex = Vertex.empty[String, Nothing, Unit]("A")
//        vertex.getProperty shouldBe ()
//        vertex.setProperty(())
//        vertex.getProperty shouldBe ()
//    }
//    it should "getProperty VertexProp" in {
//        val vertex = Vertex.empty[String, Nothing, VertexProp]("A")
//        vertex.getProperty shouldBe VertexPropHasZero.zero
//        vertex.setProperty(VertexProp(Some(1)))
//        vertex.getProperty shouldBe VertexProp(Some(1))
//    }
//    it should "getProperty EdgeProperty" in {
//        case class EdgeProperty[X <: Edge[String,Int]](maybeEdge: Option[X])
//        object EdgeProperty {
//            implicit object EdgePropertyHasZero extends HasZero[EdgeProperty[DirectedEdge[String,Int]]] {
//                def zero: EdgeProperty[DirectedEdge[String, Int]] = EdgeProperty(None)
//            }
//        }
//        import EdgeProperty._
//        val vertex = Vertex.empty[String, DirectedEdge[String,Int], EdgeProperty[DirectedEdge[String,Int]]]("A")
//        val edge = DirectedEdgeCase[String,Int]("A","B",1)
//        vertex.setProperty(EdgeProperty(Some(edge)))
//        vertex.getProperty shouldBe EdgeProperty(Some(edge))
//    }

  it should "degree" in {
    Vertex.empty[String, Nothing, Unit]("A").degree shouldBe 0
    Vertex.empty[String, Nothing, Unit]("A").addEdge(DirectedEdgeCase("A", "B", "ab")).degree shouldBe 1
  }

  it should "adjacent" in {
    Vertex.empty[String, Nothing, Unit]("A").adjacent shouldBe AdjacencyList.empty
    Vertex.empty[String, Nothing, Unit]("A").addEdge(DirectedEdgeCase("A", "B", "ab")).adjacent shouldBe AdjacencyList(Seq(DirectedEdgeCase("A", "B", "ab")))
  }

  it should "addEdge" in {
    val a = Vertex.empty[String, DirectedEdgeCase[String, String], Unit]("A")
    //noinspection RedundantNewCaseClass
    a.addEdge(DirectedEdgeCase("A", "B", "ab")) shouldBe new VertexCase[String, DirectedEdge[String, String], Unit]("A", AdjacencyList(Seq(DirectedEdgeCase("A", "B", "ab")))) // leave "new" intact.
  }
}
