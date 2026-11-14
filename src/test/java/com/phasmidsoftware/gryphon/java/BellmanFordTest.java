/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.java;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Bellman-Ford façade, which differs from Dijkstra in accepting
 * negative edge weights and in reporting a reachable negative cycle.
 */
public class BellmanFordTest {

    /**
     * The same fixture as ShortestPathsTest: all weights positive, so
     * Bellman-Ford must agree with Dijkstra on it.
     */
    private static WeightedGraph<Integer, Double> buildDijkstraGraph() {
        WeightedGraph<Integer, Double> g = WeightedGraph.directedWeighted();
        g.addEdge(new WeightedEdge<>(0, 1, 5.0));
        g.addEdge(new WeightedEdge<>(0, 4, 9.0));
        g.addEdge(new WeightedEdge<>(0, 7, 8.0));
        g.addEdge(new WeightedEdge<>(1, 2, 12.0));
        g.addEdge(new WeightedEdge<>(1, 3, 15.0));
        g.addEdge(new WeightedEdge<>(1, 7, 4.0));
        g.addEdge(new WeightedEdge<>(2, 3, 3.0));
        g.addEdge(new WeightedEdge<>(2, 6, 11.0));
        g.addEdge(new WeightedEdge<>(3, 6, 9.0));
        g.addEdge(new WeightedEdge<>(4, 5, 4.0));
        g.addEdge(new WeightedEdge<>(4, 6, 20.0));
        g.addEdge(new WeightedEdge<>(4, 7, 5.0));
        g.addEdge(new WeightedEdge<>(5, 2, 1.0));
        g.addEdge(new WeightedEdge<>(5, 6, 13.0));
        g.addEdge(new WeightedEdge<>(7, 5, 6.0));
        g.addEdge(new WeightedEdge<>(7, 2, 7.0));
        return g;
    }

    @Test
    void bellmanFord_agrees_with_dijkstra_when_all_weights_positive() {
        WeightedGraph<Integer, Double> g = buildDijkstraGraph();

        Map<Integer, WeightedEdge<Integer, Double>> viaDijkstra = ShortestPaths.dijkstra(g, 0);
        Map<Integer, WeightedEdge<Integer, Double>> viaBellmanFord =
                ShortestPaths.bellmanFord(g, 0).orElseThrow();

        assertEquals(viaDijkstra.keySet(), viaBellmanFord.keySet());
        for (int v = 1; v <= 7; v++) {
            assertEquals(viaDijkstra.get(v).from(), viaBellmanFord.get(v).from(), "from mismatch at " + v);
            assertEquals(viaDijkstra.get(v).to(), viaBellmanFord.get(v).to(), "to mismatch at " + v);
            assertEquals(viaDijkstra.get(v).attribute(), viaBellmanFord.get(v).attribute(), "weight mismatch at " + v);
        }
    }

    @Test
    void bellmanFord_start_vertex_is_absent() {
        assertFalse(ShortestPaths.bellmanFord(buildDijkstraGraph(), 0).orElseThrow().containsKey(0));
    }

    /**
     * The direct edge 0→2 costs 0.5; going round through 1 costs 1.0 + (−2.0) = −1.0.
     * Dijkstra would wrongly keep the direct edge, which is the point of Bellman-Ford.
     */
    @Test
    void bellmanFord_prefers_a_cheaper_path_through_a_negative_edge() {
        WeightedGraph<Integer, Double> g = WeightedGraph.directedWeighted();
        g.addEdge(new WeightedEdge<>(0, 1, 1.0));
        g.addEdge(new WeightedEdge<>(1, 2, -2.0));
        g.addEdge(new WeightedEdge<>(0, 2, 0.5));

        Map<Integer, WeightedEdge<Integer, Double>> spt = ShortestPaths.bellmanFord(g, 0).orElseThrow();

        assertEquals(1, spt.get(2).from(), "should reach 2 via 1, not by the direct edge");
        assertEquals(-2.0, spt.get(2).attribute());
    }

    @Test
    void bellmanFord_reports_a_reachable_negative_cycle() {
        WeightedGraph<Integer, Double> g = WeightedGraph.directedWeighted();
        g.addEdge(new WeightedEdge<>(0, 1, 1.0));
        g.addEdge(new WeightedEdge<>(1, 2, -1.0));
        g.addEdge(new WeightedEdge<>(2, 1, -1.0));

        assertEquals(Optional.empty(), ShortestPaths.bellmanFord(g, 0));
    }

    @Test
    void bellmanFord_undirected_throws() {
        WeightedGraph<Integer, Double> g = WeightedGraph.undirectedWeighted();
        g.addEdge(new WeightedEdge<>(0, 1, 1.0));
        assertThrows(IllegalStateException.class, () -> ShortestPaths.bellmanFord(g, 0));
    }

    @Test
    void bellmanFord_option3_matches_option1() {
        WeightedGraph<Integer, Double> g = buildDijkstraGraph();

        Map<Integer, WeightedEdge<Integer, Double>> spt1 =
                ShortestPaths.bellmanFord(g, 0).orElseThrow();
        Map<Integer, WeightedEdge<Integer, Double>> spt3 =
                ShortestPaths.bellmanFord(g, 0, Double::sum, 0.0, Double::compare).orElseThrow();

        for (int v = 1; v <= 7; v++) {
            assertEquals(spt1.get(v).from(), spt3.get(v).from(), "from mismatch at " + v);
            assertEquals(spt1.get(v).attribute(), spt3.get(v).attribute(), "weight mismatch at " + v);
        }
    }
}
