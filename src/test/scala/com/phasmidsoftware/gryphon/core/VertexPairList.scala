package com.phasmidsoftware.gryphon.core

/**
 * A case class representing a list of vertex pairs in a graph.
 *
 * MOVED HERE FROM `src/main` IN 1.11.0 (#70). In the published library this type was reachable
 * but inert, exactly as `EdgeList` was: its one consumer, `VertexMap.createFromVertexPairList`,
 * did nothing but read `.pairs` and hand them to `addVertexPairs`, which already took the raw
 * sequence; and `triplets`, its only behaviour, was called nowhere but the spec that tests it.
 * That consumer is gone, since `VertexMap[V].addVertexPairs(pairs)` says the same thing.
 *
 * It is kept here rather than deleted because it is a useful test implementor of
 * `SerializableGraph` and `Pairs`, which `SerializableGraphSpec` exercises through it.
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
