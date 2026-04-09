/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.java;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link Graph}, {@link Edge}, and {@link WeightedEdge}.
 */
public class GraphTest {

    // =========================================================================
    // Edge
    // =========================================================================

    @org.junit.jupiter.api.Nested
    class EdgeTests {

        @Test
        void testFromAndTo() {
            Edge<String> e = new Edge<>("A", "B");
            assertEquals("A", e.from());
            assertEquals("B", e.to());
        }

        @Test
        void testOf() {
            Edge<Integer> e = Edge.of(1, 2);
            assertEquals(1, e.from());
            assertEquals(2, e.to());
        }

        @Test
        void testReverse() {
            Edge<String> e = new Edge<>("A", "B");
            Edge<String> r = e.reverse();
            assertEquals("B", r.from());
            assertEquals("A", r.to());
        }

        @Test
        void testEquality() {
            Edge<String> e1 = new Edge<>("A", "B");
            Edge<String> e2 = new Edge<>("A", "B");
            Edge<String> e3 = new Edge<>("B", "A");
            assertEquals(e1, e2);
            assertNotEquals(e1, e3);
        }

        @Test
        void testHashCode() {
            Edge<String> e1 = new Edge<>("A", "B");
            Edge<String> e2 = new Edge<>("A", "B");
            assertEquals(e1.hashCode(), e2.hashCode());
        }

        @Test
        void testToString() {
            Edge<String> e = new Edge<>("A", "B");
            assertEquals("A -> B", e.toString());
        }
    }

    // =========================================================================
    // WeightedEdge
    // =========================================================================

    @org.junit.jupiter.api.Nested
    class WeightedEdgeTests {

        @Test
        void testAttribute() {
            WeightedEdge<String, Double> e = new WeightedEdge<>("A", "B", 3.7);
            assertEquals("A", e.from());
            assertEquals("B", e.to());
            assertEquals(3.7, e.attribute());
        }

        @Test
        void testOf() {
            WeightedEdge<String, Double> e = WeightedEdge.of("X", "Y", 1.5);
            assertEquals(1.5, e.attribute());
        }

        @Test
        void testReverse() {
            WeightedEdge<String, Double> e = new WeightedEdge<>("A", "B", 2.0);
            WeightedEdge<String, Double> r = e.reverse();
            assertEquals("B", r.from());
            assertEquals("A", r.to());
            assertEquals(2.0, r.attribute());
        }

        @Test
        void testEquality() {
            WeightedEdge<String, Double> e1 = new WeightedEdge<>("A", "B", 1.0);
            WeightedEdge<String, Double> e2 = new WeightedEdge<>("A", "B", 1.0);
            WeightedEdge<String, Double> e3 = new WeightedEdge<>("A", "B", 2.0);
            assertEquals(e1, e2);
            assertNotEquals(e1, e3);
        }

        @Test
        void testVoidAttribute() {
            WeightedEdge<String, Void> e = new WeightedEdge<>("A", "B", null);
            assertNull(e.attribute());
        }

        @Test
        void testToString() {
            WeightedEdge<String, Double> e = new WeightedEdge<>("A", "B", 3.7);
            assertEquals("A -[3.7]-> B", e.toString());
        }
    }

    // =========================================================================
    // Graph — construction
    // =========================================================================

    @org.junit.jupiter.api.Nested
    class GraphConstructionTests {

        @Test
        void testDirectedFactory() {
            Graph<String> g = Graph.directed();
            assertTrue(g.isDirected());
        }

        @Test
        void testUndirectedFactory() {
            Graph<String> g = Graph.undirected();
            assertFalse(g.isDirected());
        }

        @Test
        void testAddVertex() {
            Graph<String> g = Graph.undirected();
            g.addVertex("A");
            g.addVertex("B");
            assertEquals(Set.of("A", "B"), g.vertices());
        }

        @Test
        void testAddEdgeImplicitlyAddsVertices() {
            Graph<String> g = Graph.undirected();
            g.addEdge("A", "B");
            assertTrue(g.vertices().contains("A"));
            assertTrue(g.vertices().contains("B"));
        }

        @Test
        void testAddEdgeObject() {
            Graph<String> g = Graph.directed();
            g.addEdge(new Edge<>("A", "B"));
            assertEquals(1, g.edges().size());
        }

        @Test
        void testCanonicalEdgesStoredOnce() {
            Graph<String> g = Graph.undirected();
            g.addEdge("A", "B");
            g.addEdge("B", "C");
            // Undirected: both directions stored in adjacency,
            // but canonical list has only the original two edges.
            assertEquals(2, g.edges().size());
        }

        @Test
        void testAddWeightedEdge() {
            Graph<String> g = Graph.undirected();
            g.addEdge(new WeightedEdge<>("A", "B", 1.5));
            List<Edge<String>> edges = g.edges();
            assertEquals(1, edges.size());
            assertInstanceOf(WeightedEdge.class, edges.getFirst());
            assertEquals(1.5, ((WeightedEdge<String, Double>) edges.getFirst()).attribute());
        }

        @Test
        void testToString() {
            Graph<String> g = Graph.undirected();
            g.addEdge("A", "B");
            assertEquals("UndirectedGraph{vertices=2, edges=1}", g.toString());
        }
    }

    // =========================================================================
    // Graph — neighbours and edgesFrom
    // =========================================================================

    @org.junit.jupiter.api.Nested
    class GraphNeighboursTests {

        @Test
        void testNeighboursUndirected() {
            Graph<String> g = Graph.undirected();
            g.addEdge("A", "B");
            g.addEdge("A", "C");
            Iterable<String> ns = g.neighbours("A");
            List<String> list = toList(ns);
            assertTrue(list.contains("B"));
            assertTrue(list.contains("C"));
        }

        @Test
        void testNeighboursDirected() {
            Graph<String> g = Graph.directed();
            g.addEdge("A", "B");
            g.addEdge("A", "C");
            // B and C are reachable from A
            assertTrue(toList(g.neighbours("A")).contains("B"));
            assertTrue(toList(g.neighbours("A")).contains("C"));
            // A is NOT reachable from B in a directed graph
            assertFalse(toList(g.neighbours("B")).contains("A"));
        }

        @Test
        void testUndirectedSymmetry() {
            Graph<String> g = Graph.undirected();
            g.addEdge("A", "B");
            assertTrue(toList(g.neighbours("A")).contains("B"));
            assertTrue(toList(g.neighbours("B")).contains("A"));
        }

        @Test
        void testNeighboursUnknownVertex() {
            Graph<String> g = Graph.undirected();
            assertThrows(NoSuchElementException.class, () -> g.neighbours("X"));
        }

        @Test
        void testEdgesFrom() {
            Graph<String> g = Graph.directed();
            g.addEdge(new WeightedEdge<>("A", "B", 2.0));
            g.addEdge(new WeightedEdge<>("A", "C", 3.0));
            List<Edge<String>> edges = toList(g.edgesFrom("A"));
            assertEquals(2, edges.size());
        }
    }

    // =========================================================================
    // Graph — BFS
    // =========================================================================

    @org.junit.jupiter.api.Nested
    class BfsTests {

        @Test
        void testBfsStartMapsToItself() {
            Graph<String> g = Graph.undirected();
            g.addEdge("A", "B");
            Map<String, String> tree = g.bfs("A");
            assertEquals("A", tree.get("A"));
        }

        @Test
        void testBfsReachableVertex() {
            Graph<String> g = Graph.undirected();
            g.addEdge("A", "B");
            g.addEdge("B", "C");
            Map<String, String> tree = g.bfs("A");
            assertTrue(tree.containsKey("C"));
        }

        @Test
        void testBfsParentCorrect() {
            Graph<String> g = Graph.undirected();
            g.addEdge("A", "B");
            g.addEdge("B", "C");
            Map<String, String> tree = g.bfs("A");
            assertEquals("A", tree.get("B"));
            assertEquals("B", tree.get("C"));
        }

        @Test
        void testBfsUnreachableVertex() {
            Graph<String> g = Graph.undirected();
            g.addEdge("A", "B");
            g.addVertex("D");  // isolated
            Map<String, String> tree = g.bfs("A");
            assertFalse(tree.containsKey("D"));
        }

        @Test
        void testBfsDirected() {
            Graph<String> g = Graph.directed();
            g.addEdge("A", "B");
            g.addEdge("B", "C");
            Map<String, String> tree = g.bfs("A");
            assertTrue(tree.containsKey("B"));
            assertTrue(tree.containsKey("C"));
            // reverse direction not reachable
            assertFalse(tree.containsKey("A") && tree.get("A").equals("B"));
        }

        @Test
        void testBfsCustomNeighbours() {
            Graph<String> g = Graph.undirected();
            g.addEdge("A", "B");
            g.addEdge("A", "C");
            g.addEdge("B", "D");
            // Custom neighbour function: exclude "C"
            Map<String, String> tree = g.bfs("A",
                    v -> toList(g.neighbours(v)).stream()
                            .filter(n -> !n.equals("C"))
                            .toList());
            assertFalse(tree.containsKey("C"));
            assertTrue(tree.containsKey("D"));
        }

        @Test
        void testBfsConnectivityQuery() {
            Graph<Integer> g = Graph.undirected();
            for (int i = 0; i < 5; i++) g.addEdge(i, i + 1);
            Map<Integer, Integer> tree = g.bfs(0);
            // All vertices 0..5 reachable
            for (int i = 0; i <= 5; i++) assertTrue(tree.containsKey(i));
        }
    }

    // =========================================================================
    // Graph — DFS
    // =========================================================================

    @org.junit.jupiter.api.Nested
    class DfsTests {

        @Test
        void testDfsStartMapsToItself() {
            Graph<String> g = Graph.undirected();
            g.addEdge("A", "B");
            Map<String, String> tree = g.dfs("A");
            assertEquals("A", tree.get("A"));
        }

        @Test
        void testDfsReachableVertex() {
            Graph<String> g = Graph.undirected();
            g.addEdge("A", "B");
            g.addEdge("B", "C");
            Map<String, String> tree = g.dfs("A");
            assertTrue(tree.containsKey("C"));
        }

        @Test
        void testDfsUnreachableVertex() {
            Graph<String> g = Graph.undirected();
            g.addEdge("A", "B");
            g.addVertex("D");
            Map<String, String> tree = g.dfs("A");
            assertFalse(tree.containsKey("D"));
        }

        @Test
        void testDfsDirected() {
            Graph<String> g = Graph.directed();
            g.addEdge("A", "B");
            g.addEdge("B", "C");
            Map<String, String> tree = g.dfs("A");
            assertTrue(tree.containsKey("B"));
            assertTrue(tree.containsKey("C"));
        }

        @Test
        void testDfsCustomNeighbours() {
            Graph<String> g = Graph.undirected();
            g.addEdge("A", "B");
            g.addEdge("A", "C");
            g.addEdge("C", "D");
            // Exclude B — so D is reachable via C but not via B
            Map<String, String> tree = g.dfs("A",
                    v -> toList(g.neighbours(v)).stream()
                            .filter(n -> !n.equals("B"))
                            .toList());
            assertFalse(tree.containsKey("B"));
            assertTrue(tree.containsKey("D"));
        }

        @Test
        void testDfsParentMapAllowsPathReconstruction() {
            Graph<String> g = Graph.undirected();
            g.addEdge("A", "B");
            g.addEdge("B", "C");
            g.addEdge("C", "D");
            Map<String, String> tree = g.dfs("A");
            // Reconstruct path from D back to A
            List<String> path = reconstructPath(tree, "A", "D");
            assertEquals("A", path.getFirst());
            assertEquals("D", path.getLast());
        }

        @Test
        void testDfsLargeGraph() {
            int n = 10_000;
            Graph<Integer> g = Graph.undirected();
            for (int i = 0; i < n - 1; i++) g.addEdge(i, i + 1);
            Map<Integer, Integer> tree = g.dfs(0);
            assertEquals(n, tree.size());
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static <T> List<T> toList(Iterable<T> it) {
        List<T> list = new java.util.ArrayList<>();
        it.forEach(list::add);
        return list;
    }

    private static <V> List<V> reconstructPath(Map<V, V> parent, V start, V end) {
        List<V> path = new java.util.ArrayList<>();
        V current = end;
        while (!current.equals(start)) {
            path.addFirst(current);
            current = parent.get(current);
        }
        path.addFirst(start);
        return path;
    }
}