package com.phasmidsoftware.gryphon.adjunct

import com.phasmidsoftware.gryphon.core.*
import com.phasmidsoftware.visitor.core.{Evaluable, JournaledVisitor}
import scala.annotation.unused
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
   * Returns the in-degree of `v`, which for an undirected graph is simply its degree:
   * every edge can be traversed in both directions.
   */
  def inDegree(v: V): Int = degree(v)

  /**
   * Returns the out-degree of `v`, which for an undirected graph is simply its degree.
   */
  def outDegree(v: V): Int = degree(v)

  /**
   * Returns the maximum degree of any vertex in the graph.
   */
  lazy val maxDegree: Int =
    vertexMap.keySet.toSeq.map(degree).max

  /**
   * Returns the mean degree across all vertices in the graph.
   */
  lazy val meanDegree: Double =
    vertexMap.keySet.toSeq.map(degree).sum.toDouble / order

  /**
   * Determines whether the graph contains a cycle using DFS to detect is we encounter a previously visited vertex.
   *
   * @return true if the graph is cyclic (i.e., it contains at least one cycle), false otherwise.
   */
  lazy val isCyclic: Boolean =
    given Random = Random()

    // The visited set is threaded through rather than mutated: each call returns both its answer
    // and the set as it stands, so a later sibling sees what an earlier one reached — which is
    // what the mutable version got from sharing one `Set`.
    def dfs(v: V, parent: Option[V], visited: Set[V]): (Boolean, Set[V]) =
      adjacentVertices(v).foldLeft(false -> (visited + v)) {
        case (found@(true, _), _) =>
          found // a cycle is already known; stop looking
        case ((false, seen), w) =>
          if !seen.contains(w) then dfs(w, Some(v), seen)
          else !parent.contains(w) -> seen // a back edge to anything but the parent is a cycle
      }

    vertexMap.keySet.foldLeft(false -> Set.empty[V]) {
      case (found@(true, _), _) =>
        found
      case ((false, seen), v) =>
        if seen.contains(v) then false -> seen else dfs(v, None, seen)
    }._1

  /**
   * Returns true if this undirected graph is connected — i.e. every vertex
   * is reachable from every other via some path.
   *
   * Performs a single DFS from an arbitrary start vertex and checks whether
   * all N vertices were visited.
   */
  lazy val isConnected: Boolean =
    if order == 0 then true
    else
      given Random = Random()

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
  lazy val isBipartite: Boolean =
    given Random = Random()

    // `None` means a conflict was found and the answer is already false; otherwise the colouring
    // so far is carried forward, so each component starts from what the previous ones coloured.
    // A self-loop reaches an already-coloured `v` with the same colour, so it correctly fails:
    // a self-loop is an odd cycle.
    def dfs(v: V, c: Boolean, colours: Map[V, Boolean]): Option[Map[V, Boolean]] =
      adjacentVertices(v).foldLeft(Option(colours + (v -> c))) {
        case (None, _) =>
          None
        case (Some(acc), w) =>
          acc.get(w) match
            case Some(cw) => Option.when(cw != c)(acc)
            case None => dfs(w, !c, acc)
      }

    // The starting colour is arbitrary: a two-colouring is symmetric, so seeding a component
    // with `false` instead of `true` yields the mirror colouring and leaves `isDefined`
    // unchanged. Mutating this literal therefore survives mutation testing permanently and
    // by design — there is no test that could kill it, and none should be written. See #83.
    vertexMap.keySet.foldLeft(Option(Map.empty[V, Boolean])) {
      case (None, _) =>
        None
      case (Some(acc), v) =>
        if acc.contains(v) then Some(acc) else dfs(v, true, acc)
    }.isDefined

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
 * Factory object for creating instances of `UndirectedGraph`.
 * This object provides utility methods to construct an undirected graph
 * from an edge list representation.
 */
object UndirectedGraph {

  /**
   * Converts a sequence of triplets representing graph edges into a `Try` of `UndirectedGraph[V, E]`.
   *
   * Each triplet in the input sequence consists of two vertices (source and target)
   * and an attribute associated with the edge connecting the vertices.
   * This method attempts to construct an undirected graph from the provided triplets, returning
   * a `Success` with the graph instance if the operation is successful, or a `Failure`
   * if the construction process encounters an error.
   *
   * @param f       builds a vertex from its attribute.
   * @param triples a sequence of triplets `Triplet[V, E]` where:
   *                - the first element is the source vertex of type `V`.
   *                - the second element is the target vertex of type `V`.
   *                - the third element is the edge attribute of type `E`.
   *
   * @param absent  decides whether a triplet carrying no attribute is admissible (implicit):
   *                it is for `E = Unit`, and for nothing else, in which case the result is a
   *                `Failure` naming the offending triplet. See `AbsentAttribute` and #56.
   *
   * @tparam V the type of the vertices in the graph.
   * @tparam E the type of the edges in the graph.
   * @return a `Try[UndirectedGraph[V, E]]`, where:
   *         - `Success` contains the constructed graph if the operation is successful.
   *         - `Failure` contains a `GraphException` if the graph construction fails.
   */
  def triplesToTryGraph[V, E](f: V => Vertex[V])(triples: Seq[Triplet[V, E, EdgeType]])(using absent: AbsentAttribute[E]): Try[UndirectedGraph[V, E]] =
    AbstractAdjunctGraph.fromTriplets[V, E, UndirectedGraph[V, E]](
      f = f,
      reject = t => Option.when(t.edgeType.oneWay)(s"edge ${t.maybeAttribute} is directed."),
      adjacency = {
        // An edge with no attribute is still an edge of this graph, so it must be undirected
        // like the rest. It used to be an AttributedDirectedEdge, which UndirectedGraph.edges
        // then discarded — the graph counted the adjacency in `degree` but not the edge in
        // `edges` or `M`.
        case (vv1, vv2, Some(e)) =>
          AdjacencyEdge(UndirectedEdge(e, vv1.attribute, vv2.attribute))
        case (vv1, vv2, None) =>
          // Reachable only when E can represent an absent attribute, i.e. E is Unit: fromTriplets
          // refuses the triplet otherwise. The attribute is then that type's own value for
          // absence — (), not `None` standing in a field typed E, which is what this built up to
          // 1.11.0 and what made `.attribute` throw ClassCastException when read (#56).
          AdjacencyEdge(UndirectedEdge(absent.maybeValue.get, vv1.attribute, vv2.attribute))
      },
      bothWays = !_.edgeType.oneWay,
      build = UndirectedGraph(_)
    )(triples)
}
