package com.phasmidsoftware.gryphon.core

import com.phasmidsoftware.gryphon.core.Vertex.createWithSet
import com.phasmidsoftware.gryphon.util.RandomIterator
import com.phasmidsoftware.visitor.core.{*, given}
import org.slf4j.{Logger, LoggerFactory}
import scala.annotation.tailrec
import scala.util.Random

/**
 * Represents a mapping of vertex attributes to their corresponding vertices within a graph.
 * Provides functionality to manage and traverse vertices via DFS and BFS using the
 * Visitor 1.8.0 typeclass engine.
 *
 * The mutable `discovered` flag pattern has been removed; visited-node tracking is now
 * handled entirely by the immutable `VisitedSet[V]` inside the traversal engine.
 *
 * @tparam V the type representing the vertex attributes (invariant).
 * @param map a mapping from vertex attributes to their associated Vertex instances.
 */
case class VertexMap[V](map: Map[V, Vertex[V]]) extends Traversable[V]:

  // -----------------------------------------------------------------------
  // Traversable implementation
  // -----------------------------------------------------------------------

  /**
   * Returns the number of vertices in the graph.
   *
   * @return the number of vertices in the graph.
   */
  def order: Int = map.size

  // There is no `M` here any more, and no `edgeCount` behind it. The counter was a `var` on an
  // otherwise immutable structure: `+` incremented it on the *receiver* — a value the caller still
  // held — and returned a fresh map whose own count started at 0. So `(VertexMap[Int] + e1 + e2).M`
  // was 0, while the map it was derived from silently went up. Counting edges belongs to the graph,
  // which does it as `edges.size`.

  /**
   * Returns an iterator over the vertices adjacent to the given vertex.
   *
   * @param v      the vertex for which adjacent vertices are to be retrieved.
   * @param random the generator determining adjacency order (implicit).
   * @return an iterator over the adjacent vertex attributes.
   * @throws GraphException if the specified vertex is not found in the graph.
   */
  def adjacentVertices(v: V)(using random: Random): Iterator[V] =
    val vo = get(v)
    if vo.isEmpty then throw GraphException(s"vertex $v not found")
    for vv <- vo.iterator; va <- randomAdjacencies(vv) yield va.vertex

  /**
   * Filters the adjacencies of a given vertex based on a specified predicate.
   *
   * @param predicate a function that evaluates each `Adjacency[V]`
   *                  and returns true if the adjacency satisfies the specified condition.
   * @param v         the vertex whose adjacencies are to be filtered.
   * @param random    the generator determining adjacency order (implicit).
   * @return an iterator over the adjacencies of the given vertex that satisfy the predicate.
   */
  def filteredAdjacencies(predicate: Adjacency[V] => Boolean)(v: V)(using random: Random): Iterator[Adjacency[V]] =
    map(v).adjacencies.filter(predicate).randomIterator

  /**
   * Provides a `GraphNeighbours[V]` instance derived from the `graphNeighbours` method.
   * The `GraphNeighbours[V]` instance facilitates efficient access to the neighbors of a vertex
   * within the graph, enabling traversal algorithms to query adjacent vertices.
   *
   * Up to 1.10.0 this took its randomness from the ambient `Bag.random`, picked up through an
   * import, so a caller's seed could not reach it. It now takes the generator explicitly, which
   * is what `Traversable.graphNeighbours` has always required (#67).
   *
   * @param random the generator determining adjacency order.
   * @return a `GraphNeighbours[V]` instance for querying vertex neighbors.
   */
  def neighboursGiven(using random: Random): GraphNeighbours[V] = graphNeighbours

  /**
   * Performs DFS from `v`, accumulating results into `visitor`.
   *
   * A local `GraphNeighbours[V]` derived from `adjacentVertices` is provided to the engine.
   *
   * @param visitor the visitor to accumulate results.
   * @param order   whether a vertex is recorded before or after its descendants; pre-order by default.
   * @param v       the starting vertex.
   * @param random  the generator determining adjacency order (implicit).
   * @tparam E the journal's element type.
   * @tparam J the journal type.
   * @return the updated visitor after traversal.
   */
  def dfs[E, J <: Appendable[E, J]](visitor: Visitor[V, E, J], order: DfsOrder = DfsOrder.Pre)(v: V)(using random: Random): Visitor[V, E, J] =
    new GraphNeighboursTraversal[V, Visitor[V, E, J]](this) {
      def traversal: Visitor[V, E, J] = Traversal.dfs(v, visitor, order)
    }.traversal

  /**
   * Performs DFS for all vertices in the graph, including those not reachable from any
   * single start vertex.
   *
   * @param visitor the visitor to accumulate results.
   * @param random  the generator determining adjacency order (implicit).
   * @tparam E the journal's element type.
   * @tparam J the journal type.
   * @return the updated visitor after traversing all vertices.
   */
  def dfsAll[E, J <: Appendable[E, J]](visitor: Visitor[V, E, J])(using random: Random): Visitor[V, E, J] =
    new GraphNeighboursTraversal[V, Visitor[V, E, J]](this) {
      def traversal: Visitor[V, E, J] =
        @tailrec
        def loop(vis: Visitor[V, E, J], visited: Set[V]): Visitor[V, E, J] =
          val unvisited = map.keySet.diff(visited)
          if unvisited.isEmpty then vis
          else
            val next = unvisited.head
            // Pre-mark all already-visited vertices so the engine skips them
            val vs: VisitedSet[V] = visited.foldLeft(summon[VisitedSet[V]])(_.markVisited(_))
            val result = Traversal.dfs(next, vis)(using nbrs, vs, summon[Tracer[V]])
            // `visited` must grow by every vertex this DFS reached, not just by its root.
            // Adding only `next` left the rest of the component unvisited as far as the loop
            // was concerned, so each of them started another DFS over ground already covered,
            // and a came-from journal ended up with mutually-referring entries (0 from 1 and
            // 1 from 0) rather than a forest.
            loop(result, visited ++ reachableFrom(next, visited))

        loop(visitor, Set.empty)
    }.traversal

  /**
   * The set of vertices a traversal starting at `start` can reach without
   * entering `excluded` — that is, exactly the vertices `dfsAll`'s DFS from
   * `start` covers, given that `excluded` is pre-marked as visited.
   *
   * @param start    the vertex to start from.
   * @param excluded the vertices already visited, which the DFS will skip.
   * @return the reached vertices, including `start` unless it is excluded.
   */
  private def reachableFrom(start: V, excluded: Set[V])(using Random): Set[V] =
    @tailrec
    def loop(stack: List[V], acc: Set[V]): Set[V] = stack match
      case Nil => acc
      case v :: rest =>
        if acc.contains(v) || excluded.contains(v) then loop(rest, acc)
        else loop(adjacentVertices(v).toList ::: rest, acc + v)

    loop(List(start), Set.empty)


  /**
   * Performs BFS from `v`, accumulating results into `visitor`.
   *
   * @param visitor the visitor to accumulate results.
   * @param v       the starting vertex.
   * @param goal    early-termination predicate (default: never stop early).
   * @param random  the generator determining adjacency order (implicit).
   * @tparam E the journal's element type.
   * @tparam J the journal type.
   * @return the updated visitor after traversal.
   */
  def bfs[E, J <: Appendable[E, J]](visitor: Visitor[V, E, J])(v: V, goal: V => Boolean = _ => false)(using random: Random): Visitor[V, E, J] =
    new GraphNeighboursTraversal[V, Visitor[V, E, J]](this) {
      def traversal: Visitor[V, E, J] = Traversal.bfs(v, visitor, goal)
    }.traversal


  // -----------------------------------------------------------------------
  // Graph mutation
  // -----------------------------------------------------------------------

  /**
   * Modifies a vertex in this `VertexMap` using the provided function.
   *
   * @param f the function to apply to the vertex.
   * @param v the identifier of the vertex to modify.
   * @return a new VertexMap with the vertex modified if it exists.
   */
  def modifyVertex(f: Vertex[V] => Vertex[V])(v: V): VertexMap[V] =
    get(v).map(f) match
      case Some(vv) =>
        this + vv
      case None =>
        this

  /**
   * Retrieves all vertices in the VertexMap.
   *
   * @return an Iterable of all Vertex instances.
   */
  def vertices: Iterable[Vertex[V]] = map.values

  /**
   * Checks if the specified key is present in the VertexMap.
   */
  def contains(key: V): Boolean = map.contains(key)

  /**
   * Adds a vertex to the VertexMap.
   */
  def +(vertex: Vertex[V]): VertexMap[V] =
    VertexMap(map = map + (vertex.attribute -> vertex))

  /**
   * Adds a directed or undirected edge to the vertex map.
   */
  def +[E](edge: Edge[V, E]): VertexMap[V] =
    val addAdj = addAdjFunction
    val adjFrom = AdjacencyEdge[V, E](edge)
    val adjTo = AdjacencyEdge[V, E](edge, flipped = true)
    // Ensure both vertices exist, then always add adjacencies using modifyVertex
    // so that existing vertices correctly accumulate adjacencies from multiple edges.
    val m0 = ensure(createWithSet[V])(edge.black).ensure(createWithSet[V])(edge.white)
    val m1 = m0.modifyVertex(addAdj(adjFrom))(edge.white)
    if edge.edgeType.oneWay then m1
    else m1.modifyVertex(addAdj(adjTo))(edge.black)

  /**
   * Adds a vertex pair (with EdgeType) to the VertexMap.
   */
  def +(pair: (V, V, EdgeType)): VertexMap[V] =
    val create: V => Vertex[V] = Vertex.createWithBag[V]
    val v1 = pair._1
    val v2 = pair._2
    val vm = ensure(create)(v1).ensure(create)(v2).modifyVertex(addAdjFunction(AdjacencyVertex(v2)))(v1)
    if pair._3.oneWay then vm else vm.modifyVertex(addAdjFunction(AdjacencyVertex(v1)))(v2)

  /**
   * Retrieves the vertex associated with the specified key.
   */
  def get(key: V): Option[Vertex[V]] = map.get(key)

  /**
   * Returns the vertex for the key, or the default if not present.
   */
  def getOrElse(key: V, default: => Vertex[V]): Vertex[V] = map.getOrElse(key, default)

  /**
   * Returns the vertex for the key, throwing NoSuchElementException if absent.
   */
  @throws[NoSuchElementException]
  def apply(key: V): Vertex[V] = map.apply(key)

  /**
   * Returns the vertex for key `x`, or applies `defaultFunc` if absent.
   */
  def applyOrElse(x: V, defaultFunc: V => Vertex[V]): Vertex[V] =
    map.applyOrElse(x, defaultFunc)

  /**
   * Retrieves the set of keys present in the VertexMap.
   */
  def keySet: Set[V] = map.keySet

  /**
   * Provides an iterator over the keys in the underlying map.
   */
  def iterator: Iterator[V] = map.keysIterator

  /**
   * Adds the given edges to this VertexMap.
   *
   * This took an `EdgeList[V, E, Z]` up to 1.10.0, and did nothing with it but read `.edges`,
   * so a caller holding a sequence had to box it purely to have it unboxed again. Taking the
   * sequence directly also retires the `Z` parameter, which existed only to type `EdgeList`'s
   * `triplets` and never appeared in this method's body (#69).
   */
  def addEdges[E](edges: Seq[Edge[V, E]]): VertexMap[V] =
    edges.foldLeft[VertexMap[V]](this)((vm, e) => vm + e)

  /**
   * Adds a sequence of vertex pairs with their EdgeTypes to this VertexMap.
   */
  def addVertexPairs[E](pairs: Seq[(V, V, EdgeType)]): VertexMap[V] =
    pairs.foldLeft[VertexMap[V]](this)((vm, pair) => vm + pair)

  /**
   * Processes a sequence of triplets and adds them to the VertexMap.
   */
  def addTriplets[E, Z](vertexFunction: V => Vertex[V], edgeFunction: Z => ProtoConnexion[V, E] => Connexion[V])(triplets: Seq[Triplet[V, E, Z]]): VertexMap[V] =
    triplets.foldLeft[VertexMap[V]](this)((vm, triplet) => vm.addTriplet(edgeFunction)(vertexFunction)(triplet))

  /**
   * Returns a new VertexMap with the same vertex keys but empty adjacency lists.
   * Used to seed a reversed or transformed graph before re-adding edges.
   */
  def keysOnly: VertexMap[V] =
    VertexMap(map.keys.foldLeft(Map.empty[V, Vertex[V]]) { (m, k) =>
      m + (k -> Vertex.createWithSet(k))
    })

  /**
   * Creates vertices and adjacencies from a triplet.
   */
  def createVerticesFromTriplet[E, Z](f: V => Vertex[V])(g: (Vertex[V], Vertex[V], Option[E]) => Adjacency[V])(condition: Boolean)(triplet: Triplet[V, E, Z]): VertexMap[V] =
    val vv1: Vertex[V] = getOrCreate(f)(triplet.from)
    val vv2: Vertex[V] = getOrCreate(f)(triplet.to)
    val va: Adjacency[V] = g(vv1, vv2, triplet.maybeAttribute)
    val updatedVv1 = vv1 + va
    if vv1.attribute == vv2.attribute then
      this + updatedVv1  // self-loop: one vertex, one adjacency
    else
      val vao: Option[Adjacency[V]] = Option.when(condition) {
        va match
          case AdjacencyEdge(connexion, _) => AdjacencyEdge(connexion, flipped = true)
          case other => other
      }
      this + updatedVv1 + vao.fold(vv2)(vv2 + _)

  /**
   * Creates an Adjacency from an optional edge.
   */
  def createAdjacency[E, Z](edgeFunction: ProtoConnexion[V, E] => Connexion[V])(vv1: Vertex[V], vv2: Vertex[V], maybeE: Option[E]): Adjacency[V] =
    maybeE match
      case Some(e) => AdjacencyEdge[V, E](edgeFunction(e, vv1.attribute, vv2.attribute))
      case None => AdjacencyVertex[V](vv2.attribute)

  /**
   * Adds a single triplet to the VertexMap.
   */
  def addTriplet[Z, E](edgeFunction: Z => ProtoConnexion[V, E] => Connexion[V])(vertexFunction: V => Vertex[V])(triplet: Triplet[V, E, Z]): VertexMap[V] =
    val f: (Vertex[V], Vertex[V], Option[E]) => Adjacency[V] = createAdjacency(edgeFunction(triplet.edgeType))
    createVerticesFromTriplet[E, Z](vertexFunction)(f)(triplet.edgeType != Directed)(triplet)

  // -----------------------------------------------------------------------
  // toString
  // -----------------------------------------------------------------------

  override def toString: String =
    map.map((v, vv) => s"v:$v, vv:${vv.render}").mkString("[", ", ", "]")

  def debug: String =
    s"VertexMap(order=${map.size}, vertices=$map)"


  // -----------------------------------------------------------------------
  // Private helpers
  // -----------------------------------------------------------------------

  /**
   * Ensures a vertex for `v` is present, creating it with `f` if absent.
   */
  private def ensure(f: V => Vertex[V])(v: V): VertexMap[V] = get(v) match
    case Some(_) =>
      this
    case None =>
      this + f(v)

  private val addAdjFunction: Adjacency[V] => Vertex[V] => Vertex[V] = va => w => w + va

  private def getOrCreate(f: V => Vertex[V])(v: V): Vertex[V] =
    map.getOrElse(v, f(v))

  private def randomAdjacencies(vv: Vertex[V])(using random: Random) =
    RandomIterator(vv.adjacencies.iterator)

/**
 * Companion object for VertexMap.
 */
object VertexMap:

  def apply[V](map: Map[V, Vertex[V]]): VertexMap[V] = new VertexMap(map)

  def apply[V]: VertexMap[V] = apply(Map.empty[V, Vertex[V]])

  def createFromTriplets[V, E, Z](f: Triplet[V, E, Z] => (Vertex[V], Vertex[V]))(triplets: Triplets[V, E, Z]): VertexMap[V] =
    VertexMap(addTripletsToMap(f)(Map.empty[V, Vertex[V]])(triplets))

  // createFromVertexPairList is gone with VertexPairList itself (#70): it unwrapped the
  // wrapper and called addVertexPairs, so `VertexMap[V].addVertexPairs(pairs)` says the same
  // thing without the box.

  private def addTripletsToMap[V, E, Z](f: Triplet[V, E, Z] => (Vertex[V], Vertex[V]))(map: Map[V, Vertex[V]])(triplets: Triplets[V, E, Z]): Map[V, Vertex[V]] =
    triplets.triplets.foldLeft[Map[V, Vertex[V]]](map) { (vm, t) =>
      val (vv1, vv2) = f(t)
      vm + (t.from -> vv1) + (t.to -> vv2)
    }

  /**
   * The logger for this object.
   *
   * Nothing currently logs through it: it is kept because it is public, so removing it would break
   * binary compatibility, and 1.11.1 is a patch release. Queued for the next breaking window — see
   * #102, which is also where the question of whether it should be used instead is recorded.
   *
   * The name was the string literal `"VertexMap"`, which would not have followed a rename. Changing
   * it to the class is safe even in a patch release precisely because nothing logs through it, so
   * no logging configuration can be keyed to the old name.
   */
  val logger: Logger = LoggerFactory.getLogger(getClass)