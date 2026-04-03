package com.phasmidsoftware.gryphon

/**
 * This package object contains definitions and type aliases used across the `core` package.
 * NOTE: maybe this is not strictly necessary in Scala 3 but it's helpful to have definitions in one place.
 */
package object core {
  /**
   * A type alias representing a connection-like structure composed of three elements:
   * an edge of type `E` and two vertices of type `V`.
   *
   * This alias is typically used to describe relationships or connections in graphs
   * or other data structures where a single edge connects two vertices.
   *
   * @tparam E the type representing the edge attribute.
   * @tparam V the type representing the vertex attribute.
   */
  type ProtoConnexion[V, E] = (E, V, V)
}
