/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.oldcore

/**
 * Trait to model the behavior of a set of V objects which can be connected (or not).
 * Examples of Connected are: (1) Union-Find data structure, (2) any graph.
 *
 * @tparam V the underlying type.
 */
trait Connected[V] {

  /**
   * Method to determine if there is a connection between v1 and v2.
   *
   * @param v1 a node in a network.
   * @param v2 another node in a network.
   * @return true if there is a connection between v1 and v2.
   *         Note that this does not imply any sort of pathway between them.
   *         Merely a connection.
   */
  def isConnected(v1: V, v2: V): Boolean

  /**
   * Method to make a connection between v1 and v2.
   *
   * @param v1 a node in a network.
   * @param v2 another node in a network.
   * @return a new Connected object on which isConnected(v1, v2) will be true.
   */
  def connect(v1: V, v2: V): Connected[V]
}

/**
 * Trait to define the behavior of connected network that has paths.
 * Example of PathConnected is: any graph.
 *
 * @tparam V the underlying type.
 */
trait PathConnected[V] extends Connected[V] {

  /**
   * Method to determine if there is a path from v1 to v2.
   *
   * @param v1 the start of the possible path.
   * @param v2 the end of the possible path.
   * @return true if it is possible to follow a path from v1 to v2.
   *         It may be possible that this implies a path from v2 to v1 but that information is not expressed by this method.
   */
  def isPath(v1: V, v2: V): Boolean = isPathConnected(v1, v2)

  /**
   * Method to determine if there is a connection between v1 and v2.
   *
   * @param v1 a node in a network.
   * @param v2 another node in a network.
   * @return true if there is a connection between v1 and v2.
   */
  def isConnected(v1: V, v2: V): Boolean = isPath(v1, v2)

  /**
   * Method to determine if there is a connection between v1 and v2.
   *
   * @param v1 a node in a network.
   * @param v2 another node in a network.
   * @return true if there is a connection between v1 and v2.
   */
  def isPathConnected(v1: V, v2: V): Boolean

  /**
   * Method to get a path between v1 and v2.
   * There is no implication that this is the shortest path.
   *
   * @param v1 a node in a network.
   * @param v2 another node in a network.
   * @return the path from v1 to v2.
   *         By convention, the path consists of v1, any intermediate nodes, and v2.
   */
  def path(v1: V, v2: V): Seq[V]

}