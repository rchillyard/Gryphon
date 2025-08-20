package com.phasmidsoftware

import com.phasmidsoftware.gryphon.adjunct.{OrderedEdge, UndirectedEdge}
import com.phasmidsoftware.gryphon.core.*

package object gryphon {

  /**
   * A function that maps an `EdgeType` to a corresponding function that transforms
   * a `ProtoConnexion[Unit, Int]` into a `Connexion[Int]`.
   *
   * - For `Directed`, it returns `DirectedEdge`.
   * - For `Undirected`, it returns `UndirectedEdge`.
   * - For `Undefined`, it creates a `VertexPair` from the vertices of the proto-connexion.
   *
   * This function encapsulates edge type-specific behavior for constructing connections
   * in a graph.
   */
  val edgeFunc: EdgeType => ProtoConnexion[Unit, Int] => Connexion[Int] = {
    case Directed =>
      (_, b, c) => OrderedEdge[Int](b, c)
    case Undirected =>
      UndirectedEdge[Unit, Int]
    case Undefined =>
      (_, v1, v2) => VertexPair(v1, v2)
  }

}
