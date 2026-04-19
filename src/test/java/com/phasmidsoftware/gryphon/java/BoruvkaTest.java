/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.java;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class BoruvkaTest {

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
    // Boruvka tests
    // -------------------------------------------------------------------------

    @Test
    void boruvka_produces_n_minus_1_edges() {
        WeightedGraph<Integer, Double> g = buildPrimGraph();
        List<WeightedEdge<Integer, Double>> mst = MinimumSpanningTree.boruvka(g);
        assertEquals(7, mst.size());
    }

    @Test
    void boruvka_total_weight_is_1_81() {
        WeightedGraph<Integer, Double> g = buildPrimGraph();
        List<WeightedEdge<Integer, Double>> mst = MinimumSpanningTree.boruvka(g);
        double total = mst.stream().mapToDouble(WeightedEdge::attribute).sum();
        assertEquals(1.81, total, 1e-10);
    }

    @Test
    void boruvka_contains_known_mst_weights() {
        WeightedGraph<Integer, Double> g = buildPrimGraph();
        List<WeightedEdge<Integer, Double>> mst = MinimumSpanningTree.boruvka(g);
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
    void boruvka_excludes_cycle_creating_edge() {
        WeightedGraph<Integer, Double> g = buildPrimGraph();
        List<WeightedEdge<Integer, Double>> mst = MinimumSpanningTree.boruvka(g);
        List<Double> weights = mst.stream().map(WeightedEdge::attribute).toList();
        assertFalse(weights.contains(0.29));
    }

    @Test
    void boruvka_directed_graph_throws() {
        WeightedGraph<Integer, Double> g = WeightedGraph.directedWeighted();
        g.addEdge(new WeightedEdge<>(0, 1, 1.0));
        assertThrows(IllegalStateException.class,
                () -> MinimumSpanningTree.boruvka(g));
    }

    @Test
    void boruvka_option3_total_weight_matches_option1() {
        WeightedGraph<Integer, Double> g = buildPrimGraph();
        List<WeightedEdge<Integer, Double>> mst1 = MinimumSpanningTree.boruvka(g);
        List<WeightedEdge<Integer, Double>> mst3 = MinimumSpanningTree.boruvka(
                g, Double::compare);
        double total1 = mst1.stream().mapToDouble(WeightedEdge::attribute).sum();
        double total3 = mst3.stream().mapToDouble(WeightedEdge::attribute).sum();
        assertEquals(total1, total3, 1e-10);
    }

    @Test
    void boruvka_and_kruskal_produce_same_total_weight() {
        WeightedGraph<Integer, Double> g = buildPrimGraph();
        List<WeightedEdge<Integer, Double>> boruvkaMst = MinimumSpanningTree.boruvka(g);
        List<WeightedEdge<Integer, Double>> kruskalMst = MinimumSpanningTree.kruskal(g);
        double boruvkaTotal = boruvkaMst.stream().mapToDouble(WeightedEdge::attribute).sum();
        double kruskalTotal = kruskalMst.stream().mapToDouble(WeightedEdge::attribute).sum();
        assertEquals(kruskalTotal, boruvkaTotal, 1e-10);
    }

    @Test
    void boruvka_and_prim_produce_same_total_weight() {
        WeightedGraph<Integer, Double> g = buildPrimGraph();
        List<WeightedEdge<Integer, Double>> boruvkaMst = MinimumSpanningTree.boruvka(g);
        java.util.Map<Integer, WeightedEdge<Integer, Double>> primMst =
                MinimumSpanningTree.prim(g, 0);
        double boruvkaTotal = boruvkaMst.stream().mapToDouble(WeightedEdge::attribute).sum();
        double primTotal = primMst.values().stream().mapToDouble(WeightedEdge::attribute).sum();
        assertEquals(primTotal, boruvkaTotal, 1e-10);
    }
}