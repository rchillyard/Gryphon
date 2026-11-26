package com.phasmidsoftware.gryphon.core

/**
 * Represents a graph structure defined by a sequence of edges.
 *
 * One of the four shapes a graph can be unbuilt into, and the most direct: an `EdgeGraph`
 * already has `edges`, so `EdgeList(g.edges.toSeq)` is the whole conversion, and `triplets`
 * carries it onward to `SerializableGraph.serialize`. See #135.
 *
 * It has been here before. #69 moved it to the test tree in 1.11.0 because in the published
 * library it was reachable but inert: its one consumer, `VertexMap.addEdges`, immediately read
 * `.edges` and folded, and `triplets` was called nowhere but the spec testing it. That was right
 * on the evidence then — what was missing was not a consumer for the wrapper but the *reason* a
 * wrapper should exist, which is that a graph needs rendering as well as building. With unbuild,
 * it has one.
 *
 * The four shapes form a 2x2: attributed or not, against edge objects or raw tuples. This is the
 * attributed, edge-object corner; `Triplets` is attributed tuples, and `VertexPairList` and
 * `ProtoConnexions` are the attributeless ones. All four converge on `triplets`, which is why
 * `serialize` needs no per-shape work.
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
