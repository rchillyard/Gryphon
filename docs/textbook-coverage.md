# Gryphon against the textbooks

Issue [#72](https://github.com/rchillyard/Gryphon/issues/72). A systematic check of what Gryphon
implements against the two standard references, so that what is missing is a recorded decision
rather than an oversight.

The primary comparison is **Sedgewick & Wayne, *Algorithms*, 4th edition** — chapter 4, Graphs,
with union-find from §1.5. It is the common text, and Gryphon's own fixtures already cite it by
figure number. **CLRS, *Introduction to Algorithms***, is checked second: it covers ground
chapter 4 does not, so it finds different gaps. Chapter numbers for CLRS differ between the 3rd
and 4th editions (graphs are chapters 22–26 in the 3rd, 20–25 in the 4th), so algorithms are
named here rather than numbered.

Audited against `main` on 2026-11-25, at Gryphon 1.12.0. The **algorithms** of both books were
checked for [#72](https://github.com/rchillyard/Gryphon/issues/72); the **properties** and
**challenges**, most of which are exercise material rather than main text, were added for
[#115](https://github.com/rchillyard/Gryphon/issues/115).

Legend: **Y** implemented · **P** partial, see the note · **N** not implemented.

---

## Sedgewick & Wayne §1.5 — Union-find

| S&W | Gryphon | |
|---|---|---|
| `QuickFindUF` | — | N — superseded; see note 1 |
| `QuickUnionUF` | `ConnectivityASP` | Y |
| `WeightedQuickUnionUF` | `ConnectivityLazy` | Y |
| Path compression | `ConnectivityOptimized` | Y |

1. Quick-find is in the book as the starting point of an argument that ends at weighted quick
   union. Gryphon starts where the argument ends. `ConnectivityASP` is kept deliberately, and
   named for the Arbitrary Substitution Principle it violates, as the contrast.

## Sedgewick & Wayne §4.1 — Undirected graphs

| S&W | Gryphon | |
|---|---|---|
| `Graph` (adjacency lists) | `UndirectedGraph`, `VertexMap` | Y |
| `DepthFirstSearch` (reachability) | `Traversable.dfs`, `dfsAll` | Y |
| `DepthFirstPaths` (`pathTo`) | `getConnexions` → `Connexions` | P — note 2 |
| `BreadthFirstPaths` (`pathTo`) | `Traversable.bfs`, `getConnexions` | P — note 2 |
| `CC` (connected components) | `ConnectedComponents.components` | Y |
| `Cycle` | `UndirectedGraph.isCyclic` | Y |
| `TwoColor` (bipartite) | `UndirectedGraph.isBipartite` | Y |
| `SymbolGraph` | `GraphParser[String, …]` | Y — note 3 |
| `DegreesOfSeparation` | — | N — a client, not an algorithm |

2. **The parent map is there; the path extraction is not.** `Connexions` holds
   `Map[V, DirectedEdge[V, E]]`, which is exactly the `edgeTo[]` array the book's `pathTo` walks
   back along, so a caller can reconstruct any path — but has to write the walk. A `pathTo`
   convenience is the obvious small addition, and [#76](https://github.com/rchillyard/Gryphon/issues/76)
   already asks whether `Connexions` should present itself as a rooted tree.
3. `GraphParser` is generic in the vertex type through `Parseable[V]`, so
   `GraphParser[String, Double, EdgeType]` reads a graph whose vertices are names. That is what
   `SymbolGraph` is for. The `tunnels.graph` fixture does it for real: 80 buildings keyed by
   building code.

## Sedgewick & Wayne §4.2 — Directed graphs

| S&W | Gryphon | |
|---|---|---|
| `Digraph` | `DirectedGraph` | Y |
| `Digraph.reverse` | `DirectedGraph.reverse` | Y |
| `DirectedDFS` (multi-source reachability) | `dfs` from one source | P — note 4 |
| `DirectedCycle` | `DirectedGraph.isCyclic` | Y — note 5 |
| `DepthFirstOrder` | `DfsOrder` (Visitor), `Kosaraju` uses it | Y |
| `Topological` | `TopologicalSort.sort` | Y |
| `KosarajuSCC` | `Kosaraju.stronglyConnectedComponents` | Y |
| `TransitiveClosure` | — | N — note 6 |

4. Single-source reachability is there; the book's constructor taking an `Iterable<Integer>` of
   sources is not. It is a fold of the single-source case over the set, sharing one visited set.
5. `isCyclic` is `TopologicalSort.sort(this).isEmpty` — correct, since a digraph has a
   topological order exactly when it is acyclic, but it reports *whether*, not *which*. The book's
   `DirectedCycle` exposes the cycle. Same shape as the Bellman–Ford gap below.
6. `TransitiveClosure` in the book is DFS from every vertex, and is presented with the warning
   that it costs *V*(*V*+*E*) space and is impractical at scale. Not implemented, and not
   obviously worth it.

## Sedgewick & Wayne §4.3 — Minimum spanning trees

| S&W | Gryphon | |
|---|---|---|
| `EdgeWeightedGraph`, `Edge` | `UndirectedGraph[V, E]`, `UndirectedEdge` | Y |
| `LazyPrimMST` | — | N — note 7 |
| `PrimMST` (eager) | `MST.prim`, `PrimTraversal` | Y |
| `KruskalMST` | `Kruskal.mst` | Y |
| Borůvka (exercise 4.3.43) | `Boruvka.mst` | Y |

7. Gryphon implements the eager version only, which is the one the book recommends. The lazy
   version exists in the text as the simpler first draft.

## Sedgewick & Wayne §4.4 — Shortest paths

| S&W | Gryphon | |
|---|---|---|
| `EdgeWeightedDigraph`, `DirectedEdge` | `DirectedGraph[V, E]`, `DirectedEdge` | Y |
| `DijkstraSP` | `ShortestPaths.dijkstra`, `DijkstraTraversal` | Y |
| `DijkstraAllPairsSP` | — | N — note 8 |
| `AcyclicSP` | `AcyclicShortestPaths.shortestPaths` | Y |
| `AcyclicLP` (longest path) | — | N — note 9 |
| `CPM` (critical path method) | — | N — note 9 |
| `BellmanFordSP` | `BellmanFord.shortestPaths` | Y |
| — negative cycle **detection** | returns `None` | Y |
| — negative cycle **reporting** | — | N — note 10 |
| `EdgeWeightedDirectedCycle` | `DirectedGraph.isCyclic` | P — note 5 |

8. All-pairs is *V* runs of Dijkstra in the book — a client over the single-source algorithm
   Gryphon has, not a new algorithm. See the CLRS section, which treats all-pairs as a subject in
   its own right and is where the real gap shows.
9. The book describes longest path as shortest path with the weights negated. **That cannot be
   done here**, and an earlier draft of this document wrongly said it could: `E` carries only
   `Monoid` and `Ordering`, and neither offers negation. Demanding a `Group[E]` or `Numeric[E]`
   would narrow the API for every caller to serve one method. It is also unnecessary.
   `AcyclicShortestPaths` uses its two type classes in exactly two places — `Monoid` to
   accumulate, `Ordering` only to decide whether a candidate beats the incumbent — so longest
   path is the same body with `Ordering[E].reverse`. See
   [#114](https://github.com/rchillyard/Gryphon/issues/114). CPM is then a client of it.
10. **Confirmed by reading the code.** `BellmanFord.shortestPaths` returns
    `Option[VertexTraversalResult[…]]`, and `None` means a negative cycle reachable from the
    source was proved — by a vertex being enqueued a `vertexCount`-th time. The cycle itself is
    not returned, so a caller learns that their problem is ill-posed but not where. This is the
    specific question #72 asked, and the answer is: detection yes, reporting no.

---

## CLRS — the same check against the other book

Everything CLRS shares with S&W is covered above. What follows is what CLRS has that chapter 4
does not.

| CLRS | Gryphon | |
|---|---|---|
| BFS, DFS, topological sort, SCC | as above | Y |
| Kruskal, Prim | as above | Y |
| Bellman–Ford, DAG shortest paths, Dijkstra | as above | Y |
| Difference constraints via Bellman–Ford | — | N — a client of Bellman–Ford |
| **All-pairs shortest paths** | | |
| — matrix-multiplication method | — | N |
| — Floyd–Warshall | — | N — note 11 |
| — transitive closure (boolean Floyd–Warshall) | — | N |
| — Johnson, for sparse graphs | — | N |
| **Maximum flow** | | |
| — Ford–Fulkerson | — | N — note 12 |
| — Edmonds–Karp | — | N |
| — maximum bipartite matching | — | N — note 13 |

11. **Floyd–Warshall is the most substantial single gap in either book.** It is not reducible to
    anything Gryphon has: *V* runs of Dijkstra is a different algorithm with different
    preconditions, and Floyd–Warshall handles negative edges, which Dijkstra does not. It also
    wants a different representation — a *V*×*V* matrix rather than adjacency lists — so it is
    real work rather than a wrapper.
12. Max flow is chapter 6 of S&W, not chapter 4, so it is out of scope for the primary
    comparison and in scope for CLRS. Gryphon has no flow networks: no residual capacities, no
    augmenting paths. The largest gap by volume, and the one furthest from the existing design.
13. Gryphon can *detect* bipartiteness (`isBipartite`) but cannot find a maximum matching, which
    CLRS obtains from max flow.

---

## Graph properties

The tables above follow the chapters' **algorithms**. Most of what follows lives in the
**exercises** — `GraphProperties` (eccentricity, diameter, radius, center) is exercise material in
§4.1, as are Euler and Hamilton paths. Added for
[#115](https://github.com/rchillyard/Gryphon/issues/115).

| Property | Gryphon | |
|---|---|---|
| `adjacencies(v)` | `adjacentVertices`, `filteredAdjacencies` | Y |
| `degree(v)` | `UndirectedGraph.degree`; `EdgeGraph.inDegree`/`outDegree` | Y |
| `maxDegree` | `UndirectedGraph.maxDegree` | Y |
| `meanDegree` | `UndirectedGraph.meanDegree` | Y |
| Self-loop count | `EdgeGraph.numberOfSelfLoops` | Y |
| Adjacency lists as a String | `VertexMap.toString`, `.debug`, `Vertex.toString` | Y |
| Paths from a vertex, DFS and BFS, any graph type | parent map only | P — note 14 |
| `isDAG` | Java façade only | P — note 15 |
| Cycle detection, directed | `DirectedGraph.isCyclic`, via topological sort | Y — note 5 |
| Cycle detection, undirected | `UndirectedGraph.isCyclic` | Y |
| Two-colourability | `UndirectedGraph.isBipartite` | Y |
| Eccentricity | — | N — note 16 |
| Diameter | — | N — note 16 |
| Radius | — | N — note 16 |
| Center | — | N — note 16 |
| Biconnectedness, articulation points, bridges | — | N — note 17 |
| Transitive closure | — | N — see note 6 |
| Lowest common ancestor | — | N — note 18 |
| 2-satisfiability | — | N — note 19 |
| Planarity | — | N — note 20 |

14. The traversal records a parent map and nothing extracts a path from it.
    [#112](https://github.com/rchillyard/Gryphon/issues/112). Note the question asks for *any
    graph type, both DFS and BFS*: `getConnexions` is on `Traversable`, so it is already
    graph-agnostic, and the DFS/BFS distinction is a choice of frontier rather than a second
    implementation. So one `pathTo` answers all four combinations.
15. **An asymmetry between the two APIs.** The Java façade has `TopologicalSort.isDAG(graph)`;
    Scala has only `DirectedGraph.isCyclic`, and a Scala caller writes `!graph.isCyclic`. They
    are the same predicate under two names, and the Java one reads better for the question
    usually being asked. Worth deciding whether Scala should gain `isDAG` or Java should lose it;
    either way they should agree.
16. **These four are one piece of work, not four.** Each is a function of the
    eccentricity of every vertex — diameter is the maximum, radius the minimum, center the
    argmin — and eccentricity is the greatest BFS distance from a vertex. So all four fall out of
    *V* breadth-first searches, or out of all-pairs distances if Floyd–Warshall ever arrives.
    What Gryphon lacks is not the traversal but **distance**: `bfs` visits in distance order and
    does not report the distance. That, rather than the four properties, is the thing to add
    first. Note also that all four are defined only on a connected graph, so each needs a
    decision about what to answer otherwise — the same question `isConnected`'s empty-graph
    convention raised in #82.
17. The DFS-based algorithms (Hopcroft–Tarjan) for articulation points and bridges need the
    discovery-time and low-link bookkeeping that Kosaraju does not use, so there is nothing here
    to build on. The search for "bridge" in `src/main` finds only `JavaFacadeBridge`.
18. LCA is cheap on a tree and a different problem on a DAG. Gryphon has no tree type —
    `Connexions` is the nearest thing, being a rooted tree expressed as a parent map, which
    [#76](https://github.com/rchillyard/Gryphon/issues/76) is about — so the first question is
    what LCA would be *on*.
19. **Closer than it looks.** 2-SAT is solved by building the implication graph and checking that
    no variable shares a strongly connected component with its negation. Gryphon has the hard
    half: `Kosaraju.stronglyConnectedComponents`. What is missing is the clause-to-graph
    construction and the check, neither of which is graph machinery. It is a *client* of what is
    already here, which makes it the most tractable of the absent properties.
20. Planarity testing is a substantial algorithm in its own right. Genuinely out of scope unless
    it is wanted for its own sake.

## Challenges

| Challenge | Gryphon | |
|---|---|---|
| Euler path | — | N — note 21 |
| Hamilton path | — | N — note 22 |
| Prim variants | eager, with an indexed priority queue | P — note 23 |
| Symbol graph | `GraphParser[String, …]`, `tunnels.graph` | Y |
| Degrees of separation | — | N — note 24 |
| Minimum spanning forest | Kruskal yes; Borůvka **hung**, fixed in #117 | Y — note 25 |

21. An Eulerian path exists under a degree condition Gryphon can already evaluate —
    `degree`, `inDegree` and `outDegree` are all there — so *deciding* existence is a short
    client. *Constructing* one (Hierholzer) is the actual work.
22. Hamilton path is **NP-complete**, so its absence is not a gap in the sense the rest of this
    table means. If it is wanted it is wanted as an exponential search or a heuristic, and that
    is a different kind of decision.
23. **Partly a Visitor question, not a Gryphon one.** Gryphon implements the eager Prim, and it
    already uses Visitor's `IndexedPrioQueue` with `decreaseKey` through `CostUpdate` — the data
    structure the book's `PrimMST` calls an indexed priority queue. The lazy variant is absent by
    choice (note 7). A **Fibonacci heap** would improve the bound to O(*E* + *V* log *V*), and
    Robin is right that it belongs in Visitor rather than here: Gryphon asks for a frontier
    through a type class and would need no change at all beyond supplying a different `given`.
24. `tunnels.graph` is a real symbol graph — 80 buildings keyed by building code — so the hard
    part is done. Degrees of separation is then BFS plus `pathTo`, which is
    [#112](https://github.com/rchillyard/Gryphon/issues/112) again. It would make a good worked
    example once that exists.
25. **This row began as "not checked" and finding out turned up a defect.** Robin's reasoning on
    reading it: an MST algorithm on a connected graph can stop once the tree is complete, and
    that does not carry to the forest case. Measured on two components, `0-1` and `2-3`:

    | | Result |
    |---|---|
    | `Kruskal.mst` | 2 edges — a correct spanning forest |
    | `Boruvka.mst` | **did not terminate within 15 seconds** |
    | `Boruvka.mst`, connected control | terminated, 2 edges |

    Borůvka's only exit was "exactly one component remains", which a disconnected graph never
    reaches; once each component is internally spanned there are no crossing edges, so nothing
    merged and the `@tailrec` loop spun with identical arguments. A hang, not a wrong answer.
    Fixed in [#117](https://github.com/rchillyard/Gryphon/issues/117) by terminating when a round
    adds no edge, which is correct for both cases and also removes an O(*V*) scan per round.

    **Kruskal was already right, and the reason is worth keeping.** It is a `foldLeft` over all
    sorted edges with no early exit, so it runs out of edges and returns the forest. The standard
    optimisation of stopping at *order* − 1 edges is *not* implemented, and its absence is exactly
    what makes the forest case work — so adding it later would break this, which `SpanningForestSpec`
    now says in a comment. `MST.prim` is a third case: it grows from one start vertex and returns
    that vertex's component only, which is defensible and is now pinned by a test.

    The general lesson, which is why this note is long: **"not checked" is a finding, not a
    placeholder.** Two of the three algorithms behaved differently from the guess this row
    originally recorded.

---

## Summary

**Chapter 4 of S&W is substantially covered, and the README's claim to implement its eleven
numbered algorithms survives this audit** — each one in that table maps to code, and the mapping
is reproduced above. What the README's table does not show is the chapter's other material: the
supporting clients and implementations, which is where the gaps are. Of those, only `LazyPrimMST`
(deliberately superseded by the eager version) and `TransitiveClosure` (which the book itself
warns is impractical) are absent by choice. Gryphon additionally implements Borůvka, which is an
exercise rather than main text.

**The properties are a different story**, and #115 is why this document has a second half. The
cheap ones are all present — degrees, self-loops, adjacency rendering, both cycle detections,
two-colourability. What is absent clusters tightly around two things Gryphon does not have: a
**distance** from a traversal, and any **DFS bookkeeping** beyond the visited set.

What is genuinely missing, in rough order of how self-contained each is:

| Missing | Size | Note |
|---|---|---|
| `pathTo` on a traversal result | small | the parent map is already there; #112, and see #76 |
| Multi-source reachability | small | a fold threading one visited set; #113 |
| Longest path in a DAG, and CPM | small | a reversed `Ordering`; #114 and note 9 |
| `isDAG` on the Scala side, or not on the Java side | small | the two APIs disagree; note 15 |
| BFS that reports distance | small | the prerequisite for the next row; note 16 |
| Eccentricity, diameter, radius, center | small, once the row above exists | **one** piece of work, not four |
| 2-satisfiability | small | a client of `Kosaraju`, which is the hard half; note 19 |
| ~~A test for the minimum spanning forest~~ | — | done: #117 found a hang in Borůvka; note 25 |
| Deciding an Euler path exists | small | the degree condition is computable today |
| Constructing an Euler path (Hierholzer) | medium | note 21 |
| Report the negative cycle, not just detect it | medium | the specific #72 question |
| Report *which* cycle, for `isCyclic` | medium | same shape |
| Articulation points, bridges, biconnectedness | medium | wants discovery times and low-links |
| Lowest common ancestor | medium | first decide what it is *on*; note 18 |
| A Fibonacci heap for Prim | medium | **belongs in Visitor**, not here; note 23 |
| Floyd–Warshall and the rest of all-pairs | large | wants a matrix representation |
| Max flow, min cut, bipartite matching | large | wants flow networks; S&W ch. 6 |
| Planarity | large | out of scope unless wanted for itself |
| Hamilton path | — | NP-complete; not a gap in the same sense |

Three of these are now filed — [#112](https://github.com/rchillyard/Gryphon/issues/112),
[#113](https://github.com/rchillyard/Gryphon/issues/113) and
[#114](https://github.com/rchillyard/Gryphon/issues/114). The rest are recorded here and not
filed. **Filing is a separate decision from recording**, which is what this document is for:
#72 and #115 asked for the comparison, not for the work.

## Where the correspondence was already recorded

Five of the eight test fixtures cite the textbook directly, several by figure number:

| Fixture | Cites |
|---|---|
| `dag.graph` | the Topological Sort demo |
| `dfsu.graph` | the undirected DFS example |
| `dijkstra.graph` | the Dijkstra's Algorithm demo |
| `directed.graph` | Figure 4.2.15 (`tinyDG.txt`), plus one added edge |
| `directedSCC.graph` | Algorithm 4.6, p587 |
| `prim.graph` | Figure 4.3.1 (`tinyEWG.txt`), MST weight 1.81 |

So Gryphon was already being checked against the book, one fixture at a time. This document is
that correspondence gathered into one place, with the gaps named.
