/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.java;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the acyclic shortest-paths façade: linear-time shortest paths in a
 * DAG, by relaxing edges in topological order.
 */
public class AcyclicShortestPathsTest {

    /**
     * A diamond. 0→3 costs 7 through vertex 1 and 10 through vertex 2,
     * so the shortest path to 3 arrives from 1.
     */
    private static WeightedGraph<Integer, Double> buildDiamond() {
        WeightedGraph<Integer, Double> g = WeightedGraph.directedWeighted();
        g.addEdge(new WeightedEdge<>(0, 1, 5.0));
        g.addEdge(new WeightedEdge<>(0, 2, 3.0));
        g.addEdge(new WeightedEdge<>(1, 3, 2.0));
        g.addEdge(new WeightedEdge<>(2, 3, 7.0));
        return g;
    }

    @Test
    void acyclic_finds_the_cheaper_branch() {
        Map<Integer, WeightedEdge<Integer, Double>> spt = ShortestPaths.acyclic(buildDiamond(), 0);

        assertEquals(1, spt.get(3).from(), "should reach 3 via 1 (cost 7), not via 2 (cost 10)");
        assertEquals(0, spt.get(1).from());
        assertEquals(0, spt.get(2).from());
    }

    @Test
    void acyclic_start_vertex_is_absent() {
        assertFalse(ShortestPaths.acyclic(buildDiamond(), 0).containsKey(0));
    }

    @Test
    void acyclic_agrees_with_dijkstra_on_a_dag() {
        WeightedGraph<Integer, Double> g = buildDiamond();

        Map<Integer, WeightedEdge<Integer, Double>> viaDijkstra = ShortestPaths.dijkstra(g, 0);
        Map<Integer, WeightedEdge<Integer, Double>> viaAcyclic = ShortestPaths.acyclic(g, 0);

        assertEquals(viaDijkstra.keySet(), viaAcyclic.keySet());
        for (int v : viaDijkstra.keySet()) {
            assertEquals(viaDijkstra.get(v).from(), viaAcyclic.get(v).from(), "from mismatch at " + v);
            assertEquals(viaDijkstra.get(v).attribute(), viaAcyclic.get(v).attribute(), "weight mismatch at " + v);
        }
    }

    /**
     * Negative weights are safe in a DAG: with 1→3 at −4, the route through 1
     * costs 1 and the route through 2 still costs 10.
     */
    @Test
    void acyclic_allows_negative_weights() {
        WeightedGraph<Integer, Double> g = WeightedGraph.directedWeighted();
        g.addEdge(new WeightedEdge<>(0, 1, 5.0));
        g.addEdge(new WeightedEdge<>(0, 2, 3.0));
        g.addEdge(new WeightedEdge<>(1, 3, -4.0));
        g.addEdge(new WeightedEdge<>(2, 3, 7.0));

        Map<Integer, WeightedEdge<Integer, Double>> spt = ShortestPaths.acyclic(g, 0);

        assertEquals(1, spt.get(3).from());
        assertEquals(-4.0, spt.get(3).attribute());
    }

    @Test
    void acyclic_rejects_a_cyclic_graph() {
        WeightedGraph<Integer, Double> g = WeightedGraph.directedWeighted();
        g.addEdge(new WeightedEdge<>(0, 1, 1.0));
        g.addEdge(new WeightedEdge<>(1, 2, 1.0));
        g.addEdge(new WeightedEdge<>(2, 0, 1.0));

        assertThrows(IllegalArgumentException.class, () -> ShortestPaths.acyclic(g, 0));
    }

    @Test
    void acyclic_undirected_throws() {
        WeightedGraph<Integer, Double> g = WeightedGraph.undirectedWeighted();
        g.addEdge(new WeightedEdge<>(0, 1, 1.0));
        assertThrows(IllegalStateException.class, () -> ShortestPaths.acyclic(g, 0));
    }

    @Test
    void acyclic_option3_matches_option1() {
        WeightedGraph<Integer, Double> g = buildDiamond();

        Map<Integer, WeightedEdge<Integer, Double>> spt1 = ShortestPaths.acyclic(g, 0);
        Map<Integer, WeightedEdge<Integer, Double>> spt3 =
                ShortestPaths.acyclic(g, 0, Double::sum, 0.0, Double::compare);

        assertEquals(spt1.keySet(), spt3.keySet());
        for (int v : spt1.keySet())
            assertEquals(spt1.get(v).from(), spt3.get(v).from(), "from mismatch at " + v);
    }
}
