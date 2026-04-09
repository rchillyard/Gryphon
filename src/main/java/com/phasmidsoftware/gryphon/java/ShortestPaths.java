/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.java;

import java.util.Comparator;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.function.Function;

/**
 * Java façade for shortest-path algorithms on weighted directed graphs.
 *
 * <p>All methods operate on a {@link Graph}{@code <V>} whose edges are
 * {@link WeightedEdge}{@code <V, E>} instances. Calling these methods on a
 * graph whose edges are plain (unweighted) {@link Edge}{@code <V>} will throw
 * {@link ClassCastException} at runtime.</p>
 *
 * <p>The result of every method is a <em>shortest-path tree</em> (SPT): a
 * {@code Map<V, WeightedEdge<V, E>>} where each entry {@code v → edge} records
 * the cheapest incoming edge to {@code v} in the SPT.  The start vertex is
 * absent from the map (it has no predecessor).  From the SPT you can:</p>
 * <ul>
 *   <li>Get the immediate edge weight to {@code v}: {@code spt.get(v).attribute()}</li>
 *   <li>Find {@code v}'s predecessor: {@code spt.get(v).from()}</li>
 *   <li>Reconstruct the full path by walking {@code from()} pointers back to start.</li>
 * </ul>
 *
 * <p><b>Option 1 example</b> (edges carry {@code Double} weights):
 * <pre>{@code
 * Graph<String> g = Graph.directed();
 * g.addEdge(new WeightedEdge<>("A", "B", 3.0));
 * g.addEdge(new WeightedEdge<>("A", "C", 1.0));
 * g.addEdge(new WeightedEdge<>("C", "B", 1.0));
 *
 * Map<String, WeightedEdge<String, Double>> spt =
 *     ShortestPaths.dijkstra(g, "A");
 * // spt.get("B").from()      == "C"   (via C, total cost 2.0)
 * // spt.get("B").attribute() == 1.0   (the C→B edge weight)
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
     * <p>This is Option 1: no configuration required. Edge weights are read from
     * {@link WeightedEdge#attribute()}, and costs are combined by addition.</p>
     *
     * <p>The graph must be directed. Calling this method on an undirected graph
     * is not yet supported and will throw {@link IllegalStateException}.</p>
     *
     * @param <V>   the vertex type.
     * @param graph a directed graph whose canonical edges are {@code WeightedEdge<V, Double>}.
     * @param start the source vertex.
     * @return the shortest-path tree as a {@code Map<V, WeightedEdge<V, Double>>};
     * the start vertex is absent.
     * @throws IllegalStateException if {@code graph} is undirected.
     * @throws ClassCastException    if any edge is not a {@code WeightedEdge<V, Double>}.
     */
    @SuppressWarnings("unchecked")
    public static <V> Map<V, WeightedEdge<V, Double>> dijkstra(
            Graph<V> graph,
            V start) {
        if (!graph.isDirected())
            throw new IllegalStateException("ShortestPaths.dijkstra requires a directed graph");
        return (Map<V, WeightedEdge<V, Double>>) (Map<?, ?>)
                JavaFacadeBridge$.MODULE$.dijkstraDouble(graph.getScalaGraph(), graph.edges(), start);
    }
    // -------------------------------------------------------------------------
    // Option 3 — custom weight function and combiner
    // -------------------------------------------------------------------------

    /**
     * Runs Dijkstra's algorithm with a caller-supplied weight extractor and
     * cost combiner.
     *
     * <p>This is Option 3: the caller controls how edge weight is extracted and
     * how costs are accumulated. The {@code combine} parameter corresponds directly
     * to the {@code Monoid[E].combine} operation in the Scala engine.</p>
     *
     * <p>The graph must be directed.</p>
     *
     * @param <V>     the vertex type.
     * @param <E>     the weight type; must be {@link Comparable} for ordering.
     * @param graph   a directed graph.
     * @param start   the source vertex.
     * @param weight  extracts the weight from an edge.
     * @param combine combines two costs (e.g., addition for numeric weights).
     * @param zero    the identity element for {@code combine} (e.g., {@code 0.0}).
     * @return the shortest-path tree as a {@code Map<V, WeightedEdge<V, E>>};
     * the start vertex is absent.
     * @throws IllegalStateException if {@code graph} is undirected.
     */
    @SuppressWarnings("unchecked")
    public static <V, E extends Comparable<E>> Map<V, WeightedEdge<V, E>> dijkstra(
            Graph<V> graph,
            V start,
            Function<Edge<V>, E> weight,
            BinaryOperator<E> combine,
            E zero,
            Comparator<E> comparator) {
        if (!graph.isDirected())
            throw new IllegalStateException("ShortestPaths.dijkstra requires a directed graph");
        return JavaFacadeBridge$.MODULE$.dijkstraCustom(
                graph.getScalaGraph(), graph.edges(), start, weight, combine, zero, comparator);
    }

    public static <V, E extends Comparable<E>> Map<V, WeightedEdge<V, E>> dijkstra(
            Graph<V> graph,
            V start,
            Function<Edge<V>, E> weight,
            BinaryOperator<E> combine,
            E zero) {
        return dijkstra(graph, start, weight, combine, zero, Comparator.naturalOrder());
    }

    // Not instantiable
    private ShortestPaths() {
    }
}