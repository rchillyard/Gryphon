/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.list;

import org.junit.Test;

import java.util.Iterator;

import static org.junit.Assert.*;

public class ListTest {

    @Test
    public void cons() {
        List<Integer> list = List.cons(1, new List<>());
        Iterator<Integer> iterator = list.iterator();
        assertTrue(iterator.hasNext());
        assertEquals(Integer.valueOf(1), iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void split0() {
        assertNull(List.<Integer>empty().split());
    }

    @Test
    public void split1() {
        List.Split<Integer> split = List.cons(1, new List<>()).split();
        assertEquals(Integer.valueOf(1), split.head);
        assertFalse(split.tail.iterator().hasNext());
    }

    @Test
    public void size() {
        assertEquals(0, List.<Integer>empty().size());
        assertEquals(1, List.of(1).size());
        assertEquals(2, List.of(1, 2).size());
    }

    @Test
    public void prepend1() {
        List<Integer> list = new List<Integer>().prepend(1);
        Iterator<Integer> iterator = list.iterator();
        assertTrue(iterator.hasNext());
        assertEquals(Integer.valueOf(1), iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void prepend2() {
        List<Integer> list = new List<Integer>().prepend(1).prepend(0);
        Iterator<Integer> iterator = list.iterator();
        assertTrue(iterator.hasNext());
        assertEquals(Integer.valueOf(0), iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals(Integer.valueOf(1), iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void of0() {
        assertFalse(List.<Integer>of().iterator().hasNext());
    }

    @Test
    public void of1() {
        List<Integer> list = List.of(1);
        Iterator<Integer> iterator = list.iterator();
        assertTrue(iterator.hasNext());
        assertEquals(Integer.valueOf(1), iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void of2() {
        List<Integer> list = List.of(1, 2);
        Iterator<Integer> iterator = list.iterator();
        assertTrue(iterator.hasNext());
        assertEquals(Integer.valueOf(1), iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals(Integer.valueOf(2), iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void of3() {
        List<Integer> list = List.of(1, 2, 3);
        Iterator<Integer> iterator = list.iterator();
        assertTrue(iterator.hasNext());
        assertEquals(Integer.valueOf(1), iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals(Integer.valueOf(2), iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals(Integer.valueOf(3), iterator.next());
        assertFalse(iterator.hasNext());
    }
}