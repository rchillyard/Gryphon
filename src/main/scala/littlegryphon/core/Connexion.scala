package littlegryphon.core

/**
 * Hierarchical trait defining the behavior of a connexion between two nodes (vertices) of a graph.
 *
 * And yes, I do know how to spell Connexion.
 * Admittedly, it's an archaic form of spelling but I like it.
 *
 * @tparam V the underlying attribute type.
 * @tparam X the container type for the target of this Connexion.
 */
trait Connexion[V, X[_]] {
  /**
   * The attribute type for the start of this Connexion.
   *
   * @return an instance of V.
   */
  def v1: V

  def v2: X[V]
}

/**
 * Concrete Connexion type that simply connects two nodes (directed) without any additional attributes.
 *
 * NOTE when comparing vertices (v2) for equality for example, we do not consider connexions nor discovered.
 *
 * @param v1 the attribute of the starting node.
 * @param v2 the container (Vertex) of the ending node.
 * @tparam V the underlying node attribute type.
 */
case class Pair[V](v1: V, v2: Vertex[V]) extends Connexion[V, Vertex]

/**
 * Concrete Connexion type that connects two nodes (directed) with an additional attributes.
 *
 * NOTE when comparing vertices (v2) for equality for example, we do not consider connexions nor discovered.
 *
 * @param v1        the attribute of the starting node.
 * @param v2        the container (Vertex) of the ending node.
 * @param attribute the edge attribute, for example, a weight or cost of traversal.
 * @tparam V the underlying node attribute type.
 */
case class Edge[V, E](v1: V, v2: Vertex[V], attribute: E) extends Connexion[V, Vertex] with Attribute[E]
