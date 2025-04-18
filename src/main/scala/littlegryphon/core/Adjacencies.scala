//package littlegryphon.core
//
//import littlegryphon.visit.Visitor
//
//
///**
// * A case class representing the adjacency map of a graph.
// * It maps vertices of type `V` to a bag of adjacency relations,
// * where each adjacency points to other vertexMap.
// * This is useful for representing graph structures and performing graph traversal algorithms.
// *
// * @param map a map where each vertex of type `V` is associated with an `Unordered` of adjacencies.
// * @tparam V the type representing the vertexMap in the graph.
// */
//class Adjacencies[V](map: Map[V, Unordered[Adjacency[V]]]) {
//
//  /**
//   * Method to run depth-first-search on this `Adjacencies`.
//   *
//   * @param visitor the visitor, of type `Visitor[V, J]`.
//   * @param v       the starting vertex.
//   * @tparam J the journal type.
//   * @return a new `Visitor[V, J]`.
//   */
//  def dfs[J](visitor: Visitor[V, J])(v: V): Visitor[V, J] = {
//    initializeVisits(Some(v))
//    val result = recursiveDFS(visitor, v)
//    result.close()
//    result
//  }
//
//  /**
//   * Retrieves the value associated with the specified key, if it exists.
//   *
//   * @param key the key whose associated value is to be returned
//   * @return an Option containing the associated value wrapped in Unordered[Adjacency[V]\]
//   *         if the key exists, or None if the key is not present
//   */
//  def get(key: V): Option[Unordered[Adjacency[V]]] = map.get(key)
//
//  /**
//   * Retrieves the value associated with the specified key in the map.
//   * If the key is not present, returns the provided default value.
//   *
//   * @param key     the key for which the value is to be retrieved
//   * @param default the default value to return if the key is not found in the map
//   * @return the value associated with the specified key, or the provided default value if the key is not present
//   */
//  def getOrElse[U >: Unordered[Adjacency[V]]](key: V, default: => U): U = map.getOrElse(key, default)
//
//  /**
//   * Retrieves the adjacency list associated with the given key in the unordered map.
//   *
//   * @param key the key for which the adjacency list is to be retrieved
//   * @return the unordered adjacency list corresponding to the specified key
//   * @throws NoSuchElementException if the specified key is not present in the map
//   */
//  @throws[NoSuchElementException]
//  def apply(key: V): Unordered[Adjacency[V]] = map.apply(key)
//
//  /**
//   * Attempts to apply this map's function to the provided key `x`.
//   If the key is not present
//   * in the map, applies the `default` function instead.
//   *
//   * @param x       the key to process, which must be a subtype of `V`.
//   * @param default a fallback function that computes a result of type `U` for keys not found in the map.
//   * @return the result of the map's function on the key `x`, or the result of applying `default`
//   *         to the key if it is not found in the map.
//   */
//  def applyOrElse[K1 <: V, U >: Unordered[Adjacency[V]]](x: K1, default: K1 => U): U = map.applyOrElse(x, default)
//
//  /**
//   * Adds a key-value pair to the existing map, returning a new map with the updated pair.
//   *
//   * @param kv a tuple containing the key of type `V` and the value of type `U` to be added to the map.
//   *           `U` must be a supertype of `Unordered[Adjacency[V]]`.
//   * @return a new map of type `Map[V, U]` with the provided key-value pair added. If the key already
//   *         exists in the map, the value associated with the key will be updated.
//   */
//  def +[U >: Unordered[Adjacency[V]]](kv: (V, U)): Map[V, U] = map.+(kv)
//
//  /**
//   * Adds an edge to the current adjacencies structure, associating it with its starting vertex.
//   * The method updates the existing adjacencies by including a new adjacency representing the edge.
//   *
//   * @param edge the edge to be added, which contains the starting vertex, ending vertex, and associated attributes.
//   * @tparam E the type of the attribute associated with the edge.
//   * @return a new instance of `Adjacencies[V]` with the updated edge included.
//   */
//  def addEdge[E](edge: Edge[E, V]): Adjacencies[V] = {
//    val v1: Vertex[V] = edge.from
//    val a: Unordered[Adjacency[V]] = this (v1.attribute)
//    val adjEdge: Adjacency[V] = AdjacencyEdge(edge)
//    Adjacencies(map + (v1.attribute -> (a + adjEdge)))
//  }
//
//  /**
//   * Adds multiple edges to this `Adjacencies`.
//   * Iteratively invokes the `addEdge` method for each edge in the provided sequence.
//   *
//   * @param edges a sequence of `Edge` instances, where each edge connects two vertices and carries
//   *              an associated attribute.
//   * @tparam E the type representing the attributes of the edges.
//   * @return a new instance of `Adjacencies[V]` containing the updated set of adjacencies that
//   *         include the specified edges.
//   */
//  def addEdges[E](edges: Seq[Edge[E, V]]): Adjacencies[V] = {
//    edges.foldLeft(this) {
//      case (adj, edge) => adj.addEdge(edge)
//    }
//  }
//
//
//  def keySet: Set[V] = map.keySet
//
//  def adjacencies: Iterable[Adjacency[V]] = map.values.flatMap(_.iterator)
//
//  private def initializeVisits[J](vo: Option[V]): Unit = {
//    //    vertexMap.values foreach (_.reset())
//    //    Adjacencies.findAndMarkVertex(vertexMap, { _: Vertex[V, X, P] => () }, vo, s"initializeVisits")
//  }
//
//  /**
//   * Non-tail-recursive method to run DFS on the vertex V with the given Visitor.
//   *
//   * @param visitor the Visitor[V, J].
//   * @param v       the vertex at which we run depth-first-search.
//   * @tparam J the Journal type of the Visitor.
//   * @return a new Visitor[V, J].
//   */
//  private def recursiveDFS[J](visitor: Visitor[V, J], v: V): Visitor[V, J] =
//    recurseOnVertex(v, visitor.visitPre(v)).visitPost(v)
//
//  private def recurseOnVertex[J](v: V, visitor: Visitor[V, J]) =
//    get(v) match {
//      case Some(xa) => xa.iterator.foldLeft(visitor)((q, x) => recurseOnConnexion(v, q, x))
//      case None => throw littlegryphon.util.GraphException(s"DFS logic error 0: recursiveDFS(v = $v)")
//    }
//
//  private def recurseOnConnexion[J](v: V, visitor: Visitor[V, J], x: Adjacency[V]): Visitor[V, J] = {
//    //    s"recurseOnEdgeX: $v, $x" !!
//    findAndMarkVertex({ w => w.discovered = true }, x.vertex, s"DFS logic error 1: findAndMarkVertex(v = $v, x = $x") match {
//      case Some(z) => recursiveDFS(visitor, z)
//      case None => visitor
//    }
//  }
//
//  private def findAndMarkVertex(f: Vertex[V] => Unit, z: Vertex[V], errorMessage: String): Option[V] = {
//    val xXvo: Option[Vertex[V]] = Option.when(z.discovered)(z)
//    xXvo foreach f
//    xXvo map (_.attribute)
//  }
//}
//
///**
// * Utility object providing methods to construct and manipulate instances of the `Adjacencies` class.
// *
// * The `Adjacencies` object serves as a factory and helper for working with adjacency maps,
// * which represent graph structures as maps associating vertices
// * with unordered collections of their adjacent vertices.
// */
//object Adjacencies {
//  def apply[V](map: Map[V, Unordered[Adjacency[V]]]): Adjacencies[V] = new Adjacencies(map)
//
//  def empty[V]: Adjacencies[V] = Adjacencies(Map.empty)
//}
