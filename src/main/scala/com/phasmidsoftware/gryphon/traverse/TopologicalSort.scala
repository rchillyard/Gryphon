package com.phasmidsoftware.gryphon.traverse

import com.phasmidsoftware.gryphon.adjunct.DirectedGraph
import com.phasmidsoftware.visitor.core.{*, given}
import scala.util.Random

/**
 * Represents a "topological sort" of a directed acyclic graph, mapping each vertex to its
 * position in topological order.
 * NOTE that it would also be possible to get the same effect using the Kosaraju algorithm
 * (see `Kosaraju.stronglyConnectedComponents`) especially if you're not sure that the directed
 * graph is in fact acyclic.
 * If you have significant doubt, it may be better to use Kosaraju instead.
 * A DAG will result in a topologically sorted set of SCCs, each of which holds only one vertex.
 *
 * @param map a mapping of vertices of type `V` to their position (Int) in topological order.
 * @tparam V the type representing a vertex in the graph.
 */
case class TopologicalSort[V](map: Map[V, Int]) extends AbstractVertexTraversalResult[V, Int](map):
  /**
   * Creates a new instance with the provided vertex-to-position mapping.
   *
   * @param m the updated mapping.
   * @return a new `TraversalResult[V, Int]`.
   */
  def unit(m: Map[V, Int]): TraversalResult[V, Int] = copy(map = m)

/**
 * Provides functionality for topological sorting of directed graphs using the
 * Visitor 1.8.0 typeclass engine.
 *
 * Post-order DFS is used: a vertex is recorded only after all its descendants
 * have been recorded, naturally yielding reverse topological order.
 */
object TopologicalSort:

  /**
   * Performs a topological sort on the given directed graph.
   *
   * Uses post-order DFS (`DfsOrder.Post`) via `Traversal.dfs` so that each vertex
   * appears in the journal only after all vertices reachable from it. The resulting
   * list is in reverse post-order (i.e. topological order).
   *
   * Returns `None` if the graph is cyclic.
   *
   * ==Which of the many valid orders you get==
   *
   * A DAG generally admits several topological orders, and which one comes back is decided by
   * the order `adjacentVertices` yields each vertex's successors in — so it is decided by
   * `random`. Pin a seed and the order is reproducible; supply none and the default builds a
   * fresh generator, so it varies from call to call.
   *
   * Until 1.14.0 this method took no `Random` at all. The `given GraphNeighbours` below closed
   * over `graph.adjacentVertices(v)` with nothing in scope, so the default applied *inside the
   * closure* — a fresh generator per vertex, not even one per sort — and a caller's
   * `given Random = Random(42)` did no work whatever. That is #131, and it is the reason
   * `AcyclicShortestPaths` could not be pinned either (#154).
   *
   * @param graph the directed graph to sort.
   * @param random the generator deciding the adjacency order, and hence which valid topological
   *               order is returned.
   * @tparam V the vertex attribute type.
   * @tparam E the edge attribute type.
   * @return `Some(Seq[V])` in topological order, or `None` if the graph is cyclic.
   * @note Result: varies with adjacency order; pin a seed with `given Random` to fix it.
   *       Every result is a valid topological order; which one you get is what varies.
   */
  def sort[V, E](graph: DirectedGraph[V, E])(using random: Random = Random()): Option[Seq[V]] =
    given Evaluable[V, V] with
      def evaluate(v: V): Option[V] = Some(v)

    // `(using random)` is explicit rather than left to resolution: the whole of #131 was that this
    // line silently took a default, and an explicit argument cannot regress to one unnoticed.
    given GraphNeighbours[V] = (v: V) => graph.adjacentVertices(v)(using random)

    // Run post-order DFS over all vertices, seeding each unvisited component in turn.
    //
    // Every step here is proportional to what the DFS just reached, not to the size of the graph.
    // Until 1.9.0 each iteration did four separate O(V) things — `allVertices.diff(visited)`,
    // rebuilding the VisitedSet by folding markVisited over the whole of `visited`, mapping over
    // the *entire accumulated* journal, and `visited ++` that — so the loop was O(components × V).
    // On a 1491-vertex DAG that was ~123ms, and it dominated AcyclicShortestPaths completely,
    // making the DAG algorithm some 110 times slower than Bellman-Ford on the same graph. Fixing
    // one of the four changes nothing measurable; they have to go together.
    type J = ListJournal[(V, Option[V])]
    import scala.annotation.tailrec
    val allVertices = graph.vertexMap.keySet.toList

    @tailrec
    def loop(vis: Visitor[V, Mapped[V, V], J], visited: Set[V], vs: VisitedSet[V], candidates: List[V]): Visitor[V, Mapped[V, V], J] =
      candidates match
        case Nil =>
          vis
        case next :: rest if visited.contains(next) =>
          loop(vis, visited, vs, rest) // already reached by an earlier component
        case next :: rest =>
          val result = Traversal.dfs(next, vis, DfsOrder.Post)(using summon[GraphNeighbours[V]], vs, summon[Tracer[V]])
          // ListJournal prepends, so this DFS's vertices are exactly the prefix of the journal that
          // is not yet in `visited`. Taking them costs the size of the component, not of the graph.
          // `.iterator` matters: `result.result.map(...)` is strict, so mapping before taking
          // walks the whole accumulated journal and puts the O(V) term straight back.
          val reached = result.result.iterator.map(_._1).takeWhile(v => !visited.contains(v)).toList
          val nowVisited = visited ++ reached + next
          loop(result, nowVisited, reached.foldLeft(vs)(_.markVisited(_)).markVisited(next), rest)

    val finalVisitor = loop(JournaledVisitor.withListJournal[V, V], Set.empty, summon[VisitedSet[V]], allVertices)
    // ListJournal prepends, so head = last recorded = root in post-order = topological order
    val orderedVertices: List[V] = finalVisitor.result.map(_._1).toList
    Option.when(acyclic(graph, orderedVertices))(orderedVertices)

  /**
   * Computes a `TopologicalSort` mapping from the given directed graph, or returns
   * `None` if the graph is cyclic.
   *
   * @param graph the directed graph to sort.
   * @param random the generator deciding the adjacency order; see `sort`.
   * @tparam V the vertex attribute type.
   * @tparam E the edge attribute type.
   * @return `Some(TopologicalSort[V])` with each vertex mapped to its position,
   *         or `None` if the graph is cyclic.
   * @note Result: varies with adjacency order; pin a seed with `given Random` to fix it.
   */
  def traversal[V, E](graph: DirectedGraph[V, E])(using random: Random = Random()): Option[TopologicalSort[V]] =
    sort(graph)(using random).map { topologicalOrder =>
      val vertexOrder: Seq[(V, Int)] = topologicalOrder.zipWithIndex.map((v, i) => v -> i)
      TopologicalSort(vertexOrder.toMap)
    }

  /**
   * Determines whether a directed graph is acyclic given a proposed topological order.
   *
   * For every directed edge (white → black), white must appear *strictly before* black
   * in the topological order. If this holds for all edges the graph is acyclic.
   *
   * The comparison has to be strict. `topologicalSort` is a DFS journal filtered by a
   * visited set, so no vertex appears twice and distinct vertices always have distinct
   * positions: for an edge between two different vertices `<` and `<=` agree. They differ
   * on exactly one input, a self-loop `v -> v`, where `<=` is trivially satisfied and the
   * graph is wrongly reported acyclic. A self-loop is a cycle, so `sort` must return `None`.
   *
   * Gryphon does not intend to admit self-loops at all, and the right place to forbid one
   * is at construction — see issue #86, which proposes a constraint-based graph hierarchy
   * with exactly that constraint, tested through `addEdge`. Until that lands this check is
   * the only thing standing between a self-loop and a "topological order" for a graph that
   * has none, so it is kept strict as defence in depth rather than left to the caller.
   *
   * @param graph           the directed graph to check.
   * @param topologicalSort the proposed vertex ordering.
   * @return `true` if acyclic, `false` if cyclic.
   */
  private def acyclic[V, E](graph: DirectedGraph[V, E], topologicalSort: List[V]): Boolean =
    // Positions are indexed once, not searched per edge. `List.indexOf` is O(V), and this called
    // it twice for every edge, which made the check O(V·E) — on a 1491-vertex, 6000-edge DAG that
    // was some 18 million list steps, and it dwarfed the traversal it was checking. The companion
    // `traversal` method already built exactly this map; only `acyclic` did it the slow way.
    val position: Map[V, Int] = topologicalSort.iterator.zipWithIndex.toMap
    graph.edges.forall(edge => position(edge.white) < position(edge.black))