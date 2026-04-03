package com.phasmidsoftware.gryphon.adjunct

import com.phasmidsoftware.gryphon.core.*
import com.phasmidsoftware.gryphon.util.GraphException
import com.phasmidsoftware.visitor.core.{Evaluable, JournaledVisitor}
import scala.util.{Failure, Random, Success, Try}

/**
 * Represents a directed graph structure that supports operations on vertexMap and edges. 
 * This class is a concrete implementation of the `EdgeGraph` trait, and models directed 
 * relationships between vertices with edges.
 *
 * @tparam V the type of attributes for the vertexMap in the graph (invariant).
 * @tparam E the type of attributes for the edges in the graph (invariant).
 * @param vertexMap the `VertexMap` that represents this `Graph`.
 */
case class UndirectedGraph[V, E](vertexMap: VertexMap[V]) extends AbstractGraph[V](vertexMap) with EdgeGraph[V, E] {

  /**
   * Adds an edge to the directed graph, creating or updating the vertices and their adjacencies
   * accordingly. The edge is connected between its `from` and `black` vertices.
   *
   * CONSIDER eliminating this method.
   *
   * @param edge the edge to be added to the graph. It contains a starting vertex (`from`),
   *             an ending vertex (`black`), and an associated attribute that defines the edge.
   *
   * @return a new instance of the graph that includes the specified edge and updated vertices.
   */
  def addEdge(edge: Edge[E, V]): UndirectedGraph[V, E] =
    copy(vertexMap + edge)

  /**
   * Retrieves all edges in the directed graph as an iterable collection of edges.
   *
   * @return an iterable collection containing all edges of type `Edge[E, V]` in the graph.
   */
  def edges: Iterator[UndirectedEdge[E, V]] = {
    adjacencies.collect { case AdjacencyEdge(e: UndirectedEdge[E, V] @unchecked, false) => e }
  }

  override def M: Int = edges.size

  /**
   * Returns the degree of vertex `v` — the number of edges incident to it.
   */
  def degree(v: V): Int =
    vertexMap(v).adjacencies.size

  /**
   * Returns the maximum degree of any vertex in the graph.
   */
  def maxDegree: Int =
    vertexMap.keySet.toSeq.map(degree).max

  /**
   * Returns the mean degree across all vertices in the graph.
   */
  def meanDegree: Double =
    vertexMap.keySet.toSeq.map(degree).sum.toDouble / N

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
    if N == 0 then true
    else
      given Random = Random(0)

      given Evaluable[V, V] with
        def evaluate(v: V): Option[V] = Some(v)
      val start = vertexMap.keySet.head
      dfs(JournaledVisitor.withQueueJournal[V, V])(start)
              .result.map(_._1).toSet.size == N

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
   * Creates a new directed graph using the provided vertex map.
   *
   * @param vertexMap the vertex map to be used for constructing the graph. It defines the vertices
   *                  and their adjacencies in the graph.
   *
   * @return a new directed graph constructed with the given vertex map.
   */
  def unit(vertexMap: VertexMap[V]): Graph[V] =
    DirectedGraph(vertexMap)

  /**
   * A partial function that extracts a `DirectedEdge` from an `Adjacency`, specifically when the `Adjacency`
   * is an `AdjacencyEdge` containing a `DirectedEdge` and is not marked as `discovered`.
   *
   * This function will throw a `GraphException` if the input `Adjacency` does not match the expected pattern,
   * indicating an unsupported or an invalid edge type.
   *
   * @throws GraphException if the input is not an `AdjacencyEdge` with a `DirectedEdge`.
   */
  private val getUndirectedEdgeFromAdjacency: PartialFunction[Adjacency[V], UndirectedEdge[E, V]] = {
    case AdjacencyEdge(e: UndirectedEdge[E, V] @unchecked, false) =>
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
   *
   * @tparam V the type of the vertices in the graph.
   * @tparam E the type of the edges in the graph.
   * @return a `Try[Graph[V]]`, where:
   *         - `Success(Graph[V])` contains the constructed graph if the operation is successful.
   *         - `Failure` contains a `GraphException` if the graph construction fails.
   */
  def triplesToTryGraph[V, E](f: V => Vertex[V])(triples: Seq[Triplet[V, E, EdgeType]]): Try[Graph[V]] =
    SerializableGraph.createFromTriplets[V, E, EdgeType](triples) match {
      case triplets: Triplets[V, E, EdgeType] =>
        val vm: VertexMap[V] =
          triplets.triplets.foldLeft(VertexMap[V]) {
            (z, t) =>
              // TODO find another way to handle this anomaly
              if (t._4.oneWay) System.err.println(s"WARNING: edge ${t._3} is directed.")
              z.createVerticesFromTriplet[E, EdgeType](f) {
                        case (vv1, vv2, Some(e)) =>
                          AdjacencyEdge(UndirectedEdge(e, vv1.attribute, vv2.attribute))
                        case (vv1, vv2, None) => // TODO fix this case so that it doesn't use a directed edge.
                          AdjacencyEdge(AttributedDirectedEdge(None, vv1.attribute, vv2.attribute))
                      }(!t._4.oneWay)
                      (t)
          }
        val graph = UndirectedGraph(vm)
        // TODO find another way to handle this anomaly
        val expectedAdjacencies = triplets.triplets.map(t => if t._4.oneWay then 1 else 2).sum
        if graph.adjacencies.size != expectedAdjacencies then
          System.err.println(s"WARNING: ${graph.adjacencies.size} != $expectedAdjacencies")
        Success(graph)
      case z =>
        Failure(GraphException(s"parse failed: $z"))
    }
}
