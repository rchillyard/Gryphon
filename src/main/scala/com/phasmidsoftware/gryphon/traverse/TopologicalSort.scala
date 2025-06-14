package com.phasmidsoftware.gryphon.traverse

import com.phasmidsoftware.gryphon.adjunct.DirectedGraph
import com.phasmidsoftware.gryphon.visit.{Journal, PostVisitor, Visitor}

/**
 * Provides functionality for topological sorting of directed graphs.
 */
object TopologicalSort {

  /**
   * Performs a topological sort on the given directed graph.
   *
   * The method uses a depth-first search (DFS) traversal to visit all vertices of the graph.
   * It collects vertices in reverse postorder using a specialized `PostVisitor`, ensuring
   * that every vertex appears before its successors in the resulting sequence.
   *
   * @param graph the directed graph to be sorted. It contains vertices and directed edges
   *              representing dependencies or relations between vertices.
   *              Vertices are of type `V`, and edges have attributes of type `E`.
   * @param ev    an implicit `Journal` defining how to record and manage the visitation order
   *              of vertices in the graph during the traversal. The journal accumulates
   *              the vertices in a specific order.
   * @return a sequence of vertices (`Seq[V]`) representing the topologically sorted order
   *         of the graph's vertices.
   */
  def sort[V, E](graph: DirectedGraph[V, E])(implicit ev: Journal[List[V], V]): Seq[V] = {
    val visitor: PostVisitor[V, List[V]] = Visitor.reversePost[V]
    val result = graph.dfsAll(visitor)
    // CONSIDER checking that the edges are properly aligned
    result.journals.head
  }

  /**
   * Performs a topological sort on the given directed graph.
   *
   * The method uses a depth-first search (DFS) traversal to visit all vertices of the graph.
   * It collects vertices in reverse postorder using a specialized `PostVisitor`, ensuring
   * that every vertex appears before its successors in the resulting sequence.
   *
   * @param graph the directed graph to be sorted. It contains vertices and directed edges
   *              representing dependencies or relations between vertices.
   *              Vertices are of type `V`, and edges have attributes of type `E`.
   * @param ev    an implicit `Journal` defining how to record and manage the visitation order
   *              of vertices in the graph during the traversal. The journal accumulates
   *              the vertices in a specific order.
   * @return a sequence of vertices (`Seq[V]`) representing the topologically sorted order
   *         of the graph's vertices.
   */
  def traversal[V, E](graph: DirectedGraph[V, E])(implicit ev: Journal[List[V], V]): Traversal[V, Int] = {
    val result = sort(graph)
    val vertexOrder: Seq[(V, Int)] = for ((v, i) <- result.zipWithIndex) yield v -> i
    VertexTraversal(vertexOrder.toMap)
  }
}
