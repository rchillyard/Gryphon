/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.java;

import java.util.*;
import java.util.function.Function;

/**
 * Internal implementation of BFS and DFS traversals for the Java façade,
 * used for Option 3 (custom neighbour function) only.
 *
 * <p>Option 1 BFS and DFS now delegate to the Scala Visitor engine via
 * {@link JavaFacadeBridge}. This class handles only the Option 3 case where
 * the caller supplies a custom neighbour {@link Function}.</p>
 *
 * <p>Both traversals return a came-from map: a {@code Map<V, V>} where each
 * entry {@code v → cameFrom} records the vertex from which {@code v} was first
 * discovered. The start vertex is absent from the map — it has no predecessor.
 * Unreachable vertices are also absent.</p>
 *
 * <p>Path reconstruction: walk {@code map.get(v)} until the key is absent —
 * that vertex is the start.</p>
 */
class GraphTraversal {

    /**
     * Performs a breadth-first search from {@code start} using a custom
     * neighbour function.
     *
     * @param start      the source vertex.
     * @param neighbours a function returning the neighbours of any vertex.
     * @param <V>        the vertex type.
     * @return the BFS came-from map; start vertex absent.
     */
    static <V> Map<V, V> bfs(V start, Function<V, Iterable<V>> neighbours) {
        Map<V, V> cameFrom = new LinkedHashMap<>();
        Set<V> visited = new HashSet<>();
        Queue<V> frontier = new ArrayDeque<>();

        visited.add(start);
        frontier.add(start);

        while (!frontier.isEmpty()) {
            V current = frontier.poll();
            for (V neighbour : neighbours.apply(current)) {
                if (!visited.contains(neighbour)) {
                    visited.add(neighbour);
                    cameFrom.put(neighbour, current);
                    frontier.add(neighbour);
                }
            }
        }
        return Collections.unmodifiableMap(cameFrom);
    }

    /**
     * Performs a depth-first search from {@code start} using a custom
     * neighbour function.
     *
     * <p>Implemented iteratively (using an explicit stack) to avoid
     * stack overflow on large graphs.</p>
     *
     * @param start      the source vertex.
     * @param neighbours a function returning the neighbours of any vertex.
     * @param <V>        the vertex type.
     * @return the DFS came-from map; start vertex absent.
     */
    static <V> Map<V, V> dfs(V start, Function<V, Iterable<V>> neighbours) {
        Map<V, V> cameFrom = new LinkedHashMap<>();
        Set<V> visited = new HashSet<>();
        Deque<V> stack = new ArrayDeque<>();

        stack.push(start);

        while (!stack.isEmpty()) {
            V current = stack.pop();
            if (!visited.contains(current)) {
                visited.add(current);
                for (V neighbour : neighbours.apply(current)) {
                    if (!visited.contains(neighbour)) {
                        cameFrom.put(neighbour, current);
                        stack.push(neighbour);
                    }
                }
            }
        }
        return Collections.unmodifiableMap(cameFrom);
    }

    // Not instantiable
    private GraphTraversal() {
    }
}