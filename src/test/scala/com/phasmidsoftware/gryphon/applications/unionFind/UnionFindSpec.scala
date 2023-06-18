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
        iterator.next() shouldBe 1 -> None
        iterator.hasNext shouldBe true
        iterator.next() shouldBe 2 -> None
        iterator.hasNext shouldBe true
        iterator.next() shouldBe 3 -> None
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

    it should "remove" in {
        val target = UnionFind.create[Int](1, 2, 3)
        val removed = target.remove(3)
        removed.size shouldBe 2
    }

    it should "size" in {
        val target = UnionFind.create[Int](1, 2, 3)
        target.size shouldBe 3
        val connected = target.connect(1, 2)
        connected.size shouldBe 2
    }

    it should "unit" in {
        val empty = UnionFind.create[Int]()
        val target = empty.unit(Map(1 -> None, 2 -> None, 3 -> Some(1)))
        target.size shouldBe 2
        target.isConnected(1, 3) shouldBe true
    }

    it should "updated" in {
        val target = UnionFind.create[Int](1, 2, 3)
        target.isConnected(1, 3) shouldBe false
        val update = target.unit(target.updated(1, Some(3)))
        update.isConnected(1, 3) shouldBe true
    }


    behavior of "WeightedUnionFind"

    it should "iterator" in {
        val target = WeightedUnionFind.create[Int](1, 2, 3)
        val iterator = target.iterator
        iterator.hasNext shouldBe true
        iterator.next() shouldBe (1 -> (None -> 1))
        iterator.hasNext shouldBe true
        iterator.next() shouldBe (2 -> (None -> 1))
        iterator.hasNext shouldBe true
        iterator.next() shouldBe (3 -> (None -> 1))
        iterator.hasNext shouldBe false
    }

    it should "isConnected" in {
        val target = WeightedUnionFind.create[Int](1, 2, 3)
        target.isConnected(1, 1) shouldBe true
        target.isConnected(1, 2) shouldBe false
        target.isConnected(1, 3) shouldBe false
    }

    it should "get" in {
        val target = WeightedUnionFind.create[Int](1, 2, 3)
        target.get(0) shouldBe None
        target.get(1) shouldBe Some(None -> 1)
    }

    it should "getDisjointSet" in {
        val target = WeightedUnionFind.create[Int](1, 2, 3)
        target.getDisjointSet(1) shouldBe 1
        target.getDisjointSet(2) shouldBe 2

    }
    it should "getDisjointSet bad" in {
        val target = WeightedUnionFind.create[Int](1, 2, 3)
        a[GraphException] should be thrownBy target.getDisjointSet(0)
    }

    it should "connect" in {
        val target = WeightedUnionFind.create[Int](1, 2, 3)
        val connected = target.connect(1, 2)
        connected.isConnected(1, 2) shouldBe true
    }

    it should "remove" in {
        val target = WeightedUnionFind.create[Int](1, 2, 3)
        val removed = target.remove(3)
        removed.size shouldBe 2
    }

    it should "size" in {
        val target = WeightedUnionFind.create[Int](1, 2, 3)
        target.size shouldBe 3
        val connected = target.connect(1, 2)
        connected.size shouldBe 2
    }

    it should "unit" in {
        val empty = WeightedUnionFind.create[Int]()
        val target = empty.unit(Map(1 -> (None -> 1), 2 -> (None -> 1), 3 -> (Some(1) -> 1)))
        target.size shouldBe 2
        target.isConnected(1, 3) shouldBe true
    }

    it should "updated" in {
        val target = WeightedUnionFind.create[Int](1, 2, 3)
        target.isConnected(1, 3) shouldBe false
        val update = target.unit(target.updated(1, Some(3) -> 2))
        update.isConnected(1, 3) shouldBe true
    }
}
