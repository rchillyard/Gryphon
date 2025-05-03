package com.phasmidsoftware.gryphon.core

/**
 * Represents a graph structure defined by a sequence of edges.
 * Each edge connects two vertices of type `V` and contains an attribute of type `E`.
 * The `EdgeList` class allows for operations on the edges of the graph, such as transforming
 * them into a sequence of triplets.
 *
 * CONSIDER eliminate this type.
 *
 * The graph is also associated with a type parameter `Z`, which can be used to define
 * additional properties or characteristics of the graph or edges, such as edge types.
 *
 * @param edges a sequence of edges that define the connections in the graph.
 *              Each edge connects a source vertex (`white`) and a target vertex (`black`),
 *              and may carry an associated attribute (`attribute`) and type (`edgeType`).
 * @tparam V the type of the vertices in the graph.
 * @tparam E the type of the attributes associated with the edges.
 * @tparam Z a type parameter to describe additional properties, such as the nature of the edges.
 */
case class EdgeList[V, E, Z](edges: Seq[Edge[E, V]]) extends SerializableGraph[V, E, Z] with Edges[V, E]:
  /**
   * Generates a sequence of triplets, where each triplet represents an edge in the graph.
   * Each triplet contains the source vertex, the target vertex, the attribute associated with the edge, 
   * and the type of the edge cast to type `Z`.
   *
   * @return a sequence of `Triplet[V, E, Z]`, where each triplet describes the `white` vertex,
   *         the `black` vertex, the edge attribute (`attribute`), and the edge type (`edgeType`)
   *         of an edge in the graph.
   */
  // CONSIDER eliminate this asInstanceOf
  def triplets: Seq[Triplet[V, E, Z]] = edges.map(e => Triplet(e.white, e.black, Some(e.attribute), e.edgeType.asInstanceOf[Z]))

/**
 * A case class representing a collection of graph triplets.
 *
 * Each triplet in the collection encapsulates information about two vertices and 
 * their connecting edge (if one exists) in a graph structure. This class extends
 * the `SerializableGraph` trait, allowing it to support serialization and traversal
 * of the contained triplets.
 *
 * @tparam V the type associated with the vertices in the triplets.
 * @tparam E the type associated with the edges in the triplets.
 * @tparam Z the auxiliary type used within the triplet or graph structure.
 * @param triplets the sequence of triplets representing the graph structure.
 */
case class Triplets[V, E, Z](triplets: Seq[Triplet[V, E, Z]]) extends SerializableGraph[V, E, Z]

