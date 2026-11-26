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

    /**
     * The facade returns the same ordering every time it is asked.
     *
     * <p><b>This does not test #139.</b> #139 routed the bridge through
     * {@code DirectedGraph.topologicalOrder}, the lazy val from #118, so that
     * {@code isDAG} followed by {@code ShortestPaths.acyclic} — the sequence the facade's own
     * javadoc prescribes — sorts once rather than twice. That is a saving, not a behaviour, and
     * nothing here can see it: this test was written expecting it to fail against the old code,
     * and it passed. The ordering is deterministic in practice, so repeated runs of the
     * algorithm agree with each other just as a memoized value does.</p>
     *
     * <p>It is kept because the property is worth pinning on its own account — a facade that
     * returned a different valid ordering each call would be a nuisance to any caller comparing
     * results — but reverting #139 will not make it fail. The determinism it rests on is
     * incidental rather than guaranteed; see #131, which is about where the orderings get their
     * {@code Random}.</p>
     */
    @Test
    void repeated_sorts_of_one_graph_agree() {
        Graph<Integer> g = buildSimpleDAG();
        List<Integer> first = TopologicalSort.sort(g).get();
        for (int i = 0; i < 20; i++) {
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