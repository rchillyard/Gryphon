package com.phasmidsoftware.gryphon.core

/**
 * A case class representing a list of vertex pairs in a graph.
 *
 * CONSIDER eliminate this class.
 *
 * This class provides a concrete implementation of `SerializableGraph` and `Pairs`, enabling functionalities
 * such as accessing the pairs of vertices and transforming these pairs into triplets. Each triplet includes
 * the source vertex, target vertex, and a unit value indicating the absence of an explicit edge attribute.
 *
 * @tparam V the type of attributes associated with the vertices in the graph.
 * @param pairs a sequence of tuples, where each tuple contains two vertices representing
 *              a connection or relationship in the graph.
 */
case class VertexPairList[V](pairs: Seq[(V, V, EdgeType)]) extends SerializableGraph[V, Unit, EdgeType] with Pairs[V, EdgeType]:
  /**
   * Transforms the pairs of vertices into a sequence of triplets.
   * Each triplet includes a source vertex, a target vertex, an optional attribute
   * (set to None), and the corresponding edge type.
   *
   * @return a sequence of triplets, where each triplet contains a source vertex,
   *         a target vertex, None as the optional attribute, and the edge type.
   */
  def triplets: Seq[Triplet[V, Unit, EdgeType]] = pairs.map(p => Triplet(p._1, p._2, None, p._3))
