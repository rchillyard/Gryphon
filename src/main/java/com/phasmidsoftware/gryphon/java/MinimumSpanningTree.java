/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.java;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Java façade for minimum spanning tree algorithms on weighted undirected graphs.
 *
 * <p>All methods operate on a {@link WeightedGraph}{@code <V, E>} whose edges
 * carry an attribute of type {@code E}. The edge attribute is used directly
 * as the priority key — no separate weight extractor is needed.</p>
 *
 * <p>Both algorithms require an undirected graph. Calling either on a directed
 * graph throws {@link IllegalStateException}.</p>
 *
 * <p><b>Prim result:</b> {@code Map<V, WeightedEdge<V, E>>} mapping each
 * non-source vertex to its cheapest MST edge. The start vertex is absent.</p>
 *
 * <p><b>Kruskal result:</b> {@code List<WeightedEdge<V, E>>} in non-decreasing
 * order according to the supplied comparator.</p>
 *
 * <p><b>Option 1 example</b> (Double weights):
 * <pre>{@code
 * WeightedGraph<Integer, Double> g = WeightedGraph.undirected();
 * g.addEdge(new WeightedEdge<>(0, 1, 0.26));
 * g.addEdge(new WeightedEdge<>(0, 7, 0.16));
 *
 * Map<Integer, WeightedEdge<Integer, Double>> mst = MinimumSpanningTree.prim(g, 0);
 * double total = mst.values().stream().mapToDouble(WeightedEdge::attribute).sum();
 * }</pre>
 * </p>
 *
 * <p><b>Option 3 example</b> (custom attribute type):
 * <pre>{@code
 * WeightedGraph<Building, TunnelProperties> g = WeightedGraph.undirected();
 * g.addEdge(new WeightedEdge<>(b1, b2, tunnelProperties));
 *
 * Map<Building, WeightedEdge<Building, TunnelProperties>> mst =
 *     MinimumSpanningTree.prim(g, start, Comparator.comparingInt(tp -> tp.cost));
 *
 * mst.forEach((b, e) -> System.out.println(b + ": cost=" + e.attribute().cost
 *                                           + " length=" + e.attribute().length));
 * }</pre>
 * </p>
 */
public class MinimumSpanningTree {

    // -------------------------------------------------------------------------
    // Prim — Option 1: Double weights
    // -------------------------------------------------------------------------

    /**
     * Runs Prim's algorithm on a graph whose edges carry {@code Double} weights.
     *
     * <p>Edge attributes are compared by natural {@code Double} ordering.
     * No configuration required.</p>
     *
     * @param <V>   the vertex type.
     * @param graph an undirected {@code WeightedGraph<V, Double>}.
     * @param start the source vertex (absent from the result).
     * @return the MST as {@code Map<V, WeightedEdge<V, Double>>}.
     * @throws IllegalStateException if {@code graph} is directed.
     */
    @SuppressWarnings("unchecked")
    public static <V> Map<V, WeightedEdge<V, Double>> prim(
            WeightedGraph<V, Double> graph,
            V start) {
        if (graph.isDirected())
            throw new IllegalStateException(
                    "MinimumSpanningTree.prim requires an undirected graph");
        return (Map<V, WeightedEdge<V, Double>>) (Map<?, ?>)
                JavaFacadeBridge$.MODULE$.primDouble(
                        graph.getScalaGraph(), (java.util.List) graph.weightedEdges(), start);
    }

    // -------------------------------------------------------------------------
    // Prim — Option 3: custom attribute type, caller supplies Comparator
    // -------------------------------------------------------------------------

    /**
     * Runs Prim's algorithm with a custom edge attribute type, ordered by the
     * supplied {@link Comparator}.
     *
     * <p>The edge attribute is used directly as the priority key. No weight
     * extractor is needed.</p>
     *
     * @param <V>        the vertex type.
     * @param <E>        the edge attribute type.
     * @param graph      an undirected {@code WeightedGraph<V, E>}.
     * @param start      the source vertex (absent from the result).
     * @param comparator orders edge attributes.
     * @return the MST as {@code Map<V, WeightedEdge<V, E>>}.
     * @throws IllegalStateException if {@code graph} is directed.
     */
    @SuppressWarnings("unchecked")
    public static <V, E> Map<V, WeightedEdge<V, E>> prim(
            WeightedGraph<V, E> graph,
            V start,
            Comparator<E> comparator) {
        if (graph.isDirected())
            throw new IllegalStateException(
                    "MinimumSpanningTree.prim requires an undirected graph");
        return JavaFacadeBridge$.MODULE$.primCustom(
                graph.getScalaGraph(), graph.weightedEdges(),
                start, comparator);
    }

    // -------------------------------------------------------------------------
    // Kruskal — Option 1: Double weights
    // -------------------------------------------------------------------------

    /**
     * Runs Kruskal's algorithm on a graph whose edges carry {@code Double}
     * weights.
     *
     * <p>Edges are sorted by natural {@code Double} ordering. The returned list
     * is in non-decreasing weight order.</p>
     *
     * @param <V>   the vertex type.
     * @param graph an undirected {@code WeightedGraph<V, Double>}.
     * @return the MST as {@code List<WeightedEdge<V, Double>>} in
     *         non-decreasing weight order.
     * @throws IllegalStateException if {@code graph} is directed.
     */
    @SuppressWarnings("unchecked")
    public static <V> List<WeightedEdge<V, Double>> kruskal(
            WeightedGraph<V, Double> graph) {
        if (graph.isDirected())
            throw new IllegalStateException(
                    "MinimumSpanningTree.kruskal requires an undirected graph");
        return (List<WeightedEdge<V, Double>>) (List<?>)
                JavaFacadeBridge$.MODULE$.kruskalDouble((java.util.List) graph.weightedEdges());
    }

    // -------------------------------------------------------------------------
    // Kruskal — Option 3: custom attribute type, caller supplies Comparator
    // -------------------------------------------------------------------------

    /**
     * Runs Kruskal's algorithm with a custom edge attribute type, ordered by
     * the supplied {@link Comparator}.
     *
     * <p>The edge attribute is used directly as the sort key. The returned list
     * is in non-decreasing order according to {@code comparator}.</p>
     *
     * @param <V>        the vertex type.
     * @param <E>        the edge attribute type.
     * @param graph      an undirected {@code WeightedGraph<V, E>}.
     * @param comparator orders edge attributes.
     * @return the MST as {@code List<WeightedEdge<V, E>>} in non-decreasing
     *         order.
     * @throws IllegalStateException if {@code graph} is directed.
     */
    @SuppressWarnings("unchecked")
    public static <V, E> List<WeightedEdge<V, E>> kruskal(
            WeightedGraph<V, E> graph,
            Comparator<E> comparator) {
        if (graph.isDirected())
            throw new IllegalStateException(
                    "MinimumSpanningTree.kruskal requires an undirected graph");
        return JavaFacadeBridge$.MODULE$.kruskalCustom(
                graph.weightedEdges(), comparator);
    }


    // -------------------------------------------------------------------------
    // Boruvka — Option 1: Double weights
    // -------------------------------------------------------------------------

    /**
     * Runs Borůvka's algorithm on a graph whose edges carry {@code Double} weights.
     *
     * <p>Edges are compared by natural {@code Double} ordering. The returned list
     * contains exactly N-1 edges for a connected graph of N vertices.</p>
     *
     * @param <V>   the vertex type.
     * @param graph an undirected {@code WeightedGraph<V, Double>}.
     * @return the MST as {@code List<WeightedEdge<V, Double>>}.
     * @throws IllegalStateException if {@code graph} is directed.
     */
    @SuppressWarnings("unchecked")
    public static <V> List<WeightedEdge<V, Double>> boruvka(
            WeightedGraph<V, Double> graph) {
        if (graph.isDirected())
            throw new IllegalStateException(
                    "MinimumSpanningTree.boruvka requires an undirected graph");
        return (List<WeightedEdge<V, Double>>) (List<?>)
                JavaFacadeBridge$.MODULE$.boruvkaDouble((java.util.List) graph.weightedEdges());
    }

    // -------------------------------------------------------------------------
    // Boruvka — Option 3: custom attribute type, caller supplies Comparator
    // -------------------------------------------------------------------------

    /**
     * Runs Borůvka's algorithm with a custom edge attribute type, ordered by
     * the supplied {@link Comparator}.
     *
     * <p>The edge attribute is used directly as the sort key. The returned list
     * contains exactly N-1 edges for a connected graph of N vertices.</p>
     *
     * @param <V>        the vertex type.
     * @param <E>        the edge attribute type.
     * @param graph      an undirected {@code WeightedGraph<V, E>}.
     * @param comparator orders edge attributes.
     * @return the MST as {@code List<WeightedEdge<V, E>>}.
     * @throws IllegalStateException if {@code graph} is directed.
     */
    @SuppressWarnings("unchecked")
    public static <V, E> List<WeightedEdge<V, E>> boruvka(
            WeightedGraph<V, E> graph,
            Comparator<E> comparator) {
        if (graph.isDirected())
            throw new IllegalStateException(
                    "MinimumSpanningTree.boruvka requires an undirected graph");
        return JavaFacadeBridge$.MODULE$.boruvkaCustom(
                graph.weightedEdges(), comparator);
    }

    // Not instantiable
    private MinimumSpanningTree() {
    }
}