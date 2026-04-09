/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.java;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for StronglyConnectedComponents.kosaraju.
 * <p>
 * directed.graph: 7 vertices (0–6), 12 directed edges.
 * SCCs: {0,2,5,6}, {1}, {3}, {4}
 */
public class StronglyConnectedComponentsTest {

    // -------------------------------------------------------------------------
    // Fixture — mirrors directed.graph
    // -------------------------------------------------------------------------

    private static Graph<Integer> buildDirectedGraph() {
        Graph<Integer> g = Graph.directed();
        g.addEdge(0, 5);
        g.addEdge(0, 2);
        g.addEdge(0, 1);
        g.addEdge(3, 6);
        g.addEdge(3, 5);
        g.addEdge(3, 4);
        g.addEdge(5, 2);
        g.addEdge(6, 4);
        g.addEdge(6, 0);
        g.addEdge(3, 2);
        g.addEdge(1, 4);
        g.addEdge(2, 6);
        return g;
    }

    // -------------------------------------------------------------------------
    // kosaraju — result structure
    // -------------------------------------------------------------------------

    @Test
    void kosaraju_labels_all_vertices() {
        Map<Integer, Integer> result =
                StronglyConnectedComponents.kosaraju(buildDirectedGraph());
        assertEquals(7, result.size());
    }

    @Test
    void kosaraju_finds_exactly_4_sccs() {
        assertEquals(4, StronglyConnectedComponents.count(buildDirectedGraph()));
    }

    // -------------------------------------------------------------------------
    // kosaraju — non-trivial SCC {0, 2, 5, 6}
    // -------------------------------------------------------------------------

    @Test
    void kosaraju_0_2_5_6_share_same_scc_id() {
        Map<Integer, Integer> result =
                StronglyConnectedComponents.kosaraju(buildDirectedGraph());
        assertEquals(result.get(0), result.get(2));
        assertEquals(result.get(2), result.get(5));
        assertEquals(result.get(5), result.get(6));
    }

    @Test
    void kosaraju_scc_0256_differs_from_singletons() {
        Map<Integer, Integer> result =
                StronglyConnectedComponents.kosaraju(buildDirectedGraph());
        int scc0256 = result.get(0);
        assertNotEquals(scc0256, (int) result.get(1));
        assertNotEquals(scc0256, (int) result.get(3));
        assertNotEquals(scc0256, (int) result.get(4));
    }

    // -------------------------------------------------------------------------
    // kosaraju — singleton SCCs
    // -------------------------------------------------------------------------

    @Test
    void kosaraju_singletons_1_3_4_have_distinct_ids() {
        Map<Integer, Integer> result =
                StronglyConnectedComponents.kosaraju(buildDirectedGraph());
        Set<Integer> singletonIds = new HashSet<>(Arrays.asList(
                result.get(1), result.get(3), result.get(4)));
        assertEquals(3, singletonIds.size());
    }

    @Test
    void kosaraju_vertex1_differs_from_vertex4() {
        Map<Integer, Integer> result =
                StronglyConnectedComponents.kosaraju(buildDirectedGraph());
        assertNotEquals(result.get(1), result.get(4));
    }

    // -------------------------------------------------------------------------
    // kosaraju — inline graphs
    // -------------------------------------------------------------------------

    @Test
    void kosaraju_mutual_cycle_is_one_scc() {
        Graph<Integer> g = Graph.directed();
        g.addEdge(0, 1);
        g.addEdge(1, 0);
        Map<Integer, Integer> result = StronglyConnectedComponents.kosaraju(g);
        assertEquals(result.get(0), result.get(1));
        assertEquals(1, StronglyConnectedComponents.count(g));
    }

    @Test
    void kosaraju_one_way_edge_is_two_sccs() {
        Graph<Integer> g = Graph.directed();
        g.addEdge(0, 1);
        assertNotEquals(
                StronglyConnectedComponents.kosaraju(g).get(0),
                StronglyConnectedComponents.kosaraju(g).get(1));
        assertEquals(2, StronglyConnectedComponents.count(g));
    }

    @Test
    void kosaraju_three_vertex_cycle_is_one_scc() {
        Graph<Integer> g = Graph.directed();
        g.addEdge(0, 1);
        g.addEdge(1, 2);
        g.addEdge(2, 0);
        Map<Integer, Integer> result = StronglyConnectedComponents.kosaraju(g);
        assertEquals(result.get(0), result.get(1));
        assertEquals(result.get(1), result.get(2));
        assertEquals(1, StronglyConnectedComponents.count(g));
    }

    @Test
    void kosaraju_three_vertex_chain_is_three_sccs() {
        Graph<Integer> g = Graph.directed();
        g.addEdge(0, 1);
        g.addEdge(1, 2);
        assertEquals(3, StronglyConnectedComponents.count(g));
    }

    // -------------------------------------------------------------------------
    // components() convenience method
    // -------------------------------------------------------------------------

    @Test
    void components_groups_0_2_5_6_together() {
        Map<Integer, Set<Integer>> comps =
                StronglyConnectedComponents.components(buildDirectedGraph());
        Set<Integer> largeScc = comps.values().stream()
                .filter(s -> s.size() == 4)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no SCC of size 4"));
        assertEquals(new HashSet<>(Arrays.asList(0, 2, 5, 6)), largeScc);
    }

    @Test
    void components_has_three_singleton_sccs() {
        Map<Integer, Set<Integer>> comps =
                StronglyConnectedComponents.components(buildDirectedGraph());
        long singletons = comps.values().stream().filter(s -> s.size() == 1).count();
        assertEquals(3, singletons);
    }

    // -------------------------------------------------------------------------
    // Guard — undirected throws
    // -------------------------------------------------------------------------

    @Test
    void kosaraju_undirected_throws() {
        Graph<Integer> g = Graph.undirected();
        g.addEdge(0, 1);
        assertThrows(IllegalStateException.class,
                () -> StronglyConnectedComponents.kosaraju(g));
    }
}