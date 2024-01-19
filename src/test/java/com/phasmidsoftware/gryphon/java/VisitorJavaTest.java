/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.java;

import com.phasmidsoftware.gryphon.oldcore.Graph;
import com.phasmidsoftware.gryphon.oldcore.UndirectedEdge;
import org.junit.Test;
import scala.Tuple2;
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
        OrderedGraphBuilderJava<String, String, BoxedUnit> gb =
                OrderedGraphBuilderJava.create(w -> w, w -> w);
        Optional<List<UndirectedEdge<String, String>>> maybeEdges =
                gb.createUndirectedEdgeList("/prim.graph");
        assertTrue(maybeEdges.isPresent());
        UndirectedEdge<String, String> edge = maybeEdges.get().get(0);
        Optional<Graph<String, String, UndirectedEdge<String, String>, Tuple2<String, String>>> maybeGraph =
                gb.createGraphFromUndirectedEdgeList(maybeEdges);
        assertTrue(maybeGraph.isPresent());
        Graph<String, String, UndirectedEdge<String, String>, Tuple2<String, String>> graph = maybeGraph.get();
        Collection<String> strings = visitor.dfs(graph, edge.vertex());
        assertEquals(8, strings.size());
        System.out.println(strings);
        // NOTE we return the iterator of the visitor, not the iterator of the result of dfs.
        return visitor.iterator();
    }

}