package com.phasmidsoftware.gryphon.core

import com.phasmidsoftware.gryphon.util.TryUsing
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.net.URL
import scala.io.Source
import scala.util.{Failure, Success, Try}

class SerializableGraphSpec extends AnyFlatSpec with Matchers {

  behavior of "SerializableGraph"

  val dijkstraGraphPath = "dijkstra.graph"

  it should "createFromEdges" in {

  }

  it should "createFromVertexPairs" in {

  }

  it should "Triplets.parse" in {
    val triedSource = Try(Source.fromResource(dijkstraGraphPath))
    val wsy: Try[Seq[String]] = TryUsing.trial(triedSource)(_.getLines().toSeq)
    println(wsy)
    wsy.isSuccess shouldBe true
    wsy.get.size shouldBe 16
    
  }

  it should "Triplets.parse 3" in {
    val url: URL = Thread.currentThread().getContextClassLoader.getResource(dijkstraGraphPath)
    val triedSource = Try(Source.fromURL(url, "UTF-8"))
    val triedStrings = triedSource match {
      case Success(source) =>
        val result: Seq[String] = source.getLines().toSeq
        source.close()
        Success(result)
      case Failure(e) => Failure(e)
    }
    triedStrings.isSuccess shouldBe true
    triedStrings.get.size shouldBe 16
  }

}
