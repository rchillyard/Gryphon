/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.list;

import java.util.Iterator;

/**
 * Class to represent an immutable List.
 *
 * @param <T> the underlying type of this list.
 */
public class List<T> implements java.lang.Iterable<T> {

    /**
     * A "class" method to construct a new List of U from a U value and a List of U tail.
     *
     * @param u    an element of type U.
     * @param tail a List of underlying type U.
     * @param <U>  the underlying type of the new List.
     * @return a new List of U whose head is u and whose tail is <code>tail</code>.
     */
    public static <U> List<U> cons(final U u, final List<U> tail) {
        return tail.prepend(u);
    }

    /**
     * A "class" method to construct a new empty List of U.
     * Equivalent to invoking of().
     *
     * @param <U> the underlying type of the new empty List.
     * @return a new List of U which with the elements in the same order as the elements of us.
     */
    public static <U> List<U> empty() {
        return new List<>();
    }

    /**
     * A "class" method to construct a new List of U from a list of U values.
     *
     * @param us  a varargs list of U elements.
     * @param <U> the underlying type of the new List.
     * @return a new List of U which with the elements in the same order as the elements of us.
     */
    @SafeVarargs
    public static <U> List<U> of(final U... us) {
        List<U> result = new List<>();
        for (int i = us.length; i > 0; i--)
            result = result.prepend(us[i - 1]);
        return result;
    }

    /**
     * Method to split this List into its head and its tail.
     *
     * @return a new instance of Split of T.
     */
    public Split<T> split() {
        return list == null ? null : new Split<>(list.t, new List<>(list.next));
    }

    /**
     * Method to yield the size of this List.
     * Note that the performance of this method is linear in the length.
     *
     * @return the length of this List.
     */
    public int size() {
        int result = 0;
        for (T t : this) result++;
        return result;
    }

    /**
     * Method defined in Iterable to yield an Iterator of T.
     *
     * @return a new once-only Iterator of T.
     */
    public Iterator<T> iterator() {
        return new Iterator<>() {
            Node node = list;

            @Override
            public boolean hasNext() {
                return node != null;
            }

            @Override
            public T next() {
                Node n = node;
                node = node.next;
                return n.t;
            }
        };
    }

    /**
     * Method to prepend the element t to this List.
     *
     * @param t an element of type T.
     * @return a new List of T whose head is t and whose tail is this.
     */
    public List<T> prepend(final T t) {
        return new List<>(new Node(t, list));
    }

    public boolean isEmpty() {
        return list == null;
    }

    /**
     * Primary constructor.
     *
     * @param list the list, as a Node.
     */
    public List(final Node list) {
        this.list = list;
    }

    /**
     * Secondary constructor which creates an empty List.
     */
    public List() {
        this(null);
    }

    class Node {
        public Node(final T t, Node next) {
            this.t = t;
            this.next = next;
        }

        private final T t;
        private final Node next;
    }

    static class Split<X> {
        public Split(final X head, final List<X> tail) {
            this.head = head;
            this.tail = tail;
        }

        final X head;
        final List<X> tail;
    }

    private final Node list;
}
