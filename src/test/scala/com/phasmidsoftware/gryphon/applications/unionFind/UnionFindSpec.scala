/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.applications.unionFind

import com.phasmidsoftware.gryphon.core.GraphException
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class UnionFindSpec extends AnyFlatSpec with should.Matchers {

    behavior of "UnionFind"

    it should "iterator" in {
        val target = UnionFind.create[Int](1, 2, 3)
        val iterator = target.iterator
        iterator.hasNext shouldBe true
        iterator.next shouldBe 1 -> None
        iterator.hasNext shouldBe true
        iterator.next shouldBe 2 -> None
        iterator.hasNext shouldBe true
        iterator.next shouldBe 3 -> None
        iterator.hasNext shouldBe false
    }

    it should "isConnected" in {
        val target = UnionFind.create[Int](1, 2, 3)
        target.isConnected(1, 1) shouldBe true
        target.isConnected(1, 2) shouldBe false
        target.isConnected(1, 3) shouldBe false
    }

    it should "get" in {
        val target = UnionFind.create[Int](1, 2, 3)
        target.get(0) shouldBe None
        target.get(1) shouldBe Some(None)
    }

    it should "getDisjointSet" in {
        val target = UnionFind.create[Int](1, 2, 3)
        target.getDisjointSet(1) shouldBe 1
        target.getDisjointSet(2) shouldBe 2

    }
    it should "getDisjointSet bad" in {
        val target = UnionFind.create[Int](1, 2, 3)
        a[GraphException] should be thrownBy target.getDisjointSet(0)
    }

    it should "connect" in {
        val target = UnionFind.create[Int](1, 2, 3)
        val connected = target.connect(1, 2)
        connected.isConnected(1, 2) shouldBe true
    }

    it should "removed" in {
        val target = UnionFind.create[Int](1, 2, 3)
        val removed = target.removed(3)
        removed.size shouldBe 2
    }

    it should "size" in {
        val target = UnionFind.create[Int](1, 2, 3)
        target.size shouldBe 3
        val connected = target.connect(1, 2)
        connected.size shouldBe 2
    }

    it should "unit" in {

    }

    it should "updated" in {

    }

}
