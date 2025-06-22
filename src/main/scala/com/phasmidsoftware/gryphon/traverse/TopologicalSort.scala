package com.phasmidsoftware.gryphon.traverse

import com.phasmidsoftware.gryphon.adjunct.{DirectedEdge, DirectedGraph}
import com.phasmidsoftware.gryphon.core
import com.phasmidsoftware.gryphon.core.{Connexion, Edge}
import com.phasmidsoftware.gryphon.util.GraphException
import com.phasmidsoftware.gryphon.visit.*

import scala.util.{Failure, Success, Try, Using}

/**
 * Represents a "topological sort" of a directed graph. This class provides functionality
 * to manage the traversal order of vertices in a graph and supports creating customized
 * traversal instances with updated vertex-to-result mappings.
 *
 * @param map a mapping of vertices of type `V` to their associated traversal results of type `Int`.
 *            This map is used to store the state of the traversal.
 * @tparam V the type representing a vertex in the graph.
 */
case class TopologicalSort[V](map: Map[V, Int]) extends AbstractVertexTraversal[V, Int](map) {
  /**
   * Creates a new instance of the traversal with the provided mapping of vertices to their respective traversal results.
   *
   * @param m a map where the keys represent vertices of type `V` and the values represent their associated
   *          traversal results of type `Int`.
   * @return a new `Traversal[V, Int]` instance with the updated mapping.
   */
  def unit(m: Map[V, Int]): Traversal[V, Int] = copy(map = m)
}

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
  def sort[V, E](graph: DirectedGraph[V, E])(implicit ev: Journal[List[V], V]): Option[Seq[V]] = {
    val visitor = Visitor.reversePost[V]
    val result = graph.dfsAll(visitor)
    val vs = result.journals.head
    Option.when(acyclic(graph, vs))(vs)
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
   * @return an optional sequence of vertices (`Seq[V]`) representing the topologically sorted order
   *         of the graph's vertices. If the result is empty, it implies that the graph is cyclic.
   */
  def traversal[V, E](graph: DirectedGraph[V, E])(implicit ev: Journal[List[V], V]): Option[TopologicalSort[V]] = {
    val maybeTopologicalOrder = sort(graph)
    maybeTopologicalOrder match {
      case Some(topologicalOrder) =>
        val vertexOrder: Seq[(V, Int)] = for ((v, i) <- topologicalOrder.zipWithIndex) yield v -> i
        Some(TopologicalSort(vertexOrder.toMap))
      case None =>
        None
    }
  }

  /**
   * Determines whether a directed graph is acyclic based on a provided topological sort.
   *
   * This method verifies that in the given topological sort order, for every directed edge
   * in the graph, the start vertex (`white`) appears before or at the same position as the
   * end vertex (`black`). If this condition holds true for all edges, the graph is acyclic.
   *
   * @param graph           the directed graph to analyze, containing vertices of type `V` and edges
   *                        with attributes of type `E`.
   * @param topologicalSort a list of vertices representing a proposed topological sort order
   *                        for the graph.
   * @return true if the graph is acyclic according to the given topological sort; false otherwise.
   */
  private def acyclic[V, E](graph: DirectedGraph[V, E], topologicalSort: List[V]): Boolean =
    (for {
      edge <- graph.edges
      pos1 = topologicalSort.indexOf(edge.white)
      pos2 = topologicalSort.indexOf(edge.black)
    } yield pos1 <= pos2) forall (z => z)
}
