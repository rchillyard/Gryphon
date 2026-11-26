package com.phasmidsoftware.gryphon.core

/**
 * A trait representing a graph structure defined by its edges.
 * The `Edges` trait provides an abstraction for accessing the
 * sequence of graph edges without requiring direct knowledge of
 * the underlying graph implementation.
 *
 * NOTE that this is not polymorphic in `Z` like `EdgeList`: `pairs` here is fixed to
 * `Seq[(V, V, EdgeType)]`.
 *
 * That stopped mattering in 1.11.0, when #69 moved `EdgeList` — this trait's only implementor
 * — to the test tree, leaving the two no longer a published pair that ought to match. It
 * matters again from 1.13.0, because #135 brought `EdgeList` back into main to be `unbuild`'s
 * target. So the note that stood here, saying nothing in main implements this trait and it was
 * a candidate for removal, is withdrawn: something does, and it is not.
 *
 * Whether the mismatch is worth closing is a real question rather than a rhetorical one.
 * `EdgeList[V, E, Z >: EdgeType]` can serialize to any supertype of `EdgeType`; a caller
 * reaching it through `Edges` gets `EdgeType` and no choice. Nothing exercises the difference
 * today, since every instantiation in the library uses `Z = EdgeType`.
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
