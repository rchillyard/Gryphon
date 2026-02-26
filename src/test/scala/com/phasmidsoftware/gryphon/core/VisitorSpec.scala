/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.core

import com.phasmidsoftware.visitor.core.{Evaluable, GraphNeighbours, JournaledVisitor, Traversal, given_VisitedSet_V}
import java.io.FileWriter
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import scala.collection.immutable.Queue

/**
 * Test suite for various visitor implementations.
 *
 * This class contains test cases to validate the behaviors of different
 * visitor types, including pre-order and post-order traversal strategies,
 * as well as their interactions with journals and state tracking.
 *
 * For the missing unit tests, see `TranslatedOldVisitorSpec.scala`
 *
 * Key areas covered include:
 * - Validation of post-order traversal using `PostVisitor`.
 * - Validation of pre-order traversal using `PreVisitor`.
 * - Combined behaviors in `Visitor` and tests for various joining strategies.
 * - Testing the iterable versions of visitors, `PostVisitorIterable`
 *   and `PreVisitorIterable`.
 * - Ensuring correct functionality of state journaling mechanisms.
 *
 * Extends:
 * - `AnyFlatSpec` for defining behavior-driven test specifications.
 * - `should.Matchers` for expressive assertions.
 *
 * Behavior:
 * - "PostVisitor": Tests the post-order traversal behavior.
 * - "PreVisitor": Tests the pre-order traversal behavior.
 * - "PostVisitorIterable": Tests the iterable post-order visitor behavior.
 * - "PreVisitorIterable": Tests the iterable pre-order visitor behavior.
 * - "Visitor": Tests combined pre-order and post-order behaviors as well
 *   as joined visitor functionalities.
 */
class VisitorSpec extends AnyFlatSpec with should.Matchers {

    behavior of "PostVisitor"

    it should "traverse post-order and ignore pre-order concerns" in {
        import com.phasmidsoftware.visitor.core.DfsOrder.Post

        // Define a tiny two-node graph: 1 → 2
        given GraphNeighbours[Int] with
            def neighbours(n: Int): Iterator[Int] = n match
                case 1 => Iterator(2)
                case _ => Iterator.empty

        given Evaluable[Int, Int] with
            def evaluate(v: Int): Option[Int] = Some(v)

        val visitor = JournaledVisitor.withQueueJournal[Int, Int]
        val result = Traversal.dfs(1, visitor, Post)

        // Post-order: children before parent → 2 then 1
        result.result.map(_._1).toList shouldBe List(2, 1)
    }
}
