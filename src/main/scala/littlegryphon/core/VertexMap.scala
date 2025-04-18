package littlegryphon.core

import littlegryphon.visit.Visitor

/**
 * Represents a mapping of vertex attributes to their corresponding vertices within
 * a graph.
 * Provides functionality to manage and traverse vertices via depth-first search (DFS).
 *
 * @tparam V the type representing the vertex attributes (`V` is invariant).
 * @param map a mapping from vertex attributes to their associated Vertex instances.
 */
class VertexMap[V](map: Map[V, Vertex[V]]):

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
    VertexMap(map + (vertex.attribute -> vertex))

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
    val result = recursiveDFS(visitor, v)
    result.close()
    result
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
   * @throws littlegryphon.util.GraphException if the vertex is not found in the graph.
   */
  private def recurseOnVertex[J](v: V, visitor: Visitor[V, J]) =
    get(v) match {
      case Some(vv) =>
        vv.adjacencies.iterator.foldLeft(visitor)(recurseOnAdjacency(v))
      case None =>
        throw littlegryphon.util.GraphException(s"DFS logic error 0: recursiveDFS(v = $v)")
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
    findAndMarkVertex({ w => w.discovered = true }, x.vertex, s"DFS logic error 1: findAndMarkVertex(v = $v, x = $x") match {
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
  private def findAndMarkVertex(f: Vertex[V] => Unit, z: Vertex[V], errorMessage: String): Option[V] = {
    val vxo: Option[Vertex[V]] = Option.when(!z.discovered)(z)
    vxo foreach f
    vxo map (_.attribute)
  }

//  /**
//   * Attempts to find a vertex in the given `vertexMap` using the key provided in `maybeV`.
//   * If the vertex is found and has not been previously marked as discovered, it is marked
//   * as such by applying the function `f`. The method then returns the attribute of the vertex
//   * if it was undiscovered. If `maybeV` is `None`, the method simply returns `None`.
//   *
//   * @param vertexMap    a map where keys represent vertex attributes, and values are the
//   *                     corresponding `Vertex` instances.
//   * @param f            a function to apply to the vertex if it is found and not yet marked as discovered.
//   * @param maybeV       an optional attribute used to find the vertex within the map.
//   * @param errorMessage a placeholder string for error messages (unused in the current implementation).
//   * @return an `Option` containing the attribute of the vertex if it was found and undiscovered,
//   *         or the input `maybeV` if it was `None`.
//   */
//  private def findAndMarkVertex(vertexMap: Map[V, Vertex[V]], f: Vertex[V] => Unit, maybeV: Option[V], errorMessage: String): Option[V] = maybeV match {
//    case Some(z) =>
//      val xXvo: Option[Vertex[V]] = vertexMap.get(z) filterNot (_.discovered)
//      xXvo foreach (vertex => f(vertex))
//      xXvo map (_.attribute)
//    case None => maybeV //  NOTE: this is not a problem. // throw GraphException(errorMessage)
//  }


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
  def create[E, V](edgeList: EdgeList[V, E]): VertexMap[V] = {
    val vvVm: Map[V, Vertex[V]] = edgeList.edges.foldLeft[Map[V, Vertex[V]]](Map.empty[V, Vertex[V]]) {
      (vm, e) =>
        // TODO we need to add the appropriate form of Adjacency here.
        // This time, it's an AdjacencyEdge, but other times it might be an AdjacencyVertex.
        val from = e.from + AdjacencyEdge(e)
        val to = e.to
        vm + (from.attribute -> from) + (to.attribute -> to)
    }
    VertexMap(vvVm)
  }
