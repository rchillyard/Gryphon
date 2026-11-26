/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.java;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the TopologicalSort Java façade.
 * <p>
 * dag.graph — Sedgewick & Wayne topological sort demo:
 * 7 vertices (0–6), 11 directed edges, no cycle.
 */
public class TopologicalSortTest {

    // Simple DAG: 0->1->3, 0->2->3
    private static Graph<Integer> buildSimpleDAG() {
        Graph<Integer> g = Graph.directed();
        g.addEdge(0, 1);
        g.addEdge(0, 2);
        g.addEdge(1, 3);
        g.addEdge(2, 3);
        return g;
    }

    // Cyclic graph
    private static Graph<Integer> buildCyclicGraph() {
        Graph<Integer> g = Graph.directed();
        g.addEdge(0, 1);
        g.addEdge(1, 2);
        g.addEdge(2, 0);
        return g;
    }

    @Test
    void sort_returns_non_empty_for_dag() {
        Optional<List<Integer>> result = TopologicalSort.sort(buildSimpleDAG());
        assertTrue(result.isPresent());
    }

    @Test
    void sort_returns_all_vertices() {
        List<Integer> order = TopologicalSort.sort(buildSimpleDAG()).get();
        assertEquals(4, order.size());
        assertTrue(order.containsAll(List.of(0, 1, 2, 3)));
    }

    @Test
    void sort_respects_topological_order() {
        List<Integer> order = TopologicalSort.sort(buildSimpleDAG()).get();
        // 0 must come before 1, 2, 3; 1 and 2 must come before 3
        assertTrue(order.indexOf(0) < order.indexOf(1));
        assertTrue(order.indexOf(0) < order.indexOf(2));
        assertTrue(order.indexOf(1) < order.indexOf(3));
        assertTrue(order.indexOf(2) < order.indexOf(3));
    }

    @Test
    void sort_returns_empty_for_cyclic_graph() {
        Optional<List<Integer>> result = TopologicalSort.sort(buildCyclicGraph());
        assertFalse(result.isPresent());
    }

    // A source with four leaf children. Unlike buildSimpleDAG, this one is NOT invariant under
    // adjacency shuffling: however the sort's loop orders its start vertices, at most one leaf is
    // visited before 0 is expanded, leaving three or more unvisited children whose post-order
    // depends entirely on the shuffle. buildSimpleDAG cannot vary — its keySet order is
    // [1, 0, 2, 3], so the DFS from 1 records 3 and 1, and by the time 0 is expanded only 2 is
    // left to choose. See #139.
    private static Graph<Integer> buildStarDAG() {
        Graph<Integer> g = Graph.directed();
        g.addEdge(0, 1);
        g.addEdge(0, 2);
        g.addEdge(0, 3);
        g.addEdge(0, 4);
        return g;
    }

    /**
     * Issue #139: the facade reads {@code DirectedGraph.topologicalOrder}, the lazy val from
     * #118, rather than re-running the algorithm on every call.
     *
     * <p>{@code ShortestPaths.acyclic} tells Java callers in its own javadoc to check
     * {@code isDAG} first, and {@code isDAG} is {@code sort(graph).isPresent()}. Before the fix
     * that idiom sorted, discarded the ordering, and sorted again inside {@code acyclic} — which
     * does read {@code topologicalOrder}.</p>
     *
     * <p>The saving is not directly observable, but a consequence of it is. Adjacency iteration
     * is deliberately shuffled, and {@code TopologicalSort.sort} takes no {@code Random} of its
     * own, so each vertex expansion builds a fresh one (#131). Running the algorithm repeatedly
     * on a graph that is sensitive to the shuffle therefore yields different valid orderings,
     * while one memoized value cannot. Equality across calls witnesses the memoization.</p>
     *
     * <p>The graph matters: this assertion passes with or without the fix on
     * {@code buildSimpleDAG}, whose result is invariant under shuffling. {@code buildStarDAG}
     * is not, and fails without the fix.</p>
     */
    @Test
    void repeated_sorts_of_one_graph_agree() {
        Graph<Integer> g = buildStarDAG();
        List<Integer> first = TopologicalSort.sort(g).get();
        for (int i = 0; i < 50; i++) {
            assertEquals(first, TopologicalSort.sort(g).get());
        }
        assertTrue(TopologicalSort.isDAG(g));
        assertEquals(first, TopologicalSort.sort(g).get());
    }

    @Test
    void isDAG_true_for_dag() {
        assertTrue(TopologicalSort.isDAG(buildSimpleDAG()));
    }

    @Test
    void isDAG_false_for_cyclic() {
        assertFalse(TopologicalSort.isDAG(buildCyclicGraph()));
    }

    @Test
    void undirected_graph_throws() {
        Graph<Integer> g = Graph.undirected();
        g.addEdge(0, 1);
        assertThrows(IllegalStateException.class, () -> TopologicalSort.sort(g));
    }
}