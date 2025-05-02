package com.phasmidsoftware.gryphon.core

// NOTE backward imports

import com.phasmidsoftware.gryphon.core.Vertex.createWithSet
import com.phasmidsoftware.gryphon.core.VertexMap.maybeUndiscoveredVertex
import com.phasmidsoftware.gryphon.util
import com.phasmidsoftware.gryphon.util.RandomIterator.*
import com.phasmidsoftware.gryphon.util.{GraphException, RandomIterator}
import com.phasmidsoftware.gryphon.visit.Queueable.QueueableQueue
import com.phasmidsoftware.gryphon.visit.{MutableQueueable, Queueable, Visitor}

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
   * Computes and returns an iterator of adjacent vertices for the given vertex.
   *
   * This method retrieves the adjacency information for the provided vertex,
   * processes it to obtain random adjacencies, and returns an iterator over
   * the resulting vertices.
   *
   * @param v the vertex for which adjacencies are to be computed
   * @return an iterator of vertices adjacent to the provided vertex
   */
  def adjacencies(v: V): Iterator[V] = for (vv <- get(v).iterator; va <- randomAdjacencies(vv)) yield va.vertex

  /**
   * Retrieves an iterator over the adjacencies of the given vertex that have not yet been discovered.
   *
   * @param v the vertex for which undiscovered adjacencies are sought
   * @return an iterator over the vertices that are adjacent to the given vertex and undiscovered
   */
  def undiscoveredAdjacencies(v: V): Iterator[V] = filteredAdjacencies(undiscoveredVertex(_).isDefined)(v)

  /**
   * Returns a string representation of the object.
   *
   * @return a string representation of the map.
   */
  override def toString: String = map.toString

  /**
   * A function value that represents adding an adjacency to a vertex.
   *
   * This is a curried function where:
   * - The first parameter is an adjacency of type `Adjacency[V]`.
   * - The second parameter is a vertex of type `Vertex[V]`.
   *
   * The result is a new vertex with the adjacency added to it.
   */
  val addAdjFunction: Adjacency[V] => Vertex[V] => Vertex[V] = va => w => w + va

  /**
   * Processes a vertex within a vertex map by applying a specified function to a given vertex
   * and an additional parameter.
   *
   * @param f     A function that takes a vertex and an additional parameter of type X,
   *              then return processed vertex.
   * @param v The key of the vertex to be processed, of type V.
   * @param x An additional parameter of type X that is passed to the function f.
   * @tparam X The type of the additional parameter.
   * @return A new VertexMap[V] with the processed vertex if the key v exists,
   *         or the original VertexMap[V] if the key does not exist.
   */
  def modifyVertexX[X](f: X => Vertex[V] => Vertex[V])(v: V)(x: X): VertexMap[V] =
    get(v) map (vv => f(x)(vv)) match {
      case Some(vv) => this + vv
      case None => this
    }

  /**
   * Modifies a vertex in this `VertexMap`, if it exists, using the provided function.
   *
   * @param f the function that takes a vertex and returns a modified vertex
   * @param v the identifier of the vertex to modify
   * @return a new VertexMap with the vertex modified if it exists; otherwise, the original VertexMap
   */
  def modifyVertex(f: Vertex[V] => Vertex[V])(v: V): VertexMap[V] =
    get(v) map f match {
      case Some(vv) => this + vv
      case None => this
    }

  /**
   * Retrieves an iterable collection of all vertices present in the `VertexMap`.
   *
   * @return an `Iterable` collection of `Vertex` objects representing the vertices in the map.
   */
  def vertices: Iterable[Vertex[V]] = map.values

  /**
   * Checks if the specified key is present in the VertexMap.
   *
   * @param key the key to be checked for existence in the map.
   * @return true if the key exists, false otherwise.
   */
  def contains(key: V): Boolean = map.contains(key)

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
    val createFrom: V => Vertex[V] = createWithSet[V] andThen addAdjFunction(AdjacencyEdge(edge))
    val createTo: V => Vertex[V] = if (edge.edgeType.oneWay) createWithSet[V] else createWithSet[V] andThen addAdjFunction(AdjacencyEdge(edge, true))
    val fromFunc: VertexMap[V] => V => VertexMap[V] = mv => mv.ensure(createFrom)
    val toFunc: VertexMap[V] => V => VertexMap[V] = mv => mv.ensure(createTo)
    fromFunc(toFunc(this)(edge.to))(edge.from)
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
  def get(key: V): Option[Vertex[V]] = map.get(key)

  /**
   * Returns the value associated with the given key in the map if it exists.
   * If the key is not present in the map,
   * the provided default value is returned instead.
   *
   * @param key     the key whose associated value is to be returned.
   * @param default the default value to return if the key is not found in the map.
   * @return the value associated with the given key, or the default value if the key is not found.
   */
  def getOrElse(key: V, default: => Vertex[V]): Vertex[V] = map.getOrElse(key, default)

  /**
   * Retrieves the vertex associated with the specified key from the vertex map.
   *
   * @param key the key identifying the attribute of the vertex to retrieve.
   * @return the vertex associated with the specified key.
   * @throws NoSuchElementException if the key is not present in the vertex map.
   */
  @throws[NoSuchElementException]
  def apply(key: V): Vertex[V] = map.apply(key)

  /**
   * Applies the provided function `defaultFunc` to the key `x` if `x` is not present in the map;
   * if `x` is present, its corresponding value in the map is returned.
   * NOTE the primary difference between this method and `ensure` is that this `VertexMap` is unaffected.
   *
   * @param x           the key to lookup in the map
   * @param defaultFunc the function to apply when the key `x` is not found in the map
   * @return the value associated with the key `x` in the map, or the result of applying the `defaultFunc` function if `x` is not found
   */
  def applyOrElse(x: V, defaultFunc: V => Vertex[V]): Vertex[V] = map.applyOrElse(x, defaultFunc)

  /**
   * Retrieves the set of keys (attributes of vertices) present in the `VertexMap`.
   *
   * @return a `Set` containing the keys associated with the vertices in the `VertexMap`.
   */
  def keySet: Set[V] = map.keySet

  /**
   * Provides an iterator over the keys in the underlying map.
   *
   * @return An iterator of type `V` representing the keys in the map.
   */
  def iterator: Iterator[V] = map.keysIterator

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
    case Some(_) => this
    case None => this + f(v)
  }

  /**
   * Finds an undiscovered vertex associated with the given value, marks it as discovered, and returns it.
   *
   * @param v The value associated with the vertex to be checked for discovery status.
   * @return An Option containing the undiscovered vertex if found and marked as discovered, or None if no undiscovered vertex exists.
   */
  def undiscoveredVertex(v: V): Option[Vertex[V]] = maybeUndiscoveredVertex(map, v)

  /**
   * Adds the edges from the provided EdgeList to the existing VertexMap.
   *
   * @param edgeList the EdgeList containing the edges to be added
   * @return a new VertexMap with the edges from the EdgeList added
   */
  def addEdges[E, Z](edgeList: EdgeList[V, E, Z]): VertexMap[V] = edgeList.edges.foldLeft[VertexMap[V]](this) { (vm, e) => vm + e }

  /**
   * Adds a sequence of vertex pairs along with their associated edge types to the current vertex map.
   *
   * @param pairs A sequence of tuples where each tuple contains two vertices of type V and an edge type of type EdgeType.
   * @return A new VertexMap[V] containing the added vertex pairs and edge types.
   */
  def addVertexPairs[E](pairs: Seq[(V, V, EdgeType)]): VertexMap[V] =
    pairs.map(p => (p._1, p._2, p._3)).foldLeft[VertexMap[V]](this) { (vm, pair) => vm + pair }

  /**
   * Method to run depth-first-search on this `VertexMap`.
   *
   * @param visitor the visitor, of type Visitor[V, J].
   * @param v       the starting vertex.
   * @tparam J the journal type.
   * @return a new Visitor[V, J].
   */
  def dfs[J](visitor: Visitor[V, J])(v: V): Visitor[V, J] = {
    initializeVisits(Some(v))
    Using.resource(recursiveDFS(visitor, v)) {
      result => result
    }
  }

  /**
   * Method to run goal-terminated breadth-first-search on this VertexMap.
   *
   * CONSIDER add relax method as in bfsMutable.
   *
   * @param visitor the visitor, of type Visitor[V, J].
   * @param v       the starting vertex.
   * @param goal    a function which will return true when the goal is reached.
   * @tparam J the journal type.
   * @return a new Visitor[V, J].
   */
  def bfs[J](visitor: Visitor[V, J])(v: V)(goal: V => Boolean): Visitor[V, J] = {
    initializeVisits(Some(v))
    implicit object queuable extends QueueableQueue[V]
    val result: Visitor[V, J] = doBFSImmutable[J, Queue[V]](visitor, v)(goal)
    result.close()
    result
  }

  /**
   * Performs depth-first search on all the vertices in the graph represented by the `VertexMap`.
   * The traversal begins from each undiscovered vertex and visits all reachable vertices,
   * using the provided `Visitor` for processing during the traversal.
   *
   * @param visitor the visitor, an instance of `Visitor[V, J]`, responsible for processing vertices during traversal.
   *                It represents the current state of the traversal and is updated as the traversal progresses.
   * @tparam J the type of the journal associated with the `Visitor`.
   * @return a `Visitor[V, J]` instance that reflects the state of the graph traversal after processing all vertices.
   */
  def dfsAll[J](visitor: Visitor[V, J]): Visitor[V, J] = {
    initializeVisits(None)

    @tailrec
    def inner(q: Visitor[V, J]): Visitor[V, J] = {
      val undiscovered: Map[V, Vertex[V]] = map filter { case (_, v) => !v.discovered }
      undiscovered.headOption match {
        case Some((k, _)) =>
          inner(recursiveDFS(q, k))
        case None =>
          q
      }
    }

    inner(visitor)
  }

  /**
   * Performs a breadth-first search (BFS) traversal on the graph represented by the `VertexMap`.
   *
   * @param visitor the visitor instance (`Visitor[V, J]`) responsible for processing vertices during traversal and
   *                maintaining the state of the traversal.
   * @param vs      a sequence of starting values for the BFS traversal.
   * @param goal    a predicate function that determines whether a given vertex satisfies the goal condition.
   * @param ev      an implicit instance of `MutableQueueable[Q, V]` required to manage the queue during
   *                the mutable BFS traversal.
   * @tparam J the type of the journal associated with the visitor, used for tracking traversal state and updates.
   * @tparam Q the type of the queue used internally for managing vertices during the traversal.
   * @return an updated `Visitor[V, J]` instance that reflects the traversal state after completing the BFS.
   */
  def bfsMutable[J, Q](visitor: Visitor[V, J])(vs: Seq[V])(goal: Vertex[V] => Boolean)(implicit ev: MutableQueueable[Q, V]): Visitor[V, J] = ???
  //    FP.sequence(vs map map.get) match {
  //      case Some(vvs) => 
  //        val queue: Q = ev.empty
  //        val iterator = FP.mutableQueueIterator(queue)
  //        vvs.foldLeft[Visitor[V,J]](visitor){
  //          (z, vv) => bfsRecursive(z, vv, queue)(goal)
  //        }
  //        
  //      case None => visitor
  //    }

  /**
   * Creates vertices from a given triplet representation of a graph relationship and returns updated vertices.
   *
   * @param f       a function that transforms a vertex attribute of type `V` into a `Vertex[V]`.
   * @param f1      a function that creates an adjacency relationship of type `Adjacency[V]`
   *                from two vertices and an edge attribute of type `E`.
   * @param f2      a function that optionally creates an adjacency relationship of type `Adjacency[V]`
   *                from two vertices and an edge attribute of type `E`.
   * @param triplet a triplet representing a relationship in the graph, consisting of two vertex attributes
   *                of type `V` and an edge attribute of type `E`.
   * @return a pair of updated vertices `(Vertex[V], Vertex[V])` after applying the adjacency relationships.
   */
  def createVerticesFromTriplet[E, Z](f: V => Vertex[V])(f1: (Vertex[V], Vertex[V], E) => Adjacency[V])(f2: (Vertex[V], Vertex[V], E) => Option[Adjacency[V]])(triplet: Triplet[V, E, Z]): VertexMap[V] = {
    val vv1: Vertex[V] = getOrCreate(f)(triplet._1)
    val vv2: Vertex[V] = getOrCreate(f)(triplet._2)
    val va1: Adjacency[V] = f1(vv1, vv2, triplet._3)
    val va2: Option[Adjacency[V]] = f2(vv2, vv1, triplet._3)
    this + (vv1 + va1) + va2.fold(vv2)(vv2 + _)
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
  private def getOrCreate(f: V => Vertex[V])(v: V): Vertex[V] = map.getOrElse(v, f(v))

  /**
   * Recursively processes vertices starting from the given vertex `v`, applying a visitor function for each vertex
   * encountered prior to further recursion (or queue expansion).
   * The traversal stops when the `goal` function evaluates to true for a vertex.
   *
   * This method uses an inner tail-recursive function to iterate over a queue of vertices,
   * applying the visitor's `visitPre` method at each step, and enqueues adjacent, unvisited vertices.
   *
   * @param visitor   the `Visitor` instance used to process vertices during traversal.
   * @param v         the starting vertex (as an `Int`).
   * @param goal      a function that evaluates a condition for stopping traversal on a given vertex.
   * @param queueable an implicit type class instance to handle operations on the queue-like structure.
   * @tparam J the type representing the journal of the `Visitor`.
   * @return an updated `Visitor` instance after processing all discovered vertices in the graph.
   */
  private def doBFSImmutable[J, Q](visitor: Visitor[V, J], v: V)(goal: V => Boolean)(implicit queueable: Queueable[Q, V]): Visitor[V, J] = {
    @tailrec
    def inner(result: Visitor[V, J], work: Q): Visitor[V, J] = queueable.take(work) match {
      case Some((head, _)) if goal(head) => result.visitPre(head)
      case Some((head, tail)) => inner(result.visitPre(head), enqueueUnvisitedVertices(head, tail))
      case _ => result
    }

    inner(visitor, queueable.append(queueable.empty, v))
  }


  /**
   * Enqueues all unvisited vertices adjacent to the given vertex into the provided queue.
   *
   * This method processes the adjacencies of the vertex `v` by iterating over them.
   * If an adjacent vertex has not yet been discovered, it is marked as discovered
   * and appended to the queue. If the vertex `v` does not exist in the adjacency list,
   * an exception is thrown.
   *
   * TODO use undiscoveredAdjacencies
   *
   * @param v         the vertex whose unvisited adjacent vertices are to be enqueued
   * @param queue     the queue into which the unvisited vertices will be appended
   * @param queueable the implicit type class to handle the behavior of the queue-like object
   * @return a new queue containing the original elements and the newly enqueued vertices
   * @throws GraphException if the vertex `v` does not exist in the adjacency list
   */
  private def enqueueUnvisitedVertices[Q](v: V, queue: Q)(implicit queueable: Queueable[Q, V]): Q =
    undiscoveredAdjacencies(v).foldLeft(queue) { (q, v) => queueable.append(q, v) }

  /**
   * Initializes the visit status of all vertices in the map by resetting their state.
   *
   * @param vo an optional vertex that can be used for additional initialization, if required.
   * @tparam J the type parameter representing any journal or auxiliary type used with the method.
   * @return Unit since this method performs a side effect operation and does not return a value.
   */
  private def initializeVisits[J](vo: Option[V]): Unit = {
    map.values foreach (_.reset())
    // TODO remove this reference completely?
    //        findAndMarkVertex(map, { (_: Vertex[V]) => () }, vo, s"initializeVisits")
  }

  /**
   * Non-tail-recursive method to run DFS on the vertex V with the given Visitor.
   *
   * @param visitor the Visitor[V, J].
   * @param v       the vertex at which we run depth-first-search.
   * @tparam J the Journal type of the Visitor.
   * @return a new Visitor[V, J].
   */
  private def recursiveDFS[J](visitor: Visitor[V, J], v: V): Visitor[V, J] =
    recurseOnVertex(v, visitor.visitPre(v)).visitPost(v)

  /**
   * Recursively processes a vertex in the graph, traversing its adjacencies
   * while updating the provided visitor.
   * If the vertex is not found in the graph, an exception is thrown.
   *
   * @param v       the vertex to be processed.
   * @param visitor the visitor used to traverse and record information during the traversal.
   * @tparam J the type of the journal associated with the visitor.
   * @throws util.GraphException if the vertex is not found in the graph.
   */
  private def recurseOnVertex[J](v: V, visitor: Visitor[V, J]) =
    undiscoveredAdjacencies(v).foldLeft(visitor)((b, v) => recursiveDFS(b, v))

  /**
   * Generates a random iterator of adjacencies for the given vertex.
   *
   * @param vv the vertex whose adjacencies are to be randomized
   * @tparam J the type parameter representing the relationship or type of adjacencies
   * @return a randomized iterator of the vertex's adjacencies
   */
  private def randomAdjacencies[J](vv: Vertex[V]) = RandomIterator(vv.adjacencies.iterator)(random)

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
  def apply[V](map: Map[V, Vertex[V]]): VertexMap[V] = new VertexMap(map)

  /**
   * Creates an empty `VertexMap` instance with no vertex mappings.
   *
   * This implementation acts as a convenience method to initialize 
   * an empty `VertexMap` without explicitly passing an empty map.
   *
   * @tparam V the type of the vertex attributes.
   * @return a new `VertexMap` with no vertex mappings.
   */
  def apply[V]: VertexMap[V] = apply(Map.empty[V, Vertex[V]])

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
   * @param edgeList the list of edges representing the graph structure,
   *                 where each edge contains `from` and `to` vertices.
   * @tparam E the type of the edge attribute.
   * @tparam V the type of the vertex attribute.
   * @return a `VertexMap` that maps vertex attributes to their corresponding `Vertex` objects.
   */
  def createFromEdgeList[E, V, Z](edgeList: EdgeList[V, E, Z]): VertexMap[V] = {
    val vvVm: Map[V, Vertex[V]] = edgeList.edges.foldLeft[Map[V, Vertex[V]]](Map.empty[V, Vertex[V]]) {
      (vm, e) =>
        addEdgeToMap(vm)(e)
    }
    VertexMap(vvVm)
  }

  /**
   * Creates a `VertexMap` from the given `EdgeList` by extracting vertices
   * from the edges and constructing a mapping of vertex attributes to their
   * corresponding `Vertex` objects.
   *
   * @param vertexPairList the list of vertex pairs representing the graph structure,
   *                       where each vertex pair contains `from` and `to` vertices.
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
    Option.when(!vertex.discovered)(vertex).map { vv => vv.discovered = true; vv }
  }

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

  /**
   * Adds an edge to the given map of vertices by updating the adjacency list
   * of the vertices at both ends of the edge.
   * If the vertices at the ends of the edge already exist in the map,
   * their adjacencies are updated.
   * Otherwise, new vertices are added to the map with updated adjacency information.
   *
   * @param map the current mapping of vertex attributes to their corresponding `Vertex` instances.
   *            Represents the existing vertices and their connections.
   * @param e   the edge to be added to the map. The edge connects two vertices, `from` and `to`.
   * @tparam V the type of the vertex attributes.
   * @tparam E the type of the edge attributes.
   * @return an updated map of vertex attributes to `Vertex` instances, including the added edge.
   */
  def addEdgeToMap[V, E](map: Map[V, Vertex[V]])(e: Edge[E, V]): Map[V, Vertex[V]] =
    map + (e.from -> (map(e.from) + AdjacencyEdge(e))) + (e.to -> map(e.to))

  /**
   * Adds a pair of vertices to the given map of vertices, updating their adjacency information.
   * If the vertices already exist in the map, the adjacency list of the `from` vertex is updated
   * to include a connection to the `to` vertex.
   * Otherwise, new vertices are added to the map.
   *
   * @param map  the current mapping of vertex attributes to their corresponding `Vertex[V]` instances.
   *             Represents the existing vertices and their connections.
   * @param from the starting vertex of the edge to be added.
   *             Its adjacency list will be updated to include a connection to the `to` vertex.
   * @param to   the destination vertex of the edge to be added.
   *             It is added to the map if it does not already exist.
   * @tparam V the type of the vertex attributes.
   * @return an updated map of vertex attributes to `Vertex[V]` instances, including the updated
   *         adjacency information for the `from` vertex and the `to` vertex.
   */
  def addVertexPairToMap[V](map: Map[V, Vertex[V]])(from: Vertex[V], to: Vertex[V]): Map[V, Vertex[V]] =
    map + (from.attribute -> (from + AdjacencyVertex(to.attribute))) + (to.attribute -> to)
