/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.java;

import com.phasmidsoftware.gryphon.core.GraphException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the graph properties and predicates on the Java façade:
 * order, size, degree, self-loops, and the cyclic/connected/bipartite tests.
 */
public class GraphPropertiesTest {

    private static Graph<Integer> triangle() {
        Graph<Integer> g = Graph.undirected();
        g.addEdge(0, 1);
        g.addEdge(1, 2);
        g.addEdge(2, 0);
        return g;
    }

    private static Graph<Integer> path() {
        Graph<Integer> g = Graph.undirected();
        g.addEdge(0, 1);
        g.addEdge(1, 2);
        return g;
    }

    // -------------------------------------------------------------------------
    // Order and size
    // -------------------------------------------------------------------------

    @Test
    void order_counts_vertices() {
        assertEquals(3, triangle().order());
        assertEquals(3, path().order());
    }

    @Test
    void order_counts_an_isolated_vertex() {
        Graph<Integer> g = path();
        g.addVertex(99);
        assertEquals(4, g.order());
    }

    @Test
    void size_counts_edges() {
        assertEquals(3, triangle().size());
        assertEquals(2, path().size());
    }

    // -------------------------------------------------------------------------
    // Degree
    // -------------------------------------------------------------------------

    @Test
    void degree_of_each_vertex_in_a_triangle_is_two() {
        Graph<Integer> g = triangle();
        for (int v = 0; v < 3; v++)
            assertEquals(2, g.degree(v), "wrong degree at " + v);
    }

    @Test
    void degree_distinguishes_the_middle_of_a_path_from_its_ends() {
        Graph<Integer> g = path();
        assertEquals(1, g.degree(0));
        assertEquals(2, g.degree(1));
        assertEquals(1, g.degree(2));
    }

    @Test
    void maxDegree_is_the_largest_degree() {
        assertEquals(2, triangle().maxDegree());
        assertEquals(2, path().maxDegree());
    }

    @Test
    void meanDegree_obeys_the_handshaking_lemma() {
        Graph<Integer> t = triangle();
        assertEquals(2.0 * t.size() / t.order(), t.meanDegree(), 1e-12);
        assertEquals(2.0, t.meanDegree(), 1e-12);

        Graph<Integer> p = path();
        assertEquals(4.0 / 3.0, p.meanDegree(), 1e-12);
    }

    @Test
    void degree_is_undefined_for_a_directed_graph() {
        Graph<Integer> g = Graph.directed();
        g.addEdge(0, 1);
        assertThrows(GraphException.class, () -> g.degree(0));
        assertThrows(GraphException.class, g::maxDegree);
        assertThrows(GraphException.class, g::meanDegree);
    }

    // -------------------------------------------------------------------------
    // Self-loops
    // -------------------------------------------------------------------------

    @Test
    void numberOfSelfLoops_is_zero_without_them() {
        assertEquals(0, triangle().numberOfSelfLoops());
    }

    @Test
    void numberOfSelfLoops_counts_a_self_loop() {
        Graph<Integer> g = path();
        g.addEdge(1, 1);
        assertEquals(1, g.numberOfSelfLoops());
    }

    // -------------------------------------------------------------------------
    // Predicates
    // -------------------------------------------------------------------------

    @Test
    void isCyclic_is_true_for_a_triangle_and_false_for_a_path() {
        assertTrue(triangle().isCyclic());
        assertFalse(path().isCyclic());
    }

    @Test
    void isCyclic_detects_a_directed_cycle() {
        Graph<Integer> cycle = Graph.directed();
        cycle.addEdge(0, 1);
        cycle.addEdge(1, 2);
        cycle.addEdge(2, 0);
        assertTrue(cycle.isCyclic());

        Graph<Integer> dag = Graph.directed();
        dag.addEdge(0, 1);
        dag.addEdge(1, 2);
        dag.addEdge(0, 2);
        assertFalse(dag.isCyclic());
    }

    @Test
    void isCyclic_agrees_with_isDAG_on_directed_graphs() {
        Graph<Integer> cycle = Graph.directed();
        cycle.addEdge(0, 1);
        cycle.addEdge(1, 0);
        assertEquals(cycle.isCyclic(), !TopologicalSort.isDAG(cycle));
    }

    @Test
    void isConnected_is_false_when_there_are_two_components() {
        Graph<Integer> g = Graph.undirected();
        g.addEdge(0, 1);
        g.addEdge(2, 3);
        assertFalse(g.isConnected());
        assertTrue(path().isConnected());
    }

    @Test
    void isBipartite_separates_an_even_cycle_from_an_odd_one() {
        assertFalse(triangle().isBipartite(), "a 3-cycle is not bipartite");
        assertTrue(path().isBipartite(), "a path is bipartite");

        Graph<Integer> square = Graph.undirected();
        square.addEdge(0, 1);
        square.addEdge(1, 2);
        square.addEdge(2, 3);
        square.addEdge(3, 0);
        assertTrue(square.isBipartite(), "a 4-cycle is bipartite");
    }

    // -------------------------------------------------------------------------
    // Weighted graphs inherit all of the above
    // -------------------------------------------------------------------------

    @Test
    void a_weighted_graph_has_the_same_properties() {
        WeightedGraph<Integer, Double> g = WeightedGraph.undirectedWeighted();
        g.addEdge(new WeightedEdge<>(0, 1, 1.0));
        g.addEdge(new WeightedEdge<>(1, 2, 2.0));

        assertEquals(3, g.order());
        assertEquals(2, g.size());
        assertEquals(2, g.degree(1));
        assertTrue(g.isConnected());
        assertFalse(g.isCyclic());
    }
}
