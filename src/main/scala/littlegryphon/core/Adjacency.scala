package littlegryphon.core

/**
 * Hierarchical trait defining the behavior of a connexion.
 *
 * @tparam V the underlying vertex attribute type.
 */
trait Adjacency[V] {
  def vertex: Vertex[V]
}

case class AdjacencyVertex[V](vertex: Vertex[V]) extends Adjacency[V]

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
