/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.java;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Graph.reverse().
 */
public class GraphReverseTest {

    @Test
    void reverse_produces_directed_graph() {
        Graph<Integer> g = Graph.directed();
        g.addEdge(0, 1);
        assertTrue(g.reverse().isDirected());
    }

    @Test
    void reverse_flips_edge_direction() {
        Graph<Integer> g = Graph.directed();
        g.addEdge(0, 1);
        Graph<Integer> rev = g.reverse();
        // In original: 0 has neighbour 1; in reverse: 1 has neighbour 0
        assertTrue(containsNeighbour(rev, 1, 0));
        assertFalse(containsNeighbour(rev, 0, 1));
    }

    @Test
    void reverse_preserves_vertex_count() {
        Graph<Integer> g = Graph.directed();
        g.addEdge(0, 1);
        g.addEdge(1, 2);
        g.addEdge(2, 3);
        assertEquals(g.vertices().size(), g.reverse().vertices().size());
    }

    @Test
    void reverse_preserves_edge_count() {
        Graph<Integer> g = Graph.directed();
        g.addEdge(0, 1);
        g.addEdge(1, 2);
        g.addEdge(2, 3);
        assertEquals(g.edges().size(), g.reverse().edges().size());
    }

    @Test
    void reverse_twice_yields_original_topology() {
        Graph<Integer> g = Graph.directed();
        g.addEdge(0, 1);
        g.addEdge(1, 2);
        Graph<Integer> revRev = g.reverse().reverse();
        assertTrue(containsNeighbour(revRev, 0, 1));
        assertTrue(containsNeighbour(revRev, 1, 2));
    }

    @Test
    void reverse_of_dag_makes_topological_sort_reverse() {
        Graph<Integer> g = Graph.directed();
        g.addEdge(0, 1);
        g.addEdge(1, 2);
        List<Integer> fwd = TopologicalSort.sort(g).get();
        List<Integer> rev = TopologicalSort.sort(g.reverse()).get();
        // forward: 0,1,2 — reverse DAG: 2,1,0
        assertEquals(fwd.get(0), rev.get(rev.size() - 1));
    }

    @Test
    void undirected_graph_throws() {
        Graph<Integer> g = Graph.undirected();
        g.addEdge(0, 1);
        assertThrows(IllegalStateException.class, g::reverse);
    }

    private static <V> boolean containsNeighbour(Graph<V> g, V from, V to) {
        for (V n : g.neighbours(from))
            if (n.equals(to)) return true;
        return false;
    }
}