package com.phasmidsoftware.gryphon.core

/**
 * A case class representing a collection of graph triplets.
 *
 * Each triplet in the collection encapsulates information about two vertices and
 * their connecting edge (if one exists) in a graph structure. This class extends
 * the `SerializableGraph` trait, allowing it to support serialization and traversal
 * of the contained triplets.
 *
 * NOTE that this lived in `EdgeList.scala` until 1.11.0, beside the `EdgeList` it was paired
 * with. `EdgeList` has moved to the test tree (#69), and `Triplets` is the implementor of
 * `SerializableGraph` that main actually uses — `VertexMap.createFromTriplets` takes one.
 *
 * @tparam V the type associated with the vertices in the triplets.
 * @tparam E the type associated with the edges in the triplets.
 * @tparam Z the auxiliary type used within the triplet or graph structure.
 * @param triplets the sequence of triplets representing the graph structure.
 */
case class Triplets[V, E, Z](triplets: Seq[Triplet[V, E, Z]]) extends SerializableGraph[V, E, Z]
