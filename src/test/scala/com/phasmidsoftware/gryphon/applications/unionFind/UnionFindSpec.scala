/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.applications.unionFind

import com.phasmidsoftware.gryphon.core.GraphException
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import scala.util.Random

class UnionFindSpec extends AnyFlatSpec with should.Matchers {

    val random: Random = new Random()

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

    it should "check random" in {
        val max = 100000
        val n = 1000
        val randomInts = LazyList.continually(random.nextInt(max)).take(n).toList

        def getPair: (Int, Int) = (randomInts(random.nextInt(n)), randomInts(random.nextInt(n)))

        val target: UnionFind[Int] = UnionFind.create[Int](randomInts: _*)
        val connections = 4 * n
        val pairs: Seq[(Int, Int)] = LazyList.continually(getPair).take(connections).toList

        def connect(u: UnionFind[Int], t: (Int, Int)): UnionFind[Int] = u.connect(t._1, t._2)

        val result: UnionFind[Int] = pairs.foldLeft[UnionFind[Int]](target)(connect)
        println(s"The number of components remaining from $n originally and with $connections connections is ${result.size}")
        println(s"The maximum depth of the objects is ${result.maxDepth}")
        println(s"The mean depth of the objects is ${result.meanDepth}")
    }

    behavior of "WeightedUnionFind"

    it should "iterator" in {
        val target = WeightedUnionFind.create[Int](1, 2, 3)
        val iterator = target.iterator
        iterator.hasNext shouldBe true
        iterator.next() shouldBe (1 -> ParentSize[Int])
        iterator.hasNext shouldBe true
        iterator.next() shouldBe (2 -> ParentSize[Int])
        iterator.hasNext shouldBe true
        iterator.next() shouldBe (3 -> ParentSize[Int])
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
        target.get(1) shouldBe Some(ParentSize[Int])
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

    it should "connect 1" in {
        val target = WeightedUnionFind.create[Int](1, 2, 3)
        val connected = target.connect(1, 2)
        connected.isConnected(1, 2) shouldBe true
    }

    it should "connect 2" in {
        val target: WeightedUnionFind[String] = WeightedUnionFind.create("A", "B", "C")
        val c1 = target.connect("A", "B")
        c1.map("A") shouldBe ParentSize[String](None, 2)
        c1.map("B") shouldBe ParentSize[String]("A")
        c1.map("C") shouldBe ParentSize[String]
        val c2 = c1.connect("C", "A")
        c2.map("A") shouldBe ParentSize[String](None, 3)
        c2.map("B") shouldBe ParentSize[String]("A")
        c2.map("C") shouldBe ParentSize[String]("A")
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
        val target = empty.unit(Map(1 -> ParentSize[Int], 2 -> ParentSize[Int], 3 -> ParentSize[Int](1)))
        target.size shouldBe 2
        target.isConnected(1, 3) shouldBe true
    }

    it should "updated" in {
        val target = WeightedUnionFind.create[Int](1, 2, 3)
        target.isConnected(1, 3) shouldBe false
        val update = target.unit(target.updated(1, ParentSize(Some(3), 2)))
        update.isConnected(1, 3) shouldBe true
    }

    it should "check random" in {
        val max = 100000
        val n = 1000
        val randomInts = LazyList.continually(random.nextInt(max)).take(n).toList

        def getPair: (Int, Int) = (randomInts(random.nextInt(n)), randomInts(random.nextInt(n)))

        val target: WeightedUnionFind[Int] = WeightedUnionFind.create[Int](randomInts: _*)
        val connections = 4 * n
        val pairs: Seq[(Int, Int)] = LazyList.continually(getPair).take(connections).toList

        def connect(u: WeightedUnionFind[Int], t: (Int, Int)): WeightedUnionFind[Int] = u.connect(t._1, t._2)

        val result: WeightedUnionFind[Int] = pairs.foldLeft[WeightedUnionFind[Int]](target)(connect)
        println(s"The number of components remaining from $n originally and with $connections connections is ${result.size}")
        println(s"The maximum depth of the objects is ${result.maxDepth}")
        println(s"The mean depth of the objects is ${result.meanDepth}")
    }

}
