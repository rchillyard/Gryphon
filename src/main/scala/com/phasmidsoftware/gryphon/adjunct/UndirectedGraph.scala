package com.phasmidsoftware.gryphon.adjunct

import scala.annotation.unused
import com.phasmidsoftware.gryphon.core.*
import com.phasmidsoftware.gryphon.core.GraphException
import com.phasmidsoftware.visitor.core.{Evaluable, JournaledVisitor}
import scala.util.{Random, Try}

/**
 * Represents a directed graph structure that supports operations on vertexMap and edges. 
 * This class is a concrete implementation of the `EdgeGraph` trait, and models directed 
 * relationships between vertices with edges.
 *
 * @tparam V the type of attributes for the vertexMap in the graph (invariant).
 * @tparam E the type of attributes for the edges in the graph (invariant).
 * @param vertexMap the `VertexMap` that represents this `Graph`.
 */
case class UndirectedGraph[V, E](vertexMap: VertexMap[V])
        extends AbstractAdjunctGraph[V, E, UndirectedGraph[V, E]](vertexMap) {

  /**
   * Adds an edge to the directed graph, creating or updating the vertices and their adjacencies
   * accordingly. The edge is connected between its `from` and `black` vertices.
   *
   * CONSIDER eliminating this method.
   *
   * @param edge the edge to be added to the graph. It contains a starting vertex (`from`),
   *             an ending vertex (`black`), and an associated attribute that defines the edge.
   * @return a new instance of the graph that includes the specified edge and updated vertices.
   */
  /** An undirected graph admits undirected edges only. */
  type EdgeT = UndirectedEdge[V, E]

  /**
   * Retrieves all edges in the directed graph as an iterable collection of edges.
   *
   * @return an iterable collection containing all edges of type `Edge[V, E]` in the graph.
   */
  def edges: Iterator[UndirectedEdge[V, E]] = {
    adjacencies.collect { case AdjacencyEdge(e: UndirectedEdge[V, E] @unchecked, false) => e }
  }

  /**
   * Returns the degree of vertex `v` — the number of edges incident to it.
   */
  def degree(v: V): Int =
    vertexMap(v).adjacencies.size

  /**
   * Returns the maximum degree of any vertex in the graph.
   */
  /**
   * Returns the in-degree of `v`, which for an undirected graph is simply its degree:
   * every edge can be traversed in both directions.
   */
  def inDegree(v: V): Int = degree(v)

  /**
   * Returns the out-degree of `v`, which for an undirected graph is simply its degree.
   */
  def outDegree(v: V): Int = degree(v)

  def maxDegree: Int =
    vertexMap.keySet.toSeq.map(degree).max

  /**
   * Returns the mean degree across all vertices in the graph.
   */
  def meanDegree: Double =
    vertexMap.keySet.toSeq.map(degree).sum.toDouble / order

  /**
   * Determines whether the graph contains a cycle using DFS to detect is we encounter a previously visited vertex.
   *
   * @return true if the graph is cyclic (i.e., it contains at least one cycle), false otherwise.
   */
  def isCyclic: Boolean =
    given Random = Random(0)

    val visited = scala.collection.mutable.Set.empty[V]

    def dfs(v: V, parent: Option[V]): Boolean =
      visited += v
      adjacentVertices(v).exists { w =>
        if !visited.contains(w) then dfs(w, Some(v))
        else !parent.contains(w)
      }

    vertexMap.keySet.exists { v =>
      !visited.contains(v) && dfs(v, None)
    }

  /**
   * Returns true if this undirected graph is connected — i.e. every vertex
   * is reachable from every other via some path.
   *
   * Performs a single DFS from an arbitrary start vertex and checks whether
   * all N vertices were visited.
   */
  def isConnected: Boolean =
    if order == 0 then true
    else
      given Random = Random(0)

      given Evaluable[V, V] with
        def evaluate(v: V): Option[V] = Some(v)
      val start = vertexMap.keySet.head
      dfs(JournaledVisitor.withQueueJournal[V, V])(start)
              .result.map(_._1).toSet.size == order

  /**
   * Returns true if this undirected graph is bipartite — i.e. its vertices
   * can be partitioned into two sets such that every edge connects vertices
   * in different sets. Equivalently, the graph contains no odd-length cycle.
   *
   * Uses DFS with 2-coloring. Handles disconnected graphs by seeding each
   * unvisited component in turn.
   */
  def isBipartite: Boolean =
    given Random = Random(0)

    val color = scala.collection.mutable.Map.empty[V, Boolean]

    def dfs(v: V, c: Boolean): Boolean =
      color(v) = c
      adjacentVertices(v).forall { w =>
        if !color.contains(w) then dfs(w, !c)
        else color(w) != c
      }

    vertexMap.keySet.forall { v =>
      color.contains(v) || dfs(v, true)
    }

  /**
   * Creates a new undirected graph using the provided vertex map.
   *
   * Must build an `UndirectedGraph`: `unit` is how every derived graph is rebuilt, so returning
   * the other kind here silently changed an undirected graph into a directed one. `addVertex` is
   * the public route to it.
   *
   * @param vertexMap the vertex map to be used for constructing the graph. It defines the vertices
   *                  and their adjacencies in the graph.
   * @return a new undirected graph constructed with the given vertex map.
   */
  def unit(vertexMap: VertexMap[V]): UndirectedGraph[V, E] =
    UndirectedGraph[V, E](vertexMap)

  /**
   * A partial function that extracts a `DirectedEdge` from an `Adjacency`, specifically when the `Adjacency`
   * is an `AdjacencyEdge` containing a `DirectedEdge` and is not marked as `discovered`.
   *
   * This function will throw a `GraphException` if the input `Adjacency` does not match the expected pattern,
   * indicating an unsupported or an invalid edge type.
   *
   * NOTE this method is currently unused but I think we should keep it around.
   *
   * @throws GraphException if the input is not an `AdjacencyEdge` with a `DirectedEdge`.
   */
  @unused private val getUndirectedEdgeFromAdjacency: PartialFunction[Adjacency[V], UndirectedEdge[V, E]] = {
    case AdjacencyEdge(e: UndirectedEdge[V, E] @unchecked, false) =>
      e
    case x =>
      throw GraphException(s"unexpected edge type: $x")
  }
}

/**
 * Factory object for creating instances of `DirectedGraph`.
 * This object provides utility methods to construct a directed graph
 * from an edge list representation.
 */
object UndirectedGraph {

  /**
   * Converts a sequence of triplets representing graph edges into a `Try` of `Graph[V]`.
   *
   * Each triplet in the input sequence consists of two vertices (source and target)
   * and an attribute associated with the edge connecting the vertices.
   * This method attempts to construct a directed graph from the provided triplets, returning
   * a `Success` with the graph instance if the operation is successful, or a `Failure`
   * if the construction process encounters an error.
   *
   * @param triples a sequence of triplets `Triplet[V, E]` where:
   *                - the first element is the source vertex of type `V`.
   *                - the second element is the target vertex of type `V`.
   *                - the third element is the edge attribute of type `E`.
   * @tparam V the type of the vertices in the graph.
   * @tparam E the type of the edges in the graph.
   * @return a `Try[Graph[V]]`, where:
   *         - `Success(Graph[V])` contains the constructed graph if the operation is successful.
   *         - `Failure` contains a `GraphException` if the graph construction fails.
   */
  def triplesToTryGraph[V, E](f: V => Vertex[V])(triples: Seq[Triplet[V, E, EdgeType]]): Try[UndirectedGraph[V, E]] =
    AbstractAdjunctGraph.fromTriplets[V, E, UndirectedGraph[V, E]](
      f = f,
      warn = t => Option.when(t.edgeType.oneWay)(s"WARNING: edge ${t.maybeAttribute} is directed."),
      adjacency = {
        // An edge with no attribute is still an edge of this graph, so it must be undirected
        // like the rest. It used to be an AttributedDirectedEdge, which UndirectedGraph.edges
        // then discarded — the graph counted the adjacency in `degree` but not the edge in
        // `edges` or `M`.
        case (vv1, vv2, Some(e)) => AdjacencyEdge(UndirectedEdge(e, vv1.attribute, vv2.attribute))
        case (vv1, vv2, None) => AdjacencyEdge(UndirectedEdge(None, vv1.attribute, vv2.attribute))
      },
      bothWays = !_.edgeType.oneWay,
      build = UndirectedGraph(_)
    )(triples)
}
