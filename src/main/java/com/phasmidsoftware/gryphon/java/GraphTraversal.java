/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.java;

import java.util.*;
import java.util.function.Function;

/**
 * Internal implementation of BFS and DFS traversals for the Java façade.
 *
 * <p>This class is an implementation detail of {@link Graph} — it is
 * package-private and not part of the public API. Java students interact
 * with traversals only through {@link Graph#bfs} and {@link Graph#dfs}.</p>
 *
 * <p>Both traversals return a parent map: a {@code Map<V, V>} where each
 * entry {@code v → parent} records {@code v}'s parent in the traversal tree.
 * The start vertex maps to itself. Unreachable vertices are absent from the map.</p>
 *
 * <p>Both traversals accept a custom neighbour {@link Function} (Option 3),
 * allowing callers to filter or transform the neighbour set without subclassing.</p>
 *
 * <p>Note: currently implemented directly in Java. A future version will
 * delegate to Gryphon's Scala traversal engine via the materialised Scala
 * graph cache in {@link Graph}.</p>
 */
class GraphTraversal {

    /**
     * Performs a breadth-first search from {@code start}.
     *
     * @param start       the source vertex.
     * @param neighbours  a function returning the neighbours of any vertex.
     * @param allVertices all vertices in the graph (used to initialise state).
     * @param <V>         the vertex type.
     * @return the BFS parent map.
     */
    static <V> Map<V, V> bfs(V start,
                             Function<V, Iterable<V>> neighbours,
                             Set<V> allVertices) {
        Map<V, V> parent = new LinkedHashMap<>();
        Set<V> visited = new HashSet<>();
        Queue<V> frontier = new ArrayDeque<>();

        parent.put(start, start);
        visited.add(start);
        frontier.add(start);

        while (!frontier.isEmpty()) {
            V current = frontier.poll();
            for (V neighbour : neighbours.apply(current)) {
                if (!visited.contains(neighbour)) {
                    visited.add(neighbour);
                    parent.put(neighbour, current);
                    frontier.add(neighbour);
                }
            }
        }
        return Collections.unmodifiableMap(parent);
    }

    /**
     * Performs a depth-first search from {@code start}.
     *
     * <p>Implemented iteratively (using an explicit stack) to avoid
     * stack overflow on large graphs.</p>
     *
     * @param start       the source vertex.
     * @param neighbours  a function returning the neighbours of any vertex.
     * @param allVertices all vertices in the graph.
     * @param <V>         the vertex type.
     * @return the DFS parent map.
     */
    static <V> Map<V, V> dfs(V start,
                             Function<V, Iterable<V>> neighbours,
                             Set<V> allVertices) {
        Map<V, V> parent = new LinkedHashMap<>();
        Set<V> visited = new HashSet<>();
        Deque<V> stack = new ArrayDeque<>();

        parent.put(start, start);
        stack.push(start);

        while (!stack.isEmpty()) {
            V current = stack.pop();
            if (!visited.contains(current)) {
                visited.add(current);
                for (V neighbour : neighbours.apply(current)) {
                    if (!visited.contains(neighbour)) {
                        parent.put(neighbour, current);
                        stack.push(neighbour);
                    }
                }
            }
        }
        return Collections.unmodifiableMap(parent);
    }

    // Not instantiable
    private GraphTraversal() {
    }
}
