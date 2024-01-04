/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.list;

import java.util.Iterator;

public class List<T> implements java.lang.Iterable<T> {

    public Split<T> split() {
        return list==null ? null : new Split<T>(list.t, new List<>(list.next));
    }

    public int size() {
        int result = 0;
        for (T t : this) result++;
        return result;
    }

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

    public List(Node list) {
        this.list = list;
    }
    public List() {
        this(null);
    }

    public List<T> prepend(T t) {
        return new List<>(new Node(t, list));
    }

    public static <U> List<U> cons(U u, List<U> tail) {
        return tail.prepend(u);
    }

    public static <U> List<U> of(U... us) {
        List<U> result = new List<>();
        for (int i = us.length; i > 0; i--)
            result = result.prepend(us[i-1]);
        return result;
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
        public Split(X head, List<X> tail) {
            this.head = head;
            this.tail = tail;
        }

        final X head;
        final List<X> tail;
    }

    private final Node list;

    public static <X> List<X> empty() {
        return new List<>();
    }
}
