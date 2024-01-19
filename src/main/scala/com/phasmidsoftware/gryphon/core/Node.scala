/*
 * Copyright (c) 2024. Phasmid Software
 */

package com.phasmidsoftware.gryphon.core

/**
 * Case class to represent a Node[V].
 *
 * @param attribute  the (unique) attribute for this Node.
 * @param connexions a bag of Connexions.
 * @param discovered a mutable state to aid with graph traversal.
 * @tparam V the underlying attribute type.
 */
case class Node[V](attribute: V)(val connexions: Bag[Connexion[V, Node]])(var discovered: Boolean = false) extends Attribute[V] {

  /**
   * Method to add a connexion to this Node.
   *
   * @param connexion a Connexion[V, Node].
   * @return a new Node[V] based with the additional connexion.
   */
  def +(connexion: Connexion[V, Node]): Node[V] = Node(attribute)(connexions + connexion)(discovered)

  /**
   * Value of V->Node[V] tuple for this Node.
   */
  lazy val keyValue: (V, Node[V]) = attribute -> this

  /**
   * Method to perform strict equality between this and another Node.
   *
   * NOTE: the auto-generated equals method ONLY compares the attribute.
   *
   * @param v the other Node to compare.
   * @return true if and only if all members and parameters are equal.
   */
  def equalsStrict(v: Node[V]): Boolean = this == v && connexions == v.connexions && discovered == v.discovered

  override def toString: String = s"v$attribute"
}

object Node {
  def create[V](attribute: V, connexions: Bag[Connexion[V, Node]] = Bag.empty): Node[V] = new Node[V](attribute)(connexions)()

  trait DiscoverableVertex[V] extends Discoverable[Node[V]] {
    def isDiscovered(t: Node[V]): Boolean = t.discovered

    def setDiscovered(t: Node[V], b: Boolean): Unit = t.discovered = b
  }

  implicit object DiscoverableVertexString extends DiscoverableVertex[String]

  implicit object DiscoverableVertexInt extends DiscoverableVertex[Int]
}
