package com.phasmidsoftware.gryphon.core

import com.phasmidsoftware.gryphon.util.TryUsing
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.io.Source
import scala.util.Try

class SerializableGraphSpec extends AnyFlatSpec with Matchers {

  behavior of "SerializableGraph"

  val dijkstraGraphPath = "/dijkstra.graph"

  it should "createFromEdges" in {

  }

  it should "createFromVertexPairs" in {

  }
  
  ignore should "Edges.parse" in {
    val triedSource = Try(Source.fromResource(dijkstraGraphPath))
    val wsy: Try[Iterator[String]] = TryUsing.trial(triedSource) {
      source => source.getLines()
    }
    println(wsy)
    wsy.isSuccess shouldBe true
    wsy.get.size shouldBe 10
  }

}
