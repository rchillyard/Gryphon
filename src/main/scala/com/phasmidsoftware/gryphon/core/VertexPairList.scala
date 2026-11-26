package com.phasmidsoftware.gryphon.core

/**
 * A case class representing a list of vertex pairs in a graph.
 *
 * One of the four shapes a graph can be unbuilt into: the attributeless, raw-tuple corner, where
 * only the connections matter and `E` is `Unit`. See #135.
 *
 * It has been here before. #70 moved it to the test tree in 1.11.0 because in the published
 * library it was reachable but inert, exactly as `EdgeList` was: its one consumer,
 * `VertexMap.createFromVertexPairList`, did nothing but read `.pairs` and hand them to
 * `addVertexPairs`, which already took the raw sequence. That consumer is still gone and is not
 * coming back — `VertexMap[V].addVertexPairs(pairs)` says the same thing. What brings this type
 * back is the other direction: rendering a graph, not building one.
 *
 * Note the overlap with `ProtoConnexions`, which holds the same `Seq[(V, V, Z)]` and is also
 * attributeless. They differ only in that `Z` is fixed to `EdgeType` here. Whether both are
 * wanted is worth settling when unbuild gains its targets.
 *
 * @tparam V the type of attributes associated with the vertices in the graph.
 * @param pairs a sequence of tuples, where each tuple contains two vertices representing
 *              a connection or relationship in the graph.
 */
case class VertexPairList[V](pairs: Seq[(V, V, EdgeType)]) extends SerializableGraph[V, Unit, EdgeType] with Pairs[V, EdgeType]:
  /**
   * Transforms the pairs of vertices into a sequence of triplets.
   *
   * @return a sequence of triplets, where each triplet contains a source vertex,
   *         a target vertex, None as the optional attribute, and the edge type.
   */
  def triplets: Seq[Triplet[V, Unit, EdgeType]] =
    pairs.map(p => Triplet(p._1, p._2, None, p._3))
