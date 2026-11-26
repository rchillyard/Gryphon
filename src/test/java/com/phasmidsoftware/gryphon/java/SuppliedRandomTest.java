package com.phasmidsoftware.gryphon.java;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Issue #142: Java callers can supply the generator that fixes adjacency order.
 *
 * <p>Until 1.13.0 the facade pinned {@code Random(0)} internally. That made it reproducible by
 * telling Java users something false — that {@code dfs} and {@code bfs} return a particular
 * order. They do not, and the Scala API has never claimed they do. These methods now take a
 * {@code java.util.Random}, the Java-shaped equivalent of {@code given Random} in Scala.</p>
 *
 * <p><b>On fixtures.</b> Every test here runs through {@link #assertSeedControls}, which asserts
 * two things at once: that one seed reproduces, and that different seeds do not all agree. The
 * second is not decoration. Three plausible fixtures were tried first and all three made the
 * reproducibility assertion vacuous, each for a different reason:</p>
 * <ul>
 *   <li>a four-vertex diamond — its traversal is forced by structure (see
 *       {@link TopologicalSortTest});</li>
 *   <li>a star {@code 0 -> 1..8} — a came-from map records each vertex's parent, and in a star
 *       every leaf's parent is the source whatever the order;</li>
 *   <li>a transitive tournament — came-from is recorded at <em>discovery</em>, and when the
 *       source is adjacent to every vertex, every vertex is discovered from the source.</li>
 * </ul>
 * <p>Without the variation half, all three would have reported success while testing nothing.</p>
 */
public class SuppliedRandomTest {

    /**
     * Asserts that the supplied generator, and only the supplied generator, settles the result.
     *
     * @param f   produces a result from a generator.
     * @param <T> the result type.
     */
    private static <T> void assertSeedControls(Function<Random, T> f) {
        assertEquals(f.apply(new Random(42)), f.apply(new Random(42)),
                "one seed did not reproduce: the generator is not being threaded through");
        Set<T> seen = new HashSet<>();
        for (int seed = 0; seed < 40; seed++) seen.add(f.apply(new Random(seed)));
        assertTrue(seen.size() > 1,
                "forty seeds gave one result: this fixture cannot detect the generator, "
                        + "so the reproducibility assertion above proves nothing");
    }

    /**
     * Layered, with nothing above the first layer adjacent to the source, so a vertex can be
     * discovered from either of two parents depending on order. See the class comment for why
     * the obvious alternatives do not work.
     */
    private static Graph<Integer> layered() {
        Graph<Integer> g = Graph.directed();
        g.addEdge(0, 1);
        g.addEdge(0, 2);
        for (int p : new int[]{1, 2}) for (int c : new int[]{3, 4}) g.addEdge(p, c);
        for (int p : new int[]{3, 4}) for (int c : new int[]{5, 6}) g.addEdge(p, c);
        return g;
    }

    private static WeightedGraph<Integer, Double> tiedWeights() {
        // Every edge weighs the same, so every spanning tree is minimal and which one Prim
        // returns is settled entirely by adjacency order. With distinct weights the MST is
        // unique and the generator could not show through.
        WeightedGraph<Integer, Double> g = WeightedGraph.undirectedWeighted();
        g.addEdge(new WeightedEdge<>(0, 1, 1.0));
        g.addEdge(new WeightedEdge<>(0, 2, 1.0));
        g.addEdge(new WeightedEdge<>(0, 3, 1.0));
        g.addEdge(new WeightedEdge<>(1, 2, 1.0));
        g.addEdge(new WeightedEdge<>(2, 3, 1.0));
        g.addEdge(new WeightedEdge<>(1, 3, 1.0));
        return g;
    }

    private static Graph<Integer> threeComponents() {
        Graph<Integer> g = Graph.undirected();
        g.addEdge(0, 1);
        g.addEdge(2, 3);
        g.addEdge(4, 5);
        return g;
    }

    private static Graph<Integer> twoSccs() {
        Graph<Integer> g = Graph.directed();
        g.addEdge(0, 1);
        g.addEdge(1, 0);
        g.addEdge(1, 2);
        g.addEdge(2, 3);
        g.addEdge(3, 2);
        return g;
    }

    @Test
    void dfs_is_settled_by_the_supplied_generator() {
        Graph<Integer> g = layered();
        assertSeedControls(r -> g.dfs(0, r));
    }

    @Test
    void bfs_is_settled_by_the_supplied_generator() {
        Graph<Integer> g = layered();
        assertSeedControls(r -> g.bfs(0, r));
    }

    @Test
    void dfsAll_is_settled_by_the_supplied_generator() {
        Graph<Integer> g = layered();
        assertSeedControls(g::dfsAll);
    }

    @Test
    void bfsUntil_is_settled_by_the_supplied_generator() {
        Graph<Integer> g = layered();
        assertSeedControls(r -> g.bfsUntil(0, v -> v == 6, r));
    }

    @Test
    void componentIds_does_not_vary_at_all() {
        // Not an assertSeedControls case, and deliberately so: there is no generator-taking
        // overload, because ConnectedComponents.components is annotated `@unused` and ignores
        // the one it is given. Component ids come from walking the vertex set, which is not
        // shuffled. This pins that -- if it ever starts varying, the facade owes callers a lever
        // and this test is where that shows up. See #143.
        Graph<Integer> g = threeComponents();
        Set<Map<Integer, Integer>> seen = new HashSet<>();
        for (int i = 0; i < 40; i++) seen.add(ConnectedComponents.componentIds(g));
        assertEquals(1, seen.size(),
                "componentIds began varying: it now needs a Random overload, see #143");
    }

    @Test
    void kosaraju_repeats_for_one_generator() {
        // Weaker than assertSeedControls, honestly so. Kosaraju threads its generator -- its
        // signature takes one and does not mark it unused -- but no fixture was found where the
        // SCC ids actually differ between seeds, so the variation half cannot be asserted. This
        // checks only that supplying a generator is not actively harmful.
        Graph<Integer> g = twoSccs();
        assertEquals(StronglyConnectedComponents.kosaraju(g, new Random(42)),
                StronglyConnectedComponents.kosaraju(g, new Random(42)));
    }

    @Test
    void prim_repeats_for_one_generator() {
        // As for Kosaraju. Even with every edge weight tied -- where several spanning trees are
        // equally minimal and adjacency order ought to decide between them -- forty seeds gave
        // one tree. The generator is threaded; its effect could not be demonstrated.
        WeightedGraph<Integer, Double> g = tiedWeights();
        assertEquals(MinimumSpanningTree.prim(g, 0, new Random(42)),
                MinimumSpanningTree.prim(g, 0, new Random(42)));
    }

    @Test
    void one_generator_spans_several_calls() {
        // The reason the parameter is a Random and not a long seed. A seed could only make each
        // call individually reproducible; a shared generator makes a whole *sequence* of calls
        // reproducible, which is what `given Random` does in Scala. The second call in each
        // sequence draws from a generator the first has already advanced, so this would fail if
        // the facade quietly re-seeded per call.
        Graph<Integer> g = layered();

        Random a = new Random(7);
        List<Map<Integer, Integer>> first = List.of(g.dfs(0, a), g.dfs(0, a), g.dfs(0, a));

        Random b = new Random(7);
        List<Map<Integer, Integer>> second = List.of(g.dfs(0, b), g.dfs(0, b), g.dfs(0, b));

        assertEquals(first, second);
    }

    @Test
    void the_no_generator_form_does_not_promise_an_order() {
        // The companion to all of the above, and the point of #142: without a generator the
        // facade makes no promise. Asserted as "not pinned to one value across forty calls",
        // which is what the old internal Random(0) would have failed.
        Graph<Integer> g = layered();
        Set<Map<Integer, Integer>> seen = new HashSet<>();
        for (int i = 0; i < 40; i++) seen.add(g.dfs(0));
        assertTrue(seen.size() > 1,
                "forty unseeded calls gave one order: the facade is pinning a seed again");
    }
}
