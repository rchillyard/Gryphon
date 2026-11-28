/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.java;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.BinaryOperator;
import java.util.function.Function;

/**
 * Java façade for shortest-path algorithms on weighted directed graphs.
 *
 * <p>All methods operate on a {@link WeightedGraph}{@code <V, E>} whose edges
 * carry an attribute of type {@code E}.</p>
 *
 * <p>The result is a <em>shortest-path tree</em> (SPT): a
 * {@code Map<V, WeightedEdge<V, E>>} where each entry {@code v → edge} records
 * the cheapest incoming edge to {@code v}. The start vertex is absent.</p>
 *
 * <p><b>Note on Option 3:</b> Dijkstra accumulates path costs — the cost to
 * reach a vertex is the sum of edge weights along the path. This requires both
 * a {@code combine} function (e.g. addition) and a {@code zero} identity
 * element, unlike Prim and Kruskal which only compare individual edge weights
 * and need only a {@link Comparator}.</p>
 *
 * <p><b>Option 1 example</b> (Double weights):
 * <pre>{@code
 * WeightedGraph<String, Double> g = WeightedGraph.directedWeighted();
 * g.addEdge(new WeightedEdge<>("A", "B", 3.0));
 * g.addEdge(new WeightedEdge<>("A", "C", 1.0));
 * g.addEdge(new WeightedEdge<>("C", "B", 1.0));
 *
 * Map<String, WeightedEdge<String, Double>> spt = ShortestPaths.dijkstra(g, "A");
 * // spt.get("B").from()      == "C"   (via C, total cost 2.0)
 * // spt.get("B").attribute() == 1.0   (the C→B edge weight)
 * }</pre>
 *
 * <p><b>Option 3 example</b> (custom attribute type):
 * <pre>{@code
 * WeightedGraph<Building, TunnelProperties> g = WeightedGraph.directedWeighted();
 * g.addEdge(new WeightedEdge<>(b1, b2, props));
 *
 * Map<Building, WeightedEdge<Building, TunnelProperties>> spt =
 *     ShortestPaths.dijkstra(g, start,
 *         (a, b) -> new TunnelProperties(a.cost + b.cost, ...),  // combine
 *         TunnelProperties.ZERO,                                  // identity
 *         Comparator.comparingInt(tp -> tp.cost));                // ordering
 * }</pre>
 */
public class ShortestPaths {

    // -------------------------------------------------------------------------
    // Option 1 — Double weights, sensible defaults
    // -------------------------------------------------------------------------

    /**
     * Runs Dijkstra's algorithm on a graph whose edges carry {@code Double} weights.
     *
     * <p>Costs are combined by addition; edges are ordered by natural
     * {@code Double} ordering. No configuration required.</p>
     *
     * <p>The graph must be directed.</p>
     *
     * @param <V>   the vertex type.
     * @param graph a directed {@code WeightedGraph<V, Double>}.
     * @param start the source vertex.
     * @return the SPT as {@code Map<V, WeightedEdge<V, Double>>};
     *         the start vertex is absent.
     * @throws IllegalStateException if {@code graph} is undirected.
     */
    @SuppressWarnings("unchecked")
    public static <V> Map<V, WeightedEdge<V, Double>> dijkstra(
            WeightedGraph<V, Double> graph,
            V start) {
        return dijkstra(graph, start, new Random());
    }

    /**
     * Runs Dijkstra's algorithm, drawing its adjacency order from {@code random}.
     *
     * <p>The distances are the same whatever the order — they are what Dijkstra computes. What
     * can differ between runs is the <em>tree</em>: where two shortest paths to a vertex have
     * equal total weight, which one is recorded depends on the order neighbours are relaxed in.
     * Supply a generator to fix that choice.</p>
     *
     * @param <V>    the vertex type.
     * @param graph  a directed {@code WeightedGraph<V, Double>}.
     * @param start  the source vertex.
     * @param random the generator determining adjacency order.
     * @return the SPT as {@code Map<V, WeightedEdge<V, Double>>}; the start vertex is absent.
     * @throws IllegalStateException if {@code graph} is undirected.
     */
    @SuppressWarnings("unchecked")
    public static <V> Map<V, WeightedEdge<V, Double>> dijkstra(
            WeightedGraph<V, Double> graph,
            V start,
            Random random) {
        if (!graph.isDirected())
            throw new IllegalStateException(
                    "ShortestPaths.dijkstra requires a directed graph");
        return (Map<V, WeightedEdge<V, Double>>) (Map<?, ?>)
                JavaFacadeBridge$.MODULE$.dijkstraDouble(
                        graph.getScalaGraph(),
                        (java.util.List) graph.weightedEdges(),
                        start,
                        random);
    }

    // -------------------------------------------------------------------------
    // Option 3 — custom attribute type with combine, zero, and comparator
    // -------------------------------------------------------------------------

    /**
     * Runs Dijkstra's algorithm with a custom edge attribute type.
     *
     * <p>Because Dijkstra accumulates path costs, the caller must supply:</p>
     * <ul>
     *   <li>{@code combine} — how to add two costs (e.g. integer addition)</li>
     *   <li>{@code zero} — the identity element for {@code combine}
     *       (e.g. {@code 0} for addition)</li>
     *   <li>{@code comparator} — how to order costs</li>
     * </ul>
     *
     * <p>The graph must be directed.</p>
     *
     * @param <V>        the vertex type.
     * @param <E>        the edge attribute type.
     * @param graph      a directed {@code WeightedGraph<V, E>}.
     * @param start      the source vertex.
     * @param combine    accumulates two costs into one.
     * @param zero       the identity element for {@code combine}.
     * @param comparator orders costs.
     * @return the SPT as {@code Map<V, WeightedEdge<V, E>>};
     *         the start vertex is absent.
     * @throws IllegalStateException if {@code graph} is undirected.
     */
    public static <V, E> Map<V, WeightedEdge<V, E>> dijkstra(
            WeightedGraph<V, E> graph,
            V start,
            BinaryOperator<E> combine,
            E zero,
            Comparator<E> comparator) {
        return dijkstra(graph, start, combine, zero, comparator, new Random());
    }

    /**
     * Runs Dijkstra's algorithm with a custom edge attribute type, drawing its adjacency order
     * from {@code random}.
     *
     * <p>As with the {@code Double} form, the costs are the same whatever the order; what the
     * order settles is which of several equal-cost paths is recorded in the tree. Supply a
     * generator to make that choice reproducible.</p>
     *
     * <p>Pass the generator rather than a seed so that one generator can span several calls,
     * which is what {@code given Random} does on the Scala side.</p>
     *
     * @param <V>        the vertex type.
     * @param <E>        the edge attribute type.
     * @param graph      a directed {@code WeightedGraph<V, E>}.
     * @param start      the source vertex.
     * @param combine    accumulates two costs into one.
     * @param zero       the identity element for {@code combine}.
     * @param comparator orders costs.
     * @param random     the generator determining adjacency order.
     * @return the SPT as {@code Map<V, WeightedEdge<V, E>>}; the start vertex is absent.
     * @throws IllegalStateException if {@code graph} is undirected.
     */
    public static <V, E> Map<V, WeightedEdge<V, E>> dijkstra(
            WeightedGraph<V, E> graph,
            V start,
            BinaryOperator<E> combine,
            E zero,
            Comparator<E> comparator,
            Random random) {
        if (!graph.isDirected())
            throw new IllegalStateException(
                    "ShortestPaths.dijkstra requires a directed graph");
        return JavaFacadeBridge$.MODULE$.dijkstraCustom(
                graph.getScalaGraph(),
                graph.weightedEdges(),
                start, combine, zero, comparator, random);
    }

    // -------------------------------------------------------------------------
    // A* — one destination, guided by a heuristic
    // -------------------------------------------------------------------------

    /**
     * Runs A* from {@code start} to {@code goal} on a graph whose edges carry
     * {@code Double} weights.
     *
     * <p>Where {@link #dijkstra(WeightedGraph, Object)} answers "how far is everything from
     * {@code start}", this answers "how do I get to {@code goal}" — and uses {@code heuristic}
     * to avoid settling the rest of the graph on the way. On a 12x12 grid with an expensive wall:
     * 62 vertices settled with a Manhattan heuristic against 126 with none, for the same optimal
     * cost.</p>
     *
     * <p><b>The heuristic must be consistent</b>, not merely admissible: {@code h(v)} must be no
     * greater than {@code cost(v, w) + h(w)} for every edge {@code v → w}. The engine never
     * re-opens a settled vertex, which is correct for a consistent heuristic; with an
     * inconsistent one the path returned may be suboptimal and <em>nothing is thrown</em>.
     * Manhattan distance on a grid whose cheapest step is 1 is consistent; so is
     * {@code v -> 0.0}, which makes this Dijkstra.</p>
     *
     * <p>The map covers only the region A* settled, which is the reason to use it. It is not the
     * full single-source tree, so do not read an absent vertex as unreachable.</p>
     *
     * @param <V>       the vertex type.
     * @param graph     a directed {@code WeightedGraph<V, Double>}.
     * @param start     the source vertex.
     * @param goal      the target vertex.
     * @param heuristic the estimated remaining cost to {@code goal}; must be consistent.
     * @return the SPT over the settled region; the start vertex is absent.
     * @throws IllegalStateException if {@code graph} is undirected.
     */
    @SuppressWarnings("unchecked")
    public static <V> Map<V, WeightedEdge<V, Double>> aStar(
            WeightedGraph<V, Double> graph,
            V start,
            V goal,
            Function<V, Double> heuristic) {
        return aStar(graph, start, goal, heuristic, new Random());
    }

    /**
     * Runs A*, drawing its adjacency order from {@code random}.
     *
     * <p>The cost of the path to {@code goal} is the same whatever the order. What the order
     * settles is which of several equal-cost paths is recorded, and which other vertices end up
     * in the tree. Supply a generator to fix both.</p>
     *
     * @param <V>       the vertex type.
     * @param graph     a directed {@code WeightedGraph<V, Double>}.
     * @param start     the source vertex.
     * @param goal      the target vertex.
     * @param heuristic the estimated remaining cost to {@code goal}; must be consistent.
     * @param random    the generator determining adjacency order.
     * @return the SPT over the settled region; the start vertex is absent.
     * @throws IllegalStateException if {@code graph} is undirected.
     */
    @SuppressWarnings("unchecked")
    public static <V> Map<V, WeightedEdge<V, Double>> aStar(
            WeightedGraph<V, Double> graph,
            V start,
            V goal,
            Function<V, Double> heuristic,
            Random random) {
        if (!graph.isDirected())
            throw new IllegalStateException(
                    "ShortestPaths.aStar requires a directed graph");
        return (Map<V, WeightedEdge<V, Double>>) (Map<?, ?>)
                JavaFacadeBridge$.MODULE$.aStarDouble(
                        graph.getScalaGraph(),
                        (java.util.List) graph.weightedEdges(),
                        start,
                        goal,
                        heuristic,
                        random);
    }

    /**
     * Runs A* with a custom edge attribute type.
     *
     * <p>As with {@code dijkstra}, {@code combine} and {@code zero} are needed because path costs
     * accumulate. Here {@code combine} is also what adds {@code heuristic(v)} to the cost so far,
     * so it must agree with {@code comparator}.</p>
     *
     * <p>The consistency requirement on {@code heuristic} is as for the {@code Double} form, and
     * is easier to get wrong for a custom cost type: an inconsistent heuristic returns a
     * suboptimal path silently.</p>
     *
     * @param <V>        the vertex type.
     * @param <E>        the edge attribute type.
     * @param graph      a directed {@code WeightedGraph<V, E>}.
     * @param start      the source vertex.
     * @param goal       the target vertex.
     * @param heuristic  the estimated remaining cost to {@code goal}; must be consistent.
     * @param combine    accumulates two costs into one.
     * @param zero       the identity element for {@code combine}.
     * @param comparator orders costs.
     * @return the SPT over the settled region; the start vertex is absent.
     * @throws IllegalStateException if {@code graph} is undirected.
     */
    public static <V, E> Map<V, WeightedEdge<V, E>> aStar(
            WeightedGraph<V, E> graph,
            V start,
            V goal,
            Function<V, E> heuristic,
            BinaryOperator<E> combine,
            E zero,
            Comparator<E> comparator) {
        return aStar(graph, start, goal, heuristic, combine, zero, comparator, new Random());
    }

    /**
     * Runs A* with a custom edge attribute type, drawing its adjacency order from
     * {@code random}.
     *
     * @param <V>        the vertex type.
     * @param <E>        the edge attribute type.
     * @param graph      a directed {@code WeightedGraph<V, E>}.
     * @param start      the source vertex.
     * @param goal       the target vertex.
     * @param heuristic  the estimated remaining cost to {@code goal}; must be consistent.
     * @param combine    accumulates two costs into one.
     * @param zero       the identity element for {@code combine}.
     * @param comparator orders costs.
     * @param random     the generator determining adjacency order.
     * @return the SPT over the settled region; the start vertex is absent.
     * @throws IllegalStateException if {@code graph} is undirected.
     */
    public static <V, E> Map<V, WeightedEdge<V, E>> aStar(
            WeightedGraph<V, E> graph,
            V start,
            V goal,
            Function<V, E> heuristic,
            BinaryOperator<E> combine,
            E zero,
            Comparator<E> comparator,
            Random random) {
        if (!graph.isDirected())
            throw new IllegalStateException(
                    "ShortestPaths.aStar requires a directed graph");
        return JavaFacadeBridge$.MODULE$.aStarCustom(
                graph.getScalaGraph(),
                graph.weightedEdges(),
                start, goal, heuristic, combine, zero, comparator, random);
    }

    // -------------------------------------------------------------------------
    // Bellman-Ford — negative weights allowed
    // -------------------------------------------------------------------------

    /**
     * Runs the Bellman-Ford algorithm on a graph whose edges carry
     * {@code Double} weights.
     *
     * <p>Unlike {@link #dijkstra(WeightedGraph, Object)}, Bellman-Ford accepts
     * negative edge weights. It cannot give an answer when a negative cycle is
     * reachable from {@code start} — every path round such a cycle is cheaper
     * than the last, so no shortest path exists — and reports that by returning
     * an empty {@code Optional}.</p>
     *
     * <p>The graph must be directed.</p>
     *
     * <p>The distances this computes are invariant, but which of several equal-cost
     * predecessors is recorded is not: relaxation reads each vertex's adjacencies in a
     * shuffled order. This form builds a fresh generator, so repeated calls on a graph with
     * ties may differ; {@link #bellmanFord(WeightedGraph, Object, Random)} takes yours.</p>
     *
     * @param <V>   the vertex type.
     * @param graph a directed {@code WeightedGraph<V, Double>}.
     * @param start the source vertex.
     * @return the SPT as {@code Map<V, WeightedEdge<V, Double>>} with the start
     *         vertex absent, or an empty {@code Optional} if a negative cycle is
     *         reachable from {@code start}.
     * @throws IllegalStateException if {@code graph} is undirected.
     */
    public static <V> Optional<Map<V, WeightedEdge<V, Double>>> bellmanFord(
            WeightedGraph<V, Double> graph,
            V start) {
        return bellmanFord(graph, start, new Random());
    }

    /**
     * Runs the Bellman-Ford algorithm on {@code Double} weights, drawing its adjacency order
     * from {@code random}.
     *
     * <p>The costs are the same whatever the order. What the order settles is which of several
     * equal-cost paths is recorded, exactly as for {@code dijkstra} — the claim in earlier
     * releases that relaxation-based algorithms were immune to adjacency order was wrong, and
     * this overload is the correction.</p>
     *
     * <p>Pass the generator rather than a seed so that one generator can span several calls,
     * which is what {@code given Random} does on the Scala side.</p>
     *
     * @param <V>    the vertex type.
     * @param graph  a directed {@code WeightedGraph<V, Double>}.
     * @param start  the source vertex.
     * @param random the generator determining adjacency order.
     * @return the SPT as {@code Map<V, WeightedEdge<V, Double>>} with the start
     *         vertex absent, or an empty {@code Optional} if a negative cycle is
     *         reachable from {@code start}.
     * @throws IllegalStateException if {@code graph} is undirected.
     */
    @SuppressWarnings("unchecked")
    public static <V> Optional<Map<V, WeightedEdge<V, Double>>> bellmanFord(
            WeightedGraph<V, Double> graph,
            V start,
            Random random) {
        requireDirected(graph, "bellmanFord");
        return (Optional<Map<V, WeightedEdge<V, Double>>>) (Optional<?>)
                JavaFacadeBridge$.MODULE$.bellmanFordDouble(
                        (java.util.List) graph.weightedEdges(), start, random);
    }

    /**
     * Runs the Bellman-Ford algorithm with a custom edge attribute type.
     *
     * <p>Like Dijkstra, Bellman-Ford accumulates path costs, so {@code combine}
     * and {@code zero} are required as well as {@code comparator}.</p>
     *
     * @param <V>        the vertex type.
     * @param <E>        the edge attribute type.
     * @param graph      a directed {@code WeightedGraph<V, E>}.
     * @param start      the source vertex.
     * @param combine    accumulates two costs into one.
     * @param zero       the identity element for {@code combine}.
     * @param comparator orders costs.
     * @return the SPT, or an empty {@code Optional} if a negative cycle is
     *         reachable from {@code start}.
     * @throws IllegalStateException if {@code graph} is undirected.
     */
    public static <V, E> Optional<Map<V, WeightedEdge<V, E>>> bellmanFord(
            WeightedGraph<V, E> graph,
            V start,
            BinaryOperator<E> combine,
            E zero,
            Comparator<E> comparator) {
        return bellmanFord(graph, start, combine, zero, comparator, new Random());
    }

    /**
     * Runs the Bellman-Ford algorithm with a custom edge attribute type, drawing its adjacency
     * order from {@code random}.
     *
     * <p>As with the {@code Double} form, the costs are the same whatever the order; what the
     * order settles is which of several equal-cost paths is recorded.</p>
     *
     * @param <V>        the vertex type.
     * @param <E>        the edge attribute type.
     * @param graph      a directed {@code WeightedGraph<V, E>}.
     * @param start      the source vertex.
     * @param combine    accumulates two costs into one.
     * @param zero       the identity element for {@code combine}.
     * @param comparator orders costs.
     * @param random     the generator determining adjacency order.
     * @return the SPT, or an empty {@code Optional} if a negative cycle is
     *         reachable from {@code start}.
     * @throws IllegalStateException if {@code graph} is undirected.
     */
    public static <V, E> Optional<Map<V, WeightedEdge<V, E>>> bellmanFord(
            WeightedGraph<V, E> graph,
            V start,
            BinaryOperator<E> combine,
            E zero,
            Comparator<E> comparator,
            Random random) {
        requireDirected(graph, "bellmanFord");
        return JavaFacadeBridge$.MODULE$.bellmanFordCustom(
                graph.weightedEdges(), start, combine, zero, comparator, random);
    }

    // -------------------------------------------------------------------------
    // Acyclic shortest paths — DAGs, in linear time
    // -------------------------------------------------------------------------

    /**
     * Runs the acyclic shortest-paths algorithm on a directed acyclic graph
     * whose edges carry {@code Double} weights.
     *
     * <p>Relaxing edges in topological order visits each edge once, so this is
     * linear in the size of the graph — faster than both Dijkstra and
     * Bellman-Ford. Negative weights are allowed, since a DAG has no cycles for
     * them to make cheaper.</p>
     *
     * <p>The graph must be directed and acyclic. Test with
     * {@link TopologicalSort#isDAG(Graph)} if you are not sure.</p>
     *
     * <p><b>Reproducibility — read this before comparing results between runs.</b></p>
     *
     * <p><b>The costs are invariant. Which of several equal-cost paths is reported is not, and
     * there is no way to pin it.</b> Unlike {@code dijkstra}, {@code prim}, {@code bfs} and
     * {@code bellmanFord}, this method has no {@code java.util.Random} overload — not because
     * it does not vary, but because a generator could not settle it. It is the only method in
     * this façade in that position.</p>
     *
     * <p>What is stable: everything, for one graph object. The relaxation order comes from the
     * graph's memoized topological order, computed on first use and reused thereafter, so
     * repeated calls on the same {@code graph} agree.</p>
     *
     * <p>What is not: a rebuilt or re-parsed graph is a different object, and may memoize a
     * different valid topological order and so report a different (equally short) path. Across
     * runs of your program it is arbitrary. <b>If you are asserting a particular predecessor in
     * a test, hold one graph object; otherwise assert on the costs.</b></p>
     *
     * <p><b>Both of those can now be pinned, with two handles.</b> Pass a topological order to
     * {@link #acyclic(WeightedGraph, Object, java.util.List)} and the vertex order is yours;
     * pass a {@code Random} and the order a vertex's outgoing edges are relaxed in is yours
     * too. The second was believed not to matter — two edges can compete only as parallel edges
     * to one target at equal cost, and the builder was thought to reject such a graph. It does
     * not: {@code fromTriplets} builds parallel edges, and with the topological order held fixed
     * the recorded predecessor still varied two ways over two hundred calls (#184).</p>
     *
     * @param <V>   the vertex type.
     * @param graph a directed acyclic {@code WeightedGraph<V, Double>}.
     * @param start the source vertex.
     * @return the SPT as {@code Map<V, WeightedEdge<V, Double>>};
     *         the start vertex is absent.
     * @throws IllegalStateException    if {@code graph} is undirected.
     * @throws IllegalArgumentException if {@code graph} contains a cycle.
     */
    @SuppressWarnings("unchecked")
    public static <V> Map<V, WeightedEdge<V, Double>> acyclic(
            WeightedGraph<V, Double> graph,
            V start) {
        requireDirected(graph, "acyclic");
        return (Map<V, WeightedEdge<V, Double>>) (Map<?, ?>)
                JavaFacadeBridge$.MODULE$.acyclicShortestPathsDouble(
                        (java.util.List) graph.weightedEdges(), start);
    }

    /**
     * Runs the acyclic shortest-paths algorithm with a custom edge attribute type.
     *
     * <p>The reproducibility note on {@link #acyclic(WeightedGraph, Object)} applies here
     * unchanged: the costs are invariant, which of several equal-cost paths is reported is
     * stable for one graph object and arbitrary across them, and there is no {@code Random}
     * overload because one could not settle it.</p>
     *
     * @param <V>        the vertex type.
     * @param <E>        the edge attribute type.
     * @param graph      a directed acyclic {@code WeightedGraph<V, E>}.
     * @param start      the source vertex.
     * @param combine    accumulates two costs into one.
     * @param zero       the identity element for {@code combine}.
     * @param comparator orders costs.
     * @return the SPT; the start vertex is absent.
     * @throws IllegalStateException    if {@code graph} is undirected.
     * @throws IllegalArgumentException if {@code graph} contains a cycle.
     */
    public static <V, E> Map<V, WeightedEdge<V, E>> acyclic(
            WeightedGraph<V, E> graph,
            V start,
            BinaryOperator<E> combine,
            E zero,
            Comparator<E> comparator) {
        requireDirected(graph, "acyclic");
        return JavaFacadeBridge$.MODULE$.acyclicShortestPathsCustom(
                graph.weightedEdges(), start, combine, zero, comparator);
    }

    /**
     * Runs the acyclic shortest-paths algorithm, relaxing in the order you supply.
     *
     * <p><b>This is the form you can make reproducible.</b> The other {@code acyclic} methods
     * relax in the graph's own cached topological order, which nothing can seed — so their
     * answer is stable for one graph object and arbitrary across them. Here the order is yours,
     * so the result becomes a function of arguments you control:</p>
     *
     * <pre>{@code
     * List<V> order = TopologicalSort.sort(g).orElseThrow();
     * Map<V, WeightedEdge<V, Double>> spt = ShortestPaths.acyclic(g, start, order);
     * // that same order, later or on a rebuilt graph, gives that same tree
     * }</pre>
     *
     * <p><b>One gap, for Java callers specifically.</b> {@link TopologicalSort#sort(Graph)} is
     * itself unseeded — it reads the graph's memoized order rather than running the algorithm
     * (#139) — so to pin a result across runs you must keep the order you were given rather than
     * recompute it. Scala has a seeded topological sort and can therefore generate a fresh
     * reproducible order; the Java façade does not yet.</p>
     *
     * <p>The order is what decides which of several equal-cost paths is recorded, so the order is
     * the parameter. There is still no {@code Random} overload here, and there should not be: a
     * generator could not reach that decision, so it would read as a guarantee it could not keep.</p>
     *
     * @param <V>   the vertex type.
     * @param graph a directed acyclic {@code WeightedGraph<V, Double>}.
     * @param start the source vertex.
     * @param order a topological order of {@code graph}, which is validated.
     * @return the SPT; the start vertex is absent.
     * @throws IllegalStateException    if {@code graph} is undirected.
     * @throws IllegalArgumentException if {@code order} is not a topological order of
     *                                  {@code graph} — it must list every vertex exactly once,
     *                                  with every edge running forwards. An invalid order would
     *                                  otherwise give a wrong answer silently.
     */
    @SuppressWarnings("unchecked")
    public static <V> Map<V, WeightedEdge<V, Double>> acyclic(
            WeightedGraph<V, Double> graph,
            V start,
            java.util.List<V> order) {
        requireDirected(graph, "acyclic");
        return (Map<V, WeightedEdge<V, Double>>) (Map<?, ?>)
                JavaFacadeBridge$.MODULE$.acyclicShortestPathsDouble(
                        (java.util.List) graph.weightedEdges(), start, order);
    }

    /**
     * Runs the acyclic shortest-paths algorithm with a custom edge attribute type, relaxing in
     * the order you supply.
     *
     * <p>The reproducible form, exactly as for {@link #acyclic(WeightedGraph, Object, java.util.List)}.</p>
     *
     * @param <V>        the vertex type.
     * @param <E>        the edge attribute type.
     * @param graph      a directed acyclic {@code WeightedGraph<V, E>}.
     * @param start      the source vertex.
     * @param order      a topological order of {@code graph}, which is validated.
     * @param combine    accumulates two costs into one.
     * @param zero       the identity element for {@code combine}.
     * @param comparator orders costs.
     * @return the SPT; the start vertex is absent.
     * @throws IllegalStateException    if {@code graph} is undirected.
     * @throws IllegalArgumentException if {@code order} is not a topological order of
     *                                  {@code graph}.
     */
    public static <V, E> Map<V, WeightedEdge<V, E>> acyclic(
            WeightedGraph<V, E> graph,
            V start,
            java.util.List<V> order,
            BinaryOperator<E> combine,
            E zero,
            Comparator<E> comparator) {
        requireDirected(graph, "acyclic");
        return JavaFacadeBridge$.MODULE$.acyclicShortestPathsCustom(
                graph.weightedEdges(), start, order, combine, zero, comparator);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static void requireDirected(WeightedGraph<?, ?> graph, String method) {
        if (!graph.isDirected())
            throw new IllegalStateException(
                    "ShortestPaths." + method + " requires a directed graph");
    }

    // Not instantiable
    private ShortestPaths() {
    }
}