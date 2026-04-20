/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.java;

import java.util.*;

/**
 * Java façade for connected-components analysis on undirected graphs.
 *
 * <p>A connected component is a maximal set of vertices such that there is a
 * path between every pair of vertices in the set.</p>
 *
 * <p>The graph must be undirected. Calling these methods on a directed graph
 * will throw {@link IllegalStateException}.</p>
 *
 * <p><b>Usage example:</b>
 * <pre>{@code
 * Graph<String> g = Graph.undirected();
 * g.addEdge("A", "B");
 * g.addEdge("C", "D");  // separate component
 *
 * int n = ConnectedComponents.count(g);          // 2
 * Map<String, Integer> ids = ConnectedComponents.componentIds(g);
 * ids.get("A") == ids.get("B")  // true
 * ids.get("A") == ids.get("C")  // false
 *
 * Map<Integer, Set<String>> groups = ConnectedComponents.components(g);
 * // groups: {0 -> {A, B}, 1 -> {C, D}}
 * }</pre>
 * </p>
 */
public class ConnectedComponents {

    /**
     * Returns the number of connected components in the graph.
     *
     * @param <V>   the vertex type.
     * @param graph an undirected graph.
     * @return the number of connected components.
     * @throws IllegalStateException if {@code graph} is directed.
     */
    public static <V> int count(Graph<V> graph) {
        if (graph.isDirected())
            throw new IllegalStateException(
                    "ConnectedComponents.count requires an undirected graph");
        return (int) componentIds(graph).values().stream().distinct().count();
    }

    /**
     * Returns a map from each vertex to its integer component id.
     *
     * <p>Vertices with the same id belong to the same connected component.
     * Component ids are assigned in DFS discovery order starting from 0.</p>
     *
     * @param <V>   the vertex type.
     * @param graph an undirected graph.
     * @return an unmodifiable {@code Map<V, Integer>} mapping vertex to component id.
     * @throws IllegalStateException if {@code graph} is directed.
     */
    @SuppressWarnings("unchecked")
    public static <V> Map<V, Integer> componentIds(Graph<V> graph) {
        if (graph.isDirected())
            throw new IllegalStateException(
                    "ConnectedComponents.componentIds requires an undirected graph");
        return JavaFacadeBridge$.MODULE$.connectedComponents(graph.getScalaGraph());
    }

    /**
     * Returns the vertices grouped by component as a {@code Map<Integer, Set<V>>}.
     *
     * @param <V>   the vertex type.
     * @param graph an undirected graph.
     * @return an unmodifiable map from component id to the set of vertices in that component.
     * @throws IllegalStateException if {@code graph} is directed.
     */
    public static <V> Map<Integer, Set<V>> components(Graph<V> graph) {
        Map<V, Integer> ids = componentIds(graph);
        Map<Integer, Set<V>> result = new LinkedHashMap<>();
        ids.forEach((v, id) ->
                result.computeIfAbsent(id, k -> new LinkedHashSet<>()).add(v));
        return Collections.unmodifiableMap(result);
    }

    // Not instantiable
    private ConnectedComponents() {
    }
}