package littlegryphon.core

/**
 * Represents a vertex in a graph structure with an associated attribute and a collection
 * of adjacencies.
 * Each vertex can optionally be marked as discovered during traversal,
 * which is useful for graph algorithms.
 *
 * @tparam V the type representing the attribute of the vertex (invariant).
 * @param attribute   the data or value associated with this vertex.
 * @param adjacencies an unordered collection of adjacencies representing the connections
 *                    of this vertex to others in the graph.
 * @param discovered  an optional flag indicating whether this vertex has been discovered 
 *                    in a traversal.
 *                    Defaults to `false`.
 */
case class Vertex[V](attribute: V, adjacencies: Unordered[Adjacency[V]])(var discovered: Boolean = false) extends Attribute[V] {
  /**
   * Adds a new adjacency to the current vertex and returns a new vertex instance with the
   * added adjacency included in the collection of adjacencies.
   *
   * @param a the adjacency to be added to the vertex.
   *          This adjacency represents a connection to another vertex in the graph structure.
   * @return a new `Vertex[V]` instance with the same attribute and discovered state as the
   *         current vertex, but with the specified adjacency added to the adjacencies.
   */
  def +(a: Adjacency[V]): Vertex[V] = Vertex(attribute, adjacencies + a)(discovered)

  /**
   * Mutating method that resets the `discovered` state of this `Vertex` to `false`.
   *
   * @return Unit
   */
  def reset(): Unit = {
    discovered = false
  }

  override def toString: String = s"v$attribute"
}

/**
 * Companion object for the `Vertex` class, providing a factory method for creating
 * instances of `Vertex`.
 * Designed to construct a vertex with an associated attribute
 * and a dynamically initialized collection of adjacencies.
 */
object Vertex:
  /**
   * Creates a new `Vertex` instance with the specified attribute and a dynamically
   * initialized collection of adjacencies.
   *
   * @param f         a function that provides the initial unordered collection of adjacencies
   *                  for the vertex.
   *                  This allows for dynamic initialization of adjacencies when the vertex is created.
   * @param attribute the attribute associated with the vertex, representing its data or value.
   * @return a new `Vertex[V]` instance containing the specified attribute and the
   *         dynamically created collection of adjacencies.
   */
  def create[V](f: () => Unordered[Adjacency[V]])(attribute: V): Vertex[V] = new Vertex[V](attribute, f())()

  /**
   * Creates a new `Vertex` instance with the specified attribute and an empty
   * collection of adjacencies initialized as an `Unordered_Bag`.
   *
   * This method is a simplified factory for creating a vertex where
   * the adjacency collection is designed to be empty initially.
   *
   * @param attribute the attribute associated with the vertex, representing its data or value.
   * @return a new `Vertex[V]` instance with the specified attribute and an empty
   *         collection of adjacencies.
   */
  def createByVertex[V](attribute: V): Vertex[V] = create(() => Unordered_Bag.empty[AdjacencyVertex[V]])(attribute)

  /**
   * Creates a new `Vertex` instance with the specified attribute and an empty
   * collection of adjacencies initialized as an `Unordered_Bag` specifically
   * designed to hold `AdjacencyEdge` elements.
   *
   * This method serves as a factory for creating a vertex where the adjacency
   * collection is tailored for edges, allowing the representation of connections
   * between vertices in the form of edges.
   *
   * @param attribute the attribute associated with the vertex, representing its data or value.
   * @tparam V the type representing the attribute of the vertex.
   * @tparam E the type of edge attributes associated with the adjacencies.
   * @return a new `Vertex[V]` instance with the specified attribute and an empty
   *         collection of `AdjacencyEdge[V, E]`.
   */
  def createByEdge[V, E](attribute: V): Vertex[V] = create(() => Unordered_Bag.empty[AdjacencyEdge[V, E]])(attribute)

