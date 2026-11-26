package com.phasmidsoftware.gryphon.core

/**
 * A trait that defines a structure for managing pairs of vertices with an associated value.
 *
 * The purpose of `Pairs` is to represent relationships or connections between pairs of
 * vertices of type `V`, with an additional associated value of type `Z` for each pair.
 *
 * Its one implementor is `VertexPairList`. This file also held `ProtoConnexions` until #137,
 * which was that same type under another name — same field, same `triplets` body — and was
 * removed in favour of it.
 *
 * @tparam V the type of the vertices in the pairs.
 * @tparam Z the type of the additional value associated with each pair of vertices.
 */
trait Pairs[V, Z]:

  /**
   * Returns a sequence of vertex pairs.
   * Each tuple in the sequence represents a relationship or connection
   * between two vertices of type `V` in the graph.
   *
   * @return a sequence of tuples, where each tuple contains two vertices of type `V`.
   */
  def pairs: Seq[(V, V, Z)]
