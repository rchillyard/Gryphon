/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.java;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for issue #105: a failure inside Gryphon reaches a Java caller as an exception that
 * caller can name.
 *
 * <p>Before this, the bridge unwrapped a Scala {@code Try} with {@code .get}, so the Scala
 * throwable came out as it stood and the facade could promise only {@code RuntimeException}.
 * These tests are written from the position of a Java user who has never seen the Scala API:
 * they catch {@link GryphonException} by name, and they do not mention any Scala type except
 * through {@link Throwable#getCause()}, which is exactly the boundary the facade is supposed
 * to draw.</p>
 */
public class GryphonExceptionTest {

    @Test
    public void missingResourceThrowsGryphonException() {
        GryphonException x = assertThrows(
                GryphonException.class,
                () -> WeightedGraph.undirectedFromResource("no-such-file.graph"));
        assertTrue(x.getMessage().contains("no-such-file.graph"),
                "the message should name the resource, but was: " + x.getMessage());
    }

    @Test
    public void theScalaThrowableIsKeptAsTheCause() {
        // Nothing that was reachable through Try.get is lost: the original is still there, with
        // its own message, for a caller who does want to look.
        GryphonException x = assertThrows(
                GryphonException.class,
                () -> WeightedGraph.directedFromResource("no-such-file.graph"));
        assertNotNull(x.getCause(), "the underlying Scala throwable should be kept as the cause");
    }

    @Test
    public void itIsUncheckedSoCallersNeedNotDeclareIt() {
        // The point of extending RuntimeException: a caller who does not care about the failure
        // writes no throws clause and catches nothing. This compiles, which is the assertion.
        assertThrows(RuntimeException.class,
                () -> WeightedGraph.undirectedFromResource("no-such-file.graph"));
    }

    @Test
    public void aGoodResourceStillLoads() {
        // The wrapping must not turn a success into anything else.
        WeightedGraph<Integer, Double> g = WeightedGraph.undirectedFromResource("prim.graph");
        assertNotNull(g);
        assertFalse(g.isDirected());
    }
}
