# Contributing to Gryphon

Thank you for your interest in contributing to Gryphon! This document describes
how to get started, the conventions we follow, and what kinds of contributions
are most welcome.

---

## What is Gryphon?

Gryphon is a Scala 3 graph algorithms library built on the
[Visitor](https://github.com/rchillyard/Visitor) typeclass traversal engine.
It provides purely functional implementations of the classic graph algorithms
from Sedgewick & Wayne, *Algorithms* (4th ed.), Chapter 4, along with a
Java façade for use in the INFO6205 course at Northeastern University.

---

## Getting Started

### Prerequisites

- **Java 17 or later** (students use Java 21 or 23; the library targets Java 21)
- **Scala 3** (via sbt)
- **sbt 1.9+**
- **IntelliJ IDEA** (recommended) with the Scala plugin

### Clone and build

```bash
git clone https://github.com/rchillyard/Gryphon.git
cd Gryphon
sbt test
```

All tests should be green before you start making changes.

### Dependencies

Gryphon depends on [Visitor](https://github.com/rchillyard/Visitor), pulled in
automatically via Maven Central. If you are working on features that require
Visitor changes, clone both repositories and use sbt's `publishLocal` to test
them together:

```bash
# In the Visitor repository
sbt publishLocal

# In Gryphon, reference the local snapshot in build.sbt temporarily
```

---

## Project Structure

```
src/
  main/
    scala/com/phasmidsoftware/gryphon/
      core/       — Graph, VertexMap, Vertex, Edge, Adjacency, Traversable
      adjunct/    — DirectedGraph, UndirectedGraph, Connectivity, edges
      traverse/   — Algorithms: ShortestPaths, MST, Kruskal, Kosaraju, etc.
      parse/      — GraphParser, file format support
    java/com/phasmidsoftware/gryphon/java/
                  — Java façade: Graph, Edge, WeightedEdge, ShortestPaths,
                    MinimumSpanningTree, StronglyConnectedComponents, Connectivity
  test/
    scala/        — Scala specs (ScalaTest AnyFlatSpec)
    java/         — Java tests (JUnit 5)
  resources/
    *.graph       — Graph fixture files (Sedgewick & Wayne examples)
attic/            — Historical/non-compiling code preserved for reference
```

---

## Coding Conventions

### Scala

- **Scala 3 throughout** — use `given`/`using`, `enum`, type aliases, and
  context bounds. Avoid Scala 2 implicits.
- **No default implementations on abstract traits** — abstract methods on
  `Graph[V]` and related traits must remain abstract. Every concrete type
  provides its own correct implementation.
- **Consistent type parameter ordering** — `V` (vertex type) always precedes
  `E` (edge attribute type).
- **`E: {Monoid, Ordering}`** — use Visitor's `Monoid` rather than `Numeric`
  for edge weight constraints.
- **Purely functional results** — algorithms return immutable result types.
  Internal mutable state (e.g. in Dijkstra's `pred` and `bestCost` maps) must
  be strictly local to the algorithm and not observable from outside.
- **`shouldBe` in tests** — use `shouldBe` rather than `should be` in ScalaTest
  specs.
- **Design documents** — significant architectural changes should be reflected in
  `JavaFacadeDesign.md` and, where appropriate, `DEFERRED.md`.

### Java façade

- **Hide all Scala-isms** — no `Option`, typeclasses, `given`/`using`, or
  companion object syntax visible to Java callers.
- **Two-tier API** — every algorithm method should have an Option 1 form
  (sensible defaults, `Double` weights) and an Option 3 form (caller-supplied
  functional interfaces). See `ShortestPaths.java` for the pattern.
- **Directionality guards** — methods that require a directed or undirected graph
  must throw `IllegalStateException` with a clear message if the wrong type is
  supplied.
- **Preserve Javadoc** — do not wholesale-replace Java files; surgical edits
  preserve existing documentation.

### General

- **Fix at the lowest level** — bugs should be resolved at their root rather than
  papered over at a higher layer.
- **Attic** — non-compiling or superseded code goes in `attic/` at the project
  root (outside sbt's managed source directories), preserving the original
  `main/test/scala` structure for provenance. Do not delete historical code.
- **Incremental commits** — deploy and test before proceeding to the next change.
  Each commit should leave all tests green.

---

## Testing

```bash
sbt test          # run all tests (Scala + Java)
```

- Scala tests use **ScalaTest** (`AnyFlatSpec` + `Matchers`).
- Java tests use **JUnit 5** via `com.github.sbt:junit-interface`.
- Graph fixtures live in `src/test/resources/*.graph` and use the Sedgewick &
  Wayne examples where possible so results can be cross-checked against the
  textbook.
- Every new algorithm in the Java façade should have a corresponding `*Test.java`
  that covers: correct result on the standard fixture, all non-source vertices
  present, correct result for a small inline graph, and an `IllegalStateException`
  guard test for the wrong graph type.

---

## What Contributions Are Welcome

### Most welcome

- **New algorithm implementations** — additional algorithms from Sedgewick &
  Wayne Chapter 4 not yet in Gryphon, implemented in Scala with tests.
- **Java façade additions** — new static façade classes for algorithms already
  implemented in Scala (e.g. `ConnectedComponents`, `TopologicalSort`).
- **Bug fixes** — particularly the known issues documented in
  `JavaFacadeDesign.md` (e.g. `DirectedGraph.addEdge` not ensuring the
  destination vertex exists).
- **Test coverage** — additional edge cases, especially disconnected graphs,
  self-loops, and graphs with unique vs. duplicate edge weights.
- **Documentation** — improvements to Javadoc, Scaladoc, or the design documents.

### Please discuss first

- **API-breaking changes** — changes to `Graph[V]`, `VertexMap`, or the
  Visitor-facing typeclasses affect everything downstream. Open an issue before
  starting work.
- **Dependency additions** — Gryphon intentionally keeps its dependency footprint
  small. New dependencies (including Cats) should be discussed first.
- **Scala `Graph` unification** — collapsing `DirectedGraph` and `UndirectedGraph`
  into a single class is tracked as a deferred item and would be a significant
  refactor. Coordinate before starting.

---

## Submitting a Pull Request

1. Fork the repository and create a feature branch from `main`.
2. Make your changes, ensuring all existing tests remain green.
3. Add tests for any new behaviour.
4. Update `JavaFacadeDesign.md` and/or the README if the change affects the
   public API or architecture.
5. Open a pull request with a clear description of what changed and why.

---

## Related Projects

- [Visitor](https://github.com/rchillyard/Visitor) — the typeclass traversal
  engine Gryphon builds on. Changes to Visitor's `Traversal`, `Journal`, or
  typeclass interfaces may require corresponding changes in Gryphon.
- [DSAIPG](https://github.com/rchillyard/DSAIPG) — the course repository that
  Gryphon supports. Student assignments draw on the Java façade.

---

## License

Gryphon is licensed under the MIT License. By contributing, you agree that your
contributions will be licensed under the same terms.