package com.phasmidsoftware.gryphon.traverse

import com.phasmidsoftware.gryphon.core
import com.phasmidsoftware.gryphon.visit.{Journal, PostVisitor, Visitor}

import scala.collection.mutable
import scala.util.{Failure, Success, Try, Using}

trait Traversal[V, E, T] {

  def vertexTraverse(v: V): T

  def edgeTraverse(e: E): T
}

object Traversal {

  def vertexTraversalDfs[V, E, T](f: V => T)(traversable: core.Traversable[V])(start: V): Traversal[V, E, T] = {
    implicit object ZZ extends Journal[mutable.Map[V, T], V] {
      /**
       * An empty journal.
       */
      def empty: mutable.Map[V, T] = mutable.Map.empty[V, T]

      /**
       * Method to append a `V` value to this `Journal`.
       *
       * @param map the journal to be appended to.
       * @param v   an instance of `V` to be appended to `map`.
       * @return a new `Journal`.
       */
      def append(map: mutable.Map[V, T], v: V): mutable.Map[V, T] = {
        val _ = map.put(v, f(v))
        map
      }
    }
    val result: Try[Visitor[V, mutable.Map[V, T]]] = Using(PostVisitor[V, mutable.Map[V, T]]()) {
      (visitor: Visitor[V, mutable.Map[V, T]]) =>
        traversable.dfs[mutable.Map[V, T]](visitor)(start)
    }
    result match {
      case Success(visitor: Visitor[V, mutable.Map[V, T]]) =>
        val map: mutable.Map[V, T] = visitor.journal
        map.keys.foldLeft(MapTraversal.empty[V, E, T]) { (m, v) => m + (v -> map(v)) }

      case Failure(exception) => throw exception
    }
  }

}

case class MapTraversal[V, E, T](map: Map[V, T]) extends Traversal[V, E, T] {
  def vertexTraverse(v: V): T = map.getOrElse(v, throw new NoSuchElementException(s"no such element: $v"))

  def edgeTraverse(e: E): T = ???

  def +(t: (V, T)): MapTraversal[V, E, T] = MapTraversal(map + t)
}

object MapTraversal {
  def empty[V, E, T]: MapTraversal[V, E, T] = MapTraversal(Map.empty[V, T])
}