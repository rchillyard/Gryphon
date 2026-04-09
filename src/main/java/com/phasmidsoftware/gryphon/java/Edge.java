/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.java;

import java.util.Objects;

/**
 * Represents an unweighted directed or undirected edge between two vertices.
 *
 * <p>An {@code Edge<V>} carries only structural information — the two endpoint
 * vertices. For edges that carry a weight or attribute, use
 * {@link WeightedEdge}{@code <V, E>} instead.</p>
 *
 * <p>Edges are immutable. Equality is based on both endpoints.</p>
 *
 * <p><b>Usage example:</b>
 * <pre>{@code
 * Edge<String> e = new Edge<>("A", "B");
 * String from = e.from();   // "A"
 * String to   = e.to();     // "B"
 * Edge<String> rev = e.reverse();  // Edge("B", "A")
 * }</pre>
 * </p>
 *
 * @param <V> the vertex type.
 */
public class Edge<V> {

    // -------------------------------------------------------------------------
    // Factory methods
    // -------------------------------------------------------------------------

    /**
     * Creates a new {@code Edge} from {@code from} to {@code to}.
     *
     * @param from the source vertex.
     * @param to   the target vertex.
     * @param <V>  the vertex type.
     * @return a new {@code Edge}.
     */
    public static <V> Edge<V> of(V from, V to) {
        return new Edge<>(from, to);
    }

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Constructs an edge from {@code from} to {@code to}.
     *
     * @param from the source vertex.
     * @param to   the target vertex.
     */
    public Edge(V from, V to) {
        this.from = from;
        this.to = to;
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    /**
     * Returns the source vertex of this edge.
     *
     * @return the source vertex.
     */
    public V from() {
        return from;
    }

    /**
     * Returns the target vertex of this edge.
     *
     * @return the target vertex.
     */
    public V to() {
        return to;
    }

    // -------------------------------------------------------------------------
    // Operations
    // -------------------------------------------------------------------------

    /**
     * Returns an edge with the endpoints swapped.
     *
     * <p>For an undirected graph this is equivalent to the original edge;
     * for a directed graph it represents the reverse arc.</p>
     *
     * @return the reversed edge.
     */
    public Edge<V> reverse() {
        return new Edge<>(to, from);
    }

    // -------------------------------------------------------------------------
    // Object overrides
    // -------------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Edge<?> other)) return false;
        return Objects.equals(from, other.from) && Objects.equals(to, other.to);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to);
    }

    @Override
    public String toString() {
        return from + " -> " + to;
    }

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    private final V from;
    private final V to;
}
