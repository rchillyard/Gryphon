package littlegryphon.core

/**
 * Hierarchical trait defining the behavior of a connexion.
 *
 * @tparam V the underlying vertex attribute type.
 */
trait Connexion[V] {
  def v1: Vertex[V]

  def v2: Vertex[V]
}

case class VertexPair[V](v1: Vertex[V], v2: Vertex[V]) extends Connexion[V]

//
//case class DirectedEdge[V](v1: Vertex[V], v2: Vertex[V]) extends Connexion[V]
//
//case class DirectedAttributedEdge[V,E](v1: Vertex[V], v2: Vertex[V], attribute: E) extends Connexion[V]
//
//trait AttributedConnexion[V, E] extends Attributed[Connexion[V], E] {
//  extension (e: Connexion[V]) def attribute: E
//}
//
//given Attributed[DirectedAttributedEdge[V,E], E] with
//  extension (c: Connexion[V]) def attribute: E = c.
////given given_AttributedEdge_Int_String
