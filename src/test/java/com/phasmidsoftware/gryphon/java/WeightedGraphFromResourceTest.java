/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.java;

import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for WeightedGraph.undirectedFromResource and directedFromResource.
 * <p>
 * prim.graph (Sedgewick tinyEWG) — 8 vertices (0–7), 16 undirected weighted edges.
 * MST total weight: 1.81
 */
public class WeightedGraphFromResourceTest {

    // -------------------------------------------------------------------------
    // undirectedFromResource — Option 1
    // -------------------------------------------------------------------------

    @Test
    void undirectedFromResource_loads_prim_graph() {
        WeightedGraph<Integer, Double> g = WeightedGraph.undirectedFromResource("prim.graph");
        assertNotNull(g);
    }

    @Test
    void undirectedFromResource_is_undirected() {
        WeightedGraph<Integer, Double> g = WeightedGraph.undirectedFromResource("prim.graph");
        assertFalse(g.isDirected());
    }

    @Test
    void undirectedFromResource_kruskal_produces_7_edges() {
        WeightedGraph<Integer, Double> g = WeightedGraph.undirectedFromResource("prim.graph");
        List<WeightedEdge<Integer, Double>> mst = MinimumSpanningTree.kruskal(g);
        assertEquals(7, mst.size());
    }

    @Test
    void undirectedFromResource_kruskal_total_weight_is_1_81() {
        WeightedGraph<Integer, Double> g = WeightedGraph.undirectedFromResource("prim.graph");
        List<WeightedEdge<Integer, Double>> mst = MinimumSpanningTree.kruskal(g);
        double total = mst.stream().mapToDouble(WeightedEdge::attribute).sum();
        assertEquals(1.81, total, 1e-10);
    }

    @Test
    void undirectedFromResource_prim_total_weight_is_1_81() {
        WeightedGraph<Integer, Double> g = WeightedGraph.undirectedFromResource("prim.graph");
        Map<Integer, WeightedEdge<Integer, Double>> mst = MinimumSpanningTree.prim(g, 0);
        double total = mst.values().stream().mapToDouble(WeightedEdge::attribute).sum();
        assertEquals(1.81, total, 1e-10);
    }

    @Test
    void undirectedFromResource_boruvka_total_weight_is_1_81() {
        WeightedGraph<Integer, Double> g = WeightedGraph.undirectedFromResource("prim.graph");
        List<WeightedEdge<Integer, Double>> mst = MinimumSpanningTree.boruvka(g);
        double total = mst.stream().mapToDouble(WeightedEdge::attribute).sum();
        assertEquals(1.81, total, 1e-10);
    }

    @Test
    void undirectedFromResource_agrees_with_programmatic_graph() {
        // Graph built programmatically
        WeightedGraph<Integer, Double> programmatic = WeightedGraph.undirectedWeighted();
        programmatic.addEdge(new WeightedEdge<>(0, 7, 0.16));
        programmatic.addEdge(new WeightedEdge<>(2, 3, 0.17));
        programmatic.addEdge(new WeightedEdge<>(1, 7, 0.19));
        programmatic.addEdge(new WeightedEdge<>(0, 2, 0.26));
        programmatic.addEdge(new WeightedEdge<>(5, 7, 0.28));
        programmatic.addEdge(new WeightedEdge<>(1, 3, 0.29));
        programmatic.addEdge(new WeightedEdge<>(1, 5, 0.32));
        programmatic.addEdge(new WeightedEdge<>(2, 7, 0.34));
        programmatic.addEdge(new WeightedEdge<>(4, 5, 0.35));
        programmatic.addEdge(new WeightedEdge<>(1, 2, 0.36));
        programmatic.addEdge(new WeightedEdge<>(4, 7, 0.37));
        programmatic.addEdge(new WeightedEdge<>(0, 4, 0.38));
        programmatic.addEdge(new WeightedEdge<>(6, 2, 0.40));
        programmatic.addEdge(new WeightedEdge<>(3, 6, 0.52));
        programmatic.addEdge(new WeightedEdge<>(6, 0, 0.58));
        programmatic.addEdge(new WeightedEdge<>(6, 4, 0.93));

        WeightedGraph<Integer, Double> fromFile = WeightedGraph.undirectedFromResource("prim.graph");

        double fileTotal = MinimumSpanningTree.kruskal(fromFile)
                .stream().mapToDouble(WeightedEdge::attribute).sum();
        double programmaticTotal = MinimumSpanningTree.kruskal(programmatic)
                .stream().mapToDouble(WeightedEdge::attribute).sum();

        assertEquals(programmaticTotal, fileTotal, 1e-10);
    }

    @Test
    void undirectedFromResource_throws_for_nonexistent_resource() {
        assertThrows(FileNotFoundException.class,
                () -> WeightedGraph.undirectedFromResource("nosuchfile.graph"));
    }
}