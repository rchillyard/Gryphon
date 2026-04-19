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
 * When any algorithm is requested, it materialises an immutable Scala graph from
 * those collections, caches it, and delegates to Gryphon's Scala engine. The cache
 * is invalidated whenever the graph is mutated ({@link #addEdge}, {@link #addVertex}),
 * so the Scala graph is always consistent with the Java state.</p>
 *
 * <p>All BFS and DFS traversals — both Option 1 and Option 3 — delegate to the
 * Scala Visitor engine via {@link JavaFacadeBridge} and return a <em>came-from
 * map</em>: for each discovered vertex {@code v}, {@code result.get(v)} is the
 * vertex from which {@code v} was first discovered. The start vertex is absent
 * from the map — it has no predecessor. This is consistent with all other
 * algorithm façades in Gryphon (Dijkstra SPT, Prim/Kruskal MST, Kosaraju SCC).</p>
 *
 * <p><b>Path reconstruction:</b>
 * <pre>{@code
 * Map<String, String> tree = g.bfs("A");
 * List<String> path = new ArrayList<>();
 * String v = "D";
 * path.add(v);
 * while (tree.containsKey(v)) { v = tree.get(v); path.add(0, v); }
 * // path == ["A", "B", "C", "D"]
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
     * Returns the neighbours of {@code vertex}.
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
    // Traversals — Option 1 (delegates to Scala Visitor engine)
    // -------------------------------------------------------------------------

    /**
     * Performs a breadth-first search from {@code start}.
     *
     * <p>Returns a came-from map; start vertex absent.</p>
     *
     * <p>Path reconstruction: walk {@code result.get(v)} until the key is
     * absent — that vertex is the start.</p>
     *
     * @param start the source vertex.
     * @return the BFS came-from map.
     */
    public Map<V, V> bfs(V start) {
        return JavaFacadeBridge$.MODULE$.bfs(getScalaGraph(), start);
    }

    /**
     * Performs a depth-first search from {@code start}.
     *
     * <p>Returns a came-from map; start vertex absent.</p>
     *
     * @param start the source vertex.
     * @return the DFS came-from map.
     */
    public Map<V, V> dfs(V start) {
        return JavaFacadeBridge$.MODULE$.dfs(getScalaGraph(), start);
    }

    // -------------------------------------------------------------------------
    // Traversals — Option 3 (custom neighbour function, delegates to Scala engine)
    // -------------------------------------------------------------------------

    /**
     * Performs a breadth-first search from {@code start} using a custom
     * neighbour function.
     *
     * <p>The neighbour function is wrapped into a Scala {@code Neighbours[V,V]}
     * given instance and passed to the Visitor engine, ensuring consistent
     * behaviour with Option 1. Returns a came-from map; start vertex absent.</p>
     *
     * @param start      the source vertex.
     * @param neighbours a function mapping each vertex to its neighbours.
     * @return the BFS came-from map.
     */
    public Map<V, V> bfs(V start, Function<V, Iterable<V>> neighbours) {
        return JavaFacadeBridge$.MODULE$.bfsWithNeighbours(
                getScalaGraph(), start, neighbours);
    }

    /**
     * Performs a depth-first search from {@code start} using a custom
     * neighbour function.
     *
     * <p>The neighbour function is wrapped into a Scala {@code Neighbours[V,V]}
     * given instance and passed to the Visitor engine, ensuring consistent
     * behaviour with Option 1. Returns a came-from map; start vertex absent.</p>
     *
     * @param start      the source vertex.
     * @param neighbours a function mapping each vertex to its neighbours.
     * @return the DFS came-from map.
     */
    public Map<V, V> dfs(V start, Function<V, Iterable<V>> neighbours) {
        return JavaFacadeBridge$.MODULE$.dfsWithNeighbours(
                getScalaGraph(), start, neighbours);
    }

    // -------------------------------------------------------------------------
    // Scala graph materialisation (package-private)
    // -------------------------------------------------------------------------

    /**
     * Returns the materialised Scala graph, building it from the canonical edge
     * list if the cache has been invalidated.
     *
     * <p>This method is package-private. Java students do not call it directly;
     * it is used internally by {@code ShortestPaths}, {@code MinimumSpanningTree},
     * and other algorithm façades that delegate to the Scala engine.</p>
     *
     * @return the cached (or freshly built) Scala {@code AbstractGraph}.
     */
    @SuppressWarnings("unchecked")
    AbstractGraph<V> getScalaGraph() {
        if (scalaGraph == null)
            scalaGraph = JavaFacadeBridge$.MODULE$.materialise(canonicalEdges, directed);
        return (AbstractGraph<V>) scalaGraph;
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

    Graph(boolean directed) {
        this.directed = directed;
    }

    /**
     * Package-private constructor for graphs loaded from a resource file.
     * The pre-built Scala graph is injected directly into the cache,
     * bypassing the normal materialisation path. The Java edge and
     * adjacency collections are left empty — this graph is intended
     * for read-only algorithm use via the façade, not further mutation.
     *
     * @param directed           whether the graph is directed.
     * @param prebuiltScalaGraph the already-constructed Scala graph.
     */
    @SuppressWarnings("unchecked")
    Graph(boolean directed, Object prebuiltScalaGraph) {
        this.directed = directed;
        this.scalaGraph = (AbstractGraph<V>) prebuiltScalaGraph;
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
     */
    @SuppressWarnings("rawtypes")
    private AbstractGraph scalaGraph = null;
}