package com.phasmidsoftware.gryphon.core

import com.phasmidsoftware.gryphon.core.Vertex.createWithSet
import com.phasmidsoftware.gryphon.util.{GraphException, RandomIterator}
import com.phasmidsoftware.visitor.core.{*, given}
import org.slf4j.{Logger, LoggerFactory}
import scala.annotation.tailrec
import scala.util.Random

/**
  * Represents a mapping of vertex attributes to their corresponding vertices within a graph.
  * Provides functionality to manage and traverse vertices via DFS and BFS using the
  * Visitor V1.2.0 typeclass engine.
  *
  * The mutable `discovered` flag pattern has been removed; visited-node tracking is now
  * handled entirely by the immutable `VisitedSet[V]` inside the traversal engine.
  *
  * @tparam V the type representing the vertex attributes (invariant).
  * @param map a mapping from vertex attributes to their associated Vertex instances.
  */
case class VertexMap[V](map: Map[V, Vertex[V]])(private val random: Random = Random()) extends Traversable[V]:

  // -----------------------------------------------------------------------
  // Traversable implementation
  // -----------------------------------------------------------------------

  /**
    * Returns an iterator over the vertices adjacent to the given vertex.
    *
    * @param v the vertex for which adjacent vertices are to be retrieved.
    * @return an iterator over the adjacent vertex attributes.
    * @throws GraphException if the specified vertex is not found in the graph.
    */
  def adjacentVertices(v: V): Iterator[V] =
    val vo = get(v)
    if vo.isEmpty then throw GraphException(s"vertex $v not found")
    for vv <- vo.iterator; va <- randomAdjacencies(vv) yield va.vertex

  /**
    * Filters the adjacencies of a given vertex based on a specified predicate.
    *
    * @param predicate determines which adjacencies to include.
    * @param v         the vertex whose adjacencies are to be filtered.
    * @return an iterator of matching adjacencies.
    */
  def filteredAdjacencies(predicate: Adjacency[V] => Boolean)(v: V): Iterator[Adjacency[V]] =
    map(v).adjacencies.filter(predicate).iterator

  /**
    * Performs DFS from `v`, accumulating results into `visitor`.
    *
    * A local `GraphNeighbours[V]` derived from `adjacentVertices` is provided to the engine.
    *
    * @param visitor the visitor to accumulate results.
    * @param v       the starting vertex.
    */
  def dfs[R, J <: Appendable[(V, Option[R])]](visitor: Visitor[V, R, J])(v: V)(using ev: Evaluable[V, R]): Visitor[V, R, J] =
    given GraphNeighbours[V] = graphNeighbours

    Traversal.dfs(v, visitor)

  /**
    * Performs DFS for all vertices in the graph, including those not reachable from any
    * single start vertex.
    *
    * @param visitor the visitor to accumulate results.
    */
  def dfsAll[R, J <: Appendable[(V, Option[R])]](visitor: Visitor[V, R, J])(using ev: Evaluable[V, R]): Visitor[V, R, J] =
    given nbrs: GraphNeighbours[V] = graphNeighbours

    @tailrec
    def loop(vis: Visitor[V, R, J], visited: Set[V]): Visitor[V, R, J] =
      val unvisited = map.keySet.diff(visited)
      if unvisited.isEmpty then vis
      else
        val next = unvisited.head
        // Pre-mark all already-visited vertices so the engine skips them
        val vs: VisitedSet[V] = visited.foldLeft(summon[VisitedSet[V]])(_.markVisited(_))
        val result = Traversal.dfs(next, vis)(using nbrs, ev, vs)
        loop(result, visited + next)

    loop(visitor, Set.empty)

  /**
    * Performs BFS from `v`, accumulating results into `visitor`.
    *
    * @param visitor the visitor to accumulate results.
    * @param v       the starting vertex.
    * @param goal    early-termination predicate (default: never stop early).
    */
  def bfs[R, J <: Appendable[(V, Option[R])]](visitor: Visitor[V, R, J])(v: V, goal: V => Boolean = _ => false)(using ev: Evaluable[V, R]): Visitor[V, R, J] =
    given GraphNeighbours[V] = graphNeighbours

    Traversal.bfs(v, visitor, goal)

  /**
    * BFS over edges rather than vertices.
    * NOTE: this is a stub — the old implementation was also effectively a stub.
    *
    * @param visitor the visitor processing edges.
    * @param v       the starting vertex.
    * @param goal    early-termination predicate on destination vertices.
    */
  def bfse[E, R, J <: Appendable[(Edge[E, V], Option[R])]](visitor: Visitor[Edge[E, V], R, J])(v: V)(goal: V => Boolean)(using Evaluable[Edge[E, V], R]): Visitor[Edge[E, V], R, J] =
    visitor // stub — full edge-BFS implementation deferred

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
      case Some(vv) => this + vv
      case None => this

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
    VertexMap(map = map + (vertex.attribute -> vertex))(random)

  /**
    * Adds a directed or undirected edge to the vertex map.
    */
  def +[E](edge: Edge[E, V]): VertexMap[V] =
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
    * Ensures a vertex for `v` is present, creating it with `f` if absent.
    */
  def ensure(f: V => Vertex[V])(v: V): VertexMap[V] = get(v) match
    case Some(_) => this
    case None => this + f(v)

  /**
    * Adds the edges from the provided EdgeList to this VertexMap.
    */
  def addEdges[E, Z](edgeList: EdgeList[V, E, Z]): VertexMap[V] =
    edgeList.edges.foldLeft[VertexMap[V]](this)((vm, e) => vm + e)

  /**
    * Adds a sequence of vertex pairs with their EdgeTypes to this VertexMap.
    */
  def addVertexPairs[E](pairs: Seq[(V, V, EdgeType)]): VertexMap[V] =
    pairs.foldLeft[VertexMap[V]](this)((vm, pair) => vm + pair)

  /**
    * Processes a sequence of triplets and adds them to the VertexMap.
    */
  def addTriplets[E, Z](vertexFunction: V => Vertex[V], edgeFunction: Z => ProtoConnexion[E, V] => Connexion[V])(triplets: Seq[Triplet[V, E, Z]]): VertexMap[V] =
    triplets.foldLeft[VertexMap[V]](this)((vm, triplet) => vm.addTriplet(edgeFunction)(vertexFunction)(triplet))

  /**
    * Creates vertices and adjacencies from a triplet.
    */
  def createVerticesFromTriplet[E, Z](f: V => Vertex[V])(g: (Vertex[V], Vertex[V], Option[E]) => Adjacency[V])(condition: Boolean)(triplet: Triplet[V, E, Z]): VertexMap[V] =
    val vv1: Vertex[V] = getOrCreate(f)(triplet.from)
    val vv2: Vertex[V] = getOrCreate(f)(triplet.to)
    val va: Adjacency[V] = g(vv1, vv2, triplet.maybeAttribute)
    val vao: Option[Adjacency[V]] = Option.when(condition)(g(vv2, vv1, triplet.maybeAttribute))
    this + (vv1 + va) + vao.fold(vv2)(vv2 + _)

  /**
    * Creates an Adjacency from an optional edge.
    */
  def createAdjacency[E, Z](edgeFunction: ProtoConnexion[E, V] => Connexion[V])(vv1: Vertex[V], vv2: Vertex[V], maybeE: Option[E]): Adjacency[V] =
    maybeE match
      case Some(e) => AdjacencyEdge[V, E](edgeFunction(e, vv1.attribute, vv2.attribute))
      case None => AdjacencyVertex[V](vv2.attribute)

  /**
    * Adds a single triplet to the VertexMap.
    */
  def addTriplet[Z, E](edgeFunction: Z => ProtoConnexion[E, V] => Connexion[V])(vertexFunction: V => Vertex[V])(triplet: Triplet[V, E, Z]): VertexMap[V] =
    val f: (Vertex[V], Vertex[V], Option[E]) => Adjacency[V] = createAdjacency(edgeFunction(triplet.edgeType))
    createVerticesFromTriplet[E, Z](vertexFunction)(f)(triplet.edgeType != Directed)(triplet)

  // -----------------------------------------------------------------------
  // toString
  // -----------------------------------------------------------------------

  override def toString: String =
    map.map((v, vv) => s"v:$v, vv:${vv.render}").mkString("[", ", ", "]")

  // -----------------------------------------------------------------------
  // Private helpers
  // -----------------------------------------------------------------------

  private val addAdjFunction: Adjacency[V] => Vertex[V] => Vertex[V] = va => w => w + va

  private def getOrCreate(f: V => Vertex[V])(v: V): Vertex[V] =
    map.getOrElse(v, f(v))

  private def randomAdjacencies(vv: Vertex[V]) =
    RandomIterator(vv.adjacencies.iterator)(using random)

/**
  * Companion object for VertexMap.
  */
object VertexMap:

  def apply[V](map: Map[V, Vertex[V]]): VertexMap[V] = new VertexMap(map)()

  def apply[V]: VertexMap[V] = apply(Map.empty[V, Vertex[V]])

  def createFromTriplets[V, E, Z](f: Triplet[V, E, Z] => (Vertex[V], Vertex[V]))(triplets: Triplets[V, E, Z]): VertexMap[V] =
    VertexMap(addTripletsToMap(f)(Map.empty[V, Vertex[V]])(triplets))

  def createFromVertexPairList[V](vertexPairList: VertexPairList[V]): VertexMap[V] =
    VertexMap[V].addVertexPairs(vertexPairList.pairs)

  private def addTripletsToMap[V, E, Z](f: Triplet[V, E, Z] => (Vertex[V], Vertex[V]))(map: Map[V, Vertex[V]])(triplets: Triplets[V, E, Z]): Map[V, Vertex[V]] =
    triplets.triplets.foldLeft[Map[V, Vertex[V]]](map) { (vm, t) =>
      val (vv1, vv2) = f(t)
      vm + (t.from -> vv1) + (t.to -> vv2)
    }

  val logger: Logger = LoggerFactory.getLogger("VertexMap")