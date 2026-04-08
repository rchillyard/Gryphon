/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.java;

import com.phasmidsoftware.gryphon.adjunct.ConnectivityOptimized$;
import scala.collection.immutable.Seq;
import scala.jdk.CollectionConverters;

import java.util.Arrays;
import java.util.List;

/**
 * A mutable façade over Gryphon's purely functional disjoint-set implementations,
 * providing a Java-idiomatic API for connectivity queries.
 *
 * <p>Internally, this class delegates to an immutable Scala
 * {@code AbstractDisjointSet} instance, swapping the reference on each
 * mutating operation. From a Java caller's perspective the object behaves
 * as an ordinary mutable data structure.</p>
 *
 * <p>Two implementations are available via the factory methods:
 * <ul>
 *   <li>{@link #create(List)} — Weighted Quick Union, O(log n) per operation.</li>
 *   <li>{@link #createOptimized(List)} — Weighted Quick Union with path
 *       compression, amortized near-O(1) per operation.</li>
 * </ul>
 * For most purposes {@code createOptimized} is the better choice; {@code create}
 * is provided so students can observe the effect of path compression directly.</p>
 *
 * <p><b>Usage example:</b>
 * <pre>{@code
 * Connectivity<String> c = Connectivity.createOptimized(List.of("A", "B", "C", "D"));
 * c.connect("A", "B");
 * c.connect("C", "D");
 * c.isConnected("A", "B"); // true
 * c.isConnected("A", "C"); // false
 * c.size();                // 2  (two components)
 * }</pre>
 * </p>
 *
 * @param <V> the vertex type. Must implement {@code equals} and {@code hashCode}.
 */
public class Connectivity<V> {

    /**
     * The underlying Scala disjoint-set instance.
     * Reassigned on every mutating operation ({@link #connect}, {@link #put}).
     */
    private com.phasmidsoftware.gryphon.adjunct.AbstractDisjointSet<V, ?, ?> delegate;

    /**
     * Private constructor — use the factory methods.
     */
    private Connectivity(com.phasmidsoftware.gryphon.adjunct.AbstractDisjointSet<V, ?, ?> delegate) {
        this.delegate = delegate;
    }

    // -------------------------------------------------------------------------
    // Factory methods
    // -------------------------------------------------------------------------

    /**
     * Creates a {@code Connectivity} using Weighted Quick Union (O(log n)).
     *
     * @param <V> the vertex type.
     * @param vs  the initial vertices, each placed in its own singleton component.
     * @return a new {@code Connectivity} containing all vertices in {@code vs}.
     */
    public static <V> Connectivity<V> create(List<V> vs) {
        Seq<V> scalaSeq = CollectionConverters.ListHasAsScala(vs).asScala().toSeq();
        return new Connectivity<>(
                com.phasmidsoftware.gryphon.adjunct.Connectivity$.MODULE$.create(scalaSeq));
    }

    /**
     * Creates a {@code Connectivity} using Weighted Quick Union with path
     * compression (amortized near-O(1)).
     *
     * @param <V> the vertex type.
     * @param vs  the initial vertices, each placed in its own singleton component.
     * @return a new {@code Connectivity} containing all vertices in {@code vs}.
     */
    public static <V> Connectivity<V> createOptimized(List<V> vs) {
        Seq<V> scalaSeq = CollectionConverters.ListHasAsScala(vs).asScala().toSeq();
        return new Connectivity<>(
                ConnectivityOptimized$.MODULE$.create(scalaSeq));
    }

    /**
     * Convenience overload accepting varargs.
     *
     * @param vs  the initial vertices.
     * @param <V> the vertex type.
     * @return a new {@code Connectivity} containing all vertices in {@code vs}.
     */
    @SafeVarargs
    public static <V> Connectivity<V> create(V... vs) {
        return create(Arrays.asList(vs));
    }

    /**
     * Convenience overload accepting varargs, using path compression.
     *
     * @param vs  the initial vertices.
     * @param <V> the vertex type.
     * @return a new {@code Connectivity} containing all vertices in {@code vs}.
     */
    @SafeVarargs
    public static <V> Connectivity<V> createOptimized(V... vs) {
        return createOptimized(Arrays.asList(vs));
    }

    // -------------------------------------------------------------------------
    // Mutating operations
    // -------------------------------------------------------------------------

    /**
     * Merges the components containing {@code v1} and {@code v2}.
     *
     * <p>After this call, {@code isConnected(v1, v2)} returns {@code true}.
     * If {@code v1} and {@code v2} are already in the same component, this
     * method has no effect.</p>
     *
     * @param v1 the first vertex.
     * @param v2 the second vertex.
     */
    @SuppressWarnings("unchecked")
    public void connect(V v1, V v2) {
        delegate = (com.phasmidsoftware.gryphon.adjunct.AbstractDisjointSet<V, ?, ?>) delegate.connect(v1, v2);
    }

    /**
     * Adds {@code key} as a new singleton component.
     *
     * <p>If {@code key} is already present, this method has no effect.</p>
     *
     * @param key the vertex to add.
     */
    @SuppressWarnings("unchecked")
    public void put(V key) {
        delegate = (com.phasmidsoftware.gryphon.adjunct.AbstractDisjointSet<V, ?, ?>) delegate.put(key);
    }

    // -------------------------------------------------------------------------
    // Query operations
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} if {@code v1} and {@code v2} belong to the same component.
     *
     * @param v1 the first vertex.
     * @param v2 the second vertex.
     * @return {@code true} if connected, {@code false} otherwise.
     */
    public boolean isConnected(V v1, V v2) {
        return delegate.isConnected(v1, v2);
    }

    /**
     * Returns the number of disjoint components (not the number of vertices).
     *
     * @return the component count.
     */
    public int size() {
        return delegate.size();
    }

    /**
     * Returns the representative (root) of the component containing {@code key}.
     *
     * <p>Two vertices are in the same component if and only if their roots are equal.</p>
     *
     * @param key a vertex.
     * @return the root of {@code key}'s component.
     */
    public V getComponent(V key) {
        return delegate.getDisjointSet(key);
    }

    /**
     * Returns a human-readable summary of this {@code Connectivity}.
     */
    @Override
    public String toString() {
        return "Connectivity{components=" + delegate.size() + "}";
    }
}