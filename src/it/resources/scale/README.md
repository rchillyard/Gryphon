# Scale fixtures

Four generated graphs, 1000 vertices each, used by the `Scale` configuration (`sbt Scale/test`).
They exist because nothing in this repository exercised the library beyond a few dozen vertices
— the next largest graph is `tunnels.graph`, at 83 lines. See issue #165.

| file | shape | edges |
|---|---|---|
| `sparse.graph` | connected, undirected, weighted | 7,000 |
| `dense.graph` | connected, undirected, weighted | 30,000 |
| `sparseDag.graph` | acyclic, directed, weighted | 7,000 |
| `denseDag.graph` | acyclic, directed, weighted | 30,000 |

## How they were made

Each is **deterministic**, from a fixed seed recorded in the file's own header comment, so any
of them can be rebuilt byte-for-byte or extended without guesswork. The script below is the
generator; it is kept here rather than in the build because it is run once in a blue moon, and
a build task that nothing invokes is a liability.

The important part is that the properties the tests rely on are guaranteed by **construction**
rather than by luck:

- The undirected graphs start from a random spanning tree over a shuffled vertex order, then
  add random edges until the target count is reached. So they are connected, always.
- The DAGs start from the path `0 -> 1 -> ... -> 999`, then add random edges `a -> b` with
  `a < b`. So they are acyclic and every vertex is reachable from 0, always.

That matters: a test asserting `isConnected` on a fixture that merely happened to come out
connected would be checking the generator's luck rather than the library.

```python
import random

V = 1000

def weight(r):
    return f"{r.uniform(0.01, 1.0):.4f}"

def undirected(path, target_edges, seed, header):
    r = random.Random(seed)
    edges = set()
    perm = list(range(V)); r.shuffle(perm)
    for i in range(1, V):                       # spanning tree => connected
        j = r.randrange(0, i)
        edges.add((min(perm[i], perm[j]), max(perm[i], perm[j])))
    while len(edges) < target_edges:
        a, b = r.randrange(V), r.randrange(V)
        if a != b:
            edges.add((min(a, b), max(a, b)))
    lines = [f"// {header}",
             f"// {V} vertices, {len(edges)} undirected weighted edges. Generated, seed {seed}."]
    lines += [f"{a} = {b} {weight(r)}" for a, b in sorted(edges)]
    open(path, "w").write("\n".join(lines) + "\n")

def dag(path, target_edges, seed, header):
    r = random.Random(seed)
    edges = {(i - 1, i) for i in range(1, V)}   # a spanning path => connected and acyclic
    while len(edges) < target_edges:
        a = r.randrange(0, V - 1)
        b = r.randrange(a + 1, V)
        edges.add((a, b))                       # a < b always => acyclic
    lines = [f"// {header}",
             f"// {V} vertices, {len(edges)} directed weighted edges, acyclic. Generated, seed {seed}."]
    lines += [f"{a} > {b} {weight(r)}" for a, b in sorted(edges)]
    open(path, "w").write("\n".join(lines) + "\n")

undirected("sparse.graph",    7000, 20261127, "Sparse connected undirected weighted graph for scale testing.")
undirected("dense.graph",    30000, 20261128, "Dense connected undirected weighted graph for scale testing.")
dag("sparseDag.graph",        7000, 20261129, "Sparse DAG, weighted, for scale testing.")
dag("denseDag.graph",        30000, 20261130, "Dense DAG, weighted, for scale testing.")
```

## Format

The parser's own format, as used by every other `.graph` file here: `//` comments, then one edge
per line as `tail OP head weight`, where `OP` is `>` for a directed edge and `=` for an
undirected one.
