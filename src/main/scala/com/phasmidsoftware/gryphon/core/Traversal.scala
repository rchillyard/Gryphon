package com.phasmidsoftware.gryphon.core

/**
 * Trait representing a traversal operation on a graph structure, using vertexMap and their adjacencies.
 *
 * @tparam V the type representing the attributes of vertexMap.
 */
trait Traversal[V] {
  def optionalStart: Option[Vertex[V]]

  def preVisited: Seq[Vertex[V]]

  def postVisited: Seq[Vertex[V]]
}

/**
 * Trait representing the traversal of edges in a graph structure. This extends the general `Traversal`
 * functionality by incorporating the ability to access edges of the graph during the traversal process.
 *
 * @tparam V the type representing the attributes of the vertexMap in the graph.
 * @tparam E the type representing the attributes of the edges in the graph.
 */
trait EdgeTraversal[V, E] extends Traversal[V] {
  def edges: Iterable[Edge[E, V]]
}