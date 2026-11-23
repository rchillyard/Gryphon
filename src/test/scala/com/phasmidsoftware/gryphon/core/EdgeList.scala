package com.phasmidsoftware.gryphon.core

/**
 * Represents a graph structure defined by a sequence of edges.
 *
 * MOVED HERE FROM `src/main` IN 1.11.0 (#69). In the published library this type was reachable
 * but inert: its one consumer, `VertexMap.addEdges`, immediately read `.edges` and folded, and
 * its only real method, `triplets`, was called nowhere but the spec that exists to test it.
 * `addEdges` now takes the sequence directly, so nothing in main needs the wrapper.
 *
 * It is kept here rather than deleted because it is a useful test implementor of
 * `SerializableGraph` and `Edges` — `Edges`'s only one — and `SerializableGraphSpec` exercises
 * both traits through it. That coverage is the reason this class still exists.
 *
 * @param edges a sequence of edges that define the connections in the graph.
 * @tparam V the type of the vertices in the graph.
 * @tparam E the type of the attributes associated with the edges.
 * @tparam Z a type parameter to describe additional properties, such as the nature of the edges.
 */
case class EdgeList[V, E, Z >: EdgeType](edges: Seq[Edge[V, E]]) extends SerializableGraph[V, E, Z] with Edges[V, E]:
  /**
   * Generates a sequence of triplets, where each triplet represents an edge in the graph.
   *
   * @return a sequence of `Triplet[V, E, Z]`, where each triplet describes the `white` vertex,
   *         the `black` vertex, the edge attribute (`attribute`), and the edge type (`edgeType`)
   *         of an edge in the graph.
   */
  // `Z >: EdgeType` on the class is what retires the cast this line used to need: `edgeType` is
  // an EdgeType, so with Z a supertype of it no conversion is required. Z was unbounded, which
  // made the cast sound only for the one instantiation anyone actually uses, Z = EdgeType.
  def triplets: Seq[Triplet[V, E, Z]] =
    edges.map(e => Triplet(e.white, e.black, Some(e.attribute), e.edgeType))
