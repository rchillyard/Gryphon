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

  private def initializeVisits[J](vo: Option[V]): Unit = {
    //    vertexMap.values foreach (_.reset())
    //    Adjacencies.findAndMarkVertex(vertexMap, { _: Vertex[V, X, P] => () }, vo, s"initializeVisits")
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

  private def recurseOnVertex[J](v: V, visitor: Visitor[V, J]) =
    get(v) match {
      case Some(vv) => vv.adjacencies.iterator.foldLeft(visitor)((q, x) => recurseOnConnexion(v, q, x))
      case None => throw littlegryphon.util.GraphException(s"DFS logic error 0: recursiveDFS(v = $v)")
    }

  private def recurseOnConnexion[J](v: V, visitor: Visitor[V, J], x: Adjacency[V]): Visitor[V, J] = {
    //    s"recurseOnEdgeX: $v, $x" !!
    findAndMarkVertex({ w => w.discovered = true }, x.vertex, s"DFS logic error 1: findAndMarkVertex(v = $v, x = $x") match {
      case Some(z) => recursiveDFS(visitor, z)
      case None => visitor
    }
  }

  private def findAndMarkVertex(f: Vertex[V] => Unit, z: Vertex[V], errorMessage: String): Option[V] = {
    val xXvo: Option[Vertex[V]] = Option.when(z.discovered)(z)
    xXvo foreach f
    xXvo map (_.attribute)
  }
