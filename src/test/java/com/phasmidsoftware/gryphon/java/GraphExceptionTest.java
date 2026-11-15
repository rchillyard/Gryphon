/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.java;

import com.phasmidsoftware.gryphon.core.GraphException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GraphException is unchecked from 1.8.0.
 *
 * <p>The point of this class is that it <em>compiles</em>. Up to 1.7.0 GraphException extended
 * Exception, so Java saw a checked exception; the façade methods that throw it do not declare
 * it, and javac rejects a catch clause for a checked exception the called method cannot throw
 * ("exception is never thrown in body of corresponding try statement"). A Java caller could
 * therefore not handle it at all. If GraphException ever reverts to extending Exception, the
 * try/catch blocks below stop compiling and this file fails the build.</p>
 */
public class GraphExceptionTest {

    private static Graph<Integer> directedGraph() {
        Graph<Integer> g = Graph.directed();
        g.addEdge(0, 1);
        return g;
    }

    @Test
    void a_java_caller_can_catch_it_around_degree() {
        boolean caught = false;
        try {
            directedGraph().degree(0);
            fail("degree should have thrown on a directed graph");
        } catch (GraphException e) {
            caught = true;
            assertTrue(e.getMessage().contains("degree"), "message should name the method: " + e.getMessage());
        }
        assertTrue(caught);
    }

    @Test
    void a_java_caller_can_catch_it_around_maxDegree_and_meanDegree() {
        GraphException fromMaxDegree = null;
        try {
            directedGraph().maxDegree();
            fail("maxDegree should have thrown on a directed graph");
        } catch (GraphException e) {
            fromMaxDegree = e;
        }
        assertNotNull(fromMaxDegree);
        assertTrue(fromMaxDegree.getMessage().contains("maxDegree"),
                "message should name the method: " + fromMaxDegree.getMessage());

        GraphException fromMeanDegree = null;
        try {
            directedGraph().meanDegree();
            fail("meanDegree should have thrown on a directed graph");
        } catch (GraphException e) {
            fromMeanDegree = e;
        }
        assertNotNull(fromMeanDegree);
        assertTrue(fromMeanDegree.getMessage().contains("meanDegree"),
                "message should name the method: " + fromMeanDegree.getMessage());
    }

    @Test
    void it_is_unchecked() {
        GraphException e = new GraphException("boom");
        assertInstanceOf(RuntimeException.class, e);
        assertEquals("boom", e.getMessage());
    }

    /**
     * Being unchecked, it also passes through a lambda that declares no exceptions —
     * which it could not do while checked.
     */
    @Test
    void it_propagates_out_of_a_lambda() {
        Graph<Integer> g = directedGraph();
        assertThrows(GraphException.class, () -> g.vertices().forEach(g::degree));
    }
}
