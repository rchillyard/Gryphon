/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.java;

import java.util.Comparator;
import java.util.Map;
import java.util.function.BinaryOperator;

/**
 * Java façade for shortest-path algorithms on weighted directed graphs.
 *
 * <p>All methods operate on a {@link WeightedGraph}{@code <V, E>} whose edges
 * carry an attribute of type {@code E}.</p>
 *
 * <p>The result is a <em>shortest-path tree</em> (SPT): a
 * {@code Map<V, WeightedEdge<V, E>>} where each entry {@code v → edge} records
 * the cheapest incoming edge to {@code v}. The start vertex is absent.</p>
 *
 * <p><b>Note on Option 3:</b> Dijkstra accumulates path costs — the cost to
 * reach a vertex is the sum of edge weights along the path. This requires both
 * a {@code combine} function (e.g. addition) and a {@code zero} identity
 * element, unlike Prim and Kruskal which only compare individual edge weights
 * and need only a {@link Comparator}.</p>
 *
 * <p><b>Option 1 example</b> (Double weights):
 * <pre>{@code
 * WeightedGraph<String, Double> g = WeightedGraph.directedWeighted();
 * g.addEdge(new WeightedEdge<>("A", "B", 3.0));
 * g.addEdge(new WeightedEdge<>("A", "C", 1.0));
 * g.addEdge(new WeightedEdge<>("C", "B", 1.0));
 *
 * Map<String, WeightedEdge<String, Double>> spt = ShortestPaths.dijkstra(g, "A");
 * // spt.get("B").from()      == "C"   (via C, total cost 2.0)
 * // spt.get("B").attribute() == 1.0   (the C→B edge weight)
 * }</pre>
 * </p>
 *
 * <p><b>Option 3 example</b> (custom attribute type):
 * <pre>{@code
 * WeightedGraph<Building, TunnelProperties> g = WeightedGraph.directedWeighted();
 * g.addEdge(new WeightedEdge<>(b1, b2, props));
 *
 * Map<Building, WeightedEdge<Building, TunnelProperties>> spt =
 *     ShortestPaths.dijkstra(g, start,
 *         (a, b) -> new TunnelProperties(a.cost + b.cost, ...),  // combine
 *         TunnelProperties.ZERO,                                  // identity
 *         Comparator.comparingInt(tp -> tp.cost));                // ordering
 * }</pre>
 * </p>
 */
public class ShortestPaths {

    // -------------------------------------------------------------------------
    // Option 1 — Double weights, sensible defaults
    // -------------------------------------------------------------------------

    /**
     * Runs Dijkstra's algorithm on a graph whose edges carry {@code Double} weights.
     *
     * <p>Costs are combined by addition; edges are ordered by natural
     * {@code Double} ordering. No configuration required.</p>
     *
     * <p>The graph must be directed.</p>
     *
     * @param <V>   the vertex type.
     * @param graph a directed {@code WeightedGraph<V, Double>}.
     * @param start the source vertex.
     * @return the SPT as {@code Map<V, WeightedEdge<V, Double>>};
     *         the start vertex is absent.
     * @throws IllegalStateException if {@code graph} is undirected.
     */
    @SuppressWarnings("unchecked")
    public static <V> Map<V, WeightedEdge<V, Double>> dijkstra(
            WeightedGraph<V, Double> graph,
            V start) {
        if (!graph.isDirected())
            throw new IllegalStateException(
                    "ShortestPaths.dijkstra requires a directed graph");
        return (Map<V, WeightedEdge<V, Double>>) (Map<?, ?>)
                JavaFacadeBridge$.MODULE$.dijkstraDouble(
                        graph.getScalaGraph(),
                        (java.util.List) graph.weightedEdges(),
                        start);
    }

    // -------------------------------------------------------------------------
    // Option 3 — custom attribute type with combine, zero, and comparator
    // -------------------------------------------------------------------------

    /**
     * Runs Dijkstra's algorithm with a custom edge attribute type.
     *
     * <p>Because Dijkstra accumulates path costs, the caller must supply:</p>
     * <ul>
     *   <li>{@code combine} — how to add two costs (e.g. integer addition)</li>
     *   <li>{@code zero} — the identity element for {@code combine}
     *       (e.g. {@code 0} for addition)</li>
     *   <li>{@code comparator} — how to order costs</li>
     * </ul>
     *
     * <p>The graph must be directed.</p>
     *
     * @param <V>        the vertex type.
     * @param <E>        the edge attribute type.
     * @param graph      a directed {@code WeightedGraph<V, E>}.
     * @param start      the source vertex.
     * @param combine    accumulates two costs into one.
     * @param zero       the identity element for {@code combine}.
     * @param comparator orders costs.
     * @return the SPT as {@code Map<V, WeightedEdge<V, E>>};
     *         the start vertex is absent.
     * @throws IllegalStateException if {@code graph} is undirected.
     */
    @SuppressWarnings("unchecked")
    public static <V, E> Map<V, WeightedEdge<V, E>> dijkstra(
            WeightedGraph<V, E> graph,
            V start,
            BinaryOperator<E> combine,
            E zero,
            Comparator<E> comparator) {
        if (!graph.isDirected())
            throw new IllegalStateException(
                    "ShortestPaths.dijkstra requires a directed graph");
        return JavaFacadeBridge$.MODULE$.dijkstraCustom(
                graph.getScalaGraph(),
                graph.weightedEdges(),
                start, combine, zero, comparator);
    }

    // Not instantiable
    private ShortestPaths() {
    }
}