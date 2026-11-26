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
 * What a came-from map holds — asserted, not merely documented. Issues #144, #145, #147.
 *
 * <p>A came-from map records, for each vertex, the vertex it was <em>first seen from</em>. For
 * breadth-first search that is also the vertex it was explored from, so the map is the BFS tree.
 * For depth-first search the two differ: a vertex can be seen early from one neighbour and
 * explored later from somewhere deeper, and it is the deeper vertex that would be its parent in
 * the DFS tree.</p>
 *
 * <p>#145 wrote that down. These tests hold it in place, and mark where it would change.</p>
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
     * BFS is the half that is already right, and the half #146 must leave alone.
     *
     * <p>The star here is not a defect: in a tournament every vertex really is one edge from the
     * source, so a BFS tree rooted at 0 has all five as its children. Discovery and exploration
     * coincide for BFS, which is why its came-from map can be walked back for a shortest path.</p>
     *
     * <p>If a fix for #146 ever makes this fail, the fix has reached further than it should.</p>
     */
    @Test
    void bfs_came_from_is_the_bfs_tree() {
        Map<Integer, Integer> cameFrom = tournament().bfs(0, new Random(42));
        assertEquals(5, cameFrom.size(), "every vertex but the source");
        assertEquals(Set.of(0), new HashSet<>(cameFrom.values()),
                "every vertex is one edge from the source, so all five are its children");
    }

    /**
     * DFS as it behaves today: a discovery map, not the DFS tree.
     *
     * <p>The same star as the BFS case above, and wrong for the same reason it is right there.
     * These five vertices were seen from the source but explored in a chain, so in the DFS tree
     * each would be the child of the one before it.</p>
     *
     * <p><b>This test is expected to fail when #146 lands, and that is its purpose.</b> It is the
     * line that says when the behaviour changed, rather than letting it change quietly.
     * {@link #dfs_came_from_is_the_dfs_tree()} below is its replacement and is disabled until
     * then; exactly one of the two should pass at any time.</p>
     */
    @Test
    void dfs_came_from_is_a_discovery_map_not_the_dfs_tree() {
        Map<Integer, Integer> cameFrom = tournament().dfs(0, new Random(42));
        assertEquals(5, cameFrom.size(), "every vertex but the source");
        assertEquals(Set.of(0), new HashSet<>(cameFrom.values()),
                "all five recorded as discovered from the source — a star, where the DFS tree is a path");
    }

    /**
     * DFS as it ought to behave: the real DFS tree. Disabled pending #146.
     *
     * <p>On a tournament the DFS tree is a Hamiltonian path — the search enters a vertex, goes as
     * deep as it can, and every vertex is reachable from every earlier one — so no vertex is the
     * parent of two others. Today the source is the parent of all five.</p>
     *
     * <p>Kept compiling rather than commented out, so that it survives renames and refactorings
     * and can be enabled in one line. It is also the clearest statement of what #146 asks for:
     * whoever takes that on can remove the annotation and watch it go green.</p>
     */
    @Test
    @Disabled("Pending #146: cameFrom records discovery rather than exploration")
    void dfs_came_from_is_the_dfs_tree() {
        Map<Integer, Integer> cameFrom = tournament().dfs(0, new Random(42));
        assertEquals(5, cameFrom.size(), "every vertex but the source");
        assertEquals(cameFrom.values().size(), new HashSet<>(cameFrom.values()).size(),
                "a DFS tree on a tournament is a path, so no vertex parents two others");
        assertTrue(cameFrom.values().contains(0), "the path still starts at the source");
    }
}
