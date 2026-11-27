package com.phasmidsoftware.gryphon.traverse

import cats.kernel.Monoid
import com.phasmidsoftware.gryphon.adjunct.UndirectedGraph
import scala.util.Random

/**
 * Minimum spanning trees: the problem, with the three algorithms that solve it.
 *
 * ==Which one to use==
 *
 * All three return a spanning tree of minimum total weight, and the total is the same for each.
 * They differ in what they read, which decides both their performance and their reproducibility:
 *
 *  - `prim` walks adjacencies outwards from a start vertex, so it needs one, and it '''varies'''
 *    with adjacency order — pin a seed to settle which of several equal-weight trees you get.
 *  - `kruskal` sorts the edge list and adds edges that join two components. Needs no start
 *    vertex, and is '''invariant''' — measured on a cycle whose weights are all tied.
 *  - `boruvka` works in rounds, each vertex choosing its cheapest outgoing edge. Also needs no
 *    start vertex, also '''invariant''', and the one that parallelises naturally (#75).
 *
 * On a disconnected graph `kruskal` and `boruvka` return the spanning '''forest''', while `prim`
 * returns only the tree containing `start`.
 *
 * ==Why they live here==
 *
 * Until 1.15.0 these were three unrelated top-level objects — `MST.prim`, but `Kruskal.mst` and
 * `Boruvka.mst`, the same problem named in opposite directions. A caller who found one learned
 * nothing about the others, and the choice between them, which is the real decision, was nowhere.
 * See #171. The implementations still live in their own files; only the entry point moved.
 */
object MST:

  /**
   * Runs Prim's algorithm from `start`, returning the MST as a traversal result.
   *
   * Each entry `v → edge` in the result records the cheapest edge connecting
   * `v` to the growing MST. The start vertex is absent (it has no predecessor).
   *
   * @param graph  the weighted undirected graph.
   * @param start  the source vertex.
   * @param random controls adjacency ordering (implicit).
   * @tparam V the vertex type.
   * @tparam E the edge-weight type; must be Monoid and Ordering.
   * @return a `TraversalResult[V, Edge[V, E]]` mapping each MST vertex to its
   *         cheapest incoming edge.
   * @note Result: varies with adjacency order; pin a seed with `given Random` to fix it.
   *       The total weight does not vary -- a minimum spanning tree is minimum -- but which of
   *       several equal-weight trees comes back does.
   */
  def prim[V, E: {Monoid, Ordering}](graph: UndirectedGraph[V, E], start: V)(using random: Random = Random()): MSTParentMap[V, E] =
    // Read back through the trait rather than casting the traversal's concrete result: PrimTraversal
    // is shared with Dijkstra and returns the general vertex-keyed type, which is right for it.
    // Turning that into an MST-specific result is this entry point's job, not the traversal's.
    val result = PrimTraversal[V, E]().run(graph)(start)
    MSTParentMap(result.keySet.flatMap(v => result.vertexTraverse(v).map(v -> _)).toMap)

  /**
   * Runs Kruskal's algorithm, returning the MST as an edge list in non-decreasing weight order.
   *
   * Sorts the edges and adds each one whose endpoints are in different components, so it needs
   * no start vertex, and on a disconnected graph it returns the spanning forest.
   *
   * @param graph the weighted undirected graph.
   * @tparam V the vertex type.
   * @tparam E the edge-weight type; must have an `Ordering`.
   * @return the MST edges, in the order Kruskal added them.
   * @note Result: invariant. Measured on a cycle whose weights are ALL tied, where any spanning
   *       subset is an MST: forty seeds, one answer. Kruskal sorts `graph.edges`, which is not
   *       shuffled, and never consults an adjacency iterator -- unlike `prim`, which varies.
   */
  def kruskal[V, E: Ordering](graph: UndirectedGraph[V, E]): MSTEdgeList[V, E] =
    Kruskal.mst(graph)

  /**
   * Runs Borůvka's algorithm, returning the MST as an edge list in the order it added them.
   *
   * Proceeds in rounds, each component choosing its cheapest outgoing edge, so it needs no start
   * vertex, and on a disconnected graph it returns the spanning forest.
   *
   * @param graph the weighted undirected graph.
   * @tparam V the vertex type.
   * @tparam E the edge-weight type; must have `Monoid` and `Ordering`.
   * @return the MST edges, in round order.
   * @note Result: invariant. Measured as for `kruskal`, on fully tied weights.
   */
  def boruvka[V, E: {Monoid, Ordering}](graph: UndirectedGraph[V, E]): MSTEdgeList[V, E] =
    Boruvka.mst(graph)

/**
 * A minimum-spanning-tree algorithm, as a value.
 *
 * ==Why this exists, when `MST.kruskal` is already a function==
 *
 * It is not about passing a function around — you could always do that. It is that the three
 * algorithms had no COMMON type, for two independent reasons, and so could not be held together:
 *
 *  - They returned different things, with no useful supertype. Fixed by `MSTResult` (#177).
 *  - '''`prim` takes a start vertex and the other two do not''', so no single function type fits
 *    all three. That is this trait (#179).
 *
 * ==How the second one is resolved: the start is configuration==
 *
 * Prim grows outwards from somewhere, so it needs a start vertex; Kruskal sorts the edge list and
 * Borůvka works in rounds, so neither does. That is a real difference between the algorithms, and
 * the alternatives all handle it by lying somewhere — an `Option[V]` that means nothing for two of
 * them, or a start parameter two of them ignore.
 *
 * Currying puts the difference where it actually is. `PrimAlgorithm` is an MST algorithm
 * '''rooted at a vertex''', and says so in its constructor; the other two are not rooted at all,
 * and nothing in their signature pretends otherwise. What remains — "apply me to a graph, get a
 * spanning tree" — is genuinely common, so it is the one abstract method.
 *
 * {{{
 * val algorithms = List(KruskalAlgorithm[Int, Double](), BoruvkaAlgorithm[Int, Double](),
 *                       PrimAlgorithm[Int, Double](start = 0))
 * algorithms.map(a => a(graph).totalWeight)   // all three agree
 * }}}
 *
 * @tparam V the vertex type.
 * @tparam E the edge-weight type.
 */
abstract class MSTAlgorithm[V, E]:

  /**
   * Run this algorithm over `graph`.
   *
   * @param graph  the weighted undirected graph.
   * @param random settles which of several equal-weight trees comes back, for an algorithm that
   *               consults adjacency order. '''Prim does; Kruskal and Borůvka do not''' and
   *               ignore it -- measured under #156 on a cycle whose weights are all tied, where
   *               forty seeds give one answer. The parameter is on the shared method because a
   *               caller holding an `MSTAlgorithm` cannot know which they have, and omitting it
   *               would put Prim's seed out of reach -- which is #131's defect, in a new place.
   * @return the spanning tree, in whichever shape this algorithm naturally produces — a parent
   *         map for Prim, an ordered edge list for the other two. Read `edges` or `totalWeight`
   *         to treat them uniformly; read the concrete type for what is particular to each.
   */
  def apply(graph: UndirectedGraph[V, E])(using random: Random = Random()): MSTResult[V, E]

/**
 * Kruskal's algorithm as a value: sort the edges, add each one joining two components.
 *
 * Needs no start vertex, and on a disconnected graph gives the spanning forest.
 */
case class KruskalAlgorithm[V, E: Ordering]() extends MSTAlgorithm[V, E]:
  // `random` is ignored: Kruskal sorts the edge list and never consults an adjacency iterator.
  def apply(graph: UndirectedGraph[V, E])(using random: Random = Random()): MSTResult[V, E] =
    MST.kruskal(graph)

/**
 * Borůvka's algorithm as a value: each component takes its cheapest outgoing edge, in rounds.
 *
 * Needs no start vertex, gives the spanning forest on a disconnected graph, and is the one that
 * parallelises naturally (#75).
 */
case class BoruvkaAlgorithm[V, E: {Monoid, Ordering}]() extends MSTAlgorithm[V, E]:
  // Ignored, as for Kruskal: Borůvka reads `graph.edges`, which is not shuffled.
  def apply(graph: UndirectedGraph[V, E])(using random: Random = Random()): MSTResult[V, E] =
    MST.boruvka(graph)

/**
 * Prim's algorithm as a value, rooted at `start`.
 *
 * The start vertex is a constructor parameter rather than an argument to `apply`, which is what
 * lets this sit in a list beside the other two. It is also the more honest placement: the vertex
 * configures the algorithm rather than describing the problem, and on a disconnected graph it
 * decides which component you get back — the other two return the whole forest.
 *
 * @param start the vertex to grow the tree from.
 */
case class PrimAlgorithm[V, E: {Monoid, Ordering}](start: V) extends MSTAlgorithm[V, E]:
  // Threaded explicitly. The first draft wrote `MST.prim(graph, start)` with no generator in
  // scope, so the default built a fresh one per call and a caller's seed stopped at this line --
  // #131's defect reproduced inside the abstraction meant to tidy these up. `-Werror` caught it,
  // by reporting the test's `given Random` as unused.
  //
  // All three are named because `prim`'s context bounds merge into its trailing `using` clause,
  // so a lone `(using random)` does not type-check. Worth the verbosity: an explicit argument
  // cannot silently fall back to the default, which is exactly how the defect arose.
  def apply(graph: UndirectedGraph[V, E])(using random: Random = Random()): MSTResult[V, E] =
    MST.prim(graph, start)(using summon[Monoid[E]], summon[Ordering[E]], random)
