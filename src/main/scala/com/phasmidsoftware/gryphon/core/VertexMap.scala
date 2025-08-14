package com.phasmidsoftware.gryphon.core

// NOTE backward imports

import com.phasmidsoftware.gryphon.core.Vertex.createWithSet
import com.phasmidsoftware.gryphon.core.VertexMap.{anyUndiscoveredVertex, logger, maybeUndiscoveredVertex}
import com.phasmidsoftware.gryphon.util
import com.phasmidsoftware.gryphon.util.RandomIterator.*
import com.phasmidsoftware.gryphon.util.{GraphException, RandomIterator}
import com.phasmidsoftware.visitor.*
import org.slf4j.{Logger, LoggerFactory}

import scala.annotation.tailrec
import scala.collection.immutable.Queue
import scala.util.{Random, Using}

/**
 * Represents a mapping of vertex attributes to their corresponding vertices within
 * a graph.
 * Provides functionality to manage and traverse vertices via depth-first search (DFS).
 *
 * @tparam V the type representing the vertex attributes (`V` is invariant).
 * @param map a mapping from vertex attributes to their associated Vertex instances.
 */
case class VertexMap[V](map: Map[V, Vertex[V]], private val random: Random = Random()) extends Traversable[V]:

  /**
   * Returns an iterator over the vertices adjacent to the given vertex.
   *
   * @param v the vertex for which adjacent vertices are to be retrieved
   * @return an iterator over the vertices adjacent to the specified vertex
   * @throws GraphException if the specified vertex is not found in the graph
   */
  def adjacentVertices(v: V): Iterator[V] =
    val vo = get(v)
    if (vo.isEmpty) throw GraphException(s"vertex $v not found")
    for (vv <- vo.iterator; va <- randomAdjacencies(vv)) yield va.vertex

  /**
   * Finds the adjacent vertices of a given vertex that have not been discovered.
   *
   * @param v The vertex whose undiscovered adjacent vertices are to be found.
   * @return An iterator of vertices representing the undiscovered adjacent vertices of the given vertex.
   */
  def undiscoveredAdjacentVertices(v: V): Iterator[V] =
    filteredAdjacentVertices(undiscoveredVertex(_).isDefined)(v)

  /**
   * Filters the adjacencies of a given vertex based on a specified predicate.
   *
   * @param predicate A function that takes an instance of Discoverable[V] and returns a Boolean,
   *                  used to determine which adjacencies to include.
   * @param v         The vertex whose adjacencies are to be filtered.
   * @return An iterator of adjacencies that satisfy the provided predicate.
   */
  def filteredAdjacencies(predicate: Discoverable[V] => Boolean)(v: V): Iterator[Adjacency[V]] =
    map(v).adjacencies.filter(predicate).iterator

  /**
   * Returns a string representation of the object.
   *
   * @return a string representation of the map.
   */
  override def toString: String =
    map.toString

  /**
   * A function value that represents adding an adjacency to a vertex.
   *
   * This is a curried function where:
   * - The first parameter is an adjacency of type `Adjacency[V]`.
   * - The second parameter is a vertex of type `Vertex[V]`.
   *
   * The result is a new vertex with the adjacency added to it.
   */
  private val addAdjFunction: Adjacency[V] => Vertex[V] => Vertex[V] = va => w => w + va

  /**
   * Modifies a vertex in this `VertexMap` if it exists, using the provided function.
   *
   * @param f the function that takes a vertex and returns a modified vertex
   * @param v the identifier of the vertex to modify
   * @return a new VertexMap with the vertex modified if it exists; otherwise, the original VertexMap
   */
  def modifyVertex(f: Vertex[V] => Vertex[V])(v: V): VertexMap[V] =
    get(v) map f match {
      case Some(vv) =>
        this + vv
      case None =>
        this
    }

  /**
   * Retrieves an iterable collection of all vertices present in the `VertexMap`.
   *
   * @return an `Iterable` collection of `Vertex` objects representing the vertices in the map.
   */
  def vertices: Iterable[Vertex[V]] =
    map.values

  /**
   * Checks if the specified key is present in the VertexMap.
   *
   * @param key the key to be checked for existence in the map.
   * @return true if the key exists, false otherwise.
   */
  def contains(key: V): Boolean =
    map.contains(key)

  /**
   * Adds a vertex to the current VertexMap, associating it with its attribute as the key.
   *
   * @param vertex the vertex to be added to the VertexMap.
   * @return a new VertexMap containing the existing vertices and the newly added vertex.
   */
  def +(vertex: Vertex[V]): VertexMap[V] =
    copy(map = map + (vertex.attribute -> vertex))

  /**
   * Adds a directed or undirected edge to the vertex map, creating or updating the vertices
   * and their adjacency relationships as needed.
   *
   * @param edge The edge to be added, which associates two vertices in the graph.
   *             The `oneWay` property of the edge determines whether the edge
   *             is directed (one-way) or undirected (two-way).
   * @return A new VertexMap with the updated vertices and adjacency information
   *         reflecting the addition of the edge.
   */
  def +[E](edge: Edge[E, V]): VertexMap[V] = {
    val createFrom: V => Vertex[V] =
      createWithSet[V] andThen addAdjFunction(AdjacencyEdge(edge))
    val createTo: V => Vertex[V] =
      if (edge.edgeType.oneWay) createWithSet[V] else createWithSet[V] andThen addAdjFunction(AdjacencyEdge(edge, true))
    val fromFunc: VertexMap[V] => V => VertexMap[V] =
      mv => mv.ensure(createFrom)
    val toFunc: VertexMap[V] => V => VertexMap[V] =
      mv => mv.ensure(createTo)
    fromFunc(toFunc(this)(edge.black))(edge.white)
  }

  /**
   * Adds a new pair of vertices and their adjacency relationship to the VertexMap.
   *
   * @param pair A tuple containing two vertices of type V and a Boolean flag.
   *             The first element (pair._1) represents the source vertex.
   *             The second element (pair._2) represents the target vertex.
   *             The third element (pair._3) indicates whether the relationship is one-way (true)
   *             or bidirectional (false).
   * @return A new VertexMap[V] instance with the updated adjacency relationships.
   */
  def +(pair: (V, V, EdgeType)): VertexMap[V] = {
    val create: V => Vertex[V] = Vertex.createWithBag[V]
    val v1 = pair._1
    val v2 = pair._2
    val vm = this.ensure(create)(v1).ensure(create)(v2).modifyVertex(addAdjFunction(AdjacencyVertex(v2)))(v1)
    if (pair._3.oneWay) vm else vm.modifyVertex(addAdjFunction(AdjacencyVertex(v1)))(v2)
  }

  /**
   * Retrieves the vertex associated with the specified key.
   *
   * @param key the key representing the attribute of the vertex to retrieve.
   * @return an `Option` containing the vertex associated with the key, or `None` if the key is not present.
   */
  def get(key: V): Option[Vertex[V]] =
    map.get(key)

  /**
   * Returns the value associated with the given key in the map if it exists.
   * If the key is not present in the map,
   * the provided default value is returned instead.
   *
   * @param key     the key whose associated value is to be returned.
   * @param default the default value to return if the key is not found in the map.
   * @return the value associated with the given key, or the default value if the key is not found.
   */
  def getOrElse(key: V, default: => Vertex[V]): Vertex[V] =
    map.getOrElse(key, default)

  /**
   * Retrieves the vertex associated with the specified key from the vertex map.
   *
   * @param key the key identifying the attribute of the vertex to retrieve.
   * @return the vertex associated with the specified key.
   * @throws NoSuchElementException if the key is not present in the vertex map.
   */
  @throws[NoSuchElementException]
  def apply(key: V): Vertex[V] =
    map.apply(key)

  /**
   * Applies the provided function `defaultFunc` to the key `x` if `x` is not present in the map;
   * if `x` is present, its corresponding value in the map is returned.
   * NOTE the primary difference between this method and `ensure` is that this `VertexMap` is unaffected.
   *
   * @param x           the key to lookup in the map
   * @param defaultFunc the function to apply when the key `x` is not found in the map
   * @return the value associated with the key `x` in the map, or the result of applying the `defaultFunc` function if `x` is not found
   */
  def applyOrElse(x: V, defaultFunc: V => Vertex[V]): Vertex[V] =
    map.applyOrElse(x, defaultFunc)

  /**
   * Retrieves the set of keys (attributes of vertices) present in the `VertexMap`.
   *
   * @return a `Set` containing the keys associated with the vertices in the `VertexMap`.
   */
  def keySet: Set[V] =
    map.keySet

  /**
   * Provides an iterator over the keys in the underlying map.
   *
   * @return An iterator of type `V` representing the keys in the map.
   */
  def iterator: Iterator[V] =
    map.keysIterator

  /**
   * Ensures that a vertex corresponding to the given value is present in the VertexMap.
   * If the vertex is already present, the VertexMap remains unchanged.
   * Otherwise, the vertex is created using the provided function and added to the VertexMap.
   *
   * @param f A function that takes a value of type V and maps it to a Vertex[V].
   * @param v The value for which the presence of a corresponding vertex is ensured.
   * @return A new VertexMap[V] with the vertex ensured, or the original VertexMap[V]
   *         if the vertex is already present.
   */
  def ensure(f: V => Vertex[V])(v: V): VertexMap[V] = get(v) match {
    case Some(_) =>
      this
    case None =>
      this + f(v)
  }

  /**
   * Finds an undiscovered vertex associated with the given value, marks it as discovered, and returns it.
   *
   * @param v The value associated with the vertex to be checked for discovery status.
   * @return An Option containing the undiscovered vertex if found and marked as discovered, or None if no undiscovered vertex exists.
   */
  def undiscoveredVertex(v: V): Option[Vertex[V]] =
    maybeUndiscoveredVertex(map, v)

  /**
   * Adds the edges from the provided EdgeList to the existing VertexMap.
   *
   * @param edgeList the EdgeList containing the edges to be added
   * @return a new VertexMap with the edges from the EdgeList added
   */
  def addEdges[E, Z](edgeList: EdgeList[V, E, Z]): VertexMap[V] =
    edgeList.edges.foldLeft[VertexMap[V]](this) { (vm, e) => vm + e }

  /**
   * Adds a sequence of vertex pairs along with their associated edge types to the current vertex map.
   *
   * @param pairs A sequence of tuples where each tuple contains two vertices of type V and an edge type of type EdgeType.
   * @return A new VertexMap[V] containing the added vertex pairs and edge types.
   */
  def addVertexPairs[E](pairs: Seq[(V, V, EdgeType)]): VertexMap[V] =
    pairs.map(p => (p._1, p._2, p._3)).foldLeft[VertexMap[V]](this) { (vm, pair) => vm + pair }

  /**
   * Performs a depth-first search (DFS) starting from the given vertex.
   *
   * @param visitor an instance of DfsVisitor that defines the behavior during the DFS traversal
   * @param v       the starting vertex of the DFS traversal
   * @return the updated DfsVisitor after completing the DFS traversal
   */
  def dfs(visitor: DfsVisitor[V])(v: V): DfsVisitor[V] = {
    initializeVisits(Some(v))
    visitor.dfs(v)
  }

  /**
   * Performs a depth-first search (DFS) starting from the given vertex `v`.
   *
   * @param visitor an instance of `DfsVisitorMapped` used to track and process the DFS
   * @param f       a function that transforms a vertex of type `V` to a value of type `T`
   * @param v       the starting vertex for the DFS
   * @return the updated `DfsVisitorMapped` instance after completing the DFS
   */
  def dfsFunction[T](visitor: DfsVisitorMapped[V, T])(f: V => T)(v: V): DfsVisitorMapped[V, T] = {
    initializeVisits(Some(v))
    visitor.dfs(v)
  }

  /**
   * Performs a special Depth-First Search (DFS) on a graph starting from the given vertex `v`.
   * It utilizes a special visitor consisting of a tuple of vertices.
   *
   * @param visitor A visitor function used to process graph elements during the DFS traversal.
   * @param v       The starting vertex for the DFS traversal.
   * @return The visitor after completing the DFS traversal.
   */
  def dfsA(visitor: DfsVisitor[(V, V)])(v: V): DfsVisitor[(V, V)] = {
    initializeVisits(Some(v))
    val function: V => (V, V) = x => (v, x)
    Using.resource(recursiveDFSA(function)(visitor, v)) {
      result => result
    }
  }

  /**
   * Performs a breadth-first search (BFS) traversal starting from the given vertex.
   *
   * @param visitor the visitor object which defines the behavior during the traversal
   * @param v       the starting vertex for the BFS traversal
   * @return a tuple containing the modified visitor after traversal and an optional next vertex to visit
   */
  def bfs(visitor: BfsVisitor[V])(v: V): (BfsVisitor[V], Option[V]) = {
    initializeVisits(Some(v))
    visitor.bfs(v)
  }

  /**
   * Performs a Breadth-First Search (BFS) starting from a given node.
   *
   * @param visitor the visitor that keeps track of visited nodes and allows processing at each step of BFS
   * @param v       the starting node for the BFS
   * @param goal    a predicate function that determines if the search goal has been reached for a node
   * @return the updated visitor after completing the BFS
   */
  def bfs(visitor: Visitor[V])(v: V)(goal: V => Boolean): Visitor[V] = {
    initializeVisits(Some(v))
    val result: Visitor[V] = doBFSImmutable(visitor, Queue(v))(goal)
    result.close()
    result
  }

  def bfse[E](visitor: Visitor[Edge[E, V]])(v: V)(goal: V => Boolean): Visitor[Edge[E, V]] = {
    initializeVisits(Some(v)) // initialize the edge, not the vertices
    val result = doBFSE[E](visitor, v)(goal)
    result.close()
    result
  }

  /**
   * Performs a Depth-First Search (DFS) ensuring that all vertices are discovered.
   *
   * @param visitor the visitor that processes the vertices during the DFS traversal
   * @return the updated visitor after processing all undiscovered vertices
   */
  def dfsAll(visitor: DfsVisitor[V]): DfsVisitor[V] = {
    initializeVisits(None)

    @tailrec
    def inner(q: DfsVisitor[V]): DfsVisitor[V] =
      anyUndiscoveredVertex(map) match {
        case Some(v) =>
          inner(recursiveDFS(q, v.attribute))
        case None =>
          q
      }

    inner(visitor)
  }

  /**
   * Processes a sequence of triplets and adds them to the VertexMap using the provided vertex and edge functions.
   *
   * @param vertexFunction a function to transform a value of type V into a Vertex
   * @param edgeFunction   a function to transform a value of type Z into a function that creates an Edge given E, V, and V
   * @param triplets       a sequence of triplets containing vertex-edge information to be added
   * @return a new VertexMap with the added triplets
   */
  def addTriplets[E, Z](vertexFunction: V => Vertex[V], edgeFunction: Z => ProtoConnexion[E, V] => Connexion[V])(triplets: Seq[Triplet[V, E, Z]]): VertexMap[V] =
    triplets.foldLeft[VertexMap[V]](this) {
      (vm, triplet) =>
        vm.addTriplet(edgeFunction)(vertexFunction)(triplet)
    }

  /**
   * Creates vertices and their adjacencies from a given triplet based on the specified parameters.
   *
   * @param f         a function that transforms a value of type V into a Vertex[V]
   * @param g         a function that creates an adjacency between two vertices, optionally considering an edge of type E
   * @param condition a boolean condition upon which certain adjacencies are created
   * @param triplet   a Triplet containing two vertex values of type V and optionally an edge of type E
   * @tparam E the type of the edge in the triplet, if present
   * @tparam Z the auxiliary type used in the triplet
   * @return a VertexMap[V] representing the updated graph structure with newly created or modified vertices
   */
  def createVerticesFromTriplet[E, Z](f: V => Vertex[V])(g: (Vertex[V], Vertex[V], Option[E]) => Adjacency[V])(condition: Boolean)(triplet: Triplet[V, E, Z]): VertexMap[V] = {
    val vv1: Vertex[V] = getOrCreate(f)(triplet._1)
    val vv2: Vertex[V] = getOrCreate(f)(triplet._2)
    val va: Adjacency[V] = g(vv1, vv2, triplet._3)
    val vao: Option[Adjacency[V]] = Option.when(condition)(g(vv2, vv1, triplet._3))
    this + (vv1 + va) + vao.fold(vv2)(vv2 + _)
  }

  /**
   * Creates an adjacency representation for a graph structure based on the provided edge function,
   * two vertices, and an optional edge.
   *
   * @param edgeFunction A function that takes an edge instance of type E, and the attributes of two vertices,
   *                     returning an Edge instance.
   * @param vv1          The first vertex of type Vertex[V].
   * @param vv2          The second vertex of type Vertex[V].
   * @param maybeE       An optional edge of type E. If defined, it is used to create an adjacency edge,
   *                     otherwise an adjacency vertex is created.
   * @return An adjacency of type Adjacency[V], which represents the relationship between vertices in the graph.
   */
  def createAdjacency[E, Z](edgeFunction: ProtoConnexion[E, V] => Connexion[V])(vv1: Vertex[V], vv2: Vertex[V], maybeE: Option[E]): Adjacency[V] =
    maybeE match {
      case Some(e) =>
        AdjacencyEdge[V, E](edgeFunction(e, vv1.attribute, vv2.attribute))
      case None =>
        AdjacencyVertex[V](vv2.attribute)
    }

  /**
   * Adds a triplet by creating vertices and connecting them using the provided edge and vertex creation functions.
   *
   * @param edgeFunction   A function that takes a type of edge data `Z`
   *                       and returns a function that creates an `Edge[E, V]` given an edge type `E` and a pair of vertices `V`.
   * @param vertexFunction A function that takes a vertex `V` and creates a `Vertex[V]`.
   * @param triplet        A `Triplet[V, E, Z]` containing the vertices, edge, and edge type to be processed.
   * @return A `VertexMap[V]` representing the updated set of vertices with their adjacencies.
   */
  def addTriplet[Z, E](edgeFunction: Z => ProtoConnexion[E, V] => Connexion[V])(vertexFunction: V => Vertex[V])(triplet: Triplet[V, E, Z]): VertexMap[V] = {
    val f: (Vertex[V], Vertex[V], Option[E]) => Adjacency[V] = createAdjacency(edgeFunction(triplet.edgeType))
    createVerticesFromTriplet[E, Z](vertexFunction)(f)(triplet.edgeType != Directed)(triplet)
  }

  /**
   * Retrieves the existing Vertex associated with the given key `v` from the map, 
   * or creates a new Vertex using the provided function `f` if the key is not present.
   * NOTE that this method does NOT insert a newly created Vertex into the map.
   *
   * @param f A function that takes a value of type `V` and produces a Vertex of type `Vertex[V]`.
   * @param v The key of type `V` for which a Vertex is to be retrieved or created.
   * @return The Vertex associated with the key `v`, either retrieved from the map or created using the function `f`.
   */
  private def getOrCreate(f: V => Vertex[V])(v: V): Vertex[V] =
    map.getOrElse(v, f(v))

  /**
   * Performs a breadth-first search (BFS) traversal on a graph in an immutable fashion.
   * Updates the visitor with the traversal order, starting from the vertices in the provided queue.
   * If a goal condition is met for a vertex, it is marked in the visitor.
   *
   * @param visitor The Visitor object that keeps track of the traversal state and visited nodes.
   * @param queue   The initial queue of vertices to start the BFS traversal from.
   * @param goal    A predicate function that specifies the goal condition to check for each vertex.
   *                If the condition is satisfied, the vertex is marked in the visitor as part of the result.
   * @return The updated Visitor object after completing the BFS traversal.
   */
  private def doBFSImmutable(visitor: Visitor[V], queue: Queue[V])(goal: V => Boolean): Visitor[V] = {
    @tailrec
    def inner(result: Visitor[V], work: Queue[V]): Visitor[V] = {
      work match {
        case q if q.isEmpty =>
          result
        case q =>
          val (v, q1) = q.dequeue
          if (goal(v)) result.visit(Pre)(v)
          else {
            val (a, b) = undiscoveredAdjacentVertices(v).foldLeft(result -> q1) {
              case ((r, p), w) =>
                get(w).foreach(v => v.discover())
                r.visit(Pre)(w) -> p.enqueue(w)
            }
            inner(a, b)
          }
      }
    }

    inner(visitor, queue)
  }

  /**
   * Performs a breadth-first search (BFS) traversal starting from the given vertex.
   *
   * NOTE: this is never used and most certainly needs work if it is to be used!
   *
   * @param visitor The visitor used to process edges during the traversal.
   * @param v       The starting vertex of the BFS traversal.
   * @param goal    A function defining the goal condition; the traversal continues until this condition is met.
   * @return A Visitor instance after the BFS traversal has completed, potentially updated with visited edges.
   */
  private def doBFSE[E](visitor: Visitor[Edge[E, V]], v: V)(goal: V => Boolean): Visitor[Edge[E, V]] = {
    //    if (goal(v))
    visitor
    //    else {
    //      val q: Queue[Edge[E, V]] = enqueueUntraversedEdges[Edge[E, V], Queue[Edge[E, V]]](v, visitor.journal)
    //      if (q.isEmpty)
    //        visitor
    //      else
    //        val (e, w) = q.dequeue
    //        e match {
    //          case DirectedEdge(_, _, w) =>
    //            doBFSE(visitor.visitPre(e), w)(goal).visitPost(e)
    //          case UndirectedEdge(_, _, _) =>
    //            doBFSE(visitor.visitPre(e), to)(goal).visitPost(e)
    //        }
    //    }
    //    @tailrec
    //    def inner(result: Visitor[E, J], work: Q): Visitor[Edge[E, V], J] = {
    //      queueable.take(work) match {
    //      case Some((head, q)) =>
    //        val (e, w) = head match {
    //          case DirectedEdge(a,_,c) =>
    //            (a, c)
    //          case x@UndirectedEdge(a,b,c) =>
    //              x.other(v)
    //
    //        }
    //        val (a,b,c) = head
    //        val edgeType = head.edgeType
    //        val white = head.white
    //        val black = head.black
    //        inner(result.visitPre(head), enqueueUntraversedEdges(head, tail))
    //      case _ =>
    //        result
    //    }
    //    }
    //
    //    inner(visitor, queueable.empty)
  }

  /**
   * Initializes the visits for all vertices in the graph and resets the state for a specific vertex, if provided.
   *
   * @param vo An optional vertex object of type V. If provided, the corresponding vertex in the map will be specifically reset.
   * @return The vertex of type Vertex[V] corresponding to the provided vertex `vo` if it exists, or null if no vertex is provided.
   * @throws GraphException If the provided vertex `vo` is not found in the map.
   */
  private def initializeVisits(vo: Option[V]): Vertex[V] = {
    map.values foreach (_.reset())
    vo match {
      case Some(v) =>
        get(v) match {
          case Some(vv) =>
            vv.reset()
          case None =>
            logger.warn(s"Vertex $v not found in map.")
            // TODO understand why the exception is not thrown
            throw GraphException(s"Vertex $v not found in map.")
        }
      case None =>
        null
    }
  }

  /**
   * Performs a recursive depth-first search (DFS) on the given vertex.
   *
   * @param visitor The DFS visitor instance that maintains the state and logic for visiting vertices during the search.
   * @param v       The current vertex being visited in the recursive DFS traversal.
   * @return The updated DFS visitor instance after completing the traversal for the current vertex.
   */
  private def recursiveDFS(visitor: DfsVisitor[V], v: V): DfsVisitor[V] =
    recurseOnVertex(v, visitor).visit(Post)(v)

  /**
   * Performs a recursive Depth-First Search (DFS) operation on a given vertex.
   *
   * @param f       A function that transforms a vertex of type `V` into a value of type `A`.
   * @param visitor An instance of `DfsVisitor[A]` used to keep track of the DFS state and results.
   * @param v       The vertex `V` on which the DFS operation is to be performed.
   * @return An updated `DfsVisitor[A]` instance after processing the given vertex and its descendants.
   */
  private def recursiveDFSA[A](f: V => A)(visitor: DfsVisitor[A], v: V): DfsVisitor[A] =
    recurseOnVertexA(f)(v, visitor).visit(Post)(f(v))

  /**
   * Recursively performs depth-first search (DFS) starting from the given vertex.
   *
   * @param v       The vertex from which to start the DFS.
   * @param visitor The DFS visitor that handles the traversal events.
   */
  private def recurseOnVertex(v: V, visitor: DfsVisitor[V]) =
    undiscoveredAdjacentVertices(v).foldLeft(visitor)((jVv, w) => recursiveDFS(jVv.visit(Pre)(w), w))

  /**
   * Recursively performs a depth-first search (DFS) starting from the given vertex `v`,
   * visiting adjacent vertices that are undiscovered, and applies the provided function `f`
   * to each vertex. Updates the DFS traversal state using the provided `visitor`.
   *
   * @param f       A function that transforms a vertex `V` into a value of type `A`.
   * @param v       The starting vertex for the DFS traversal.
   * @param visitor The DFS visitor to keep track of the traversal.
   */
  private def recurseOnVertexA[A](f: V => A)(v: V, visitor: DfsVisitor[A]) =
    undiscoveredAdjacentVertices(v).foldLeft(visitor)((jVv, w) => {
      val g: V => A = ((x: V) => (w, x)).asInstanceOf[V => A]
      recursiveDFSA(g)(jVv.visit(Pre)(f(w)), w)
    })

  /**
   * Generates a random iterator of adjacencies for the given vertex.
   *
   * @param vv the vertex whose adjacencies are to be randomized
   * @return a randomized iterator of the vertex's adjacencies
   */
  private def randomAdjacencies(vv: Vertex[V]) = RandomIterator(vv.adjacencies.iterator)(random)

/**
 * Companion object for creating instances of `VertexMap` and providing
 * related utility methods for graph data structure management.
 */
object VertexMap:
  /**
   * Creates a new instance of `VertexMap` using the provided mapping of vertex attributes
   * to their corresponding vertices.
   *
   * @param map a mapping from attributes of type `V` to their associated `Vertex[V]` instances.
   * @tparam V the type of the vertex attributes.
   * @return a new `VertexMap` containing the specified mapping of vertex attributes to vertices.
   */
  def apply[V](map: Map[V, Vertex[V]]): VertexMap[V] =
    new VertexMap(map)

  /**
   * Creates an empty `VertexMap` instance with no vertex mappings.
   *
   * This implementation acts as a convenience method to initialize 
   * an empty `VertexMap` without explicitly passing an empty map.
   *
   * @tparam V the type of the vertex attributes.
   * @return a new `VertexMap` with no vertex mappings.
   */
  def apply[V]: VertexMap[V] =
    apply(Map.empty[V, Vertex[V]])

  /**
   * Creates a `VertexMap` from the given `Triplets` by mapping each triplet to a pair of vertices
   * using the provided function `f` and aggregating the vertices into a map.
   *
   * @param f        a function that takes a triplet and returns a pair of vertices.
   *                 This function is used to extract or create vertices from each triplet.
   * @param triplets the graph edges represented as a sequence of triplets, where each
   *                 triplet contains a source vertex, a target vertex, and an edge attribute.
   * @tparam V the type of the vertex attributes.
   * @tparam E the type of the edge attributes.
   * @return a `VertexMap` that maps vertex attributes to their corresponding `Vertex` objects
   *         derived from the `triplets`.
   */
  def createFromTriplets[V, E, Z](f: Triplet[V, E, Z] => (Vertex[V], Vertex[V]))(triplets: Triplets[V, E, Z]): VertexMap[V] =
    VertexMap(addTripletsToMap(f)(Map.empty[V, Vertex[V]])(triplets))

  /**
   * Creates a `VertexMap` from the given `EdgeList` by extracting vertices
   * from the edges and constructing a mapping of vertex attributes to their
   * corresponding `Vertex` objects.
   *
   * @param vertexPairList the list of vertex pairs representing the graph structure,
   *                       where each vertex pair contains `white` and `black` vertices.
   * @tparam V the type of the vertex attribute.
   * @return a `VertexMap` that maps vertex attributes to their corresponding `Vertex` objects.
   */
  def createFromVertexPairList[V](vertexPairList: VertexPairList[V]): VertexMap[V] =
    VertexMap[V].addVertexPairs(vertexPairList.pairs)

  /**
   * Attempts to retrieve a vertex from the given map of vertex attributes to `Vertex` instances
   * if it has not yet been discovered. If the vertex has not been discovered, it is marked as
   * discovered and returned. Otherwise, returns `None`.
   *
   * @param vvVm a map that associates vertex attributes of type `V` with their corresponding
   *             `Vertex[V]` instances.
   * @param v    the vertex attribute of type `V` to look up in the map.
   * @tparam V the type of the vertex attributes.
   * @return an `Option` containing the `Vertex[V]` if it was undiscovered, or `None` if the vertex
   *         was already discovered.
   */
  def maybeUndiscoveredVertex[V](vvVm: Map[V, Vertex[V]], v: V): Option[Vertex[V]] = {
    val vertex = vvVm(v)
    Option.when(vertex.undiscovered)(vertex) map (_.discover())
  }

  /**
   * Searches for any undiscovered vertex in the given map of vertices and marks it as discovered
   * if one is found.
   * NOTE that this method and maybeUndiscoveredVertex must operate similarly
   *
   * @param vvVm a map associating vertex attributes of type `V` with their corresponding `Vertex[V]` instances.
   *             Each vertex has a `discovered` flag indicating its discovery status during graph traversal.
   * @tparam V the type of the vertex attributes.
   * @return an `Option` containing an undiscovered `Vertex[V]` that is subsequently marked as discovered,
   *         or `None` if no undiscovered vertices are found in the map.
   */
  def anyUndiscoveredVertex[V](vvVm: Map[V, Vertex[V]]): Option[Vertex[V]] =
    vvVm.values.find(_.undiscovered) map (_.discover())

  /**
   * Updates a map of vertex attributes to `Vertex` instances by processing a collection of triplets.
   * Each triplet is transformed into a pair of vertices using the provided function, and the map
   * is updated with these vertices.
   *
   * @param f        a function that takes a triplet as input and returns a pair of vertices.
   * @param map      the initial mapping of vertex attributes to their corresponding `Vertex` instances.
   * @param triplets the collection of triplets representing graph edges, where each triplet contains:
   *                 a source vertex, a target vertex, and an edge attribute.
   * @tparam E the type of the edge attributes.
   * @tparam V the type of the vertex attributes.
   * @return an updated map of vertex attributes to `Vertex` instances, including vertices derived
   *         from the provided triplets.
   */
  private def addTripletsToMap[V, E, Z](f: Triplet[V, E, Z] => (Vertex[V], Vertex[V]))(map: Map[V, Vertex[V]])(triplets: Triplets[V, E, Z]): Map[V, Vertex[V]] =
    triplets.triplets.foldLeft[Map[V, Vertex[V]]](map) {
      (vm, t) =>
        val (vv1, vv2) = f(t)
        vm + (t._1 -> vv1) + (t._2 -> vv2)
    }

  val logger: Logger = LoggerFactory.getLogger("VertexMap")
