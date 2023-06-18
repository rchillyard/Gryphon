/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.core

import scala.annotation.tailrec

/**
 * Trait to model the behavior of a disjoint set.
 * The most typical application of this trait is the Union-Find problem.
 *
 * @tparam V the underlying object type.
 */
trait DisjointSet[V] extends Connected[V] {
    /**
     * Method to return the set to which the given key belongs.
     *
     * @param key a key.
     * @return the key corresponding to the root of the tree to which key belongs.
     */
    def getDisjointSet(key: V): V

    /**
     * The number of disjoint sets (not the number of objects).
     *
     * @return the number of disjoint sets (or components).
     */
    def size: Int

    /**
     * Method to connect two disjoint sets.
     *
     * @param v1 an object in the first of the sets to be connected.
     * @param v2 an object in the second of the sets to be connected.
     * @return a new DisjointSet on which isConnected(v1, v2) will be true.
     */
    def connect(v1: V, v2: V): DisjointSet[V]
}

/**
 * An abstract class to model the behavior of a disjoint set.
 * Each object in the given Map is associated with an optional parent object which represents the disjoint set to which the object belongs.
 * If the optional parent is None, then the object has no parent and is "root" of its own disjoint set.
 *
 * @param map a Map of V -> Option[V] which represents the component map for each object.
 * @tparam V the underlying type of the objects.
 * @tparam W the type of the key to the map (this may be the same as V or it might be compounded from V).
 */
abstract class AbstractDisjointSet[V, W](map: Map[V, W])(f: W => Option[V]) extends DisjointSet[V] {

    /**
     * Method to determine if object <code>v</code> in this AbstractDisjointSet is the root of its component.
     *
     * @param v an object of type V.
     * @return true is the parent of v is empty.
     */
    def isRoot(v: V): Boolean = parent(v).isEmpty

    /**
     * Method to yield the parent of object <code>v</code>.
     *
     * @param v an object of type V.
     * @return an optional V representing the parent component.
     */
    def parent(v: V): Option[V] = get(v) flatMap f

    /**
     * Get the number of disjoint sets.
     * Note that if you want the number of objects, just invoke map.size.
     *
     * @return the number of disjoint sets (or components).
     */
    override def size: Int = map.keys.count(v => isRoot(v))

    /**
     * Method to create a new DisjointSet from the given map.
     *
     * @param map See description of <code>map</code> field for this class.
     * @return
     */
    def unit(map: Map[V, W]): DisjointSet[V]

    /**
     * Method to remove a key from this disjoint set.
     *
     * @param key the key to be remove.
     * @return a new DisjointSet which is <code>this</code> without <code>key</code>.
     */
    def remove(key: V): DisjointSet[V] = unit(map.removed(key))

    /**
     * Method to yield an updated map based on this map but with a new key->value pair.
     *
     * @param key   the key (if the key already exists, then its value will be updated).
     * @param value the (new) value.
     * @tparam W1 the underlying type of the value (basically an Option[W], but note that the value type in a Map is covariant).
     * @return a new Map[W, W1].
     */
    def updated[W1 >: W](key: V, value: W1): Map[V, W1] = map.updated(key, value)

    /**
     * Method to get the value of a key.
     *
     * @param key the given key.
     * @return an Option of Option[V].
     *         If the result is None, then key is not in this disjoint set.
     *         If the result is Some(None), then key represents the root node of a component.
     *         If the result is Some(Some(w)), then the result is the parent node for the given key.
     */
    def get(key: V): Option[W] = map.get(key)

    /**
     * Method to get an iterator over the entries in the map.
     *
     * @return an Iterator of (V, Option[V]) tuples.
     */
    def iterator: Iterator[(V, W)] = map.iterator

    /**
     * Method to determine if v1 and v2 belong to the same disjoint set.
     *
     * @param v1 an object.
     * @param v2 another object.
     * @return true if v1 and v2 belong to the same disjoint set.
     */
    def isConnected(v1: V, v2: V): Boolean = getDisjointSet(v1) == getDisjointSet(v2)

    /**
     * Method to return the set to which the given key belongs.
     *
     * @param key a key.
     * @return the key corresponding to the root of the tree to which key belongs.
     */
    def getDisjointSet(key: V): V = {
        @tailrec
        def inner(w: V): V = get(w) map f match {
            case Some(None) => w
            case Some(Some(x)) => inner(x)
            case None => throw GraphException(s"DisjointSet: getDisjointSet: key $key does not exist in any set")
        }

        inner(key)
    }
}