/*
 * Copyright (c) 2024. Phasmid Software
 */

package com.phasmidsoftware.list;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertArrayEquals;

public class LazyListTest {

    @Test
    public void of() {
        LazyList<Integer> lazyList = LazyList.of(x -> x + 1, 1);
        List<Integer> list = lazyList.take(4).toList();
        assertArrayEquals(new Integer[]{1, 2, 3, 4}, list.toArray());
    }

    @Test
    public void from() {
        LazyList<Integer> lazyList = LazyList.from(1);
        List<Integer> list = lazyList.take(4).toList();
        assertArrayEquals(new Integer[]{1, 2, 3, 4}, list.toArray());
    }

    @Test(expected = java.lang.RuntimeException.class)
    public void toList() {
        LazyList.from(1).toList();
    }

    @Test
    public void map() {
        LazyList<String> lazyList = LazyList.from(1).map(Object::toString);
        List<String> list = lazyList.take(4).toList();
        assertArrayEquals(new String[]{"1", "2", "3", "4"}, list.toArray());
    }

    @Test
    public void drop() {
        LazyList<Integer> lazyList = LazyList.from(0);
        List<Integer> list = lazyList.drop(1).take(4).toList();
        assertArrayEquals(new Integer[]{1, 2, 3, 4}, list.toArray());
    }
}