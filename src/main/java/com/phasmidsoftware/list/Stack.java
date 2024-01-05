/*
 * Copyright (c) 2024. Phasmid Software
 */

package com.phasmidsoftware.list;

/**
 * Class to represent an immutable Stack of E elements.
 *
 * @param <E> underlying type of this Stack.
 */
public class Stack<E> {

    /**
     * Method to split this List into its head and its tail.
     *
     * @return a new instance of Split of T.
     */
    public Popped<E> pop() {
        final List.Split<E> split = list.split();
        if (split == null) throw new RuntimeException("Stack: is empty");
        else return new Popped<>(split.head, new Stack<>(split.tail));
    }

    /**
     * Method to prepend the element t to this List.
     *
     * @param e an element of type E.
     * @return a new Stack of E whose head is t and whose tail is this.
     */
    public Stack<E> push(final E e) {
        return new Stack<>(list.prepend(e));
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }

    /**
     * Primary constructor.
     *
     * @param list a list of E.
     */
    public Stack(final List<E> list) {
        this.list = list;
    }

    /**
     * Secondary constructor to create an empty Stack.
     */
    public Stack() {
        this(new List<>());
    }

    static class Popped<X> {
        public Popped(final X x, final Stack<X> stack) {
            this.x = x;
            this.stack = stack;
        }

        final X x;
        final Stack<X> stack;
    }

    private final List<E> list;
}
