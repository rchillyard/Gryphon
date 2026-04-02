/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.adjunct

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

/**
 * Tests for UnionFind, WeightedUnionFind, and ParentSize.
 *
 * Both implementations satisfy the same DisjointSet contract; they are tested
 * in parallel. WeightedUnionFind additionally provides balanced trees and
 * meaningful size/depth guarantees.
 */
class UnionFindSpec extends AnyFlatSpec with should.Matchers:

  // -------------------------------------------------------------------------
  // ParentSize
  // -------------------------------------------------------------------------

  behavior of "ParentSize"

  it should "construct a singleton root with apply[V]" in {
    val ps = ParentSize[Int]
    ps.parent shouldBe None
    ps.size shouldBe 1
  }

  it should "construct a non-root with apply(v)" in {
    val ps = ParentSize(42)
    ps.parent shouldBe Some(42)
    ps.size shouldBe 1
  }

  it should "reparent correctly" in {
    val ps = ParentSize[Int].reparent(Some(7))
    ps.parent shouldBe Some(7)
    ps.size shouldBe 1
  }

  it should "resize correctly" in {
    val ps = ParentSize[Int].resize(5)
    ps.parent shouldBe None
    ps.size shouldBe 5
  }

  // -------------------------------------------------------------------------
  // UnionFind — basic structure
  // -------------------------------------------------------------------------

  behavior of "UnionFind — structure"

  it should "create an empty UnionFind" in {
    val uf = UnionFind.empty[Int]
    uf.size shouldBe 0
  }

  it should "create singletons with create" in {
    val uf = UnionFind.create(0, 1, 2, 3, 4)
    uf.size shouldBe 5
  }

  it should "add a singleton with put" in {
    val uf = UnionFind.empty[Int].put(0).put(1)
    uf.size shouldBe 2
  }

  it should "report each vertex as its own root initially" in {
    val uf = UnionFind.create(0, 1, 2)
    uf.getDisjointSet(0) shouldBe 0
    uf.getDisjointSet(1) shouldBe 1
    uf.getDisjointSet(2) shouldBe 2
  }

  it should "report no two vertices connected initially" in {
    val uf = UnionFind.create(0, 1, 2)
    uf.isConnected(0, 1) shouldBe false
    uf.isConnected(1, 2) shouldBe false
  }

  // -------------------------------------------------------------------------
  // UnionFind — connect / isConnected
  // -------------------------------------------------------------------------

  behavior of "UnionFind — connect"

  it should "connect two vertices" in {
    val uf = UnionFind.create(0, 1, 2).connect(0, 1)
    uf.isConnected(0, 1) shouldBe true
  }

  it should "reduce size by one after connecting two components" in {
    val uf = UnionFind.create(0, 1, 2).connect(0, 1)
    uf.size shouldBe 2
  }

  it should "not change size when connecting already-connected vertices" in {
    val uf = UnionFind.create(0, 1, 2).connect(0, 1).connect(0, 1)
    uf.size shouldBe 2
  }

  it should "not change size when connecting a vertex to itself" in {
    val uf = UnionFind.create(0, 1).connect(0, 0)
    uf.size shouldBe 2
  }

  it should "transitively connect vertices" in {
    val uf = UnionFind.create(0, 1, 2).connect(0, 1).connect(1, 2)
    uf.isConnected(0, 2) shouldBe true
  }

  it should "merge all five vertices into one component" in {
    val uf = UnionFind.create(0, 1, 2, 3, 4)
            .connect(0, 1).connect(1, 2).connect(2, 3).connect(3, 4)
    uf.size shouldBe 1
    uf.isConnected(0, 4) shouldBe true
  }

  it should "keep unconnected vertices separate" in {
    val uf = UnionFind.create(0, 1, 2, 3).connect(0, 1).connect(2, 3)
    uf.isConnected(0, 2) shouldBe false
    uf.size shouldBe 2
  }

  // -------------------------------------------------------------------------
  // WeightedUnionFind — basic structure
  // -------------------------------------------------------------------------

  behavior of "WeightedUnionFind — structure"

  it should "create an empty WeightedUnionFind" in {
    val wuf = WeightedUnionFind.empty[Int]
    wuf.size shouldBe 0
  }

  it should "create singletons with create" in {
    val wuf = WeightedUnionFind.create(0, 1, 2, 3, 4)
    wuf.size shouldBe 5
  }

  it should "add a singleton with put" in {
    val wuf = WeightedUnionFind.empty[Int].put(0).put(1)
    wuf.size shouldBe 2
  }

  it should "report each vertex as its own root initially" in {
    val wuf = WeightedUnionFind.create(0, 1, 2)
    wuf.getDisjointSet(0) shouldBe 0
    wuf.getDisjointSet(1) shouldBe 1
    wuf.getDisjointSet(2) shouldBe 2
  }

  it should "have size 1 for all initial components" in {
    val wuf = WeightedUnionFind.create(0, 1, 2)
    wuf.get(0).map(_.size) shouldBe Some(1)
    wuf.get(1).map(_.size) shouldBe Some(1)
  }

  // -------------------------------------------------------------------------
  // WeightedUnionFind — connect / isConnected
  // -------------------------------------------------------------------------

  behavior of "WeightedUnionFind — connect"

  it should "connect two vertices" in {
    val wuf = WeightedUnionFind.create(0, 1, 2).connect(0, 1)
    wuf.isConnected(0, 1) shouldBe true
  }

  it should "reduce size by one after connecting two components" in {
    val wuf = WeightedUnionFind.create(0, 1, 2).connect(0, 1)
    wuf.size shouldBe 2
  }

  it should "not change size when connecting already-connected vertices" in {
    val wuf = WeightedUnionFind.create(0, 1, 2).connect(0, 1).connect(0, 1)
    wuf.size shouldBe 2
  }

  it should "not change size when connecting a vertex to itself" in {
    val wuf = WeightedUnionFind.create(0, 1).connect(0, 0)
    wuf.size shouldBe 2
  }

  it should "transitively connect vertices" in {
    val wuf = WeightedUnionFind.create(0, 1, 2).connect(0, 1).connect(1, 2)
    wuf.isConnected(0, 2) shouldBe true
  }

  it should "merge all five vertices into one component" in {
    val wuf = WeightedUnionFind.create(0, 1, 2, 3, 4)
            .connect(0, 1).connect(1, 2).connect(2, 3).connect(3, 4)
    wuf.size shouldBe 1
    wuf.isConnected(0, 4) shouldBe true
  }

  it should "keep unconnected vertices separate" in {
    val wuf = WeightedUnionFind.create(0, 1, 2, 3).connect(0, 1).connect(2, 3)
    wuf.isConnected(0, 2) shouldBe false
    wuf.size shouldBe 2
  }

  // -------------------------------------------------------------------------
  // WeightedUnionFind — size and depth properties
  // -------------------------------------------------------------------------

  behavior of "WeightedUnionFind — size and depth"

  it should "track component size correctly after union" in {
    val wuf = WeightedUnionFind.create(0, 1, 2, 3).connect(0, 1).connect(2, 3)
    // Each merged component has size 2 at the root
    val root01 = wuf.getDisjointSet(0)
    wuf.get(root01).map(_.size) shouldBe Some(2)
  }

  it should "track component size after merging two components" in {
    val wuf = WeightedUnionFind.create(0, 1, 2, 3)
            .connect(0, 1).connect(2, 3).connect(0, 2)
    val root = wuf.getDisjointSet(0)
    wuf.get(root).map(_.size) shouldBe Some(4)
  }

  it should "keep tree depth bounded — depth never exceeds log2(n) + 1" in {
    // With 8 elements, log2(8) = 3, so max depth should be <= 4
    val wuf = WeightedUnionFind.create(0, 1, 2, 3, 4, 5, 6, 7)
            .connect(0, 1).connect(2, 3).connect(4, 5).connect(6, 7)
            .connect(0, 2).connect(4, 6).connect(0, 4)
    wuf.maxDepth should be <= 4.0
  }

  it should "give meanDepth <= maxDepth" in {
    val wuf = WeightedUnionFind.create(0, 1, 2, 3, 4)
            .connect(0, 1).connect(1, 2).connect(2, 3).connect(3, 4)
    wuf.meanDepth should be <= wuf.maxDepth
  }

  // -------------------------------------------------------------------------
  // isRoot / parent / depth
  // -------------------------------------------------------------------------

  behavior of "WeightedUnionFind — isRoot / parent / depth"

  it should "report all initial vertices as roots" in {
    val wuf = WeightedUnionFind.create(0, 1, 2)
    wuf.isRoot(0) shouldBe true
    wuf.isRoot(1) shouldBe true
  }

  it should "report a non-root correctly after connect" in {
    val wuf = WeightedUnionFind.create(0, 1).connect(0, 1)
    // exactly one of {0,1} is the root; the other is not
    val root = wuf.getDisjointSet(0)
    val child = if root == 0 then 1 else 0
    wuf.isRoot(root) shouldBe true
    wuf.isRoot(child) shouldBe false
  }

  it should "report depth 1 for all initial singletons" in {
    val wuf = WeightedUnionFind.create(0, 1, 2)
    wuf.depth(0) shouldBe 1
    wuf.depth(1) shouldBe 1
  }

  it should "report depth 2 for a child after connecting two singletons" in {
    val wuf = WeightedUnionFind.create(0, 1).connect(0, 1)
    val root = wuf.getDisjointSet(0)
    val child = if root == 0 then 1 else 0
    wuf.depth(root) shouldBe 1
    wuf.depth(child) shouldBe 2
  }

  // -------------------------------------------------------------------------
  // remove
  // -------------------------------------------------------------------------

  behavior of "DisjointSet — remove"

  it should "remove a singleton and reduce component count" in {
    val wuf: WeightedUnionFind[Int] = WeightedUnionFind.create(0, 1, 2).remove(2)
    wuf.size shouldBe 2
    wuf.get(2) shouldBe None
  }