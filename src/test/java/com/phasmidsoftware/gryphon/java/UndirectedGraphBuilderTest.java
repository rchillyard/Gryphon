/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.java;

import com.phasmidsoftware.gryphon.core.Graph;
import com.phasmidsoftware.gryphon.core.UndirectedEdge;
import org.junit.Test;
import scala.Tuple2;
import scala.runtime.BoxedUnit;

import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertTrue;

public class UndirectedGraphBuilderTest {

    @Test
    public void createUndirectedEdgeList() {
        OrderedGraphBuilderJava<String, String, BoxedUnit> gb =
                OrderedGraphBuilderJava.create(w -> w, w -> w);
        Optional<List<UndirectedEdge<String, String>>> maybeEdges =
                gb.createUndirectedEdgeList("/prim.graph");
        assertTrue(maybeEdges.isPresent());
        System.out.println(maybeEdges);
        Optional<Graph<String, String, UndirectedEdge<String, String>, Tuple2<String, String>>> maybeGraph =
                gb.createGraphFromUndirectedEdgeList(maybeEdges);
        assertTrue(maybeGraph.isPresent());
        System.out.println(maybeGraph);
    }
}