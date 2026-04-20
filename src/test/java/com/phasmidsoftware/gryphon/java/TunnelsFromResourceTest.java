/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.java;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for GraphBuilder / WeightedGraph.fromResource using the
 * Northeastern University tunnel network loaded from tunnels.graph.
 * <p>
 * Known results (Tunnels_Gryphon.java, verified since 2018):
 * Buildings:  80
 * MST edges:  79
 * Total cost: $6,648,954
 */
public class TunnelsFromResourceTest {

    private static final double KNOWN_MST_COST = 6_648_954.0;
    private static final int KNOWN_MST_EDGES = 79;
    private static final int KNOWN_BUILDINGS = 80;

    private static final Comparator<Double> BY_COST = Double::compare;

    private static WeightedGraph<String, Double> graph;

    @BeforeAll
    static void loadGraph() {
        graph = WeightedGraph.undirectedFromResource("tunnels.graph",
                s -> s, Double::parseDouble);
    }

    // -------------------------------------------------------------------------
    // Structure
    // -------------------------------------------------------------------------

    @Test
    void graph_loads_successfully() {
        assertNotNull(graph);
    }

    @Test
    void graph_is_undirected() {
        assertFalse(graph.isDirected());
    }

    @Test
    void graph_has_80_buildings() {
        assertEquals(KNOWN_BUILDINGS, graph.vertices().size());
    }

    @Test
    void graph_has_79_edges() {
        assertEquals(KNOWN_MST_EDGES, graph.edges().size());
    }

    // -------------------------------------------------------------------------
    // Kruskal
    // -------------------------------------------------------------------------

    @Test
    void kruskal_produces_79_edges() {
        List<WeightedEdge<String, Double>> mst = MinimumSpanningTree.kruskal(graph, BY_COST);
        assertEquals(KNOWN_MST_EDGES, mst.size());
    }

    @Test
    void kruskal_total_cost_matches_known_result() {
        double total = MinimumSpanningTree.kruskal(graph, BY_COST)
                .stream().mapToDouble(WeightedEdge::attribute).sum();
        assertEquals(KNOWN_MST_COST, total, 1.0);
    }

    // -------------------------------------------------------------------------
    // Prim
    // -------------------------------------------------------------------------

    @Test
    void prim_produces_79_edges() {
        Map<String, WeightedEdge<String, Double>> mst =
                MinimumSpanningTree.prim(graph, "MA", BY_COST);
        assertEquals(KNOWN_MST_EDGES, mst.size());
    }

    @Test
    void prim_total_cost_matches_known_result() {
        double total = MinimumSpanningTree.prim(graph, "MA", BY_COST)
                .values().stream().mapToDouble(WeightedEdge::attribute).sum();
        assertEquals(KNOWN_MST_COST, total, 1.0);
    }

    // -------------------------------------------------------------------------
    // Boruvka
    // -------------------------------------------------------------------------

    @Test
    void boruvka_produces_79_edges() {
        List<WeightedEdge<String, Double>> mst = MinimumSpanningTree.boruvka(graph, BY_COST);
        assertEquals(KNOWN_MST_EDGES, mst.size());
    }

    @Test
    void boruvka_total_cost_matches_known_result() {
        double total = MinimumSpanningTree.boruvka(graph, BY_COST)
                .stream().mapToDouble(WeightedEdge::attribute).sum();
        assertEquals(KNOWN_MST_COST, total, 1.0);
    }

    // -------------------------------------------------------------------------
    // Agreement — file-loaded vs programmatic (agrees with TunnelsTest)
    // -------------------------------------------------------------------------

    @Test
    void kruskal_from_file_agrees_with_programmatic() {
        double fromFile = MinimumSpanningTree.kruskal(graph, BY_COST)
                .stream().mapToDouble(WeightedEdge::attribute).sum();
        assertEquals(KNOWN_MST_COST, fromFile, 1.0);
    }

    @Test
    void three_algorithms_agree_on_total_cost() {
        double primTotal = MinimumSpanningTree.prim(graph, "MA", BY_COST)
                .values().stream().mapToDouble(WeightedEdge::attribute).sum();
        double kruskalTotal = MinimumSpanningTree.kruskal(graph, BY_COST)
                .stream().mapToDouble(WeightedEdge::attribute).sum();
        double boruvkaTotal = MinimumSpanningTree.boruvka(graph, BY_COST)
                .stream().mapToDouble(WeightedEdge::attribute).sum();
        assertEquals(primTotal, kruskalTotal, 1.0);
        assertEquals(primTotal, boruvkaTotal, 1.0);
    }
}