package com.phasmidsoftware.gryphon.core

import com.phasmidsoftware.visitor.core.{DfsOrder, Evaluable, GraphNeighbours, JournaledVisitor, Traversal, given_VisitedSet_V}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class TranslatedOldVisitorSpec extends AnyFlatSpec with Matchers:

  import com.phasmidsoftware.visitor.core.DfsOrder.*

  // Minimal two-node graph: 1 → 2, used for most tests
  given GraphNeighbours[Int] with
    def neighbours(n: Int): Iterator[Int] = n match
      case 1 => Iterator(2)
      case _ => Iterator.empty

  given Evaluable[Int, Int] with
    def evaluate(v: Int): Option[Int] = Some(v)

  // Run DFS and return visited node sequence
  private def runDfs(order: DfsOrder, start: Int = 1, useList: Boolean = false): List[Int] =
    if useList then
      Traversal.dfs(start, JournaledVisitor.withListJournal[Int, Int], order).result.map(_._1).toList
    else
      Traversal.dfs(start, JournaledVisitor.withQueueJournal[Int, Int], order).result.map(_._1).toList

  // -------------------------------------------------------
  // Post-order (was: PostVisitor, PostVisitorIterable)
  // -------------------------------------------------------

  behavior of "post-order DFS (was PostVisitor / PostVisitorIterable)"

  it should "visit nodes post-order with queue journal" in :
    runDfs(Post) shouldBe List(2, 1)

  it should "visit nodes post-order with list journal (reverse)" in :
    runDfs(Post, useList = true) shouldBe List(1, 2)

  // -------------------------------------------------------
  // Pre-order (was: PreVisitor, PreVisitorIterable)
  // -------------------------------------------------------

  behavior of "pre-order DFS (was PreVisitor / PreVisitorIterable)"

  it should "visit nodes pre-order with queue journal" in :
    runDfs(Pre) shouldBe List(1, 2)

  it should "visit nodes pre-order with list journal (reverse)" in :
    runDfs(Pre, useList = true) shouldBe List(2, 1)

  // -------------------------------------------------------
  // Multi-node (was: createPre, createPost, reversePre, reversePost)
  // -------------------------------------------------------

  behavior of "multi-node traversal (was Visitor.createPre/createPost/reversePre/reversePost)"

  object TreeFixture:
    given GraphNeighbours[Int] with
      def neighbours(n: Int): Iterator[Int] = n match
        case 10 => Iterator(5, 13)
        case 5 => Iterator(2, 6)
        case 13 => Iterator(11, 15)
        case 2 => Iterator(1, 3)
        case _ => Iterator.empty

    given Evaluable[Int, Int] with
      def evaluate(v: Int): Option[Int] = Some(v)

  it should "implement createPost equivalent: queue journal post-order" in :
    import TreeFixture.given
    val result = Traversal.dfs(10, JournaledVisitor.withQueueJournal[Int, Int], Post)
    result.result.map(_._1).toList shouldBe List(1, 3, 2, 6, 5, 11, 15, 13, 10)

  it should "implement reversePost equivalent: list journal post-order" in :
    import TreeFixture.given
    val result = Traversal.dfs(10, JournaledVisitor.withListJournal[Int, Int], Post)
    result.result.map(_._1).toList shouldBe List(10, 13, 15, 11, 5, 6, 2, 3, 1)

  it should "implement createPre equivalent: queue journal pre-order" in :
    import TreeFixture.given
    val result = Traversal.dfs(10, JournaledVisitor.withQueueJournal[Int, Int], Pre)
    result.result.map(_._1).toList shouldBe List(10, 5, 2, 1, 3, 6, 13, 11, 15)

  it should "implement reversePre equivalent: list journal pre-order" in :
    import TreeFixture.given
    val result = Traversal.dfs(10, JournaledVisitor.withListJournal[Int, Int], Pre)
    result.result.map(_._1).toList shouldBe List(15, 11, 13, 6, 3, 1, 2, 5, 10)