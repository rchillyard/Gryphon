package com.phasmidsoftware.gryphon.traverse

import com.phasmidsoftware.gryphon.adjunct.{DirectedGraph, UndirectedGraph}
import com.phasmidsoftware.gryphon.core.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scala.util.{Random, Success}

/**
 * The contract shared by the two component algorithms — issue #168.
 *
 * `ConnectedComponents.components` and `StronglyConnectedComponents.kosaraju` answer the same
 * kind of question and, since 1.15.0, return the same kind of thing: an
 * [[AbstractComponentResult]]. These tests are about the supertype rather than either algorithm,
 * and in particular about the one statement it exists to make.
 *
 * ==The statement==
 *
 * `partition` is a property of the graph; `map` is a labelling whose stability is an
 * implementation detail. The two algorithms differ in exactly the way that makes this worth
 * saying once: connected components happens not to vary its ids, strongly connected components
 * genuinely does. A caller told "the result is invariant" would be right about one and wrong
 * about the other, which is how the asymmetry survived being documented twice.
 */
class ComponentResultSpec extends AnyFlatSpec with Matchers:

  /** Two undirected components: {0,1,2} and {3,4}. */
  private def undirectedPair: UndirectedGraph[Int, Unit] =
    val ts: Seq[Triplet[Int, Unit, EdgeType]] = Seq(
      Triplet(0, 1, Some(()), Undirected), Triplet(1, 2, Some(()), Undirected),
      Triplet(3, 4, Some(()), Undirected))
    UndirectedGraph.triplesToTryGraph[Int, Unit](Vertex.createWithSet)(ts) match
      case Success(g: UndirectedGraph[Int, Unit] @unchecked) => g
      case other => fail(s"construction failed: $other")

  /** Two strongly connected components, {0,1,2} and {3,4}, with an arc between them. */
  private def directedPair: DirectedGraph[Int, Unit] =
    val ts: Seq[Triplet[Int, Unit, EdgeType]] = Seq(
      Triplet(0, 1, Some(()), Directed), Triplet(1, 2, Some(()), Directed), Triplet(2, 0, Some(()), Directed),
      Triplet(2, 3, Some(()), Directed),
      Triplet(3, 4, Some(()), Directed), Triplet(4, 3, Some(()), Directed))
    DirectedGraph.triplesToTryGraph[Int, Unit](Vertex.createWithSet)(ts) match
      case Success(g: DirectedGraph[Int, Unit] @unchecked) => g
      case other => fail(s"construction failed: $other")

  behavior of "the shared supertype"

  // The point of the type. Before 1.15.0 these two returned unrelated things -- a rich result and
  // a bare Map -- so no caller could write one function over "a component decomposition".
  it should "let one function take either algorithm's result" in {
    def componentCount(r: AbstractComponentResult[Int]): Int = r.partition.size

    given Random = Random(42)

    componentCount(ConnectedComponents.components[Int, Unit](undirectedPair)) shouldBe 2
    componentCount(StronglyConnectedComponents.kosaraju[Int, Unit](directedPair)) shouldBe 2
  }

  it should "give both a partition that covers every vertex exactly once" in {
    given Random = Random(42)

    for r <- Seq(
      ConnectedComponents.components[Int, Unit](undirectedPair),
      StronglyConnectedComponents.kosaraju[Int, Unit](directedPair)
    ) do
      // Deliberately through the TRAIT's API -- keySet and vertexTraverse -- rather than through
      // either subtype's `map` field. `map` is a constructor parameter of the supertype and so
      // is not a member of it, which is right: a caller holding an AbstractComponentResult should
      // be asking which component a vertex is in, not reaching for the labelling.
      r.partition.flatten shouldBe r.keySet
      r.partition.toSeq.map(_.size).sum shouldBe r.size
      r.partition.foreach(c => c.flatMap(r.vertexTraverse).size shouldBe 1) // one id per component
  }

  // The trap, inherited from AbstractVertexTraversalResult and pinned for both subtypes at once.
  it should "inherit size as the vertex count, with the component count on partition" in {
    given Random = Random(42)

    val connected = ConnectedComponents.components[Int, Unit](undirectedPair)
    connected.size shouldBe 5 // vertices
    connected.partition.size shouldBe 2 // components

    val strong = StronglyConnectedComponents.kosaraju[Int, Unit](directedPair)
    strong.size shouldBe 5
    strong.partition.size shouldBe 2
  }

  behavior of "the asymmetry the supertype exists to state"

  // These two tests are the reason the contract belongs on a shared type rather than being
  // written twice: the same member is invariant on both, while the member beside it is
  // invariant on one and not the other.
  it should "hold partition invariant for both algorithms" in {
    (0 until 20).map { s =>
      given Random = Random(s)
      ConnectedComponents.components[Int, Unit](undirectedPair).partition
    }.distinct.size shouldBe 1

    (0 until 20).map { s =>
      given Random = Random(s)
      StronglyConnectedComponents.kosaraju[Int, Unit](directedPair).partition
    }.distinct.size shouldBe 1
  }

  it should "expose the component sets, so no caller has to derive them from the ids" in {
    given Random = Random(42)

    ConnectedComponents.components[Int, Unit](undirectedPair).partition shouldBe
            Set(Set(0, 1, 2), Set(3, 4))
    StronglyConnectedComponents.kosaraju[Int, Unit](directedPair).partition shouldBe
            Set(Set(0, 1, 2), Set(3, 4))
  }
