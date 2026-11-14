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
 */
case class GraphException(m: String) extends Exception(m)
