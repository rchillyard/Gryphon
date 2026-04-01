package com.phasmidsoftware.gryphon.core

/**
 * Hierarchical trait defining the behavior of an "adjacency."
 * An adjacency is a relationship between two adjacent vertices in a graph structure.
 *
 * Adjacencies are the fundamental building blocks of a graph structure because from any given vertex,
 * it is only possible to traverse to the vertices that are adjacent to it.
 *
 * The `discovered` flag has been removed from `Adjacency` and its implementations.
 * Visited-node tracking is now handled by the immutable `VisitedSet[V]` typeclass
 * inside the Visitor V1.2.0 traversal engine.
 *
 * @tparam V the underlying vertex attribute type.
 */
trait Adjacency[V] {
  /**
   * Retrieves the vertex associated with this adjacency.
   *
   * @return the vertex of type `V` that this adjacency points to.
   */
  def vertex: V

  /**
   * Retrieves the `Edge` associated with this adjacency, if one exists.
   *
   * @return an `Option[Edge[E, V]]` containing the Edge if present, or None.
   */
  def maybeEdge[E]: Option[Edge[E, V]]
}

/**
 * Represents an adjacency defined purely by a vertex (no associated edge).
 *
 * @param vertex the vertex associated with this adjacency.
 * @tparam V the type of the vertex attribute.
 */
case class AdjacencyVertex[V](vertex: V) extends Adjacency[V]:
  def maybeEdge[E]: Option[Edge[E, V]] = None

/**
 * Companion object for `AdjacencyVertex`.
 */
object AdjacencyVertex:
  /**
   * Constructs an `AdjacencyVertex` from a `Vertex` instance.
   */
  def apply[V](vertex: Vertex[V]): AdjacencyVertex[V] =
    AdjacencyVertex(vertex.attribute)

/**
 * Represents an adjacency in the form of an edge within a graph structure.
 * In the case of an undirected edge, `flipped` indicates which direction is used.
 *
 * @param connexion the edge instance containing vertex connection and attribute information.
 * @param flipped   true if the nominal direction of the edge is reversed.
 * @tparam V the type of the vertex attributes.
 * @tparam E the type of the edge attributes.
 */
case class AdjacencyEdge[V, E](connexion: Connexion[V], flipped: Boolean = false) extends Adjacency[V]:
  /**
   * Returns the destination vertex for this adjacency.
   * If `flipped`, returns `white`; otherwise `black`.
   */
  def vertex: V =
    if flipped then connexion.white else connexion.black

  /**
   * Returns the edge if the connexion is an Edge instance.
   */
  def maybeEdge[E2]: Option[Edge[E2, V]] = connexion match
    case e: Edge[E2, V] @unchecked =>
      Some(e)
    case _ =>
      None

/**
 * Type alias for an unordered collection of adjacencies for a given vertex type.
 *
 * NOTE: when `AdjacencyEdge` is used, an `Unordered_Set` is appropriate (no duplicate edges).
 * When `AdjacencyVertex` is used, use `Unordered_Bag` (duplicate vertices are possible).
 *
 * @tparam V the type of the vertex attribute.
 */
type Adjacencies[V] = Unordered[Adjacency[V]]

/**
 * Creates an empty `Adjacencies` instance backed by an `Unordered_Bag`.
 */
def emptyAdjacenciesBag[V]: Adjacencies[V] =
  Unordered_Bag.empty[Adjacency[V]]

/**
 * Creates an empty `Adjacencies` instance backed by an `Unordered_Set`.
 */
def emptyAdjacenciesSet[V]: Adjacencies[V] =
  Unordered_Set.empty[Adjacency[V]]