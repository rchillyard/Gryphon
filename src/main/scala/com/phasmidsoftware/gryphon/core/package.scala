package com.phasmidsoftware.gryphon

/**
 * This package object contains definitions and type aliases used across the `core` package.
 * NOTE: maybe this is not strictly necessary in Scala 3 but it's helpful to have definitions in one place.
 */
package object core {

  /**
   * A type alias representing a triplet containing two elements of type `V` and one of type `E`.
   * This type is used for modeling graph relationships, where the triplet includes the starting vertex,
   * the ending vertex, and the edge connecting them.
   *
   * @tparam V the type of the vertex attribute in a graph.
   * @tparam E the type of the edge attribute in the graph.
   */
  type Triplet[V, E] = (V, V, E)

}
