/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.java;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class MinimumSpanningTreeTest {

    // MST for prim.graph (Sedgewick & Wayne tinyEWG):
    //   0-7 (0.16), 2-3 (0.17), 1-7 (0.19), 0-2 (0.26),
    //   5-7 (0.28), 4-5 (0.35), 6-2 (0.40)
    // Total weight: 1.81

    private static WeightedGraph<Integer, Double> buildPrimGraph() {
        WeightedGraph<Integer, Double> g = WeightedGraph.undirectedWeighted();
        g.addEdge(new WeightedEdge<>(0, 7, 0.16));
        g.addEdge(new WeightedEdge<>(2, 3, 0.17));
        g.addEdge(new WeightedEdge<>(1, 7, 0.19));
        g.addEdge(new WeightedEdge<>(0, 2, 0.26));
        g.addEdge(new WeightedEdge<>(5, 7, 0.28));
        g.addEdge(new WeightedEdge<>(1, 3, 0.29));
        g.addEdge(new WeightedEdge<>(1, 5, 0.32));
        g.addEdge(new WeightedEdge<>(2, 7, 0.34));
        g.addEdge(new WeightedEdge<>(4, 5, 0.35));
        g.addEdge(new WeightedEdge<>(1, 2, 0.36));
        g.addEdge(new WeightedEdge<>(4, 7, 0.37));
        g.addEdge(new WeightedEdge<>(0, 4, 0.38));
        g.addEdge(new WeightedEdge<>(6, 2, 0.40));
        g.addEdge(new WeightedEdge<>(3, 6, 0.52));
        g.addEdge(new WeightedEdge<>(6, 0, 0.58));
        g.addEdge(new WeightedEdge<>(6, 4, 0.93));
        return g;
    }

    // -------------------------------------------------------------------------
    // Prim tests
    // -------------------------------------------------------------------------

    @Test
    void prim_produces_n_minus_1_edges() {
        WeightedGraph<Integer, Double> g = buildPrimGraph();
        Map<Integer, WeightedEdge<Integer, Double>> mst = MinimumSpanningTree.prim(g, 0);
        assertEquals(7, mst.size());
    }

    @Test
    void prim_start_vertex_absent() {
        WeightedGraph<Integer, Double> g = buildPrimGraph();
        Map<Integer, WeightedEdge<Integer, Double>> mst = MinimumSpanningTree.prim(g, 0);
        assertFalse(mst.containsKey(0));
    }

    @Test
    void prim_all_non_source_vertices_present() {
        WeightedGraph<Integer, Double> g = buildPrimGraph();
        Map<Integer, WeightedEdge<Integer, Double>> mst = MinimumSpanningTree.prim(g, 0);
        for (int v = 1; v <= 7; v++)
            assertTrue(mst.containsKey(v), "MST missing vertex " + v);
    }

    @Test
    void prim_total_weight_is_1_81() {
        WeightedGraph<Integer, Double> g = buildPrimGraph();
        Map<Integer, WeightedEdge<Integer, Double>> mst = MinimumSpanningTree.prim(g, 0);
        double total = mst.values().stream()
                .mapToDouble(WeightedEdge::attribute)
                .sum();
        assertEquals(1.81, total, 1e-10);
    }

    @Test
    void prim_individual_edge_weights_match_known_mst() {
        WeightedGraph<Integer, Double> g = buildPrimGraph();
        Map<Integer, WeightedEdge<Integer, Double>> mst = MinimumSpanningTree.prim(g, 0);
        assertEquals(0.16, mst.get(7).attribute(), 1e-10);
        assertEquals(0.17, mst.get(3).attribute(), 1e-10);
        assertEquals(0.19, mst.get(1).attribute(), 1e-10);
        assertEquals(0.26, mst.get(2).attribute(), 1e-10);
        assertEquals(0.28, mst.get(5).attribute(), 1e-10);
        assertEquals(0.35, mst.get(4).attribute(), 1e-10);
        assertEquals(0.40, mst.get(6).attribute(), 1e-10);
    }

    @Test
    void prim_no_edge_heavier_than_max_mst_edge() {
        WeightedGraph<Integer, Double> g = buildPrimGraph();
        Map<Integer, WeightedEdge<Integer, Double>> mst = MinimumSpanningTree.prim(g, 0);
        mst.values().forEach(e ->
                assertTrue(e.attribute() <= 0.401,
                        "MST edge too heavy: " + e.attribute()));
    }

    @Test
    void prim_directed_graph_throws() {
        WeightedGraph<Integer, Double> g = WeightedGraph.directedWeighted();
        g.addEdge(new WeightedEdge<>(0, 1, 1.0));
        assertThrows(IllegalStateException.class,
                () -> MinimumSpanningTree.prim(g, 0));
    }

    @Test
    void prim_option3_total_weight_matches_option1() {
        WeightedGraph<Integer, Double> g = buildPrimGraph();
        Map<Integer, WeightedEdge<Integer, Double>> mst1 = MinimumSpanningTree.prim(g, 0);
        Map<Integer, WeightedEdge<Integer, Double>> mst3 = MinimumSpanningTree.prim(
                g, 0, Double::compare);
        double total1 = mst1.values().stream().mapToDouble(WeightedEdge::attribute).sum();
        double total3 = mst3.values().stream().mapToDouble(WeightedEdge::attribute).sum();
        assertEquals(total1, total3, 1e-10);
    }

    @Test
    void prim_mst_weight_independent_of_start_vertex() {
        WeightedGraph<Integer, Double> g = buildPrimGraph();
        Map<Integer, WeightedEdge<Integer, Double>> mst0 = MinimumSpanningTree.prim(g, 0);
        Map<Integer, WeightedEdge<Integer, Double>> mst3 = MinimumSpanningTree.prim(g, 3);
        double total0 = mst0.values().stream().mapToDouble(WeightedEdge::attribute).sum();
        double total3 = mst3.values().stream().mapToDouble(WeightedEdge::attribute).sum();
        assertEquals(total0, total3, 1e-10);
    }

    // -------------------------------------------------------------------------
    // Kruskal tests
    // -------------------------------------------------------------------------

    @Test
    void kruskal_produces_n_minus_1_edges() {
        WeightedGraph<Integer, Double> g = buildPrimGraph();
        List<WeightedEdge<Integer, Double>> mst = MinimumSpanningTree.kruskal(g);
        assertEquals(7, mst.size());
    }

    @Test
    void kruskal_total_weight_is_1_81() {
        WeightedGraph<Integer, Double> g = buildPrimGraph();
        List<WeightedEdge<Integer, Double>> mst = MinimumSpanningTree.kruskal(g);
        double total = mst.stream().mapToDouble(WeightedEdge::attribute).sum();
        assertEquals(1.81, total, 1e-10);
    }

    @Test
    void kruskal_edges_in_non_decreasing_order() {
        WeightedGraph<Integer, Double> g = buildPrimGraph();
        List<WeightedEdge<Integer, Double>> mst = MinimumSpanningTree.kruskal(g);
        for (int i = 0; i < mst.size() - 1; i++)
            assertTrue(mst.get(i).attribute() <= mst.get(i + 1).attribute(),
                    "edges not in non-decreasing order at index " + i);
    }

    @Test
    void kruskal_contains_known_mst_weights() {
        WeightedGraph<Integer, Double> g = buildPrimGraph();
        List<WeightedEdge<Integer, Double>> mst = MinimumSpanningTree.kruskal(g);
        List<Double> weights = mst.stream().map(WeightedEdge::attribute).toList();
        assertTrue(weights.contains(0.16));
        assertTrue(weights.contains(0.17));
        assertTrue(weights.contains(0.19));
        assertTrue(weights.contains(0.26));
        assertTrue(weights.contains(0.28));
        assertTrue(weights.contains(0.35));
        assertTrue(weights.contains(0.40));
    }

    @Test
    void kruskal_excludes_cycle_creating_edge() {
        WeightedGraph<Integer, Double> g = buildPrimGraph();
        List<WeightedEdge<Integer, Double>> mst = MinimumSpanningTree.kruskal(g);
        List<Double> weights = mst.stream().map(WeightedEdge::attribute).toList();
        assertFalse(weights.contains(0.29));
    }

    @Test
    void kruskal_directed_graph_throws() {
        WeightedGraph<Integer, Double> g = WeightedGraph.directedWeighted();
        g.addEdge(new WeightedEdge<>(0, 1, 1.0));
        assertThrows(IllegalStateException.class,
                () -> MinimumSpanningTree.kruskal(g));
    }

    @Test
    void kruskal_option3_total_weight_matches_option1() {
        WeightedGraph<Integer, Double> g = buildPrimGraph();
        List<WeightedEdge<Integer, Double>> mst1 = MinimumSpanningTree.kruskal(g);
        List<WeightedEdge<Integer, Double>> mst3 = MinimumSpanningTree.kruskal(
                g, Double::compare);
        double total1 = mst1.stream().mapToDouble(WeightedEdge::attribute).sum();
        double total3 = mst3.stream().mapToDouble(WeightedEdge::attribute).sum();
        assertEquals(total1, total3, 1e-10);
    }

    @Test
    void kruskal_and_prim_produce_same_total_weight() {
        WeightedGraph<Integer, Double> g = buildPrimGraph();
        List<WeightedEdge<Integer, Double>> kruskalMst = MinimumSpanningTree.kruskal(g);
        Map<Integer, WeightedEdge<Integer, Double>> primMst = MinimumSpanningTree.prim(g, 0);
        double kruskalTotal = kruskalMst.stream().mapToDouble(WeightedEdge::attribute).sum();
        double primTotal = primMst.values().stream().mapToDouble(WeightedEdge::attribute).sum();
        assertEquals(kruskalTotal, primTotal, 1e-10);
    }
}