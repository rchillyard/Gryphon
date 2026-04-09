/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.java;

import java.util.Objects;

/**
 * Represents a weighted edge between two vertices, carrying an attribute of type {@code E}.
 *
 * <p>Extends {@link Edge}{@code <V>} with an additional weight (or attribute).
 * Used wherever edge attributes matter — shortest paths (Dijkstra), minimum
 * spanning trees (Prim, Kruskal), and any other algorithm that discriminates
 * between edges by weight.</p>
 *
 * <p>The attribute type {@code E} is intentionally general: it may be
 * {@code Double} for a numeric weight, {@code String} for a label,
 * or any other domain-specific type. Use {@code Void} (with {@code null}
 * attribute) to represent an explicitly unweighted edge in a context
 * that requires the {@code WeightedEdge} type.</p>
 *
 * <p>Edges are immutable. Equality is based on both endpoints and the attribute.</p>
 *
 * <p><b>Usage example:</b>
 * <pre>{@code
 * WeightedEdge<String, Double> e = new WeightedEdge<>("A", "B", 3.7);
 * String from   = e.from();      // "A"
 * String to     = e.to();        // "B"
 * double weight = e.attribute(); // 3.7
 * WeightedEdge<String, Double> rev = e.reverse(); // WeightedEdge("B", "A", 3.7)
 * }</pre>
 * </p>
 *
 * @param <V> the vertex type.
 * @param <E> the attribute (weight) type.
 */
public class WeightedEdge<V, E> extends Edge<V> {

    // -------------------------------------------------------------------------
    // Factory methods
    // -------------------------------------------------------------------------

    /**
     * Creates a new {@code WeightedEdge}.
     *
     * @param from      the source vertex.
     * @param to        the target vertex.
     * @param attribute the edge attribute (weight).
     * @param <V>       the vertex type.
     * @param <E>       the attribute type.
     * @return a new {@code WeightedEdge}.
     */
    public static <V, E> WeightedEdge<V, E> of(V from, V to, E attribute) {
        return new WeightedEdge<>(from, to, attribute);
    }

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Constructs a weighted edge from {@code from} to {@code to} with the
     * given {@code attribute}.
     *
     * @param from      the source vertex.
     * @param to        the target vertex.
     * @param attribute the edge attribute (weight).
     */
    public WeightedEdge(V from, V to, E attribute) {
        super(from, to);
        this.attribute = attribute;
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    /**
     * Returns the attribute (weight) of this edge.
     *
     * @return the edge attribute.
     */
    public E attribute() {
        return attribute;
    }

    // -------------------------------------------------------------------------
    // Operations
    // -------------------------------------------------------------------------

    /**
     * Returns a {@code WeightedEdge} with the endpoints swapped, preserving
     * the attribute.
     *
     * @return the reversed weighted edge.
     */
    @Override
    public WeightedEdge<V, E> reverse() {
        return new WeightedEdge<>(to(), from(), attribute);
    }

    // -------------------------------------------------------------------------
    // Object overrides
    // -------------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WeightedEdge<?, ?> other)) return false;
        return super.equals(o) && Objects.equals(attribute, other.attribute);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), attribute);
    }

    @Override
    public String toString() {
        return from() + " -[" + attribute + "]-> " + to();
    }

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    private final E attribute;
}
