package com.phasmidsoftware.gryphon.core

/**
 * A trait representing a graph structure defined by its edges.
 * The `Edges` trait provides an abstraction for accessing the
 * sequence of graph edges without requiring direct knowledge of
 * the underlying graph implementation.
 *
 * NOTE: this is not polymorphic in Z like EdgeList
 * CONSIDER making them similar.
 *
 * @tparam V the type of attributes associated with the vertices in the graph.
 * @tparam E the type of attributes associated with the edges in the graph.
 */
trait Edges[V, E] extends Pairs[V, EdgeType] {

  /**
   * Retrieves a sequence of edges in the graph.
   *
   * @return a `Seq` containing all edges in the graph. 
   *         Each edge connects two vertices and may carry an associated attribute.
   */
  def edges: Seq[Edge[V, E]]

  /**
   * Retrieves a sequence of pairs of vertices representing the connections in the graph.
   * Each pair corresponds to an edge in the graph, where the first element is the starting vertex
   * and the second element is the ending vertex of the edge.
   *
   * @return a sequence of vertex pairs `(V, V)` representing the connections in the graph. Each pair
   *         corresponds to the `white` and `black` vertices of an edge.
   */
  def pairs: Seq[(V, V, EdgeType)] =
    edges.map(e => (e.white, e.black, e.edgeType))
}
