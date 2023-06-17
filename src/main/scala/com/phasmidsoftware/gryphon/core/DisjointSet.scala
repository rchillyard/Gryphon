/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.core

import scala.annotation.tailrec

trait DisjointSet[V] extends Connected[V] with Map[V, Option[V]] {
    /**
     * Method to return the set to which the given key belongs.
     *
     * @param key a key.
     * @return the key corresponding to the root of the tree to which key belongs.
     */
    def getDisjointSet(key: V): V

    def size: Int

    def connect(v1: V, v2: V): DisjointSet[V]
}

abstract class AbstractDisjointSet[V](map: Map[V, Option[V]]) extends DisjointSet[V] {

    override def size: Int = map.values.count(vo => vo.isEmpty)

    def unit(map: Map[V, Option[V]]): DisjointSet[V]

    def removed(key: V): Map[V, Option[V]] = map.removed(key)

    def updated[V1 >: Option[V]](key: V, value: V1): Map[V, V1] = map.updated(key, value)

    def get(key: V): Option[Option[V]] = map.get(key)

    def iterator: Iterator[(V, Option[V])] = map.iterator

    def isConnected(v1: V, v2: V): Boolean = getDisjointSet(v1) == getDisjointSet(v2)

    /**
     * Method to return the set to which the given key belongs.
     *
     * @param key a key.
     * @return the key corresponding to the root of the tree to which key belongs.
     */
    def getDisjointSet(key: V): V = {
        @tailrec
        def inner(w: V): V = get(w) match {
            case Some(None) => w
            case Some(Some(x)) => inner(x)
            case None => throw GraphException(s"DisjointSet: getDisjointSet: key $key does not exist in any set")
        }

        inner(key)
    }

}
