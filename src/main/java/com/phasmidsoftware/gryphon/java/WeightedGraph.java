/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.java;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A mutable, lazily-materialised façade over Gryphon's purely functional graph
 * types, with a typed edge attribute {@code E}.
 *
 * <p>{@code WeightedGraph<V, E>} extends {@link Graph}{@code <V>} by enforcing
 * that all edges carry an attribute of type {@code E}. This gives the compiler
 * (and the algorithm façades) a typed handle on edge weights, eliminating the
 * runtime casts required when using plain {@link Graph}{@code <V>} with
 * {@link WeightedEdge}{@code <V, E>}.</p>
 *
 * <p>Directionality is fixed at construction time via the factory methods
 * {@link #directed()} and {@link #undirected()}.</p>
 *
 * <p>Plain BFS and DFS are inherited from {@link Graph}{@code <V>} and work
 * identically — edge weights are ignored for traversal purposes.</p>
 *
 * <p>Weighted algorithms ({@link ShortestPaths}, {@link MinimumSpanningTree})
 * accept a {@code WeightedGraph<V, E>} directly, removing the need for runtime
 * casts and making the weight type visible to the compiler.</p>
 *
 * <p><b>Usage example:</b>
 * <pre>{@code
 * WeightedGraph<Building, TunnelProperties> g = WeightedGraph.undirected();
 * g.addEdge(new WeightedEdge<>(b1, b2, tunnelProperties));
 *
 * Map<Building, WeightedEdge<Building, TunnelProperties>> mst =
 *     MinimumSpanningTree.prim(g, start,
 *         Comparator.comparingInt(tp -> tp.cost));
 * }</pre>
 * </p>
 *
 * @param <V> the vertex type. Must implement {@code equals} and {@code hashCode}.
 * @param <E> the edge attribute (weight) type.
 */
public class WeightedGraph<V, E> extends Graph<V> {

    // -------------------------------------------------------------------------
    // Factory methods
    // -------------------------------------------------------------------------

    /**
     * Creates an empty directed weighted graph.
     *
     * @param <V> the vertex type.
     * @param <E> the edge attribute type.
     * @return a new empty directed {@code WeightedGraph}.
     */
    public static <V, E> WeightedGraph<V, E> directedWeighted() {
        return new WeightedGraph<>(true);
    }

    /**
     * Creates an empty undirected weighted graph.
     *
     * @param <V> the vertex type.
     * @param <E> the edge attribute type.
     * @return a new empty undirected {@code WeightedGraph}.
     */
    public static <V, E> WeightedGraph<V, E> undirectedWeighted() {
        return new WeightedGraph<>(false);
    }

    // -------------------------------------------------------------------------
    // File reader factory methods
    // -------------------------------------------------------------------------

    /**
     * Reads an undirected weighted graph with {@code Integer} vertices and
     * {@code Double} edge weights from a classpath resource.
     *
     * <p>The resource must use the Gryphon {@code .graph} format:
     * {@code 0 = 1 0.16} for an undirected edge, {@code //} for comments.</p>
     *
     * @param resourceName the classpath resource name (e.g. {@code "prim.graph"}).
     * @return a populated undirected {@code WeightedGraph<Integer, Double>}.
     * @throws RuntimeException if the resource is not found or cannot be parsed.
     */
    @SuppressWarnings("unchecked")
    public static WeightedGraph<Integer, Double> undirectedFromResource(String resourceName) {
        scala.Tuple2<?, ?> t = JavaFacadeBridge$.MODULE$.undirectedWeightedFromResource(resourceName);
        return fromScalaGraph(t._1(), (java.util.List<WeightedEdge<Integer, Double>>) t._2(), false);
    }

    /**
     * Reads a directed weighted graph with {@code Integer} vertices and
     * {@code Double} edge weights from a classpath resource.
     *
     * @param resourceName the classpath resource name (e.g. {@code "routes.graph"}).
     * @return a populated directed {@code WeightedGraph<Integer, Double>}.
     * @throws RuntimeException if the resource is not found or cannot be parsed.
     */
    @SuppressWarnings("unchecked")
    public static WeightedGraph<Integer, Double> directedFromResource(String resourceName) {
        scala.Tuple2<?, ?> t = JavaFacadeBridge$.MODULE$.directedWeightedFromResource(resourceName);
        return fromScalaGraph(t._1(), (java.util.List<WeightedEdge<Integer, Double>>) t._2(), true);
    }

    /**
     * Reads an undirected weighted graph from a classpath resource using
     * custom vertex and edge parsers.
     *
     * <p>Use this variant when your vertices or edges are not plain
     * {@code Integer}/{@code Double} — for example, building names or
     * composite tunnel properties.</p>
     *
     * @param <V>          the vertex type.
     * @param <E>          the edge attribute type.
     * @param resourceName the classpath resource name.
     * @param vertexParser parses a single token from the graph file into a {@code V}.
     * @param edgeParser   parses a single token from the graph file into an {@code E}.
     * @return a populated undirected {@code WeightedGraph<V, E>}.
     * @throws RuntimeException if the resource is not found or cannot be parsed.
     */
    @SuppressWarnings("unchecked")
    public static <V, E> WeightedGraph<V, E> undirectedFromResource(
            String resourceName,
            java.util.function.Function<String, V> vertexParser,
            java.util.function.Function<String, E> edgeParser) {
        scala.Tuple2<?, ?> t = JavaFacadeBridge$.MODULE$.undirectedWeightedFromResourceCustom(
                resourceName, vertexParser, edgeParser);
        return fromScalaGraph(t._1(), (java.util.List<WeightedEdge<V, E>>) t._2(), false);
    }

    /**
     * Reads a directed weighted graph from a classpath resource using
     * custom vertex and edge parsers.
     *
     * @param <V>          the vertex type.
     * @param <E>          the edge attribute type.
     * @param resourceName the classpath resource name.
     * @param vertexParser parses a single token from the graph file into a {@code V}.
     * @param edgeParser   parses a single token from the graph file into an {@code E}.
     * @return a populated directed {@code WeightedGraph<V, E>}.
     * @throws RuntimeException if the resource is not found or cannot be parsed.
     */
    @SuppressWarnings("unchecked")
    public static <V, E> WeightedGraph<V, E> directedFromResource(
            String resourceName,
            java.util.function.Function<String, V> vertexParser,
            java.util.function.Function<String, E> edgeParser) {
        scala.Tuple2<?, ?> t = JavaFacadeBridge$.MODULE$.directedWeightedFromResourceCustom(
                resourceName, vertexParser, edgeParser);
        return fromScalaGraph(t._1(), (java.util.List<WeightedEdge<V, E>>) t._2(), true);
    }

    /**
     * Wraps a pre-built Scala graph and its extracted edge list into a Java
     * {@code WeightedGraph} façade. The Scala graph is injected into the cache
     * and the edge list is stored so algorithm bridge methods work correctly.
     */
    @SuppressWarnings("unchecked")
    private static <V, E> WeightedGraph<V, E> fromScalaGraph(
            Object scalaGraph, java.util.List<WeightedEdge<V, E>> edges, boolean directed) {
        return new WeightedGraph<>(directed, scalaGraph, edges);
    }

    // -------------------------------------------------------------------------
    // Mutation
    // -------------------------------------------------------------------------

    /**
     * Adds a typed weighted edge to this graph.
     *
     * <p>Both endpoint vertices are added automatically if not already present.
     * For undirected graphs the reverse edge is also recorded internally.</p>
     *
     * <p>NOTE that the present design results in maintaining two edge lists.
     * Clearly, we could easily improve the implementation but for pedagogical purposes,
     * this is acceptable.</p>
     *
     * @param edge the weighted edge to add.
     */
    public void addEdge(WeightedEdge<V, E> edge) {
        weightedEdges.add(edge);
        super.addEdge(edge);
    }

    /**
     * Adds an unweighted edge.
     *
     * <p>Overrides {@link Graph#addEdge(Edge)} to throw
     * {@link IllegalArgumentException} if the edge is not a
     * {@link WeightedEdge} — a {@code WeightedGraph} requires all edges to
     * carry an attribute.</p>
     *
     * @param edge the edge to add; must be a {@link WeightedEdge}.
     * @throws IllegalArgumentException if {@code edge} is not a
     *                                  {@link WeightedEdge}.
     */
    @Override
    public void addEdge(Edge<V> edge) {
        if (!(edge instanceof WeightedEdge<?, ?> we))
            throw new IllegalArgumentException(
                    "WeightedGraph requires WeightedEdge instances; got: " +
                            edge.getClass().getSimpleName());
        @SuppressWarnings("unchecked")
        WeightedEdge<V, E> cast = (WeightedEdge<V, E>) we;
        addEdge(cast);
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    /**
     * Returns the typed weighted edge list — each edge stored once, as
     * originally added, with full type information preserved.
     *
     * <p>This is the method used by the algorithm bridge to materialise the
     * Scala graph without runtime casts.</p>
     *
     * @return an unmodifiable list of typed weighted edges.
     */
    public List<WeightedEdge<V, E>> weightedEdges() {
        return Collections.unmodifiableList(weightedEdges);
    }

    // -------------------------------------------------------------------------
    // Object overrides
    // -------------------------------------------------------------------------

    @Override
    public String toString() {
        return (isDirected() ? "Directed" : "Undirected") +
                "WeightedGraph{vertices=" + vertices().size() +
                ", edges=" + weightedEdges.size() + "}";
    }

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    private WeightedGraph(boolean directed) {
        super(directed);
    }

    /**
     * Package-private constructor for graphs built from a resource file.
     * The pre-built Scala graph is injected into the cache directly.
     */
    @SuppressWarnings("unchecked")
    private WeightedGraph(boolean directed, Object prebuiltScalaGraph) {
        super(directed, prebuiltScalaGraph);
    }

    /**
     * Constructor for graphs built from a resource file, with pre-extracted
     * edge list. The Scala graph is injected into the cache and the edge list
     * is stored so algorithm bridge methods work correctly.
     */
    @SuppressWarnings("unchecked")
    private WeightedGraph(boolean directed, Object prebuiltScalaGraph,
                          java.util.List<WeightedEdge<V, E>> edges) {
        super(directed, prebuiltScalaGraph);
        weightedEdges.addAll(edges);
    }

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    /**
     * Typed canonical edge list — parallel to {@link Graph#edges()} but
     * preserving the {@code E} type parameter without casts.
     */
    private final List<WeightedEdge<V, E>> weightedEdges = new ArrayList<>();
}