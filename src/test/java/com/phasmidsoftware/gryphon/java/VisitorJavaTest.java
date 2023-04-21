/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.java;

import com.phasmidsoftware.gryphon.core.Graph;
import com.phasmidsoftware.gryphon.core.UndirectedOrderedEdge;
import org.junit.Test;
import scala.runtime.BoxedUnit;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class VisitorJavaTest {

    @Test
    public void createPre() {
        VisitorJavaQueue<String> visitor = VisitorJava.createPre();
        Iterator<String> strings = getVertices(visitor);
        assertEquals("0", strings.next());
    }

    @Test
    public void createPost() {
        VisitorJavaQueue<String> visitor = VisitorJava.createPost();
        Iterator<String> strings = getVertices(visitor);
        assertEquals("5", strings.next());
    }

    @Test
    public void createReversePost() {
        VisitorJavaStack<String> visitor = VisitorJava.createReversePost();
        Iterator<String> strings = getVertices(visitor);
        assertEquals("0", strings.next());
    }

    private static Iterator<String> getVertices(VisitorJava<String> visitor) {
        GraphBuilderJava<String, String, BoxedUnit> gb = GraphBuilderJava.create(w -> w, w -> w);
        Optional<List<UndirectedOrderedEdge<String, String>>> maybeEdges = gb.createUndirectedEdgeList("/prim.graph");
        assertTrue(maybeEdges.isPresent());
        UndirectedOrderedEdge<String, String> edge = maybeEdges.get().get(0);
        Optional<Graph<String, String, UndirectedOrderedEdge<String, String>, BoxedUnit>> maybeGraph = gb.createGraphFromUndirectedEdgeList(maybeEdges);
        assertTrue(maybeGraph.isPresent());
        Graph<String, String, UndirectedOrderedEdge<String, String>, BoxedUnit> graph = maybeGraph.get();
        Collection<String> strings = visitor.dfs(graph, edge.vertex());
        assertEquals(8, strings.size());
        System.out.println(strings);
        // NOTE we return the iterator of the visitor, not the iterator of the result of dfs.
        return visitor.iterator();
    }

}