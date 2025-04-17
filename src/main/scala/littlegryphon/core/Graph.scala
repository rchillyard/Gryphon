package littlegryphon.core

/**
 * A trait representing an abstract graph structure composed of vertices.
 *
 * @tparam V the type representing the attributes associated with the vertices in the graph
 *           V is invariant.
 *
 *           This trait provides a collection of fundamental operations for manipulating and
 *           interacting with graph vertexMap. It allows querying for the set of vertices, adding 
 *           new vertices to the graph, and defining custom behaviors for graph implementations.
 */
trait Graph[V] {
  //  def traverse: Traversal[V]
  def vertexMap: VertexMap[V]

  def addVertex(vertex: Vertex[V]): Graph[V] = unit(vertexMap + vertex)

  def unit(vertexMap: VertexMap[V]): Graph[V]
}

object Graph {
  def addVertexToMap[W](vertexMap: Map[W, Vertex[W]])(vertex: Vertex[W]): Map[W, Vertex[W]] =
    vertexMap + (vertex.attribute -> vertex)
}

/**
 * An abstract base class representing a graph structure composed of vertices.
 * CONSIDER eliminating this class.
 *
 * @tparam V the type representing the attributes associated with the vertices within the graph.
 *           `V` is invariant, ensuring that the specific type of vertex attributes cannot be
 *           changed in different contexts of the same graph.
 *
 *           This class extends the `Graph` trait and serves as a foundation for building graph
 *           implementations. The graph structure is defined and manipulated using its associated
 *           `VertexMap`, which organizes and manages the vertices of the graph.
 * @constructor Creates a new `BaseGraph` instance with the specified `vertexMap`.
 * @param vertexMap the `VertexMap` instance that holds and organizes the vertices of the graph.
 */
abstract class BaseGraph[V](vertexMap: VertexMap[V]) extends Graph[V] {
}

/**
 * A trait representing a graph structure that incorporates both vertices and edges. 
 * This trait extends the capabilities of the `Graph` trait by supporting edge-based operations.
 *
 * @tparam V the type representing the attributes associated with the vertices in the graph.
 * @tparam E the type representing the attributes associated with the edges in the graph.
 */
trait EdgeGraph[V, E] extends Graph[V] {
  //  def edgeTraversal: EdgeTraversal[V, E]
  def edges: Iterable[Edge[E, V]]

  def addEdge(edge: Edge[E, V]): EdgeGraph[V, E]
}
