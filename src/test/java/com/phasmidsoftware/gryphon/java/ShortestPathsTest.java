/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.java;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

public class ShortestPathsTest {

    /**
     * Builds the dijkstra.graph test fixture as a WeightedGraph<Integer, Double>.
     * Mirrors the graph loaded from dijkstra.graph in ShortestPathsSpec.scala.
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
    void dijkstra_spt_matches_scala_spec() {
        WeightedGraph<Integer, Double> g = buildDijkstraGraph();
        Map<Integer, WeightedEdge<Integer, Double>> spt = ShortestPaths.dijkstra(g, 0);

        assertFalse(spt.containsKey(0));

        assertSptEdge(spt, 1, 0, 1, 5.0);
        assertSptEdge(spt, 2, 5, 2, 1.0);
        assertSptEdge(spt, 3, 2, 3, 3.0);
        assertSptEdge(spt, 4, 0, 4, 9.0);
        assertSptEdge(spt, 5, 4, 5, 4.0);
        assertSptEdge(spt, 6, 2, 6, 11.0);
        assertSptEdge(spt, 7, 0, 7, 8.0);
    }

    @Test
    void dijkstra_all_vertices_reachable() {
        WeightedGraph<Integer, Double> g = buildDijkstraGraph();
        Map<Integer, WeightedEdge<Integer, Double>> spt = ShortestPaths.dijkstra(g, 0);
        for (int v = 1; v <= 7; v++)
            assertTrue(spt.containsKey(v), "vertex " + v + " missing from SPT");
    }

    @Test
    void dijkstra_undirected_throws() {
        WeightedGraph<Integer, Double> g = WeightedGraph.undirectedWeighted();
        g.addEdge(new WeightedEdge<>(0, 1, 1.0));
        assertThrows(IllegalStateException.class, () -> ShortestPaths.dijkstra(g, 0));
    }

    @Test
    void dijkstra_option3_matches_option1() {
        WeightedGraph<Integer, Double> g = buildDijkstraGraph();

        Map<Integer, WeightedEdge<Integer, Double>> spt1 = ShortestPaths.dijkstra(g, 0);
        Map<Integer, WeightedEdge<Integer, Double>> spt3 = ShortestPaths.dijkstra(
                g, 0,
                Double::sum,
                0.0,
                Double::compare);

        for (int v = 1; v <= 7; v++) {
            assertEquals(spt1.get(v).from(), spt3.get(v).from(), "from mismatch at " + v);
            assertEquals(spt1.get(v).to(), spt3.get(v).to(), "to mismatch at " + v);
            assertEquals(spt1.get(v).attribute(), spt3.get(v).attribute(), "weight mismatch at " + v);
        }
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private static void assertSptEdge(
            Map<Integer, WeightedEdge<Integer, Double>> spt,
            int vertex, int expectedFrom, int expectedTo, double expectedWeight) {
        assertTrue(spt.containsKey(vertex), "SPT missing vertex " + vertex);
        WeightedEdge<Integer, Double> e = spt.get(vertex);
        assertEquals(expectedFrom, e.from(), "wrong from at vertex " + vertex);
        assertEquals(expectedTo, e.to(), "wrong to at vertex " + vertex);
        assertEquals(expectedWeight, e.attribute(), "wrong weight at vertex " + vertex);
    }
}