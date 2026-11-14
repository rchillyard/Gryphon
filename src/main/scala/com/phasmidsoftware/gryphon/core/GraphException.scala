/*
 * Copyright (c) 2024. Phasmid Software
 */

package com.phasmidsoftware.gryphon.core

/**
 * A custom exception class for handling errors specifically related to graph operations.
 *
 * This is the one graph exception type. The `util` package's `GraphException` is a deprecated alias for it,
 * kept so that code written against Gryphon 1.6.0 or earlier still compiles.
 *
 * @param m The detail message associated with the exception, providing context or information
 *          about the graph-related error that occurred.
 *
 * Unchecked, since 2.0.0. It extends `RuntimeException` rather than `Exception` because it is
 * thrown out of the Java façade, whose methods do not declare it — and Java will not compile a
 * `catch` for a checked exception the called method cannot throw. Up to 1.7.0 a Java caller could
 * therefore neither handle it nor, in most cases, discover it before run time. Scala does not
 * distinguish the two, so nothing changes for Scala callers.
 */
case class GraphException(m: String) extends RuntimeException(m)
