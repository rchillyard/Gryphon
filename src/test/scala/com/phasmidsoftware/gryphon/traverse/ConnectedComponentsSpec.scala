/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.traverse

import com.phasmidsoftware.gryphon.adjunct.UndirectedGraph
import com.phasmidsoftware.gryphon.core.*
import com.phasmidsoftware.gryphon.parse.GraphParser
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

  given Random = Random(42)

  private def withDfsuGraph[A](f: Graph[Int] => A): A =
    val p = new GraphParser[Int, Unit, EdgeType]
    val triedSource = Try(Source.fromResource("dfsu.graph"))
    TryUsing.tryIt(triedSource) { source =>
      p.parseSource[Triplet[Int, Unit, EdgeType]](p.parseTriple)(source)
    } match
      case Success(triplets) =>
        UndirectedGraph.triplesToTryGraph[Int, Unit](Vertex.createWithSet)(triplets) match
          case Success(graph: Graph[Int]) => f(graph)
          case Failure(x) => fail(x)
      case Failure(x) =>
        fail(x)

  behavior of "ConnectedComponents"

  it should "identify 3 components in dfsu.graph" in :
    withDfsuGraph { graph =>
      val (_, componentMap) = ConnectedComponents.components[Int, Unit](graph)
      componentMap.values.toSet.size shouldBe 3
    }

  it should "label all 13 vertices in dfsu.graph" in :
    withDfsuGraph { graph =>
      val (_, componentMap) = ConnectedComponents.components[Int, Unit](graph)
      componentMap.size shouldBe 13
    }

  it should "assign the same component ID to all vertices in component A" in :
    withDfsuGraph { graph =>
      val (_, componentMap) = ConnectedComponents.components[Int, Unit](graph)
      val compA = componentMap(0)
      Set(0, 1, 2, 3, 4, 5, 6).forall(componentMap(_) == compA) shouldBe true
    }

  it should "assign the same component ID to both vertices in component B" in :
    withDfsuGraph { graph =>
      val (_, componentMap) = ConnectedComponents.components[Int, Unit](graph)
      componentMap(7) shouldBe componentMap(8)
    }

  it should "assign the same component ID to all vertices in component C" in :
    withDfsuGraph { graph =>
      val (_, componentMap) = ConnectedComponents.components[Int, Unit](graph)
      val compC = componentMap(9)
      Set(9, 10, 11, 12).forall(componentMap(_) == compC) shouldBe true
    }

  it should "assign distinct component IDs to A, B and C" in :
    withDfsuGraph { graph =>
      val (_, componentMap) = ConnectedComponents.components[Int, Unit](graph)
      val compA = componentMap(0)
      val compB = componentMap(7)
      val compC = componentMap(9)
      compA should not be compB
      compB should not be compC
      compA should not be compC
    }

  it should "track exactly 10 connexions (13 vertices minus 3 component roots)" in :
    withDfsuGraph { graph =>
      val (connexions, _) = ConnectedComponents.components[Int, Unit](graph)
      connexions.connexions.size shouldBe 10
    }

  it should "find no connexion entry for component roots" in :
    withDfsuGraph { graph =>
      val (connexions, componentMap) = ConnectedComponents.components[Int, Unit](graph)
      val roots = componentMap.keySet -- connexions.connexions.keySet
      roots.size shouldBe 3
      roots.foreach { root =>
        connexions.connexions.get(root) shouldBe None
      }
    }
  // ---------------------------------------------------------------------------------------
  // Issue #143: the generator reaches the traversal.
  //
  // The two halves of the result behave differently, and both are pinned here.
  //
  // The component map does not vary: `visited` is the set a component's DFS reached, and ids
  // are handed out as `unvisited.head` walks a Set. Neither is shuffled.
  //
  // The Connexions tree does vary: it records the edge by which each vertex was discovered,
  // and within a component that is exactly what adjacency order decides. Until 1.13.0 the
  // `Random` parameter was annotated `@unused` and the DFS built its own, so the tree varied
  // but no seed could reach it -- varying and uncontrollable at once.
  //
  // It was tempting to conclude from "no caller reads the tree" that the parameter was
  // pointless and should be removed. That would have been wrong: unobserved is not invariant,
  // and removing it would have frozen into the API a claim the value does not support.

  it should "give the same component map whatever the generator" in :
    withDfsuGraph { graph =>
      val maps = (0 until 20).map(s =>
        ConnectedComponents.components[Int, Unit](graph)(using Random(s))._2).distinct
      maps.size shouldBe 1
    }

  it should "give a reproducible connexions tree for one seed" in :
    withDfsuGraph { graph =>
      val first = ConnectedComponents.components[Int, Unit](graph)(using Random(42))._1
      val second = ConnectedComponents.components[Int, Unit](graph)(using Random(42))._1
      first.connexions shouldBe second.connexions
    }

  it should "give different connexions trees for different seeds" in :
    withDfsuGraph { graph =>
      // The half that would have been lost by deleting the parameter. Component A has seven
      // vertices, so there are many discovery trees to choose between; twenty seeds agreeing
      // would mean the generator is not reaching getConnexions, which is what #143 was.
      val trees = (0 until 20).map(s =>
        ConnectedComponents.components[Int, Unit](graph)(using Random(s))._1.connexions).distinct
      trees.size should be > 1
    }
