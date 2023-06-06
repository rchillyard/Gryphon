/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.core

import java.io.FileWriter
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import scala.collection.immutable.Queue

class VisitorSpec extends AnyFlatSpec with should.Matchers {

    behavior of "PostVisitor"

    it should "visitPost and ignore visitPre" in {
        val target: PostVisitor[Int, Queue[Int]] = PostVisitor()
        val t1 = target.visitPost(1)
        t1 shouldBe PostVisitor(Queue(1))
        val t2 = t1.visitPre(1)
        t2 shouldBe t1
        val t3 = t2.visitPost(2)
        t3 shouldBe PostVisitor(Queue(1, 2))
    }

    it should "visitPre twice to StringBuilder" in {
        val target: PostVisitor[Int, StringBuilder] = PostVisitor()
        val t2 = target.visitPost(1)
        t2.journal.toString() shouldBe "1\n"
        val t3 = t2.visitPost(2)
        t3.journal.toString() shouldBe "1\n2\n"
    }

    it should "implement visitPost twice and journal" in {
        val target = Visitor.createPost[Int]
        target.visitPre(1) shouldBe target
        val t1 = target.visitPost(1)
        t1.journal shouldBe Seq(1)
        val t2 = t1.visitPost(2)
        t2.journal shouldBe Queue(1, 2)
    }

    it should "implement reversePost" in {
        val target = Visitor.reversePost[Int]
        target.visitPre(1) shouldBe target
        val t1 = target.visitPost(1)
        t1 shouldBe PostVisitor(List(1))
        val t2 = t1.visitPost(2)
        t2 shouldBe PostVisitor(List(2, 1))
    }

    behavior of "PreVisitor"

    it should "visitPre twice" in {
        val target: PreVisitor[Int, Queue[Int]] = PreVisitor()
        val t2 = target.visitPre(1)
        t2 shouldBe PreVisitor(Queue(1))
        val t3 = t2.visitPre(2)
        t3 shouldBe PreVisitor(Queue(1, 2))
    }

    it should "visitPre twice to StringBuilder" in {
        val target: PreVisitor[Int, StringBuilder] = PreVisitor()
        val t2 = target.visitPre(1)
        t2.journal.toString() shouldBe "1\n"
        val t3 = t2.visitPre(2)
        t3.journal.toString() shouldBe "1\n2\n"
    }

    it should "visitPre twice to FileWriter" in {
        val target: PreVisitor[Int, FileWriter] = PreVisitor()
        val t2 = target.visitPre(1)
        t2.visitPre(2).close()
    }

    it should "visitPre twice to named FileWriter" in {
        val journal = new FileWriter("test.txt")
        val target: PreVisitor[Int, FileWriter] = PreVisitor(journal)
        val t2 = target.visitPre(1)
        val t3 = t2.visitPre(2)
        t3.journal.close()
    }

    it should "implement visitPre twice and journal" in {
        val target = Visitor.createPre[Int]
        val t1 = target.visitPre(1)
        t1.journal shouldBe Seq(1)
        val t2 = t1.visitPre(2)
        t2.journal shouldBe Queue(1, 2)
    }

    it should "implement reversePre with two visits" in {
        val target = Visitor.reversePre[Int]
        val t1 = target.visitPre(1)
        t1 shouldBe PreVisitor(List(1))
        val t2 = t1.visitPre(2)
        t2 shouldBe PreVisitor(List(2, 1))
    }
    behavior of "PostVisitorIterable"

    it should "visitPost and ignore visitPre" in {
        val target: PostVisitorIterable[Int, Queue[Int]] = PostVisitorIterable()
        val t1: IterableVisitor[Int, Queue[Int]] = target.visitPost(1)
        t1.iterator.toSeq shouldBe Seq(1)
        val t2: IterableVisitor[Int, Queue[Int]] = t1.visitPre(1)
        t2 shouldBe t1
        val t3: IterableVisitor[Int, Queue[Int]] = t2.visitPost(2)
        t3.iterator.toSeq shouldBe Seq(1, 2)
    }

    it should "implement visitPost twice and journal" in {
        val target = Visitor.createPostQueue[Int]
        target.visitPre(1) shouldBe target
        val t1 = target.visitPost(1)
        t1.journal shouldBe Seq(1)
        val t2 = t1.visitPost(2)
        t2.journal shouldBe Queue(1, 2)
    }

    it should "implement reversePost" in {
        val target = Visitor.reversePostList[Int]
        target.visitPre(1) shouldBe target
        val t1 = target.visitPost(1)
        t1.iterator.toSeq shouldBe List(1)
        val t2 = t1.visitPost(2)
        t2.iterator.toSeq shouldBe List(2, 1)
    }

    behavior of "PreVisitorIterable"

    it should "visitPre twice" in {
        val target: PreVisitorIterable[Int, Queue[Int]] = PreVisitorIterable()
        val t2 = target.visitPre(1)
        t2.iterator.toSeq shouldBe Seq(1)
        val t3 = t2.visitPre(2)
        t3.iterator.toSeq shouldBe Seq(1, 2)
    }

    it should "implement visitPre twice and journal" in {
        val target = Visitor.createPreQueue[Int]
        val t1 = target.visitPre(1)
        t1.iterator.toSeq shouldBe Seq(1)
        val t2 = t1.visitPre(2)
        t2.iterator.toSeq shouldBe Queue(1, 2)
    }

    it should "implement reversePre with two visits" in {
        val target = Visitor.reversePreList[Int]
        val t1 = target.visitPre(1)
        t1.iterator.toSeq shouldBe List(1)
        val t2 = t1.visitPre(2)
        t2.iterator.toSeq shouldBe List(2, 1)
    }

    behavior of "Visitor"

    it should "preFunc" in {
        val target: PreVisitor[Int, Queue[Int]] = PreVisitor()
        val queue = Queue.empty[Int]
        val a1: Option[Queue[Int]] = target.preFunc(1)(queue)
        a1 shouldBe Some(Queue(1))
    }

    it should "join 1" in {
        val preVisitor: PreVisitor[Int, Queue[Int]] = PreVisitor()
        val postVisitor: PostVisitor[Int, Queue[Int]] = PostVisitor()
        val target: Visitor[Int, Queue[Int]] = preVisitor join postVisitor
        target.visitPre(1).journal shouldBe Queue(1)
    }

    it should "join 2" in {
        val preVisitor: PreVisitor[Int, List[Int]] = PreVisitor()
        val postVisitor = Visitor.reversePost[Int]
        val target: Visitor[Int, List[Int]] = preVisitor join postVisitor
        val z1: Visitor[Int, List[Int]] = target.visitPre(1)
        z1.visitPost(2).journal shouldBe List(2, 1)
    }

    it should "join 3" in {
        val target: Visitor[Int, List[Int]] = Visitor.preAndPost[Int]
        val z1: Visitor[Int, List[Int]] = target.visitPre(1)
        z1.visitPost(2).journal shouldBe List(2, 1)
    }

    it should "postFunc" in {
        val target: PostVisitor[Int, Queue[Int]] = PostVisitor()
        val queue = Queue.empty[Int]
        target.postFunc(1)(queue) shouldBe Some(Queue(1))
    }

}
