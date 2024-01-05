/*
 * Copyright (c) 2024. Phasmid Software
 */

package com.phasmidsoftware.list;

import org.junit.Test;

import static org.junit.Assert.*;

public class StackTest {

    @Test
    public void push_pop() {
        Stack<Integer> stack = new Stack<>();
        assertTrue(stack.isEmpty());
        Stack<Integer> stack1 = stack.push(1);
        assertFalse(stack1.isEmpty());
        Stack.Popped<Integer> pop = stack1.pop();
        assertEquals(Integer.valueOf(1), pop.x);
        assertTrue(pop.stack.isEmpty());
    }
}