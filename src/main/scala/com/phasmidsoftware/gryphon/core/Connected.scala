/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.core

/**
 * Trait to model the behavior of a set of V objects which can be connected (or not).
 *
 * @tparam V the underlying type.
 */
trait Connected[V] {

    def isConnected(v1: V, v2: V): Boolean

    def connect(v1: V, v2: V): Connected[V]
}
