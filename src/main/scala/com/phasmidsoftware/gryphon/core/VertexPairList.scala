package com.phasmidsoftware.gryphon.core

/**
 * A case class representing a list of vertex pairs in a graph.
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
   * Transforms the sequence of vertex pairs into a sequence of triplets.
   * Each triplet consists of the attributes of the source vertex, the target vertex,
   * and a unit value (`()`), indicating the absence of an explicit edge attribute.
   *
   * @return a sequence of triplets where each triplet contains the attribute of the source vertex,
   *         the attribute of the target vertex, and a unit value.
   */
  def triplets: Seq[(V, V, Unit, EdgeType)] = pairs.map(p => (p._1, p._2, (), p._3))
