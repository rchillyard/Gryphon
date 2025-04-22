/*
 * Copyright (c) 2024. Phasmid Software
 */

package com.phasmidsoftware.gryphon.visit

import com.phasmidsoftware.gryphon.core.{Adjacency, Unordered, Vertex}
import com.phasmidsoftware.gryphon.util.GraphException
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.annotation.tailrec
import scala.collection.immutable.Queue

class QueueableSpec extends AnyFlatSpec with Matchers {

  behavior of "Queueable"

  def map: Map[Int, Vertex[Int]] = Map(1 -> Vertex.createWithBag(1), 2 -> Vertex.createWithBag(2), 3 -> Vertex.createWithBag(3))

  private def doBFSImmutable[J](visitor: Visitor[Int, J], v: Int)(goal: Int => Boolean)(implicit queueable: Queueable[Queue[Int], Int]): Visitor[Int, J] = {
    @tailrec
    def inner(result: Visitor[Int, J], work: Queue[Int]): Visitor[Int, J] = queueable.take(work) match {
      case Some((head, _)) if goal(head) => result.visitPre(head)
      case Some((head, tail)) => inner(result.visitPre(head), enqueueUnvisitedVertices(head, tail))
      case _ => result
    }

    inner(visitor, queueable.append(queueable.empty, v))
  }

  private def enqueueUnvisitedVertices(v: Int, queue: Queue[Int])(implicit queueable: Queueable[Queue[Int], Int]): Queue[Int] = optAdjacencyList(v) match {
    case Some(vau: Unordered[Adjacency[Int]]) =>
      val iterator: Iterator[Adjacency[Int]] = vau.iterator
      iterator.foldLeft(queue) { (q, x) =>
        if (!x.vertex.discovered) {
          x.vertex.discovered = true
          queueable.append(q, x.vertex.attribute)
        }
        else
          q
      }
    case None => throw GraphException(s"BFS logic error 0: enqueueUnvisitedVertices(v = $v)")
  }

  private def optAdjacencyList(v: Int): Option[Unordered[Adjacency[Int]]] = map.get(v) map (_.adjacencies)

  ignore should "take" in {
    // NOTE this is a test of the Queueable trait, not the implementation of bfsMutable.
    // Admittedly, it's very roundabout way of testing Queuable but the idea is to remove
    // bits we don't need until we're left with just the code we want to test.
    val visitor: PreVisitor[Int, Queue[Int]] = Visitor.createPre[Int]
    import Queueable.*
    val result = doBFSImmutable(visitor, 1)(x => x == 3)
    // TODO this is not working because we haven't entered the adjacencies for the vertices as are done in VertexMapSpec.
    result.journal.size shouldBe 3
    result.journal.head shouldBe 1
    result.journal.head shouldBe 3
  }

  it should "appendAll" in {

  }

}
