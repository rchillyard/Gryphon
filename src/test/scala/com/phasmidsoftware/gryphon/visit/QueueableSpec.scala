/*
 * Copyright (c) 2024. Phasmid Software
 */

package com.phasmidsoftware.gryphon.visit

import com.phasmidsoftware.gryphon.core.*
import com.phasmidsoftware.gryphon.util.GraphException
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.annotation.tailrec
import scala.collection.immutable.Queue

class QueueableSpec extends AnyFlatSpec with Matchers {

  behavior of "Queueable"

  private val v1 = Vertex.createWithBag(1)
  private val v2 = Vertex.createWithBag(2)
  private val v3 = Vertex.createWithBag(3)

  private val v2_av = v2 + AdjacencyVertex(v3)
  private val v1_av = v1 + AdjacencyVertex(v2_av)

  private val vxVm: Map[Int, Vertex[Int]] =
    Map.empty[Int, Vertex[Int]] + (3 -> v3) + (2 -> v2_av) + (1 -> v1_av)

  /**
   * Recursively processes vertices starting from the given vertex `v`, applying a visitor function for each vertex
   * encountered prior to further recursion (or queue expansion).
   * The traversal stops when the `goal` function evaluates to true for a vertex.
   * NOTE that this method has a concrete type `Int` instead of `V` (for testing), not a parametric type.
   *
   * This method uses an inner tail-recursive function to iterate over a queue of vertices,
   * applying the visitor's `visitPre` method at each step, and enqueues adjacent, unvisited vertices.
   *
   * @param visitor   the `Visitor` instance used to process vertices during traversal.
   * @param v         the starting vertex (as an `Int`).
   * @param goal      a function that evaluates a condition for stopping traversal on a given vertex.
   * @param queueable an implicit type class instance to handle operations on the queue-like structure.
   * @tparam J the type representing the journal of the `Visitor`.
   * @return an updated `Visitor` instance after processing all discovered vertices in the graph.
   */
  private def takeRecursively[J](visitor: Visitor[Int, J], v: Int)(goal: Int => Boolean)(implicit queueable: Queueable[Queue[Int], Int]): Visitor[Int, J] = {
    @tailrec
    def inner(result: Visitor[Int, J], work: Queue[Int]): Visitor[Int, J] = queueable.take(work) match {
      case Some((head, _)) if goal(head) => result.visitPre(head)
      case Some((head, tail)) =>
        vxVm(head).discovered = true
        val queue = enqueueUnvisitedVertices(vxVm, head, tail)
        inner(result.visitPre(head), queue)
      case _ => result
    }

    inner(visitor, queueable.append(queueable.empty, v))
  }

  /**
   * Enqueues all unvisited vertices adjacent to the given vertex into the provided queue.
   * NOTE that this method has a concrete type `Int` (for testing), not a parametric type.
   *
   * This method processes the adjacencies of the vertex `v` by iterating over them.
   * If an adjacent vertex has not yet been discovered, it is marked as discovered
   * and appended to the queue. If the vertex `v` does not exist in the adjacency list,
   * an exception is thrown.
   *
   * @param v         the vertex whose unvisited adjacent vertices are to be enqueued
   * @param queue     the queue into which the unvisited vertices will be appended
   * @param queueable the implicit type class to handle the behavior of the queue-like object
   * @return a new queue containing the original elements and the newly enqueued vertices
   * @throws GraphException if the vertex `v` does not exist in the adjacency list
   */
  private def enqueueUnvisitedVertices(map: Map[Int, Vertex[Int]], v: Int, queue: Queue[Int])(implicit queueable: Queueable[Queue[Int], Int]): Queue[Int] = optAdjacencyList(v) match {
    case Some(vau: Unordered[Adjacency[Int]]) =>
      vau.iterator.foldLeft(queue) {
        (vq, va) =>
          VertexMap.maybeUndiscoveredVertex(map, va.vertex).map(v => queueable.append(vq, v.attribute)).getOrElse(vq)
      }
    case None => throw GraphException(s"BFS logic error 0: enqueueUnvisitedVertices(v = $v)")
  }

  /**
   * Retrieves the list of adjacencies for a given vertex if it exists in the graph.
   * This method searches `vxVm` for the specified vertex and, if found,
   * returns its associated adjacencies wrapped in an `Option`.
   * NOTE that this method has a concrete type `Int` (for testing), not a parametric type.
   *
   * @param v the vertex identifier for which adjacencies are to be retrieved.
   * @return an `Option` containing the unordered collection of adjacencies for the given vertex,
   *         or `None` if the vertex does not exist in the graph.
   */
  private def optAdjacencyList(v: Int): Option[Unordered[Adjacency[Int]]] = vxVm.get(v) map (_.adjacencies)

  ignore should "take" in {
    // NOTE this is a test of the Queueable trait, not the implementation of bfsMutable.
    // Admittedly, it's very roundabout way of testing Queuable but the idea is to remove
    // bits we don't need until we're left with just the code we want to test.

    println(s"vxVm = $vxVm")

    val visitor: PreVisitor[Int, Queue[Int]] = Visitor.createPre[Int]
    import Queueable.*
    val result = takeRecursively(visitor, 1)(x => x == 3)
    // TODO this is not working because we haven't entered the adjacencies for the vertices as is done in VertexMapSpec.
    val journal = result.journals.head
    journal.size shouldBe 3
    journal.head shouldBe 1
    journal.head shouldBe 3
  }

  it should "appendAll" in {

  }

}
