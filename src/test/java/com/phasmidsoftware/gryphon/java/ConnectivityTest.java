/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.java;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

/**
 * Tests for the Java {@link Connectivity} façade.
 *
 * These tests verify that the façade behaves as a Java student would expect:
 * mutating operations update the object in place, and query operations
 * reflect the current state.
 */
public class ConnectivityTest {

    // -------------------------------------------------------------------------
    // create / createOptimized
    // -------------------------------------------------------------------------

    @Test
    public void testCreateFromList() {
        Connectivity<Integer> c = Connectivity.create(List.of(0, 1, 2, 3, 4));
        assertEquals(5, c.size());
    }

    @Test
    public void testCreateOptimizedFromList() {
        Connectivity<Integer> c = Connectivity.createOptimized(List.of(0, 1, 2, 3, 4));
        assertEquals(5, c.size());
    }

    @Test
    public void testCreateFromVarargs() {
        Connectivity<String> c = Connectivity.create("A", "B", "C");
        assertEquals(3, c.size());
    }

    @Test
    public void testCreateOptimizedFromVarargs() {
        Connectivity<String> c = Connectivity.createOptimized("A", "B", "C");
        assertEquals(3, c.size());
    }

    // -------------------------------------------------------------------------
    // put
    // -------------------------------------------------------------------------

    @Test
    public void testPutAddsNewComponent() {
        Connectivity<Integer> c = Connectivity.create(List.of());
        c.put(1);
        c.put(2);
        assertEquals(2, c.size());
    }

    // -------------------------------------------------------------------------
    // connect / isConnected — create
    // -------------------------------------------------------------------------

    @Test
    public void testConnectTwoVertices() {
        Connectivity<Integer> c = Connectivity.create(0, 1, 2);
        c.connect(0, 1);
        assertTrue(c.isConnected(0, 1));
    }

    @Test
    public void testConnectReducesSize() {
        Connectivity<Integer> c = Connectivity.create(0, 1, 2);
        c.connect(0, 1);
        assertEquals(2, c.size());
    }

    @Test
    public void testConnectSameVertexNoEffect() {
        Connectivity<Integer> c = Connectivity.create(0, 1);
        c.connect(0, 0);
        assertEquals(2, c.size());
    }

    @Test
    public void testConnectAlreadyConnectedNoEffect() {
        Connectivity<Integer> c = Connectivity.create(0, 1, 2);
        c.connect(0, 1);
        c.connect(0, 1);
        assertEquals(2, c.size());
    }

    @Test
    public void testTransitiveConnectivity() {
        Connectivity<Integer> c = Connectivity.create(0, 1, 2);
        c.connect(0, 1);
        c.connect(1, 2);
        assertTrue(c.isConnected(0, 2));
    }

    @Test
    public void testUnconnectedVerticesRemainSeparate() {
        Connectivity<Integer> c = Connectivity.create(0, 1, 2, 3);
        c.connect(0, 1);
        c.connect(2, 3);
        assertFalse(c.isConnected(0, 2));
        assertEquals(2, c.size());
    }

    @Test
    public void testMergeAllIntoOneComponent() {
        Connectivity<Integer> c = Connectivity.create(0, 1, 2, 3, 4);
        c.connect(0, 1);
        c.connect(1, 2);
        c.connect(2, 3);
        c.connect(3, 4);
        assertEquals(1, c.size());
        assertTrue(c.isConnected(0, 4));
    }

    // -------------------------------------------------------------------------
    // connect / isConnected — createOptimized
    // -------------------------------------------------------------------------

    @Test
    public void testOptimizedConnectTwoVertices() {
        Connectivity<Integer> c = Connectivity.createOptimized(0, 1, 2);
        c.connect(0, 1);
        assertTrue(c.isConnected(0, 1));
    }

    @Test
    public void testOptimizedTransitiveConnectivity() {
        Connectivity<Integer> c = Connectivity.createOptimized(0, 1, 2);
        c.connect(0, 1);
        c.connect(1, 2);
        assertTrue(c.isConnected(0, 2));
    }

    @Test
    public void testOptimizedMergeAllIntoOneComponent() {
        Connectivity<Integer> c = Connectivity.createOptimized(0, 1, 2, 3, 4);
        c.connect(0, 1);
        c.connect(1, 2);
        c.connect(2, 3);
        c.connect(3, 4);
        assertEquals(1, c.size());
        assertTrue(c.isConnected(0, 4));
    }

    @Test
    public void testOptimizedUnconnectedVerticesRemainSeparate() {
        Connectivity<Integer> c = Connectivity.createOptimized(0, 1, 2, 3);
        c.connect(0, 1);
        c.connect(2, 3);
        assertFalse(c.isConnected(0, 2));
        assertEquals(2, c.size());
    }

    // -------------------------------------------------------------------------
    // getComponent
    // -------------------------------------------------------------------------

    @Test
    public void testGetComponentInitiallyReturnsSelf() {
        Connectivity<Integer> c = Connectivity.create(0, 1, 2);
        assertEquals(Integer.valueOf(0), c.getComponent(0));
        assertEquals(Integer.valueOf(1), c.getComponent(1));
    }

    @Test
    public void testGetComponentSameAfterConnect() {
        Connectivity<Integer> c = Connectivity.create(0, 1, 2);
        c.connect(0, 1);
        // both 0 and 1 must now share the same representative
        assertEquals(c.getComponent(0), c.getComponent(1));
    }

    // -------------------------------------------------------------------------
    // toString
    // -------------------------------------------------------------------------

    @Test
    public void testToString() {
        Connectivity<Integer> c = Connectivity.create(0, 1, 2);
        c.connect(0, 1);
        assertEquals("Connectivity{components=2}", c.toString());
    }

    // -------------------------------------------------------------------------
    // Stress test
    // -------------------------------------------------------------------------

    @Test
    public void testStressOptimized() {
        int n = 1000;
        List<Integer> vs = java.util.stream.IntStream.range(0, n)
                .boxed().toList();
        Connectivity<Integer> c = Connectivity.createOptimized(vs);
        java.util.Random rng = new java.util.Random(42);
        for (int i = 0; i < 4 * n; i++)
            c.connect(rng.nextInt(n), rng.nextInt(n));
        assertTrue(c.size() < n);
    }
}