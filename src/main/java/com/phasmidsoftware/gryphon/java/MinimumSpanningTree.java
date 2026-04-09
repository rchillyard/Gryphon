/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.java;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Java façade for minimum spanning tree algorithms on weighted undirected graphs.
 *
 * <p>All methods operate on a {@link Graph}{@code <V>} whose edges are
 * {@link WeightedEdge}{@code <V, E>} instances. Calling these methods on a
 * graph whose edges are plain (unweighted) {@link Edge}{@code <V>} will throw
 * {@link ClassCastException} at runtime.</p>
 *
 * <p>The result of every method is a minimum spanning tree (MST): a
 * {@code Map<V, WeightedEdge<V, E>>} where each entry {@code v → edge} records
 * the cheapest edge connecting {@code v} to the growing MST. The start vertex
 * is absent from the map (it has no predecessor). From the MST map you can:</p>
 * <ul>
 *   <li>Get the MST edge weight for {@code v}: {@code mst.get(v).attribute()}</li>
 *   <li>Find {@code v}'s MST neighbour: {@code mst.get(v).from()} or {@code mst.get(v).to()}</li>
 *   <li>Collect all MST edges: {@code new ArrayList<>(mst.values())}</li>
 *   <li>Compute total MST weight: {@code mst.values().stream().mapToDouble(WeightedEdge::attribute).sum()}</li>
 * </ul>
 *
 * <p><b>Option 1 example</b> (edges carry {@code Double} weights):
 * <pre>{@code
 * Graph<Integer> g = Graph.undirected();
 * g.addEdge(new WeightedEdge<>(0, 1, 0.5));
 * g.addEdge(new WeightedEdge<>(0, 2, 0.3));
 * g.addEdge(new WeightedEdge<>(1, 2, 0.8));
 *
 * Map<Integer, WeightedEdge<Integer, Double>> mst =
 *     MinimumSpanningTree.prim(g, 0);
 * // mst.size()               == 2      (N-1 edges)
 * // mst.get(2).attribute()   == 0.3
 * // new ArrayList<>(mst.values()) gives all MST edges
 * }</pre>
 * </p>
 */
public class MinimumSpanningTree {

    // -------------------------------------------------------------------------
    // Prim — Option 1: Double weights
    // -------------------------------------------------------------------------

    /**
     * Runs Prim's algorithm on a graph whose edges carry {@code Double} weights,
     * starting from {@code start}.
     *
     * <p>This is Option 1: no configuration required. Edge weights are read from
     * {@link WeightedEdge#attribute()}, and the minimum edge is selected by
     * natural {@code Double} ordering.</p>
     *
     * <p>The graph must be undirected. Calling this method on a directed graph
     * will throw {@link IllegalStateException}.</p>
     *
     * @param <V>   the vertex type.
     * @param graph an undirected graph whose canonical edges are
     *              {@code WeightedEdge<V, Double>}.
     * @param start the source vertex (absent from the result).
     * @return the MST as a {@code Map<V, WeightedEdge<V, Double>>};
     * the start vertex is absent.
     * @throws IllegalStateException if {@code graph} is directed.
     * @throws ClassCastException    if any edge is not a
     *                               {@code WeightedEdge<V, Double>}.
     */
    @SuppressWarnings("unchecked")
    public static <V> Map<V, WeightedEdge<V, Double>> prim(
            Graph<V> graph,
            V start) {
        if (graph.isDirected())
            throw new IllegalStateException(
                    "MinimumSpanningTree.prim requires an undirected graph");
        return (Map<V, WeightedEdge<V, Double>>) (Map<?, ?>)
                JavaFacadeBridge$.MODULE$.primDouble(
                        graph.getScalaGraph(), graph.edges(), start);
    }

    // -------------------------------------------------------------------------
    // Prim — Option 3: custom weight function and comparator
    // -------------------------------------------------------------------------

    /**
     * Runs Prim's algorithm with a caller-supplied weight extractor and comparator.
     *
     * <p>This is Option 3: the caller controls how edge weight is extracted and
     * how edges are compared. The {@code comparator} parameter corresponds to the
     * {@code Ordering[E]} typeclass in the Scala engine.</p>
     *
     * <p>The graph must be undirected.</p>
     *
     * @param <V>        the vertex type.
     * @param <E>        the weight type.
     * @param graph      an undirected graph.
     * @param start      the source vertex.
     * @param weight     extracts the weight from an edge.
     * @param zero       the identity element (e.g. {@code 0.0} for addition).
     * @param comparator orders weights (e.g. {@code Comparator.naturalOrder()}).
     * @return the MST as a {@code Map<V, WeightedEdge<V, E>>};
     * the start vertex is absent.
     * @throws IllegalStateException if {@code graph} is directed.
     */
    @SuppressWarnings("unchecked")
    public static <V, E> Map<V, WeightedEdge<V, E>> prim(
            Graph<V> graph,
            V start,
            Function<Edge<V>, E> weight,
            E zero,
            Comparator<E> comparator) {
        if (graph.isDirected())
            throw new IllegalStateException(
                    "MinimumSpanningTree.prim requires an undirected graph");
        return JavaFacadeBridge$.MODULE$.primCustom(
                graph.getScalaGraph(), graph.edges(), start,
                weight, zero, comparator);
    }

    /*
     * Copyright (c) 2026. Phasmid Software
     */

// ---- Add to MinimumSpanningTree.java ----------------------------------------

    // -------------------------------------------------------------------------
    // Kruskal — Option 1: Double weights
    // -------------------------------------------------------------------------

    /**
     * Runs Kruskal's algorithm on an undirected graph whose edges carry
     * {@code Double} weights.
     *
     * <p>This is Option 1: no configuration required. Edges are sorted by
     * {@link WeightedEdge#attribute()} in ascending order; the MST is built
     * greedily using a disjoint-set structure to detect cycles.</p>
     *
     * <p>The graph must be undirected. Calling this method on a directed graph
     * will throw {@link IllegalStateException}.</p>
     *
     * <p>The returned list is in non-decreasing weight order.</p>
     *
     * @param <V>   the vertex type.
     * @param graph an undirected graph whose canonical edges are
     *              {@code WeightedEdge<V, Double>}.
     * @return the MST as a {@code List<WeightedEdge<V, Double>>} in
     * non-decreasing weight order; contains exactly N-1 edges for a
     * connected graph of N vertices.
     * @throws IllegalStateException if {@code graph} is directed.
     * @throws ClassCastException    if any edge is not a
     *                               {@code WeightedEdge<V, Double>}.
     */
    @SuppressWarnings("unchecked")
    public static <V> List<WeightedEdge<V, Double>> kruskal(Graph<V> graph) {
        if (graph.isDirected())
            throw new IllegalStateException(
                    "MinimumSpanningTree.kruskal requires an undirected graph");
        return (List<WeightedEdge<V, Double>>) (List<?>)
                JavaFacadeBridge$.MODULE$.kruskalDouble(graph.edges());
    }

    // -------------------------------------------------------------------------
    // Kruskal — Option 3: custom weight function and comparator
    // -------------------------------------------------------------------------

    /**
     * Runs Kruskal's algorithm with a caller-supplied weight extractor and
     * comparator.
     *
     * <p>This is Option 3: the caller controls how edge weight is extracted and
     * how edges are ordered. The returned list is in non-decreasing order
     * according to {@code comparator}.</p>
     *
     * <p>The graph must be undirected.</p>
     *
     * @param <V>        the vertex type.
     * @param <E>        the weight type.
     * @param graph      an undirected graph.
     * @param weight     extracts the weight from an edge.
     * @param comparator orders weights (e.g. {@code Comparator.naturalOrder()}).
     * @return the MST as a {@code List<WeightedEdge<V, E>>} in non-decreasing
     * weight order.
     * @throws IllegalStateException if {@code graph} is directed.
     */
    @SuppressWarnings("unchecked")
    public static <V, E> List<WeightedEdge<V, E>> kruskal(
            Graph<V> graph,
            Function<Edge<V>, E> weight,
            Comparator<E> comparator) {
        if (graph.isDirected())
            throw new IllegalStateException(
                    "MinimumSpanningTree.kruskal requires an undirected graph");
        return JavaFacadeBridge$.MODULE$.kruskalCustom(graph.edges(), weight, comparator);
    }

    // Not instantiable
    private MinimumSpanningTree() {
    }
}