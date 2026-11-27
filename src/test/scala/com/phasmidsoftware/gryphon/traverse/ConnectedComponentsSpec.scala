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
      val componentMap = ConnectedComponents.components[Int, Unit](graph).map
      componentMap.values.toSet.size shouldBe 3
    }

  it should "label all 13 vertices in dfsu.graph" in :
    withDfsuGraph { graph =>
      val componentMap = ConnectedComponents.components[Int, Unit](graph).map
      componentMap.size shouldBe 13
    }

  it should "assign the same component ID to all vertices in component A" in :
    withDfsuGraph { graph =>
      val componentMap = ConnectedComponents.components[Int, Unit](graph).map
      val compA = componentMap(0)
      Set(0, 1, 2, 3, 4, 5, 6).forall(componentMap(_) == compA) shouldBe true
    }

  it should "assign the same component ID to both vertices in component B" in :
    withDfsuGraph { graph =>
      val componentMap = ConnectedComponents.components[Int, Unit](graph).map
      componentMap(7) shouldBe componentMap(8)
    }

  it should "assign the same component ID to all vertices in component C" in :
    withDfsuGraph { graph =>
      val componentMap = ConnectedComponents.components[Int, Unit](graph).map
      val compC = componentMap(9)
      Set(9, 10, 11, 12).forall(componentMap(_) == compC) shouldBe true
    }

  it should "assign distinct component IDs to A, B and C" in :
    withDfsuGraph { graph =>
      val componentMap = ConnectedComponents.components[Int, Unit](graph).map
      val compA = componentMap(0)
      val compB = componentMap(7)
      val compC = componentMap(9)
      compA should not be compB
      compB should not be compC
      compA should not be compC
    }

  it should "track exactly 10 connexions (13 vertices minus 3 component roots)" in :
    withDfsuGraph { graph =>
      val connexions = ConnectedComponents.components[Int, Unit](graph).connexions
      connexions.connexions.size shouldBe 10
    }

  it should "find no connexion entry for component roots" in :
    withDfsuGraph { graph =>
      val result = ConnectedComponents.components[Int, Unit](graph)
      val (connexions, componentMap) = (result.connexions, result.map)
      val roots = componentMap.keySet -- connexions.connexions.keySet
      roots.size shouldBe 3
      roots.foreach { root =>
        connexions.connexions.get(root) shouldBe None
      }
    }
  // ---------------------------------------------------------------------------------------
  // Issue #143: the generator reaches the traversal.
  //
  // The three views on the result behave differently, and all are pinned here.
  //
  // The component map does not vary: `visited` is the set a component's DFS reached, and ids
  // are handed out as `unvisited.head` walks a Set. Neither is shuffled.
  //
  // The connexions tree does vary: it records the edge by which each vertex was discovered,
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
        ConnectedComponents.components[Int, Unit](graph)(using Random(s)).map).distinct
      maps.size shouldBe 1
    }

  it should "give a reproducible connexions tree for one seed" in :
    withDfsuGraph { graph =>
      val first = ConnectedComponents.components[Int, Unit](graph)(using Random(42)).connexions
      val second = ConnectedComponents.components[Int, Unit](graph)(using Random(42)).connexions
      first.connexions shouldBe second.connexions
    }

  it should "give different connexions trees for different seeds" in :
    withDfsuGraph { graph =>
      // The half that would have been lost by deleting the parameter. Component A has seven
      // vertices, so there are many discovery trees to choose between; twenty seeds agreeing
      // would mean the generator is not reaching getConnexions, which is what #143 was.
      val trees = (0 until 20).map(s =>
        ConnectedComponents.components[Int, Unit](graph)(using Random(s)).connexions.connexions).distinct
      trees.size should be > 1
    }

  // ---------------------------------------------------------------------------------------
  // Issue #157: the result is a TraversalResult, and carries the partition.

  behavior of "ConnectedResult"

  it should "expose the partition, which is the components themselves" in :
    withDfsuGraph { graph =>
      ConnectedComponents.components[Int, Unit](graph).partition shouldBe
              Set(Set(0, 1, 2, 3, 4, 5, 6), Set(7, 8), Set(9, 10, 11, 12))
    }

  // This is the assertion the whole issue was for, and it is stronger than the one above it on
  // the component map. The ids do not vary either, but only because `unvisited.head` walks a Set
  // -- an implementation detail. The partition is a property of the graph: two runs that
  // disagreed about which id each component received would still produce this same value.
  it should "give the identical partition whatever the generator" in :
    withDfsuGraph { graph =>
      val partitions = (0 until 20).map(s =>
        ConnectedComponents.components[Int, Unit](graph)(using Random(s)).partition).distinct
      partitions.size shouldBe 1
    }

  it should "derive the partition from the map, so the two cannot disagree" in :
    withDfsuGraph { graph =>
      val result = ConnectedComponents.components[Int, Unit](graph)
      // Every vertex appears in exactly one component...
      result.partition.flatten shouldBe result.map.keySet
      result.partition.toSeq.map(_.size).sum shouldBe result.map.size
      // ...and each component is exactly the set of vertices sharing one id.
      result.partition.foreach(c => c.map(result.map).size shouldBe 1)
    }

  // The trap #157 named: `size` is inherited from AbstractVertexTraversalResult and counts
  // VERTICES, like every other TraversalResult. On a type called ConnectedResult it is tempting
  // to read it as the component count, which is `partition.size`. Both are pinned here so that
  // anyone changing one has to look at the other.
  it should "inherit size as the vertex count, with the component count on partition" in :
    withDfsuGraph { graph =>
      val result = ConnectedComponents.components[Int, Unit](graph)
      result.size shouldBe 13
      result.partition.size shouldBe 3
    }

  it should "behave as a vertex-keyed TraversalResult" in :
    withDfsuGraph { graph =>
      val result = ConnectedComponents.components[Int, Unit](graph)
      // The point of the change: it can be passed to a function written over the trait, which a
      // bare tuple could not be.
      def componentOf(r: TraversalResult[Int, Int])(v: Int): Option[Int] = r.vertexTraverse(v)

      componentOf(result)(0) shouldBe Some(result.map(0))
      componentOf(result)(99) shouldBe None
      result.keySet shouldBe (0 to 12).toSet
      // `unit` replaces the map and keeps the discovery tree.
      result.unit(Map(0 -> 7)) match
        case c: ConnectedResult[Int, Unit] @unchecked =>
          c.map shouldBe Map(0 -> 7)
          c.connexions shouldBe result.connexions
        case other => fail(s"expected a ConnectedResult, got $other")
    }

  it should "give an empty partition for a graph with no vertices" in :
    val empty = UndirectedGraph[Int, Unit](VertexMap[Int])
    val result = ConnectedComponents.components[Int, Unit](empty)
    result.partition shouldBe Set.empty
    result.size shouldBe 0
