Gryphon Graph Library
=====================
Gryphon is a library for representing and traversing graphs.

The core concepts are in the package com.phasmidsoftware.gryphon.core.
As always, consult the spec files (under test directory) to learn how to use the library.

Attributed
----------
Attributed is a trait which supports an attribute of some type.
Attributed types are _Graph_, _Edge_, _Vertex_.

Graph
-----
Graphs come in two basic types (represented by traits): _DirectedGraph_ and _UndirectedGraph_.
Only directed edges can be added to a directed graph and similarly for undirected graphs.
A Graph has an attribute which is its description.

Edge
----
There are three kinds of edges:

  * undirected edge
  * directed edge
  * vertex pair

A vertex pair has no representation of its connection, see _VertexPair_.
Otherwise, undirected and directed edges are as you might expect.

The attribute type pertaining to an _Edge_ is whatever you want it to be.
If, for example, you want to work with a Minimum Spanning Tree, or the Shortest Paths Tree,
then you would want your edge attribute to be orderable.
In this case, you would want either a _UndirectedOrderedEdge_ or a _DirectedOrderedEdge_.
For edges which do not have an intrinsic ordering, use _UndirectedEdge_ and _DirectedEdge_.
The mechanism for providing the ordering for an edge is through the _Ordering_ type class (part of Scala's implicits).

Vertex
------
From the perspective of application code, vertices are represented only by their attribute.
Again, you choose the appropriate attribute type.
Note, however, that if you are working with undirected edges,
the attribute type for a vertex must have an intrinsic ordering similarly defined to that of edges (see above).

There is a _Vertex_ type, but it is generally only referenced internally by library methods.

Graph Traversal
---------------
Depth-first and breadth-first search are supported.
In each case, a visitor of type _Visitor_ is passed to _dfs_ or _bfs_.

Visitor
-------
The visitor type provides a generic visitor which can be used by _dfs_ or _bfs_.
Please note that, like everything else in the Gryphon library, _Visitor_ is immutable.
It's best to see how it is used by looking at, for example, _GraphSpec_.

Applications
============
At present, there is only one application: the minimum spanning tree (MST).

Versions
========
The current version is 0.1.0
This supports the Prim application, and provides dfs and bfs graph traversal.