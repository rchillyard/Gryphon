/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.java;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Integration tests for MST algorithms on the Northeastern University tunnel network.
 * <p>
 * The graph is the Prim MST from Tunnels_Gryphon.java (80 buildings, 79 edges),
 * verified since 2018. Edge weights are tunnel construction costs in dollars (Long).
 * Building codes starting with digits are prefixed with 'B' (e.g. "177" -> "B177");
 * hyphens are replaced with underscores (e.g. "142-148" -> "B142_148").
 * <p>
 * Known results (from Tunnels_Gryphon.java output):
 * MST edges:   79  (80 buildings - 1)
 * Total cost:  $6,648,954
 * <p>
 * Prim, Kruskal, and Borůvka should all agree on total cost since the input graph
 * is already a tree (each edge is the unique connection for at least one vertex
 * pair), so all three algorithms must select all 79 edges.
 */
public class TunnelsTest {

    private static final long KNOWN_MST_COST = 6_648_954L;
    private static final int KNOWN_MST_EDGES = 79;
    private static final int KNOWN_BUILDINGS = 80;

    private static final Comparator<Long> BY_COST = Comparator.comparingLong(c -> c);

    private static WeightedGraph<String, Long> graph;

    @BeforeAll
    static void buildGraph() {
        graph = WeightedGraph.undirectedWeighted();
        for (Object[] e : MST_EDGES)
            graph.addEdge(new WeightedEdge<>((String) e[0], (String) e[1], (Long) e[2]));
    }

    @Test
    void graph_has_80_buildings() {
        assertEquals(KNOWN_BUILDINGS, graph.vertices().size());
    }

    @Test
    void graph_has_79_edges() {
        assertEquals(KNOWN_MST_EDGES, graph.edges().size());
    }

    @Test
    void prim_produces_79_edges() {
        assertEquals(KNOWN_MST_EDGES, MinimumSpanningTree.prim(graph, "MA", BY_COST).size());
    }

    @Test
    void prim_total_cost_is_6648954() {
        long total = MinimumSpanningTree.prim(graph, "MA", BY_COST)
                .values().stream().mapToLong(WeightedEdge::attribute).sum();
        assertEquals(KNOWN_MST_COST, total);
    }

    // -------------------------------------------------------------------------
    // Kruskal
    // -------------------------------------------------------------------------

    @Test
    void kruskal_produces_79_edges() {
        assertEquals(KNOWN_MST_EDGES, MinimumSpanningTree.kruskal(graph, BY_COST).size());
    }

    @Test
    void kruskal_total_cost_is_6648954() {
        long total = MinimumSpanningTree.kruskal(graph, BY_COST)
                .stream().mapToLong(WeightedEdge::attribute).sum();
        assertEquals(KNOWN_MST_COST, total);
    }

    // -------------------------------------------------------------------------
    // Boruvka
    // -------------------------------------------------------------------------

    @Test
    void boruvka_produces_79_edges() {
        assertEquals(KNOWN_MST_EDGES, MinimumSpanningTree.boruvka(graph, BY_COST).size());
    }

    @Test
    void boruvka_total_cost_is_6648954() {
        long total = MinimumSpanningTree.boruvka(graph, BY_COST)
                .stream().mapToLong(WeightedEdge::attribute).sum();
        assertEquals(KNOWN_MST_COST, total);
    }

    @Test
    void three_algorithms_agree_on_total_cost() {
        long primTotal = MinimumSpanningTree.prim(graph, "MA", BY_COST)
                .values().stream().mapToLong(WeightedEdge::attribute).sum();
        long kruskalTotal = MinimumSpanningTree.kruskal(graph, BY_COST)
                .stream().mapToLong(WeightedEdge::attribute).sum();
        long boruvkaTotal = MinimumSpanningTree.boruvka(graph, BY_COST)
                .stream().mapToLong(WeightedEdge::attribute).sum();
        assertEquals(primTotal, kruskalTotal);
        assertEquals(primTotal, boruvkaTotal);
    }

    private static final Object[][] MST_EDGES = {
            {"MA", "GG", 146062L}, {"WV", "BU", 106996L},
            {"RO", "B337", 34772L}, {"DG", "EV", 68045L},
            {"DG", "EL", 395L}, {"LA", "KA", 42172L},
            {"RY", "WPG", 73228L}, {"SE", "B142_148", 89473L},
            {"SN", "EC", 53319L}, {"CB", "HA", 563L},
            {"RP", "INV", 121218L}, {"SE", "SW", 51372L},
            {"SP", "SW", 75742L}, {"HA", "SL", 1115L},
            {"MC", "BN", 302646L}, {"CP", "B768", 88693L},
            {"AF", "ME", 33338L}, {"CN", "GG", 73920L},
            {"CA", "TF", 92076L}, {"CV", "B768", 31368L},
            {"ISEC", "CPG", 82168L}, {"CH", "FR", 503L},
            {"B236", "BVG", 234852L}, {"SH", "NI", 69676L},
            {"NI", "LC", 34662L}, {"BV", "B177", 93096L},
            {"RG", "RPG", 348035L}, {"AC", "KDY", 53098L},
            {"ME", "SH", 60534L}, {"FR", "SN", 455L},
            {"ME", "HO", 39127L}, {"BN", "CB", 78196L},
            {"HT", "RB", 67289L}, {"B768", "B780", 68113L},
            {"CSC", "EL", 357L}, {"BK", "WPG", 63971L},
            {"KDY", "SM", 43951L}, {"HA", "CH", 878L},
            {"EL", "HA", 564L}, {"KH", "MH", 26633L},
            {"CU", "CA", 26139L}, {"HO", "LA", 37367L},
            {"MU", "EL", 482L}, {"KH", "LF", 37497L},
            {"SB", "CPG", 43798L}, {"MC", "SP", 83969L},
            {"KA", "DK", 42216L}, {"LF", "B142_148", 52701L},
            {"HT", "CN", 62465L}, {"BU", "B464", 35808L},
            {"B271", "BVG", 509226L}, {"MA", "B236", 538053L},
            {"CP", "DC", 61985L}, {"MC", "WH", 86107L},
            {"LV", "B337", 50765L}, {"ISEC", "B780", 125925L},
            {"HF", "CC", 10672L}, {"RO", "B319", 57779L},
            {"YMC", "EV", 47353L}, {"ST", "KN", 49654L},
            {"B177", "BVG", 216483L}, {"RI", "HA", 276L},
            {"HF", "B319", 74002L}, {"ISEC", "RPG", 173413L},
            {"HF", "FC", 37955L}, {"INV", "RPG", 126256L},
            {"FC", "LH", 39189L}, {"FR", "LC", 57217L},
            {"HO", "WI", 50358L}, {"MH", "SM", 65073L},
            {"KA", "ST", 60167L}, {"YMC", "B319", 573544L},
            {"CG", "ST", 27148L}, {"WV", "BK", 81334L},
            {"CU", "KH", 32790L}, {"EC", "RG", 82842L},
            {"WH", "B407", 32227L}, {"CN", "EV", 38936L},
            {"BK", "AF", 67112L},
    };
}