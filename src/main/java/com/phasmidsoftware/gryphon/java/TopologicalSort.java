/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.java;

import java.util.List;
import java.util.Optional;

/**
 * Java façade for topological sorting of directed acyclic graphs (DAGs).
 *
 * <p>A topological sort orders vertices such that for every directed edge
 * {@code u → v}, vertex {@code u} appears before {@code v} in the ordering.
 * Topological sort is only defined for DAGs — if the graph contains a cycle,
 * {@link #sort} returns {@link Optional#empty()}.</p>
 *
 * <p>The graph must be directed. Calling these methods on an undirected graph
 * will throw {@link IllegalStateException}.</p>
 *
 * <p><b>Usage example:</b>
 * <pre>{@code
 * Graph<String> g = Graph.directed();
 * g.addEdge("A", "B");
 * g.addEdge("A", "C");
 * g.addEdge("B", "D");
 * g.addEdge("C", "D");
 *
 * Optional<List<String>> order = TopologicalSort.sort(g);
 * // order: Optional[A, B, C, D] or Optional[A, C, B, D] (both valid)
 *
 * // Cyclic graph:
 * Graph<String> cyclic = Graph.directed();
 * cyclic.addEdge("A", "B");
 * cyclic.addEdge("B", "A");
 * TopologicalSort.sort(cyclic);  // Optional.empty()
 * }</pre>
 * </p>
 */
public class TopologicalSort {

    /**
     * Computes a topological ordering of the vertices in a directed graph.
     *
     * <p>Uses post-order DFS — each vertex appears in the result only after
     * all vertices reachable from it have been recorded.</p>
     *
     * @param <V>   the vertex type.
     * @param graph a directed graph.
     * @return {@code Optional} containing a topological ordering of all vertices,
     * or {@code Optional.empty()} if the graph contains a cycle.
     * @throws IllegalStateException if {@code graph} is undirected.
     */
    @SuppressWarnings("unchecked")
    public static <V> Optional<List<V>> sort(Graph<V> graph) {
        if (!graph.isDirected())
            throw new IllegalStateException(
                    "TopologicalSort.sort requires a directed graph");
        return JavaFacadeBridge$.MODULE$.topologicalSort(graph.getScalaGraph());
    }

    /**
     * Returns {@code true} if the graph is a DAG (contains no directed cycle).
     *
     * <p>Convenience method — equivalent to {@code sort(graph).isPresent()}.</p>
     *
     * @param <V>   the vertex type.
     * @param graph a directed graph.
     * @return {@code true} if acyclic, {@code false} if cyclic.
     * @throws IllegalStateException if {@code graph} is undirected.
     */
    public static <V> boolean isDAG(Graph<V> graph) {
        return sort(graph).isPresent();
    }

    // Not instantiable
    private TopologicalSort() {
    }
}