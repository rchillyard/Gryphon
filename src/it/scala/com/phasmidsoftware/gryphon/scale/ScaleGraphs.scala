package com.phasmidsoftware.gryphon.scale

import com.phasmidsoftware.gryphon.adjunct.{DirectedGraph, UndirectedGraph}
import com.phasmidsoftware.gryphon.core.*
import com.phasmidsoftware.gryphon.parse.GraphParser
import com.phasmidsoftware.gryphon.util.TryUsing
import scala.io.Source
import scala.util.{Failure, Success, Try}

/**
 * Loads the four 1000-vertex fixtures, once per JVM.
 *
 * Parsing is the expensive part — about 0.3 s for a sparse graph and 2 s for a dense one, which
 * is most of what a scale run costs — so each graph is a `lazy val` here rather than a method on
 * a spec. Two specs reading the same graph then pay for it once between them.
 *
 * The graphs themselves are generated and deterministic; `src/it/resources/scale/README.md`
 * records the generator and its seeds, so they can be rebuilt or extended rather than being
 * opaque blobs that happen to be checked in.
 */
object ScaleGraphs:

  private def triplets(resource: String): Seq[Triplet[Int, Double, EdgeType]] =
    val p = new GraphParser[Int, Double, EdgeType]
    TryUsing.tryIt(Try(Source.fromResource(resource)))(s =>
      p.parseSource[Triplet[Int, Double, EdgeType]](p.parseTriple)(s)) match
      case Success(ts) => ts
      case Failure(x) => throw x

  private def undirected(resource: String): UndirectedGraph[Int, Double] =
    UndirectedGraph.triplesToTryGraph[Int, Double](Vertex.createWithSet)(triplets(resource)) match
      case Success(g: UndirectedGraph[Int, Double] @unchecked) => g
      case other => throw GraphException(s"$resource: $other")

  private def directed(resource: String): DirectedGraph[Int, Double] =
    DirectedGraph.triplesToTryGraph[Int, Double](Vertex.createWithSet)(triplets(resource)) match
      case Success(g: DirectedGraph[Int, Double] @unchecked) => g
      case other => throw GraphException(s"$resource: $other")

  /** 1000 vertices, 7000 undirected weighted edges, connected by construction. */
  lazy val sparse: UndirectedGraph[Int, Double] = undirected("scale/sparse.graph")

  /** 1000 vertices, 30000 undirected weighted edges, connected by construction. */
  lazy val dense: UndirectedGraph[Int, Double] = undirected("scale/dense.graph")

  /** 1000 vertices, 7000 directed weighted edges, acyclic by construction. */
  lazy val sparseDag: DirectedGraph[Int, Double] = directed("scale/sparseDag.graph")

  /** 1000 vertices, 30000 directed weighted edges, acyclic by construction. */
  lazy val denseDag: DirectedGraph[Int, Double] = directed("scale/denseDag.graph")

  val vertices: Int = 1000
