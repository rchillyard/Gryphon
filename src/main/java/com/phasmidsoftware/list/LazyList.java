/*
 * Copyright (c) 2024. Phasmid Software
 */

package com.phasmidsoftware.list;

import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * Class to implement a LazyList of E.
 * Basically, LazyList is just a wrapper around Stream of E.
 *
 * @param <E> the underlying element type.
 */
public class LazyList<E> {

    /**
     * Class method to construct a LazyList from a start element and a function.
     *
     * @param f     the function to apply to one element in order to get the value of the next.
     * @param start the starting value.
     * @param <X>   the underlying type of the result.
     * @return a new LazyList of X.
     */
    public static <X> LazyList<X> of(UnaryOperator<X> f, X start) {
        return new LazyList<>(Stream.iterate(start, f));
    }

    /**
     * Class method to construct a LazyList from the int start element with given increment.
     *
     * @param start     the starting value.
     * @param increment the increment to apply between successive elements.
     * @return a new LazyList of Integer.
     */
    public static LazyList<Integer> from(int start, int increment) {
        return of(x -> x + increment, start);
    }

    /**
     * Class method to construct a LazyList from the int start element.
     *
     * @param start the starting value.
     * @return a new LazyList of Integer.
     */
    public static LazyList<Integer> from(int start) {
        return from(start, 1);
    }

    /**
     * Method to map this LazyList into a new LazyList.
     *
     * @param f   the mapping function.
     * @param <X> the underlying type of the result.
     * @return a new LazyList of X.
     */
    public <X> LazyList<X> map(Function<E, X> f) {
        return new LazyList<>(stream.map(f));
    }

    /**
     * Method to take only a certain number of elements from this LazyList.
     *
     * @param n the number of elements to take.
     * @return a new LazyList of E with finite length.
     */
    public LazyList<E> take(long n) {
        return new LazyList<>(stream.limit(n), true);
    }

    /**
     * Method to drop a certain number of elements from this LazyList.
     *
     * @param n the number of elements to drop.
     * @return a new LazyList of E based on this LazyList but without the first n elements.
     */
    public LazyList<E> drop(long n) {
        return new LazyList<>(stream.skip(n));
    }

    /**
     * Method to convert this LazyList into a (finite and strict) List.
     * NOTE: this will fail if this LazyList has infinite length.
     *
     * @return a new List of E based on this LazyList.
     */
    public List<E> toList() {
        if (lengthIsFinite)
            return stream.toList();
        else
            throw new RuntimeException("LazyList: is not finite");
    }

    /**
     * Primary constructor.
     *
     * @param stream         a Stream of E.
     * @param lengthIsFinite a boolean indicator of finiteness.
     */
    public LazyList(Stream<E> stream, boolean lengthIsFinite) {
        this.stream = stream;
        this.lengthIsFinite = lengthIsFinite;
    }

    /**
     * Secondary constructor.
     *
     * @param stream a Stream of E.
     */
    public LazyList(Stream<E> stream) {
        this(stream, false);
    }

    private final Stream<E> stream;
    private final boolean lengthIsFinite;
}
