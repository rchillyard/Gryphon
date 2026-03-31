/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.traverse

import com.phasmidsoftware.gryphon.adjunct.UndirectedGraph
import com.phasmidsoftware.gryphon.core.*
import com.phasmidsoftware.gryphon.parse.GraphParser
import com.phasmidsoftware.gryphon.util.FP.sequence
import com.phasmidsoftware.gryphon.util.TryUsing
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import scala.io.Source
import scala.util.{Failure, Random, Success, Try}

class ConnectedComponentsSpec extends AnyFlatSpec with should.Matchers:

  // dfsu.graph has 3 components:
  //   Component A: {0, 1, 2, 3, 4, 5, 6}  (7 vertices)
  //   Component B: {7, 8}                  (2 vertices)
  //   Component C: {9, 10, 11, 12}         (4 vertices)

  // Use a fixed seed for deterministic adjacency ordering
  given Random = Random(42)

  behavior of "ConnectedComponents"

  it should "identify 3 components in dfsu.graph" in {
    val p = new GraphParser[Int, Unit, EdgeType]
    val triedSource = Try(Source.fromResource("dfsu.graph"))
    val ws = TryUsing.trial(triedSource)(_.getLines().toSeq).get filterNot (_.startsWith("//"))
    sequence(for w <- ws yield p.parseTriple(w)) match
      case Success(triplets) =>
        UndirectedGraph.triplesToTryGraph(triplets) match
          case Success(graph: Graph[Int]) =>
            val (_, componentMap) = ConnectedComponents.components[Int, Unit](graph)
            componentMap.values.toSet.size shouldBe 3
          case Failure(x) => fail(x)
      case Failure(x) => fail(x)
  }

  it should "label all 13 vertices in dfsu.graph" in {
    val p = new GraphParser[Int, Unit, EdgeType]
    val triedSource = Try(Source.fromResource("dfsu.graph"))
    val ws = TryUsing.trial(triedSource)(_.getLines().toSeq).get filterNot (_.startsWith("//"))
    sequence(for w <- ws yield p.parseTriple(w)) match
      case Success(triplets) =>
        UndirectedGraph.triplesToTryGraph(triplets) match
          case Success(graph: Graph[Int]) =>
            val (_, componentMap) = ConnectedComponents.components[Int, Unit](graph)
            componentMap.size shouldBe 13
          case Failure(x) => fail(x)
      case Failure(x) => fail(x)
  }

  it should "assign the same component ID to all vertices in component A" in {
    val p = new GraphParser[Int, Unit, EdgeType]
    val triedSource = Try(Source.fromResource("dfsu.graph"))
    val ws = TryUsing.trial(triedSource)(_.getLines().toSeq).get filterNot (_.startsWith("//"))
    sequence(for w <- ws yield p.parseTriple(w)) match
      case Success(triplets) =>
        UndirectedGraph.triplesToTryGraph(triplets) match
          case Success(graph: Graph[Int]) =>
            val (_, componentMap) = ConnectedComponents.components[Int, Unit](graph)
            val compA = componentMap(0)
            Set(0, 1, 2, 3, 4, 5, 6).forall(componentMap(_) == compA) shouldBe true
          case Failure(x) => fail(x)
      case Failure(x) => fail(x)
  }

  it should "assign the same component ID to both vertices in component B" in {
    val p = new GraphParser[Int, Unit, EdgeType]
    val triedSource = Try(Source.fromResource("dfsu.graph"))
    val ws = TryUsing.trial(triedSource)(_.getLines().toSeq).get filterNot (_.startsWith("//"))
    sequence(for w <- ws yield p.parseTriple(w)) match
      case Success(triplets) =>
        UndirectedGraph.triplesToTryGraph(triplets) match
          case Success(graph: Graph[Int]) =>
            val (_, componentMap) = ConnectedComponents.components[Int, Unit](graph)
            componentMap(7) shouldBe componentMap(8)
          case Failure(x) => fail(x)
      case Failure(x) => fail(x)
  }

  it should "assign the same component ID to all vertices in component C" in {
    val p = new GraphParser[Int, Unit, EdgeType]
    val triedSource = Try(Source.fromResource("dfsu.graph"))
    val ws = TryUsing.trial(triedSource)(_.getLines().toSeq).get filterNot (_.startsWith("//"))
    sequence(for w <- ws yield p.parseTriple(w)) match
      case Success(triplets) =>
        UndirectedGraph.triplesToTryGraph(triplets) match
          case Success(graph: Graph[Int]) =>
            val (_, componentMap) = ConnectedComponents.components[Int, Unit](graph)
            val compC = componentMap(9)
            Set(9, 10, 11, 12).forall(componentMap(_) == compC) shouldBe true
          case Failure(x) => fail(x)
      case Failure(x) => fail(x)
  }

  it should "assign distinct component IDs to A, B and C" in {
    val p = new GraphParser[Int, Unit, EdgeType]
    val triedSource = Try(Source.fromResource("dfsu.graph"))
    val ws = TryUsing.trial(triedSource)(_.getLines().toSeq).get filterNot (_.startsWith("//"))
    sequence(for w <- ws yield p.parseTriple(w)) match
      case Success(triplets) =>
        UndirectedGraph.triplesToTryGraph(triplets) match
          case Success(graph: Graph[Int]) =>
            val (_, componentMap) = ConnectedComponents.components[Int, Unit](graph)
            val compA = componentMap(0)
            val compB = componentMap(7)
            val compC = componentMap(9)
            compA should not be compB
            compB should not be compC
            compA should not be compC
          case Failure(x) => fail(x)
      case Failure(x) => fail(x)
  }

  it should "track exactly 10 connexions (13 vertices minus 3 component roots)" in {
    val p = new GraphParser[Int, Unit, EdgeType]
    val triedSource = Try(Source.fromResource("dfsu.graph"))
    val ws = TryUsing.trial(triedSource)(_.getLines().toSeq).get filterNot (_.startsWith("//"))
    sequence(for w <- ws yield p.parseTriple(w)) match
      case Success(triplets) =>
        UndirectedGraph.triplesToTryGraph(triplets) match
          case Success(graph: Graph[Int]) =>
            val (connexions, _) = ConnectedComponents.components[Int, Unit](graph)
            connexions.connexions.size shouldBe 10
          case Failure(x) => fail(x)
      case Failure(x) => fail(x)
  }

  it should "find no connexion entry for component roots" in {
    val p = new GraphParser[Int, Unit, EdgeType]
    val triedSource = Try(Source.fromResource("dfsu.graph"))
    val ws = TryUsing.trial(triedSource)(_.getLines().toSeq).get filterNot (_.startsWith("//"))
    sequence(for w <- ws yield p.parseTriple(w)) match
      case Success(triplets) =>
        UndirectedGraph.triplesToTryGraph(triplets) match
          case Success(graph: Graph[Int]) =>
            val (connexions, componentMap) = ConnectedComponents.components[Int, Unit](graph)
            // Roots are exactly the vertices absent from connexions
            val roots = componentMap.keySet -- connexions.connexions.keySet
            roots.size shouldBe 3
            roots.foreach { root =>
              connexions.connexions.get(root) shouldBe None
            }
          case Failure(x) =>
            fail(x)
      case Failure(x) =>
        fail(x)
  }