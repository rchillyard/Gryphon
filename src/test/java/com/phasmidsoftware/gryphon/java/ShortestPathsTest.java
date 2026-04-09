/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.java;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ShortestPathsTest {

    /**
     * Builds the dijkstra.graph test fixture as a Java Graph<Integer>.
     * Mirrors the graph loaded from dijkstra.graph in ShortestPathsSpec.scala.
     * Edges and weights taken directly from the Scala spec assertions.
     */
    private static Graph<Integer> buildDijkstraGraph() {
        Graph<Integer> g = Graph.directed();
        g.addEdge(new WeightedEdge<>(0, 1, 5.0));
        g.addEdge(new WeightedEdge<>(0, 4, 9.0));
        g.addEdge(new WeightedEdge<>(0, 7, 8.0));
        g.addEdge(new WeightedEdge<>(1, 2, 12.0));
        g.addEdge(new WeightedEdge<>(1, 3, 15.0));
        g.addEdge(new WeightedEdge<>(1, 7, 4.0));  // was missing
        g.addEdge(new WeightedEdge<>(2, 3, 3.0));
        g.addEdge(new WeightedEdge<>(2, 6, 11.0));
        g.addEdge(new WeightedEdge<>(3, 6, 9.0));
        g.addEdge(new WeightedEdge<>(4, 5, 4.0));
        g.addEdge(new WeightedEdge<>(4, 6, 20.0));
        g.addEdge(new WeightedEdge<>(4, 7, 5.0));
        g.addEdge(new WeightedEdge<>(5, 2, 1.0));
        g.addEdge(new WeightedEdge<>(5, 6, 13.0));
        g.addEdge(new WeightedEdge<>(7, 5, 6.0));
        g.addEdge(new WeightedEdge<>(7, 2, 7.0));  // was 7→4 (wrong)
        return g;
    }

    @Test
    void dijkstra_spt_matches_scala_spec() {
        Graph<Integer> g = buildDijkstraGraph();
        Map<Integer, WeightedEdge<Integer, Double>> spt = ShortestPaths.dijkstra(g, 0);

        // Start vertex is absent
        assertFalse(spt.containsKey(0));

        // Match the Scala spec assertions exactly
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
        Graph<Integer> g = buildDijkstraGraph();
        Map<Integer, WeightedEdge<Integer, Double>> spt = ShortestPaths.dijkstra(g, 0);
        // Vertices 1–7 must all appear in the SPT
        for (int v = 1; v <= 7; v++)
            assertTrue(spt.containsKey(v), "vertex " + v + " missing from SPT");
    }

    @Test
    void dijkstra_undirected_throws() {
        Graph<Integer> g = Graph.undirected();
        g.addEdge(new WeightedEdge<>(0, 1, 1.0));
        assertThrows(IllegalStateException.class, () -> ShortestPaths.dijkstra(g, 0));
    }

    @Test
    void dijkstra_option3_matches_option1() {
        Graph<Integer> g = buildDijkstraGraph();

        Map<Integer, WeightedEdge<Integer, Double>> spt1 = ShortestPaths.dijkstra(g, 0);
        Map<Integer, WeightedEdge<Integer, Double>> spt3 = ShortestPaths.dijkstra(
                g, 0,
                e -> ((WeightedEdge<Integer, Double>) e).attribute(),
                Double::sum,
                0.0);

        // Both options must agree on the full SPT
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