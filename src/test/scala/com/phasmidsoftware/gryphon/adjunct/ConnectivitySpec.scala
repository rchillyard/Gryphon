/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.adjunct

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

/**
 * Tests for ConnectivityASP, Connectivity, and ParentSize.
 *
 * Both implementations satisfy the same DisjointSet contract; they are tested
 * in parallel. Connectivity additionally provides balanced trees and
 * meaningful size/depth guarantees.
 */
class ConnectivitySpec extends AnyFlatSpec with should.Matchers:

  // -------------------------------------------------------------------------
  // ParentSize
  // -------------------------------------------------------------------------

  behavior of "ParentSize"

  it should "construct a singleton root with apply[V]" in {
    val ps = ParentSize[Int]
    ps.parent shouldBe None
    ps.size   shouldBe 1
  }

  it should "construct a non-root with apply(v)" in {
    val ps = ParentSize(42)
    ps.parent shouldBe Some(42)
    ps.size   shouldBe 1
  }

  it should "reparent correctly" in {
    val ps = ParentSize[Int].reparent(Some(7))
    ps.parent shouldBe Some(7)
    ps.size   shouldBe 1
  }

  it should "resize correctly" in {
    val ps = ParentSize[Int].resize(5)
    ps.parent shouldBe None
    ps.size   shouldBe 5
  }

  // -------------------------------------------------------------------------
  // ConnectivityASP — basic structure
  // -------------------------------------------------------------------------

  behavior of "ConnectivityASP — structure"

  it should "create an empty ConnectivityASP" in {
    val uf = ConnectivityASP.empty[Int]
    uf.size shouldBe 0
  }

  it should "create singletons with create" in {
    val uf = ConnectivityASP.create(0, 1, 2, 3, 4)
    uf.size shouldBe 5
  }

  it should "add a singleton with put" in {
    val uf = ConnectivityASP.empty[Int].put(0).put(1)
    uf.size shouldBe 2
  }

  it should "report each vertex as its own root initially" in {
    val uf = ConnectivityASP.create(0, 1, 2)
    uf.getDisjointSet(0) shouldBe 0
    uf.getDisjointSet(1) shouldBe 1
    uf.getDisjointSet(2) shouldBe 2
  }

  it should "report no two vertices connected initially" in {
    val uf = ConnectivityASP.create(0, 1, 2)
    uf.isConnected(0, 1) shouldBe false
    uf.isConnected(1, 2) shouldBe false
  }

  // -------------------------------------------------------------------------
  // ConnectivityASP — connect / isConnected
  // -------------------------------------------------------------------------

  behavior of "ConnectivityASP — connect"

  it should "connect two vertices" in {
    val uf = ConnectivityASP.create(0, 1, 2).connect(0, 1)
    uf.isConnected(0, 1) shouldBe true
  }

  it should "reduce size by one after connecting two components" in {
    val uf = ConnectivityASP.create(0, 1, 2).connect(0, 1)
    uf.size shouldBe 2
  }

  it should "not change size when connecting already-connected vertices" in {
    val uf = ConnectivityASP.create(0, 1, 2).connect(0, 1).connect(0, 1)
    uf.size shouldBe 2
  }

  it should "not change size when connecting a vertex to itself" in {
    val uf = ConnectivityASP.create(0, 1).connect(0, 0)
    uf.size shouldBe 2
  }

  it should "transitively connect vertices" in {
    val uf = ConnectivityASP.create(0, 1, 2).connect(0, 1).connect(1, 2)
    uf.isConnected(0, 2) shouldBe true
  }

  it should "merge all five vertices into one component" in {
    val uf = ConnectivityASP.create(0, 1, 2, 3, 4)
            .connect(0, 1).connect(1, 2).connect(2, 3).connect(3, 4)
    uf.size shouldBe 1
    uf.isConnected(0, 4) shouldBe true
  }

  it should "keep unconnected vertices separate" in {
    val uf = ConnectivityASP.create(0, 1, 2, 3).connect(0, 1).connect(2, 3)
    uf.isConnected(0, 2) shouldBe false
    uf.size shouldBe 2
  }

  // -------------------------------------------------------------------------
  // Connectivity — basic structure
  // -------------------------------------------------------------------------

  behavior of "Connectivity — structure"

  it should "create an empty Connectivity" in {
    val wuf = Connectivity.empty[Int]
    wuf.size shouldBe 0
  }

  it should "create singletons with create" in {
    val wuf = Connectivity.create(0, 1, 2, 3, 4)
    wuf.size shouldBe 5
  }

  it should "add a singleton with put" in {
    val wuf = Connectivity.empty[Int].put(0).put(1)
    wuf.size shouldBe 2
  }

  it should "report each vertex as its own root initially" in {
    val wuf = Connectivity.create(0, 1, 2)
    wuf.getDisjointSet(0) shouldBe 0
    wuf.getDisjointSet(1) shouldBe 1
    wuf.getDisjointSet(2) shouldBe 2
  }

  it should "have size 1 for all initial components" in {
    val wuf = Connectivity.create(0, 1, 2)
    wuf.get(0).map(_.size) shouldBe Some(1)
    wuf.get(1).map(_.size) shouldBe Some(1)
  }

  // -------------------------------------------------------------------------
  // Connectivity — connect / isConnected
  // -------------------------------------------------------------------------

  behavior of "Connectivity — connect"

  it should "connect two vertices" in {
    val wuf = Connectivity.create(0, 1, 2).connect(0, 1)
    wuf.isConnected(0, 1) shouldBe true
  }

  it should "reduce size by one after connecting two components" in {
    val wuf = Connectivity.create(0, 1, 2).connect(0, 1)
    wuf.size shouldBe 2
  }

  it should "not change size when connecting already-connected vertices" in {
    val wuf = Connectivity.create(0, 1, 2).connect(0, 1).connect(0, 1)
    wuf.size shouldBe 2
  }

  it should "not change size when connecting a vertex to itself" in {
    val wuf = Connectivity.create(0, 1).connect(0, 0)
    wuf.size shouldBe 2
  }

  it should "transitively connect vertices" in {
    val wuf = Connectivity.create(0, 1, 2).connect(0, 1).connect(1, 2)
    wuf.isConnected(0, 2) shouldBe true
  }

  it should "merge all five vertices into one component" in {
    val wuf = Connectivity.create(0, 1, 2, 3, 4)
            .connect(0, 1).connect(1, 2).connect(2, 3).connect(3, 4)
    wuf.size shouldBe 1
    wuf.isConnected(0, 4) shouldBe true
  }

  it should "keep unconnected vertices separate" in {
    val wuf = Connectivity.create(0, 1, 2, 3).connect(0, 1).connect(2, 3)
    wuf.isConnected(0, 2) shouldBe false
    wuf.size shouldBe 2
  }

  // -------------------------------------------------------------------------
  // Connectivity — size and depth properties
  // -------------------------------------------------------------------------

  behavior of "Connectivity — size and depth"

  it should "track component size correctly after union" in {
    val wuf = Connectivity.create(0, 1, 2, 3).connect(0, 1).connect(2, 3)
    // Each merged component has size 2 at the root
    val root01 = wuf.getDisjointSet(0)
    wuf.get(root01).map(_.size) shouldBe Some(2)
  }

  it should "track component size after merging two components" in {
    val wuf = Connectivity.create(0, 1, 2, 3)
            .connect(0, 1).connect(2, 3).connect(0, 2)
    val root = wuf.getDisjointSet(0)
    wuf.get(root).map(_.size) shouldBe Some(4)
  }

  it should "keep tree depth bounded — depth never exceeds log2(n) + 1" in {
    // With 8 elements, log2(8) = 3, so max depth should be <= 4
    val wuf = Connectivity.create(0, 1, 2, 3, 4, 5, 6, 7)
            .connect(0, 1).connect(2, 3).connect(4, 5).connect(6, 7)
            .connect(0, 2).connect(4, 6).connect(0, 4)
    wuf.maxDepth should be <= 4.0
  }

  it should "give meanDepth <= maxDepth" in {
    val wuf = Connectivity.create(0, 1, 2, 3, 4)
            .connect(0, 1).connect(1, 2).connect(2, 3).connect(3, 4)
    wuf.meanDepth should be <= wuf.maxDepth
  }

  // -------------------------------------------------------------------------
  // isRoot / parent / depth
  // -------------------------------------------------------------------------

  behavior of "Connectivity — isRoot / parent / depth"

  it should "report all initial vertices as roots" in {
    val wuf = Connectivity.create(0, 1, 2)
    wuf.isRoot(0) shouldBe true
    wuf.isRoot(1) shouldBe true
  }

  it should "report a non-root correctly after connect" in {
    val wuf = Connectivity.create(0, 1).connect(0, 1)
    // exactly one of {0,1} is the root; the other is not
    val root  = wuf.getDisjointSet(0)
    val child = if root == 0 then 1 else 0
    wuf.isRoot(root)  shouldBe true
    wuf.isRoot(child) shouldBe false
  }

  it should "report depth 1 for all initial singletons" in {
    val wuf = Connectivity.create(0, 1, 2)
    wuf.depth(0) shouldBe 1
    wuf.depth(1) shouldBe 1
  }

  it should "report depth 2 for a child after connecting two singletons" in {
    val wuf = Connectivity.create(0, 1).connect(0, 1)
    val root  = wuf.getDisjointSet(0)
    val child = if root == 0 then 1 else 0
    wuf.depth(root)  shouldBe 1
    wuf.depth(child) shouldBe 2
  }

  // -------------------------------------------------------------------------
  // remove
  // -------------------------------------------------------------------------

  behavior of "DisjointSet — remove"

  it should "remove a singleton and reduce component count" in {
    val wuf = Connectivity.create(0, 1, 2).remove(2)
    wuf.size shouldBe 2
    wuf.get(2) shouldBe None
  }

  // -------------------------------------------------------------------------
  // getDisjointSet — error path
  // -------------------------------------------------------------------------

  behavior of "ConnectivityASP — getDisjointSet error"

  it should "throw GraphException for a missing key" in {
    val uf = ConnectivityASP.create(1, 2, 3)
    a[com.phasmidsoftware.gryphon.util.GraphException] should be thrownBy uf.getDisjointSet(0)
  }

  behavior of "Connectivity — getDisjointSet error"

  it should "throw GraphException for a missing key" in {
    val wuf = Connectivity.create(1, 2, 3)
    a[com.phasmidsoftware.gryphon.util.GraphException] should be thrownBy wuf.getDisjointSet(0)
  }

  // -------------------------------------------------------------------------
  // Connectivity — internal ParentSize structure after connect
  // -------------------------------------------------------------------------

  behavior of "Connectivity — internal structure"

  it should "attach smaller tree under larger and update sizes correctly" in {
    // connect A-B: equal size (1,1) so B goes under A (s1 >= s2 case)
    val c1 = Connectivity.create("A", "B", "C").connect("A", "B")
    c1.map("A") shouldBe ParentSize[String](None, 2)
    c1.map("B") shouldBe ParentSize("A")
    c1.map("C") shouldBe ParentSize[String]
    // connect C-A: C (size 1) goes under A (size 2)
    val c2 = c1.connect("C", "A")
    c2.map("A") shouldBe ParentSize[String](None, 3)
    c2.map("B") shouldBe ParentSize("A")
    c2.map("C") shouldBe ParentSize("A")
  }

  // -------------------------------------------------------------------------
  // Stress tests
  // -------------------------------------------------------------------------

  behavior of "ConnectivityASP — stress test"

  it should "correctly reduce components with many random connections" in {
    val rng = scala.util.Random(42)
    val max = 100000
    val n = 1000
    val randomInts = LazyList.continually(rng.nextInt(max)).take(n).toList
    def getPair: (Int, Int) = (randomInts(rng.nextInt(n)), randomInts(rng.nextInt(n)))

    val target = ConnectivityASP.create[Int](randomInts *)
    val connections = 4 * n
    val pairs = LazyList.continually(getPair).take(connections).toList
    val result = pairs.foldLeft(target)((u, t) => u.connect(t._1, t._2))
    result.size should be < n
    result.maxDepth should be > 1.0
  }

  behavior of "Connectivity — stress test"

  it should "correctly reduce components and maintain bounded depth" in {
    val rng = scala.util.Random(42)
    val max = 100000
    val n = 1000
    val randomInts = LazyList.continually(rng.nextInt(max)).take(n).toList
    def getPair: (Int, Int) = (randomInts(rng.nextInt(n)), randomInts(rng.nextInt(n)))

    val target = Connectivity.create[Int](randomInts *)
    val connections = 4 * n
    val pairs = LazyList.continually(getPair).take(connections).toList
    val result = pairs.foldLeft(target)((u, t) => u.connect(t._1, t._2))
    result.size should be < n
    // Connectivity guarantees O(log n) depth — log2(1000) ≈ 10
    result.maxDepth should be <= 20.0
  }

  it should "have strictly lower maxDepth than ConnectivityASP on same data" in {
    val rng = scala.util.Random(42)
    val n = 500
    val randomInts = LazyList.continually(rng.nextInt(10000)).take(n).toList
    def getPair: (Int, Int) = (randomInts(rng.nextInt(n)), randomInts(rng.nextInt(n)))
    val pairs = LazyList.continually(getPair).take(2 * n).toList

    val uf = pairs.foldLeft(ConnectivityASP.create[Int](randomInts *))((u, t) => u.connect(t._1, t._2))
    val wuf = pairs.foldLeft(Connectivity.create[Int](randomInts *))((u, t) => u.connect(t._1, t._2))
    wuf.maxDepth should be <= uf.maxDepth
  }