/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.java;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the ConnectedComponents Java façade.
 */
public class ConnectedComponentsTest {

    // Two-component graph: {0,1,2} and {3,4}
    private static Graph<Integer> buildTwoComponentGraph() {
        Graph<Integer> g = Graph.undirected();
        g.addEdge(0, 1);
        g.addEdge(1, 2);
        g.addEdge(3, 4);
        return g;
    }

    @Test
    void count_single_component() {
        Graph<Integer> g = Graph.undirected();
        g.addEdge(0, 1);
        g.addEdge(1, 2);
        g.addEdge(2, 3);
        assertEquals(1, ConnectedComponents.count(g));
    }

    @Test
    void count_two_components() {
        assertEquals(2, ConnectedComponents.count(buildTwoComponentGraph()));
    }
    
    @Test
    void count_three_separate_components() {
        Graph<Integer> g = Graph.undirected();
        g.addEdge(0, 1);
        g.addEdge(2, 3);
        g.addEdge(4, 5);
        assertEquals(3, ConnectedComponents.count(g));
    }

    @Test
    void componentIds_same_id_for_connected_vertices() {
        Map<Integer, Integer> ids = ConnectedComponents.componentIds(buildTwoComponentGraph());
        assertEquals(ids.get(0), ids.get(1));
        assertEquals(ids.get(1), ids.get(2));
    }

    @Test
    void componentIds_different_id_for_separate_components() {
        Map<Integer, Integer> ids = ConnectedComponents.componentIds(buildTwoComponentGraph());
        assertNotEquals(ids.get(0), ids.get(3));
    }

    @Test
    void components_correct_grouping() {
        Map<Integer, Set<Integer>> comps = ConnectedComponents.components(buildTwoComponentGraph());
        assertEquals(2, comps.size());
        // Each component set has the right vertices
        boolean found0 = comps.values().stream().anyMatch(s -> s.containsAll(Set.of(0, 1, 2)));
        boolean found1 = comps.values().stream().anyMatch(s -> s.containsAll(Set.of(3, 4)));
        assertTrue(found0, "Component {0,1,2} not found");
        assertTrue(found1, "Component {3,4} not found");
    }

    @Test
    void directed_graph_throws() {
        Graph<Integer> g = Graph.directed();
        g.addEdge(0, 1);
        assertThrows(IllegalStateException.class, () -> ConnectedComponents.count(g));
    }
}