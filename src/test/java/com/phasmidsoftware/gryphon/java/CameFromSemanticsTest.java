package com.phasmidsoftware.gryphon.java;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What a came-from map holds — asserted, not merely documented. Issues #144, #145, #146, #147.
 *
 * <p><b>Each traversal now returns its own tree</b>, as of 1.15.0. A came-from map records, for
 * each vertex, the vertex it was <em>explored from</em> for depth-first search and the vertex it
 * was first <em>seen from</em> for breadth-first — which are the parents the DFS tree and the
 * BFS tree are respectively defined by.</p>
 *
 * <p>They used to be the same mechanism, recording first-seen for both. That is right for BFS,
 * where discovery and exploration coincide, and wrong for DFS, where a vertex can be seen early
 * from one neighbour and explored later from somewhere deeper. #145 documented the discrepancy
 * and these tests pinned it; Visitor 1.10.0 removed it (#146).</p>
 *
 * <p>The test that asserted the old DFS behaviour is gone — it existed to mark where the
 * behaviour would change, and it has. The BFS test remains unchanged and still passes, which is
 * the evidence that the fix reached exactly as far as it should.</p>
 */
public class CameFromSemanticsTest {

    /**
     * The transitive tournament on six vertices: an edge from every {@code i} to every
     * {@code j > i}. A tournament has exactly one edge between each pair of vertices, as in
     * round-robin play where the edge points to the winner.
     *
     * <p>Chosen because it separates the two readings as sharply as any graph can. Every vertex
     * is adjacent to the source, so every vertex is <em>discovered</em> from the source — while
     * a depth-first <em>exploration</em> runs through all six in a chain. The discovery map is
     * therefore a star and the DFS tree a Hamiltonian path, which is the largest possible
     * disagreement between them.</p>
     */
    private static Graph<Integer> tournament() {
        Graph<Integer> g = Graph.directed();
        for (int i = 0; i < 6; i++)
            for (int j = i + 1; j < 6; j++)
                g.addEdge(i, j);
        return g;
    }

    /**
     * BFS is the half that was always right, and the half #146 had to leave alone.
     *
     * <p>The star here is not a defect: in a tournament every vertex really is one edge from the
     * source, so a BFS tree rooted at 0 has all five as its children. Discovery and exploration
     * coincide for BFS, which is why its came-from map can be walked back for a shortest path.</p>
     *
     * <p>It still passes after #146, which is the point: the fix changed DFS and left BFS alone,
     * because discovery and exploration coincide for breadth-first search. If this ever fails,
     * a change has reached further than it should.</p>
     */
    @Test
    void bfs_came_from_is_the_bfs_tree() {
        Map<Integer, Integer> cameFrom = tournament().bfs(0, new Random(42));
        assertEquals(5, cameFrom.size(), "every vertex but the source");
        assertEquals(Set.of(0), new HashSet<>(cameFrom.values()),
                "every vertex is one edge from the source, so all five are its children");
    }

    /**
     * DFS now returns the DFS tree, as of Visitor 1.10.0.
     *
     * <p>The assertion is deliberately NOT "no vertex parents two others", which is what the
     * pending version of this test asserted while #146 was open. <b>That invariant is false.</b>
     * A DFS tree on a tournament is a Hamiltonian path only if the search never backtracks, and
     * Gryphon shuffles adjacency order — so a search may take 3 first, descend, and then
     * backtrack to the source for 1 and 2, leaving the source with two children. That is a
     * perfectly correct DFS tree.</p>
     *
     * <p>The seed is pinned because the shape of a valid DFS tree depends on the adjacency order,
     * and with the order pinned the tree is determined. That is the honest way to assert this:
     * the properties that hold for <em>every</em> DFS tree — real edges, one parent each, rooted
     * at the source — are also satisfied by the old star, so they cannot tell the two apart.</p>
     *
     * <p>What distinguishes the two is depth. The old map was a star of depth one; a DFS tree on
     * a tournament reaches much deeper, because the search descends before it backtracks.</p>
     */
    @Test
    void dfs_came_from_is_the_dfs_tree() {
        Map<Integer, Integer> cameFrom = tournament().dfs(0, new Random(42));
        assertEquals(5, cameFrom.size(), "every vertex but the source");

        // Every recorded parent precedes its child, since the tournament only runs i -> j for i < j.
        cameFrom.forEach((child, parent) -> assertTrue(parent < child,
                "parent " + parent + " should precede child " + child));

        // The discriminating property. In the old discovery map every vertex hung directly off
        // the source, so every chain was length one. A DFS descends, so some vertex is further.
        int deepest = 0;
        for (int v : cameFrom.keySet()) {
            int depth = 0;
            for (Integer at = v; at != null; at = cameFrom.get(at)) depth++;
            deepest = Math.max(deepest, depth);
        }
        assertTrue(deepest > 2, "a DFS tree descends; the old discovery map was a star of depth 2");

        // Walking back from any vertex must reach the source without cycling.
        for (int v : cameFrom.keySet()) {
            Integer at = v;
            Set<Integer> seen = new HashSet<>();
            while (cameFrom.containsKey(at)) {
                assertTrue(seen.add(at), "came-from map should not cycle");
                at = cameFrom.get(at);
            }
            assertEquals(0, at, "every chain ends at the source");
        }
    }
}
