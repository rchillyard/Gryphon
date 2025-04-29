package com.phasmidsoftware.gryphon.core

// NOTE backward imports

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
   * Returns a string representation of the object.
   *
   * @return a string representation of the map.
   */
  override def toString: String = map.toString

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
   * @param v         the vertex whose unvisited adjacent vertices are to be enqueued
   * @param queue     the queue into which the unvisited vertices will be appended
   * @param queueable the implicit type class to handle the behavior of the queue-like object
   * @return a new queue containing the original elements and the newly enqueued vertices
   * @throws GraphException if the vertex `v` does not exist in the adjacency list
   */
  private def enqueueUnvisitedVertices[Q](v: V, queue: Q)(implicit queueable: Queueable[Q, V]): Q = optAdjacencyList(v) match {
    case Some(vau: Unordered[Adjacency[V]]) =>
      vau.iterator.foldLeft(queue) {
        (q, va) =>
          if (!va.vertex.discovered) {
            va.vertex.discovered = true
            queueable.append(q, va.vertex.attribute)
          }
          else
            q
      }
    case None => throw GraphException(s"BFS logic error 0: enqueueUnvisitedVertices(v = $v)")
  }


  /**
   * Retrieves the list of adjacencies for a given vertex if it exists in the graph.
   * This method searches `vxVm` for the specified vertex and, if found,
   * returns its associated adjacencies wrapped in an `Option`.
   *
   * @param v the vertex identifier for which adjacencies are to be retrieved.
   * @return an `Option` containing the unordered collection of adjacencies for the given vertex,
   *         or `None` if the vertex does not exist in the graph.
   */
  private def optAdjacencyList(v: V): Option[Unordered[Adjacency[V]]] = map.get(v) map (_.adjacencies)

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
  def getOrElse[U >: Vertex[V]](key: V, default: => U): U = map.getOrElse(key, default)

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
   * Applies the function `default` to the provided value `x` if `x` is not defined in the map, 
   * otherwise applies the function defined for `x` in the map.
   *
   * @param x       the key for which an associated value is being retrieved or computed using `default`.
   * @param default a function to compute a fallback value if `x` does not exist in the map.
   * @return the value associated with `x` in the map, or the result of `default(x)` if no such association exists.
   */
  def applyOrElse[K1 <: V, U >: Vertex[V]](x: K1, default: K1 => U): U = map.applyOrElse(x, default)

  /**
   * Retrieves the set of keys (attributes of vertices) present in the `VertexMap`.
   *
   * @return a `Set` containing the keys associated with the vertices in the `VertexMap`.
   */
  def keySet: Set[V] = map.keySet

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
    get(v) match {
      case Some(vv) =>
        // CONSIDER just using `iterator` because the iterator of `adjacencies` may already be randomized.
        val iterator: Iterator[Adjacency[V]] = RandomIterator(vv.adjacencies.iterator)(random)
        iterator.foldLeft(visitor)(recurseOnAdjacency(v))
      case None =>
        throw util.GraphException(s"DFS logic error 0: recursiveDFS(v = $v)")
    }

  /**
   * Applies depth-first search logic on a given adjacency, marking the associated vertex as discovered
   * and invoking recursive processing if the vertex is found.
   *
   * @param v       the current vertex attribute being processed.
   * @param visitor the visitor instance to be used for traversal logic.
   * @param x       the adjacency containing the vertex to process.
   * @tparam J the type of the visitor's journal.
   * @return an updated Visitor[V, J] after processing the given adjacency and its contained vertex.
   */
  private def recurseOnAdjacency[J](v: V)(visitor: Visitor[V, J], x: Adjacency[V]): Visitor[V, J] =
    findAndMarkVertex(x.vertex, s"DFS logic error 1: findAndMarkVertex(v = $v, x = $x") { w => w.discovered = true } match {
      case Some(z) =>
        recursiveDFS(visitor, z)
      case None =>
        visitor
    }

  /**
   * Searches for the given vertex to check if it is undiscovered, marks it as discovered,
   * and applies the provided function to it.
   * If the vertex is discovered during the process, its attribute is returned.
   *
   * @param f            a function to be applied to the vertex if it is not yet discovered.
   * @param z            the vertex to be checked and marked.
   * @param errorMessage an error message to indicate any expected issue (not used directly in this method).
   * @return an `Option` containing the attribute of the vertex if it was undiscovered,
   *         otherwise `None`.
   */
  private def findAndMarkVertex(z: Vertex[V], errorMessage: String)(f: Vertex[V] => Unit): Option[V] = {
    val vxo: Option[Vertex[V]] = Option.when(!z.discovered)(z)
    vxo foreach f
    vxo map (_.attribute)
  }

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

  def createFromTriplets[V, E](f: Triplet[V, E] => (Vertex[V], Vertex[V]))(triplets: Triplets[V, E]): VertexMap[V] = {
    val vvVm: Map[V, Vertex[V]] = triplets.triplets.foldLeft[Map[V, Vertex[V]]](Map.empty[V, Vertex[V]]) {
      (vm, t) =>
        val (vv1, vv2) = f(t)
        vm + (t._1 -> vv1) + (t._2 -> vv2)
    }
    VertexMap(vvVm)
  }

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
  def createFromEdgeList[E, V](edgeList: EdgeList[V, E]): VertexMap[V] = {
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
  def createFromVertexPairList[V](vertexPairList: VertexPairList[V]): VertexMap[V] = {
    val vvVm: Map[V, Vertex[V]] = vertexPairList.pairs.foldLeft[Map[V, Vertex[V]]](Map.empty[V, Vertex[V]]) {
      (vm, pair) =>
        val function = addVertexPairToMap(vm)
        function.tupled(pair)
    }
    VertexMap(vvVm)
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
  private def addEdgeToMap[V, E](map: Map[V, Vertex[V]])(e: Edge[E, V]): Map[V, Vertex[V]] =
    map + (e.from.attribute -> (e.from + AdjacencyEdge(e))) + (e.to.attribute -> e.to)

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
  private def addVertexPairToMap[V](map: Map[V, Vertex[V]])(from: Vertex[V], to: Vertex[V]): Map[V, Vertex[V]] =
    map + (from.attribute -> (from + AdjacencyVertex(to))) + (to.attribute -> to)
