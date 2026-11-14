/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.java;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for in-degree and out-degree, which — unlike `degree` — are defined for
 * directed graphs as well as undirected ones.
 */
public class DegreeTest {

    /** 0 → 1, 0 → 2, 1 → 2, 3 → 0. Vertex 0 has out-degree 2 and in-degree 1. */
    private static Graph<Integer> digraph() {
        Graph<Integer> g = Graph.directed();
        g.addEdge(0, 1);
        g.addEdge(0, 2);
        g.addEdge(1, 2);
        g.addEdge(3, 0);
        return g;
    }

    // -------------------------------------------------------------------------
    // Directed
    // -------------------------------------------------------------------------

    @Test
    void outDegree_counts_edges_leaving() {
        Graph<Integer> g = digraph();
        assertEquals(2, g.outDegree(0));
        assertEquals(1, g.outDegree(1));
        assertEquals(0, g.outDegree(2), "2 is a sink");
        assertEquals(1, g.outDegree(3));
    }

    @Test
    void inDegree_counts_edges_arriving() {
        Graph<Integer> g = digraph();
        assertEquals(1, g.inDegree(0));
        assertEquals(1, g.inDegree(1));
        assertEquals(2, g.inDegree(2));
        assertEquals(0, g.inDegree(3), "3 is a source");
    }

    /**
     * Every arc leaves exactly one vertex and arrives at exactly one, so both
     * sums equal the edge count. This catches a `white`/`black` mix-up that
     * per-vertex assertions could miss if it were symmetric.
     */
    @Test
    void both_degree_sums_equal_the_edge_count() {
        Graph<Integer> g = digraph();
        int in = 0;
        int out = 0;
        for (int v : g.vertices()) {
            in += g.inDegree(v);
            out += g.outDegree(v);
        }
        assertEquals(g.size(), in, "in-degrees should sum to the edge count");
        assertEquals(g.size(), out, "out-degrees should sum to the edge count");
    }

    /**
     * The direction really is distinguished: reversing the graph swaps the two.
     */
    @Test
    void reversing_the_graph_swaps_in_and_out() {
        Graph<Integer> g = digraph();
        Graph<Integer> r = g.reverse();
        for (int v : g.vertices()) {
            assertEquals(g.outDegree(v), r.inDegree(v), "out-degree of " + v);
            assertEquals(g.inDegree(v), r.outDegree(v), "in-degree of " + v);
        }
    }

    @Test
    void a_self_loop_counts_once_each_way() {
        Graph<Integer> g = Graph.directed();
        g.addEdge(0, 0);
        assertEquals(1, g.inDegree(0));
        assertEquals(1, g.outDegree(0));
    }

    // -------------------------------------------------------------------------
    // Undirected
    // -------------------------------------------------------------------------

    @Test
    void undirected_in_and_out_degree_are_both_the_degree() {
        Graph<Integer> g = Graph.undirected();
        g.addEdge(0, 1);
        g.addEdge(1, 2);

        for (int v : g.vertices()) {
            assertEquals(g.degree(v), g.inDegree(v), "in-degree at " + v);
            assertEquals(g.degree(v), g.outDegree(v), "out-degree at " + v);
        }
        assertEquals(2, g.degree(1));
    }

    /**
     * `degree` still refuses a directed graph, but `inDegree` and `outDegree`
     * answer — which is the gap these two close.
     */
    @Test
    void the_degree_family_still_refuses_a_directed_graph() {
        Graph<Integer> g = digraph();
        assertThrows(com.phasmidsoftware.gryphon.core.GraphException.class, () -> g.degree(0));
        assertDoesNotThrow(() -> g.inDegree(0));
        assertDoesNotThrow(() -> g.outDegree(0));
    }

    @Test
    void a_weighted_graph_has_them_too() {
        WeightedGraph<Integer, Double> g = WeightedGraph.directedWeighted();
        g.addEdge(new WeightedEdge<>(0, 1, 1.0));
        g.addEdge(new WeightedEdge<>(0, 2, 2.0));

        assertEquals(2, g.outDegree(0));
        assertEquals(0, g.inDegree(0));
        assertEquals(1, g.inDegree(1));
    }
}
