/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.java;

/**
 * The exception thrown by the Java facade when a Gryphon operation fails.
 *
 * <p>The facade exists so that Java users need not reach into the Scala API. Loading a graph was
 * the one place where a failure made them: the bridge unwrapped a Scala {@code Try} with
 * {@code .get}, which rethrows whatever the failure held — a
 * {@code com.phasmidsoftware.gryphon.core.GraphException}, a parse error, or anything else. The
 * facade methods could only document that as {@code RuntimeException}, which tells a caller
 * nothing they can act on. To tell "no such resource" from "the file parsed but describes an
 * inconsistent graph", a Java caller had to catch {@code RuntimeException} and match on the
 * message, or name a Scala class the facade otherwise hides. See issue #105.</p>
 *
 * <p>That matters more since the graph-construction failures became carefully worded: a triplet
 * carrying no attribute where the edge type cannot represent one, a triplet whose orientation
 * disagrees with the graph being built, and an adjacency count that does not match the triplets
 * each produce a message naming the offender and saying what to do about it. Those messages now
 * reach a Java caller on an exception they can name.</p>
 *
 * <p>It is unchecked, so no signature churn for callers who do not care, and the original
 * throwable is always kept as the {@linkplain #getCause() cause}, so nothing that was available
 * before is lost.</p>
 *
 * <p>Note what this does <em>not</em> cover. The {@code IllegalStateException}s elsewhere in the
 * facade — in {@code MinimumSpanningTree}, {@code TopologicalSort}, {@code ShortestPaths},
 * {@code StronglyConnectedComponents} and {@code Graph} — are precondition checks thrown before
 * any Scala call is made. They report a caller's misuse of the facade rather than a failure
 * inside Gryphon, which is a different thing, and they are left as they are.</p>
 */
public class GryphonException extends RuntimeException {

    /**
     * Constructs a new exception with the given message and cause.
     *
     * @param message what went wrong, taken from the underlying failure.
     * @param cause   the throwable the Scala {@code Failure} held.
     */
    public GryphonException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new exception with the given message and no cause.
     *
     * @param message what went wrong.
     */
    public GryphonException(String message) {
        super(message);
    }
}
