package littlegryphon.core

/**
 * Hierarchical trait defining the behavior of a connexion.
 *
 * @tparam V the underlying vertex attribute type.
 */
trait Adjacency[V] {
  /**
   * Retrieves the vertex associated with this adjacency.
   *
   * @return the vertex of type `Vertex[V]` linked to this adjacency.
   */
  def vertex: Vertex[V]

  /**
   * Retrieves the `Edge` associated with this adjacency, if one exists.
   * An edge connects two vertices, providing a relationship or linkage between them.
   *
   * @return an `Option[Edge[_, V]]` that contains the `Edge` if present, or `None` if no edge is associated.
   */
  def maybeEdge: Option[Edge[_, V]]
}

/**
 * Represents an adjacency relationship in a graph structure, where the relationship
 * is defined by a direct reference to a vertex.
 *
 * This case class is a concrete implementation of the `Adjacency` trait,
 * encapsulating a single vertex as the adjacency.
 * Instances of this class are typically used to define graph connections between vertices.
 *
 * @param vertex the vertex that this adjacency directly connects to.
 * @tparam V the type representing the attribute of the vertex.
 */
case class AdjacencyVertex[V](vertex: Vertex[V]) extends Adjacency[V]:
  /**
   * Retrieves an optional `Edge` associated with this adjacency.
   * The edge, if present, represents a connection between two vertices
   * in the graph.
   * This method allows an adjacency to optionally include
   * a direct reference to its associated edge within the graph structure.
   *
   * @return an `Option` containing the `Edge` if present, or `None` if no edge is associated.
   */
  def maybeEdge: Option[Edge[_, V]] = None

/**
 * Represents an Adjacency in the form of an edge within a graph structure.
 * In the case of edge being an undirected edge, the flipped member is used to 
 * represent which direction of the edge is used for this particular adjacency.
 *
 * @tparam V the type of the vertex attributes associated with the edge.
 * @tparam E the type of the edge attributes.
 * @param edge    the edge instance, containing the vertex connection and attribute information.
 * @param flipped an optional flag indicating if the directionality of the edge is reversed
 *                (only relevant when the edge is undirected).
 */
case class AdjacencyEdge[V, E](edge: Edge[E, V], flipped: Boolean = false) extends Adjacency[V] {
  def vertex: Vertex[V] = if (flipped) edge.from else edge.to

  def maybeEdge: Option[Edge[_, V]] = Some(edge)
}

//
//case class DirectedEdge[V](vertex: Vertex[V], v2: Vertex[V]) extends Adjacency[V]
//
//case class DirectedAttributedEdge[V,E](vertex: Vertex[V], v2: Vertex[V], attribute: E) extends Adjacency[V]
//
//trait AttributedConnexion[V, E] extends Attributed[Adjacency[V], E] {
//  extension (e: Adjacency[V]) def attribute: E
//}
//
//given Attributed[DirectedAttributedEdge[V,E], E] with
//  extension (c: Adjacency[V]) def attribute: E = c.
////given given_AttributedEdge_Int_String
