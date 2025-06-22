package com.phasmidsoftware.gryphon.core

/**
 * A trait representing an entity that can transition between discovered and undiscovered states.
 * This is typically useful in algorithms or systems where traversal, state tracking, or discovery
 * logic is applied, such as graph algorithms.
 *
 * @tparam V the type associated with the entity implementing this trait.
 */
trait Discoverable[V] {

  /**
   * Indicates whether the current entity has been discovered.
   *
   * @return `true` if the entity is in a discovered state, `false` otherwise.
   */
  def discovered: Boolean

  /**
   * Mutating method that resets the `discovered` state of this `Vertex` to `false`.
   *
   * @return Unit
   */
  def reset(): Discoverable[V]

  /**
   * Marks the current vertex as discovered by setting its internal `discovered` state to `true`.
   * This operates by side-effect.
   *
   * @return the current vertex (`Vertex[V]`) instance with its `discovered` state updated.
   */
  def discover(): Discoverable[V]

  /**
   * Checks if the vertex is not discovered.
   * NOTE that this must remain a def (not a lazy val) because the `discovered` property is mutable.
   *
   * This method inversely reflects the `discovered` property of a vertex.
   * If a vertex is marked as not discovered, this method returns `true`.
   *
   * @return `true` if the vertex has not been discovered, `false` otherwise.
   */
  def undiscovered: Boolean
}
