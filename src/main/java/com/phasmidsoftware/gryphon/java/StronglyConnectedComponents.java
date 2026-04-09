/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.java;

import java.util.*;

/**
 * Java façade for strongly-connected-components analysis on directed graphs,
 * using Kosaraju's two-pass algorithm.
 *
 * <p>A strongly connected component (SCC) is a maximal set of vertices such
 * that there is a directed path from every vertex in the set to every other.
 * Kosaraju's algorithm finds all SCCs in O(V + E) time.</p>
 *
 * <p>The result is a {@code Map<V, Integer>} mapping each vertex to an integer
 * SCC identifier. Vertices that share the same id belong to the same SCC.
 * SCC ids are assigned in topological order of the condensation DAG — the SCC
 * with the smallest id is a "source" component (no incoming cross-edges from
 * other SCCs).</p>
 *
 * <p>The graph must be directed. Calling these methods on an undirected graph
 * will throw {@link IllegalStateException}.</p>
 *
 * <p><b>Usage example:</b>
 * <pre>{@code
 * Graph<String> g = Graph.directed();
 * g.addEdge("A", "B");
 * g.addEdge("B", "C");
 * g.addEdge("C", "A");   // cycle: A, B, C form one SCC
 * g.addEdge("C", "D");   // D is a singleton SCC
 *
 * Map<String, Integer> sccs = StronglyConnectedComponents.kosaraju(g);
 * sccs.get("A") == sccs.get("B")  // true — same SCC
 * sccs.get("A") == sccs.get("D")  // false — different SCCs
 *
 * // Count SCCs
 * long count = sccs.values().stream().distinct().count();  // 2
 *
 * // Group vertices by SCC
 * Map<Integer, List<String>> groups = new HashMap<>();
 * sccs.forEach((v, id) ->
 *     groups.computeIfAbsent(id, k -> new ArrayList<>()).add(v));
 * }</pre>
 * </p>
 */
public class StronglyConnectedComponents {

    /**
     * Computes the strongly connected components of a directed graph using
     * Kosaraju's algorithm.
     *
     * <p>The graph may have unweighted edges (plain {@link Edge}{@code <V>})
     * or weighted edges ({@link WeightedEdge}{@code <V, E>}) — edge weights
     * are ignored for SCC computation.</p>
     *
     * @param <V>   the vertex type.
     * @param graph a directed graph.
     * @return a {@code Map<V, Integer>} mapping each vertex to its SCC id;
     * vertices with the same id are in the same SCC.
     * @throws IllegalStateException if {@code graph} is undirected.
     */
    @SuppressWarnings("unchecked")
    public static <V> Map<V, Integer> kosaraju(Graph<V> graph) {
        if (!graph.isDirected())
            throw new IllegalStateException(
                    "StronglyConnectedComponents.kosaraju requires a directed graph");
        return JavaFacadeBridge$.MODULE$.kosaraju(graph.getScalaGraph(), graph.edges());
    }

    /**
     * Returns the number of strongly connected components in the graph.
     *
     * <p>Convenience method — equivalent to
     * {@code kosaraju(graph).values().stream().distinct().count()}.</p>
     *
     * @param <V>   the vertex type.
     * @param graph a directed graph.
     * @return the number of SCCs.
     * @throws IllegalStateException if {@code graph} is undirected.
     */
    public static <V> int count(Graph<V> graph) {
        return (int) kosaraju(graph).values().stream().distinct().count();
    }

    /**
     * Returns the vertices grouped by SCC as a {@code Map<Integer, Set<V>>}.
     *
     * <p>Convenience method for when the full grouping is needed rather than
     * per-vertex lookup.</p>
     *
     * @param <V>   the vertex type.
     * @param graph a directed graph.
     * @return a map from SCC id to the set of vertices in that component.
     * @throws IllegalStateException if {@code graph} is undirected.
     */
    public static <V> Map<Integer, Set<V>> components(Graph<V> graph) {
        Map<V, Integer> sccMap = kosaraju(graph);
        Map<Integer, Set<V>> result = new LinkedHashMap<>();
        sccMap.forEach((v, id) ->
                result.computeIfAbsent(id, k -> new LinkedHashSet<>()).add(v));
        return Collections.unmodifiableMap(result);
    }

    // Not instantiable
    private StronglyConnectedComponents() {
    }
}