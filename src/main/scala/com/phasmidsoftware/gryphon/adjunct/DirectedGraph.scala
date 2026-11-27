package com.phasmidsoftware.gryphon.adjunct

import cats.kernel.Monoid
import com.phasmidsoftware.gryphon.core.*
import com.phasmidsoftware.gryphon.traverse.{BellmanFord, TopologicalSort, VertexTraversalResult}
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
case class DirectedGraph[V, E](vertexMap: VertexMap[V])
        extends AbstractAdjunctGraph[V, E, DirectedGraph[V, E]](vertexMap) {
  /**
   * Returns a new DirectedGraph with all edges reversed (every u→v becomes v→u).
   * The vertex set is preserved, including any vertices with no outgoing edges.
   *
   * This is the first step in Kosaraju's strongly-connected-components algorithm.
   *
   * @return a new `DirectedGraph[V, E]` with reversed edges.
   */
  def reverse: DirectedGraph[V, E] =
    edges.foldLeft(DirectedGraph[V, E](vertexMap.keysOnly)) { (g, e) =>
      val rev: DirectedEdge[V, E] = e match
        case AttributedDirectedEdge(attr, from, to) =>
          AttributedDirectedEdge(attr, to, from)
        case OrderedEdge(from, to) =>
          OrderedEdge(to, from)
        case other =>
          throw GraphException(s"unexpected edge type in reverse: $other")
      g.addEdge(rev)
    }

  /**
   * Returns the in-degree of `v`: the number of arcs whose head is `v`.
   * A self-loop counts once, towards both the in-degree and the out-degree.
   */
  def inDegree(v: V): Int = edges.count(_.black == v)

  /**
   * Returns the out-degree of `v`: the number of arcs whose tail is `v`.
   * A self-loop counts once, towards both the in-degree and the out-degree.
   */
  def outDegree(v: V): Int = edges.count(_.white == v)

  /**
   * Checks whether the directed graph contains a cycle.
   *
   * This method determines if the graph is cyclic by attempting to perform a
   * topological sort. A graph is cyclic if and only if a topological order
   * cannot be established.
   *
   * @return true if the graph is cyclic, false otherwise.
   */
  def isCyclic: Boolean = topologicalOrder.isEmpty

  /**
   * A topological order of this graph, or `None` if it is cyclic. Computed once per instance.
   *
   * Cached because it was being computed twice on the ordinary path (#118): `isCyclic` is
   * `topologicalOrder.isEmpty`, and the façade's own scaladoc tells a Java caller to check
   * `isDAG` before going on. A `lazy val` is safe here where a mutable cache would not be: a
   * derived graph is a different object with its own uninitialised field, so there is nothing to
   * invalidate, and it is not a constructor parameter so `equals`, `hashCode`, `copy` and
   * `unapply` do not see it.
   *
   * NOTE on which order you get, and on the one thing this member cannot do.
   *
   * A DAG generally admits several topological orders, and `TopologicalSort.sort` picks between
   * them by adjacency order — which since 1.14.0 it takes a `Random` to decide (#131). A `val`
   * cannot have a `using` clause, so '''this member is unseedable by construction''': it calls
   * `sort` with no generator in scope, the default builds one, and the result is memoized.
   *
   * What that buys, and what it costs, are two sides of the same coin. Within one graph instance
   * the order is fixed after the first read, so `isCyclic`, the Java façade's `topologicalSort`
   * and `AcyclicShortestPaths` agree with each other and with every later call — that was #118.
   * Across runs of the program it is arbitrary, and no seed a caller pins can change it.
   *
   * '''A caller who needs a reproducible order should not read this.''' Call
   * `TopologicalSort.sort(graph)` with a `given Random` in scope instead: uncached, one sort per
   * call, and settled entirely by the seed.
   *
   * That route is open to a Scala caller doing their own sort, and closed to everyone reached
   * through this member — which is `isCyclic`, the façade's `topologicalSort`, and every result
   * of `AcyclicShortestPaths`. Giving those a pinnable order means offering a seeded entry point
   * that does not destroy the cache, and the trade was weighed in 1.14.0 and declined: sorting
   * afresh to honour a seed would have cost every caller the saving #118 bought, to serve the
   * few who want reproducibility. See #158 for what a better answer would look like.
   */
  lazy val topologicalOrder: Option[Seq[V]] = TopologicalSort.sort(this)

  // There is no isConnected or isBipartite here, and that is the point: both used to be declared
  // on the Graph trait and implemented here as `throw UnsupportedOperationException`. Connectivity
  // of a digraph has two meanings, weak and strong, and this library commits to neither — so
  // rather than failing at run time, `digraph.isConnected` no longer compiles. Use
  // ConnectedComponents for weak connectivity, or Kosaraju for strong.

  /**
   * Computes shortest paths from `start` using Bellman-Ford-Moore.
   * Returns None if a negative cycle is reachable from start.
   *
   * The distances do not depend on `random`; which of several equal-cost predecessors is
   * recorded does. See `BellmanFord.shortestPaths`, and #154 — until 1.14.0 this method had no
   * `Random` to pass on, so the generator was built inside the algorithm and could not be
   * reached from here.
   */
  def shortestPaths(start: V)(using mo: Monoid[E], or: Ordering[E], random: Random = Random()): Option[VertexTraversalResult[V, DirectedEdge[V, E]]] =
    BellmanFord.shortestPaths(this, start)

  /** A directed graph admits directed edges only. */
  type EdgeT = DirectedEdge[V, E]

  /**
   * Retrieves all edges in the directed graph as an iterable collection of edges.
   *
   * @return an iterable collection containing all edges of type `Edge[V, E]` in the graph.
   */
  def edges: Iterator[DirectedEdge[V, E]] =
    adjacencies map DirectedGraph.getDirectedEdgeFromAdjacency[V, E]

  /**
   * Creates a new directed graph using the provided vertex map.
   *
   * @param vertexMap the vertex map to be used for constructing the graph. It defines the vertices
   *                  and their adjacencies in the graph.
   *
   * @return a new directed graph constructed with the given vertex map.
   */
  def unit(vertexMap: VertexMap[V]): DirectedGraph[V, E] =
    DirectedGraph(vertexMap)
}

/**
 * Factory object for creating instances of `DirectedGraph`.
 * This object provides utility methods to construct a directed graph
 * from an edge list representation.
 */
object DirectedGraph {

  /**
   * Constructs a new, directed graph using the provided vertex map.
   *
   * @param vertexMap a map containing the vertices of type `V` as keys and their associated
   *                  adjacency lists or other vertex-specific data.
   *
   * @tparam V the type of the vertices in the graph.
   * @tparam E the type of the edges in the graph.
   * @return an instance of `DirectedGraph[V, E]` initialized with the given vertex map.
   */
  def apply[V, E](vertexMap: VertexMap[V]): DirectedGraph[V, E] =
    new DirectedGraph(vertexMap)

  /**
   * Creates a new instance of a `DirectedGraph` with an empty vertex map.
   *
   * This method provides a convenient way to initialize an empty directed graph
   * without explicitly providing a `VertexMap`. The vertex map is initialized
   * using the default empty `VertexMap` implementation.
   *
   * @tparam V the type of the vertices in the graph.
   * @tparam E the type of the edges in the graph.
   * @return an empty instance of `DirectedGraph[V, E]`.
   */
  def apply[V, E]: DirectedGraph[V, E] =
    apply[V, E](VertexMap[V])

  /**
   * Converts a sequence of triplets representing graph edges into a `Try` of `Graph[V]`.
   *
   * Each triplet in the input sequence consists of two vertices (source and target)
   * and an attribute associated with the edge connecting the vertices.
   * This method attempts to construct a directed graph from the provided triplets, returning
   * a `Success` with the graph instance if the operation is successful, or a `Failure`
   * if the construction process encounters an error.
   *
   * A triplet carrying no attribute is admissible only where the implicit `AbsentAttribute[E]`
   * says so — that is, for `E = Unit` and for nothing else; otherwise the result is a `Failure`
   * naming the offending triplet. See `AbsentAttribute` and #56.
   *
   * @param f       builds a vertex from its attribute.
   * @param triples a sequence of triplets `Triplet[V, E]` where:
   *                - the first element is the source vertex of type `V`.
   *                - the second element is the target vertex of type `V`.
   *                - the third element is the edge attribute of type `E`.
   *
   * @tparam V the type of the vertices in the graph.
   * @tparam E the type of the edges in the graph.
   * @return a `Try[DirectedGraph[V, E]]`, where:
   *         - `Success` contains the constructed graph if the operation is successful.
   *         - `Failure` contains a `GraphException` if the graph construction fails.
   */
  def triplesToTryGraph[V, E](f: V => Vertex[V])(triples: Seq[Triplet[V, E, EdgeType]])(using AbsentAttribute[E]): Try[DirectedGraph[V, E]] =
    AbstractAdjunctGraph.fromTriplets[V, E, DirectedGraph[V, E]](
      f = f,
      reject = t => Option.when(!t.edgeType.oneWay)(s"edge ${t.maybeAttribute} is not directed."),
      adjacency = {
        case (vv1, vv2, Some(e)) =>
          AdjacencyEdge(AttributedDirectedEdge(e, vv1.attribute, vv2.attribute))
        case (vv1, vv2, None) =>
          // Reachable only when E can represent an absent attribute, i.e. E is Unit: fromTriplets
          // refuses the triplet otherwise, before any adjacency is built. So OrderedEdge, which
          // IS a DirectedEdge[V, Unit], is the honest representation here and needs no cast —
          // where this used to build a VertexPair that was later cast to DirectedEdge[V, E] (#56).
          AdjacencyEdge(OrderedEdge(vv1.attribute, vv2.attribute))
      },
      bothWays = _ => false,
      build = DirectedGraph(_)
    )(triples)

  /**
   * A partial function that extracts a `DirectedEdge` from an `Adjacency`, specifically when the `Adjacency`
   * is an `AdjacencyEdge` containing a `DirectedEdge` and is not marked as `discovered`.
   *
   * This function will throw a `GraphException` if the input `Adjacency` does not match the expected pattern,
   * indicating an unsupported or an invalid-edge type.
   *
   * @throws GraphException if the input is not an `AdjacencyEdge` with a `DirectedEdge`.
   */
  // The VertexPair case that used to sit here is gone with the cast it needed (#56). An
  // attributeless triplet now either becomes an OrderedEdge at construction, where E is Unit and
  // OrderedEdge is already a DirectedEdge[V, Unit], or is refused outright. Either way no
  // VertexPair reaches this point, and nothing has to pretend one carries an E.
  private def getDirectedEdgeFromAdjacency[V, E](va: Adjacency[V]): DirectedEdge[V, E] = va match {
    case AdjacencyEdge(e: DirectedEdge[V, E] @unchecked, false) =>
      e
    case x =>
      throw GraphException(s"unexpected edge type: $x")
  }
}