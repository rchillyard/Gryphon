package com.phasmidsoftware.gryphon.core

/**
 * Hierarchical trait defining the behavior of an "adjacency."
 * An adjacency is a relationship between two adjacent vertices in a graph structure.
 * It might be based simply on a reference to a vertex, or it might be based on a reference to an edge.
 *
 * Adjacencies are the fundamental building blocks of a graph structure because from any given vertex,
 * it is only possible to traverse to the vertices that are adjacent to it.
 *
 * Adjacencies are represented by the Adjacency trait, which defines the behavior of an adjacency.
 * The Adjacency trait is an abstract type, which means that it cannot be instantiated directly.
 * Instead, it is used as a type parameter for other concrete implementations of the Adjacency trait.
 *
 * The Adjacency trait provides a common interface for accessing the vertex associated with an adjacency.
 * It also provides a method for retrieving the edge if one exists.
 *
 * The Adjacency trait is used to define the behavior of the AdjacencyVertex and AdjacencyEdge classes.
 *
 * @tparam V the underlying vertex attribute type.
 */
trait Adjacency[V] extends Discoverable[V] {
  /**
   * Retrieves the vertex associated with this adjacency.
   *
   * @return the vertex of type `V` that this adjacency points to.
   */
  def vertex: V

  /**
   * Retrieves the `Edge` associated with this adjacency, if one exists.
   * An edge connects two vertices, providing a relationship or linkage between them.
   *
   * @return an `Option[Edge[_, V]]` that contains the `Edge` if present, or `None` if no edge is associated.
   */
  def maybeEdge[E]: Option[Edge[E, V]]
}

/**
 * Represents a specific type of adjacency in a graph structure
 * where the adjacency is defined purely by a vertex.
 *
 * The `AdjacencyVertex` serves as a minimal form of adjacency, representing a connection
 * to another vertex without any associated edge. This can be useful in scenarios
 * where the graph structure requires lightweight adjacency relationships or where
 * edges are not explicitly modeled.
 *
 * @param vertex the vertex associated with this adjacency.
 *               This defines the connection point within the graph structure.
 * @tparam V the type of the vertex attribute.
 */
case class AdjacencyVertex[V](vertex: V) extends AbstractAdjacency[V]:
  /**
   * Retrieves an optional `Edge` associated with this adjacency.
   * The edge, if present, represents a connection between two vertices
   * in the graph.
   * This method allows an adjacency to optionally include
   * a direct reference to its associated edge within the graph structure.
   *
   * @return an `Option` containing the `Edge` if present, or `None` if no edge is associated.
   */
  def maybeEdge[E]: Option[Edge[E, V]] = None

/**
 * Companion object for the `AdjacencyVertex` class.
 *
 * Provides a factory method to construct an `AdjacencyVertex` instance
 * from a given `Vertex` instance.
 */
object AdjacencyVertex:
  /**
   * Constructs a new `AdjacencyVertex` from the provided `Vertex`.
   *
   * @param vertex the `Vertex` instance from which the `AdjacencyVertex` is created.
   *               The `attribute` of the `Vertex` is used as the parameter for the `AdjacencyVertex`.
   * @return an `AdjacencyVertex` containing the `attribute` of the given `Vertex`.
   */
  def apply[V](vertex: Vertex[V]): AdjacencyVertex[V] =
    AdjacencyVertex(vertex.attribute)

/**
 * Represents an Adjacency in the form of an edge within a graph structure.
 * In the case of an edge being an undirected edge, the flipped member is used to
 * represent which direction of the edge is used for this particular adjacency.
 *
 * @tparam V the type of the vertex attributes associated with the edge.
 * @tparam E the type of the edge attributes.
 * @param connexion the edge instance, containing the vertex connection and attribute information.
 * @param flipped   an optional flag indicating if the directionality of the edge is reversed
 *                  (only relevant when the edge is undirected).
 */
case class AdjacencyEdge[V, E](connexion: Connexion[V], flipped: Boolean = false) extends AbstractAdjacency[V] {
  /**
   * Determines the vertex associated with this adjacency based on the nominal direction of the given edge.
   * If the edge is flipped, the originating vertex (`white`) is returned.
   * Otherwise, the terminating vertex (`black`) is returned.
   *
   * @return the vertex of type `V` associated with this adjacency, determined by the value of `flipped`.
   */
  def vertex: V =
    if (flipped) connexion.white else connexion.black

  /**
   * Retrieves an optional instance of the edge associated with this adjacency.
   *
   * @return an `Option` containing the connexion if it is an edge; `None` otherwise.
   */
  def maybeEdge[E2]: Option[Edge[E2, V]] = connexion match {
    case e: Edge[E2, V] =>
      Some(e)
    case _ =>
      None
  }
}

/**
 * An abstract class representing a base implementation of the `Adjacency` trait for graph structures.
 *
 * This class introduces a `discovered` property that tracks whether the vertex associated with this adjacency
 * has been visited or not during graph traversal. The `discovered` state can be manipulated through mutating methods
 * and queried as needed.
 *
 * @tparam V the type of the vertex attribute associated with this adjacency.
 * @constructor Creates a new instance with an initial `discovered` state, which defaults to `false`.
 *
 *              The core functionality of this class includes:
 *              - Managing the discovery state of a vertex.
 *              - Providing methods for resetting, marking, and querying the discovery state.
 */
abstract class AbstractAdjacency[V](var discovered: Boolean = false) extends Adjacency[V] {

  /**
   * Mutating method that resets the `discovered` state of this `Vertex` to `false`.
   *
   * @return Unit
   */
  def reset(): Adjacency[V] = {
    discovered = false
    this
  }

  /**
   * Marks the current vertex as discovered by setting its internal `discovered` state to `true`.
   * This operates by side effect.
   *
   * @return the current vertex (`Vertex[V]`) instance with its `discovered` state updated.
   */
  def discover(): Adjacency[V] = {
    discovered = true
    this
  }

  /**
   * Checks if the vertex is not discovered.
   * NOTE that this must remain a def (not a lazy val) because the `discovered` property is mutable.
   *
   * This method inversely reflects the `discovered` property of a vertex.
   * If a vertex is marked as not discovered, this method returns `true`.
   *
   * @return `true` if the vertex has not been discovered, `false` otherwise.
   */
  def undiscovered: Boolean = !discovered
}

/**
 * Type alias to represent a collection of adjacencies for a given vertex type.
 *
 * An adjacency defines the relationship between two vertices in a graph, while
 * this type alias represents an unordered collection of such adjacencies for
 * all vertices, enabling graph traversal and exploration.
 *
 * NOTE that when an AdjacencyEdge is used, we can place it into a set because
 * we will not (typically) have duplicate edges between vertices.
 * However, when an AdjacencyVertex is used, we can't place it into a set because
 * we may have duplicate vertices.
 * Therefore, we typically use an Unordered_Bag for AdjacencyVertex, and an Unordered_Set for AdjacencyEdge.
 *
 * NOTE I'm beginning to really like Scala 3!
 * From here on, in this module, the code would not have been possible in Scala 2.13.
 *
 * V: the type of the vertex attribute.
 */
type Adjacencies[V] = Unordered[Adjacency[V]]

/**
 * Creates an empty `Adjacencies` instance for storing adjacency relationships
 * between vertices within a graph structure. This collection is initially empty and
 * can be used as a starting point for adding adjacencies.
 *
 * @tparam V the type of vertices associated with the adjacencies.
 * @return an empty instance of `Adjacencies[V]`, represented as an unordered bag
 *         containing no adjacency elements.
 */
def emptyAdjacenciesBag[V]: Adjacencies[V] =
  Unordered_Bag.empty[Adjacency[V]]

/**
 * Creates an empty set of adjacencies for a graph.
 *
 * This method initializes and returns an empty `Adjacencies[V]`, representing a collection
 * containing no adjacency elements. It serves as a starting point for constructing or
 * managing adjacency relationships in a graph.
 *
 * @tparam V the type of the vertex attribute associated with the adjacency.
 * @return an empty instance of `Adjacencies[V]`.
 */
def emptyAdjacenciesSet[V]: Adjacencies[V] =
  Unordered_Set.empty[Adjacency[V]]
