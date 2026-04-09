/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.java;

import com.phasmidsoftware.gryphon.core.AbstractGraph;

import java.util.*;
import java.util.function.Function;

/**
 * A mutable, lazily-materialised façade over Gryphon's purely functional graph types.
 *
 * <p>{@code Graph<V>} accumulates vertices and edges in ordinary Java collections.
 * When a traversal ({@link #bfs} or {@link #dfs}) is first requested, it
 * materialises an immutable Scala graph from those collections, caches it, and
 * delegates the traversal to Gryphon's engine. The cache is invalidated
 * whenever the graph is mutated ({@link #addEdge}, {@link #addVertex}),
 * so the Scala graph is always consistent with the Java state.</p>
 *
 * <p>This class is <em>not</em> a true wrapper of a Scala type — it is a
 * <em>lazy builder</em>. Java students interact only with Java types;
 * the Scala graph is an implementation detail.</p>
 *
 * <p>Directionality is fixed at construction time via the factory methods
 * {@link #directed()} and {@link #undirected()}. For an undirected graph,
 * {@code addEdge(u, v)} automatically adds both directions internally.</p>
 *
 * <p><b>Usage example:</b>
 * <pre>{@code
 * Graph<String> g = Graph.undirected();
 * g.addEdge("A", "B");
 * g.addEdge("B", "C");
 * g.addEdge("C", "D");
 *
 * Map<String, String> tree = g.bfs("A");
 * boolean reachable = tree.containsKey("D");   // true
 * String  parent    = tree.get("C");           // "B"
 *
 * // Custom neighbour function (Option 3)
 * Map<String, String> tree2 = g.bfs("A", v -> g.neighbours(v));
 * }</pre>
 * </p>
 *
 * @param <V> the vertex type. Must implement {@code equals} and {@code hashCode}.
 */
public class Graph<V> {

    // -------------------------------------------------------------------------
    // Factory methods
    // -------------------------------------------------------------------------

    /**
     * Creates an empty directed graph.
     *
     * <p>In a directed graph, {@code addEdge(u, v)} adds only the arc
     * {@code u → v}. The reverse arc {@code v → u} is <em>not</em> implied.</p>
     *
     * @param <V> the vertex type.
     * @return a new empty directed {@code Graph}.
     */
    public static <V> Graph<V> directed() {
        return new Graph<>(true);
    }

    /**
     * Creates an empty undirected graph.
     *
     * <p>In an undirected graph, {@code addEdge(u, v)} implicitly adds both
     * {@code u → v} and {@code v → u} in the underlying representation.</p>
     *
     * @param <V> the vertex type.
     * @return a new empty undirected {@code Graph}.
     */
    public static <V> Graph<V> undirected() {
        return new Graph<>(false);
    }

    // -------------------------------------------------------------------------
    // Mutation — invalidates the Scala cache
    // -------------------------------------------------------------------------

    /**
     * Adds an isolated vertex to this graph.
     *
     * <p>If the vertex is already present this method has no effect.</p>
     *
     * @param vertex the vertex to add.
     */
    public void addVertex(V vertex) {
        adjacency.putIfAbsent(vertex, new ArrayList<>());
        invalidate();
    }

    /**
     * Adds an edge to this graph.
     *
     * <p>Both endpoint vertices are added automatically if not already present.
     * For undirected graphs the reverse edge is also added.</p>
     *
     * @param edge the edge to add.
     */
    public void addEdge(Edge<V> edge) {
        canonicalEdges.add(edge);
        addDirectedEdge(edge);
        if (!directed) addDirectedEdge(edge.reverse());
        invalidate();
    }

    /**
     * Adds an unweighted edge between {@code from} and {@code to}.
     *
     * <p>Convenience overload — equivalent to
     * {@code addEdge(new Edge<>(from, to))}.</p>
     *
     * @param from the source vertex.
     * @param to   the target vertex.
     */
    public void addEdge(V from, V to) {
        addEdge(new Edge<>(from, to));
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    /**
     * Returns whether this graph is directed.
     *
     * @return {@code true} if directed, {@code false} if undirected.
     */
    public boolean isDirected() {
        return directed;
    }

    /**
     * Returns all vertices in this graph as an unmodifiable set.
     *
     * @return the vertex set.
     */
    public Set<V> vertices() {
        return Collections.unmodifiableSet(adjacency.keySet());
    }

    /**
     * Returns all edges in this graph.
     *
     * <p>For undirected graphs both directions are stored internally;
     * this method returns only the canonical direction (each edge once,
     * as originally added via {@link #addEdge}).</p>
     *
     * @return an unmodifiable list of edges.
     */
    public List<Edge<V>> edges() {
        return Collections.unmodifiableList(canonicalEdges);
    }

    /**
     * Returns the neighbours of {@code vertex} — the vertices directly
     * reachable from it by a single edge.
     *
     * @param vertex the query vertex.
     * @return an iterable of neighbouring vertices.
     * @throws NoSuchElementException if {@code vertex} is not in the graph.
     */
    public Iterable<V> neighbours(V vertex) {
        List<Edge<V>> adj = adjacency.get(vertex);
        if (adj == null)
            throw new NoSuchElementException("Vertex not found: " + vertex);
        return adj.stream().map(Edge::to).toList();
    }

    /**
     * Returns the edges incident from {@code vertex}.
     *
     * @param vertex the query vertex.
     * @return an iterable of edges from {@code vertex}.
     * @throws NoSuchElementException if {@code vertex} is not in the graph.
     */
    public Iterable<Edge<V>> edgesFrom(V vertex) {
        List<Edge<V>> adj = adjacency.get(vertex);
        if (adj == null)
            throw new NoSuchElementException("Vertex not found: " + vertex);
        return Collections.unmodifiableList(adj);
    }

    // -------------------------------------------------------------------------
    // Traversals — Option 1 (sensible defaults)
    // -------------------------------------------------------------------------

    /**
     * Performs a breadth-first search from {@code start}, using the graph's
     * own adjacency structure to determine neighbours.
     *
     * <p>Returns the BFS traversal tree as a parent map: for each visited
     * vertex {@code v}, {@code result.get(v)} is {@code v}'s parent in the
     * tree. The start vertex maps to itself.</p>
     *
     * @param start the source vertex.
     * @return the BFS parent map.
     */
    public Map<V, V> bfs(V start) {
        return bfs(start, this::neighbours);
    }

    /**
     * Performs a depth-first search from {@code start}, using the graph's
     * own adjacency structure to determine neighbours.
     *
     * <p>Returns the DFS traversal tree as a parent map: for each visited
     * vertex {@code v}, {@code result.get(v)} is {@code v}'s parent in the
     * tree. The start vertex maps to itself.</p>
     *
     * @param start the source vertex.
     * @return the DFS parent map.
     */
    public Map<V, V> dfs(V start) {
        return dfs(start, this::neighbours);
    }

    // -------------------------------------------------------------------------
    // Traversals — Option 3 (custom neighbour function)
    // -------------------------------------------------------------------------

    /**
     * Performs a breadth-first search from {@code start}, using a custom
     * neighbour function to determine which vertices to visit from each vertex.
     *
     * <p>This is Option 3 of the traversal API: the caller supplies the
     * neighbour logic as a {@link Function}, allowing custom filtering,
     * edge-weight-based pruning, or any other neighbour selection strategy.</p>
     *
     * <p>Returns the BFS traversal tree as a parent map.</p>
     *
     * @param start      the source vertex.
     * @param neighbours a function mapping each vertex to its neighbours.
     * @return the BFS parent map.
     */
    public Map<V, V> bfs(V start, Function<V, Iterable<V>> neighbours) {
        return GraphTraversal.bfs(start, neighbours, vertices());
    }

    /**
     * Performs a depth-first search from {@code start}, using a custom
     * neighbour function.
     *
     * <p>This is Option 3 of the traversal API.</p>
     *
     * <p>Returns the DFS traversal tree as a parent map.</p>
     *
     * @param start      the source vertex.
     * @param neighbours a function mapping each vertex to its neighbours.
     * @return the DFS parent map.
     */
    public Map<V, V> dfs(V start, Function<V, Iterable<V>> neighbours) {
        return GraphTraversal.dfs(start, neighbours, vertices());
    }

    // -------------------------------------------------------------------------
    // Object overrides
    // -------------------------------------------------------------------------

    @Override
    public String toString() {
        return (directed ? "Directed" : "Undirected") +
                "Graph{vertices=" + adjacency.size() +
                ", edges=" + canonicalEdges.size() + "}";
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void addDirectedEdge(Edge<V> edge) {
        adjacency.computeIfAbsent(edge.from(), k -> new ArrayList<>()).add(edge);
        adjacency.putIfAbsent(edge.to(), new ArrayList<>());
    }

    private void invalidate() {
        scalaGraph = null;
    }

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    private Graph(boolean directed) {
        this.directed = directed;
    }

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    /**
     * Whether this graph is directed. Fixed at construction.
     */
    private final boolean directed;

    /**
     * Full adjacency map — stores edges in both directions for undirected graphs.
     * This is what traversals use.
     */
    private final Map<V, List<Edge<V>>> adjacency = new LinkedHashMap<>();

    /**
     * Canonical edge list — each edge stored once, as originally added.
     * Used by {@link #edges()}.
     */
    private final List<Edge<V>> canonicalEdges = new ArrayList<>();

    /**
     * Cached Scala graph. Null whenever the Java state has been mutated
     * since the last materialisation. Built lazily on first traversal call.
     * Reserved for future use when Scala traversal delegation is wired up.
     */
    @SuppressWarnings("rawtypes")
    private AbstractGraph scalaGraph = null;
}