/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.java;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the two traversal entry points the façade previously lacked:
 * `dfsAll`, which covers every component, and `bfsUntil`, which stops at a goal.
 */
public class DfsAllAndBfsUntilTest {

    /** Two disjoint edges: components {0,1} and {2,3}. */
    private static Graph<Integer> twoComponents() {
        Graph<Integer> g = Graph.undirected();
        g.addEdge(0, 1);
        g.addEdge(2, 3);
        return g;
    }

    /** The path 0—1—2—3. */
    private static Graph<Integer> path() {
        Graph<Integer> g = Graph.undirected();
        g.addEdge(0, 1);
        g.addEdge(1, 2);
        g.addEdge(2, 3);
        return g;
    }

    // -------------------------------------------------------------------------
    // dfsAll
    // -------------------------------------------------------------------------

    @Test
    void dfs_reaches_only_its_own_component() {
        Map<Integer, Integer> tree = twoComponents().dfs(0);
        assertTrue(tree.containsKey(1));
        assertFalse(tree.containsKey(2), "2 is in the other component");
        assertFalse(tree.containsKey(3), "3 is in the other component");
    }

    /**
     * Which vertex of a component is its root is not specified — it is whichever
     * the traversal reaches first — so this asserts that each component
     * contributes exactly one discovered vertex, not which one.
     */
    @Test
    void dfsAll_reaches_every_component() {
        Map<Integer, Integer> forest = twoComponents().dfsAll();

        assertEquals(1, countPresent(forest, 0, 1), "component {0,1} should contribute one non-root");
        assertEquals(1, countPresent(forest, 2, 3), "component {2,3} should contribute one non-root");
    }

    private static int countPresent(Map<Integer, Integer> map, int... vertices) {
        int n = 0;
        for (int v : vertices) if (map.containsKey(v)) n++;
        return n;
    }

    /**
     * Each component's root has no predecessor, so the vertices absent from the
     * forest are exactly the roots — one per component.
     */
    @Test
    void dfsAll_omits_one_root_per_component() {
        Graph<Integer> g = twoComponents();
        Map<Integer, Integer> forest = g.dfsAll();
        assertEquals(g.order() - 2, forest.size(), "two components, so two roots are absent");
    }

    @Test
    void dfsAll_on_a_connected_graph_omits_only_the_single_root() {
        Graph<Integer> g = path();
        assertEquals(g.order() - 1, g.dfsAll().size());
    }

    @Test
    void dfsAll_came_from_entries_are_real_edges() {
        Map<Integer, Integer> forest = path().dfsAll();

        assertFalse(forest.isEmpty(), "the forest should have entries to check");
        for (Map.Entry<Integer, Integer> entry : forest.entrySet()) {
            int v = entry.getKey();
            int from = entry.getValue();
            assertEquals(1, Math.abs(v - from),
                    "came-from " + from + "→" + v + " is not an edge of the path");
        }
    }

    // -------------------------------------------------------------------------
    // bfsUntil
    // -------------------------------------------------------------------------

    @Test
    void bfsUntil_with_an_unreachable_goal_matches_a_full_bfs() {
        Graph<Integer> g = path();
        assertEquals(g.bfs(0), g.bfsUntil(0, v -> false));
    }

    @Test
    void bfsUntil_stops_once_the_goal_is_found() {
        Graph<Integer> g = path();
        Map<Integer, Integer> partial = g.bfsUntil(0, v -> v == 2);

        assertTrue(partial.containsKey(2), "the goal itself should have been discovered");
        assertTrue(partial.size() <= g.bfs(0).size(), "stopping early cannot discover more");
    }

    @Test
    void bfsUntil_reconstructs_the_path_to_the_goal() {
        Map<Integer, Integer> tree = path().bfsUntil(0, v -> v == 3);

        // Walk back from the goal to the start.
        int v = 3;
        int steps = 0;
        while (tree.containsKey(v)) {
            v = tree.get(v);
            steps++;
        }
        assertEquals(0, v, "walking back should end at the start vertex");
        assertEquals(3, steps, "0—1—2—3 is three edges");
    }

    @Test
    void bfsUntil_start_vertex_is_absent() {
        assertFalse(path().bfsUntil(0, v -> v == 3).containsKey(0));
    }
}
