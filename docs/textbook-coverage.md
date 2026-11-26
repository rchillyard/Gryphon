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

Audited against `main` on 2026-11-25, at Gryphon 1.12.0.

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
9. Longest path in a DAG is shortest path with the weights negated, and CPM is a client of it.
   Both are small given `AcyclicShortestPaths`, and the pair is the most self-contained missing
   item in this table.
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

## Summary

**Chapter 4 of S&W is substantially covered, and the README's claim to implement its eleven
numbered algorithms survives this audit** — each one in that table maps to code, and the mapping
is reproduced above. What the README's table does not show is the chapter's other material: the
supporting clients and implementations, which is where the gaps are. Of those, only `LazyPrimMST`
(deliberately superseded by the eager version) and `TransitiveClosure` (which the book itself
warns is impractical) are absent by choice. Gryphon additionally implements Borůvka, which is an
exercise rather than main text.

What is genuinely missing, in rough order of how self-contained each is:

| Missing | Size | Note |
|---|---|---|
| `pathTo` on a traversal result | small | the parent map is already there; see #76 |
| Multi-source reachability | small | a fold of the single-source case |
| Longest path in a DAG, and CPM | small | negate the weights |
| Report the negative cycle, not just detect it | medium | the specific #72 question |
| Report *which* cycle, for `isCyclic` | medium | same shape |
| Floyd–Warshall and the rest of all-pairs | large | wants a matrix representation |
| Max flow, min cut, bipartite matching | large | wants flow networks; S&W ch. 6 |

None of these is filed as an issue yet. **Filing them is a separate decision from recording
them**, which is what this document is for: #72 asked for the comparison, not for the work.

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
