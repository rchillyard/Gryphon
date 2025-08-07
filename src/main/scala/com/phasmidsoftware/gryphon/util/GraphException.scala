package com.phasmidsoftware.gryphon.util

/**
 * A custom exception class for handling errors specifically related to graph operations.
 *
 * @param m The detail message associated with the exception, providing context or information 
 *          about the graph-related error that occurred.
 */
case class GraphException(m: String) extends Exception(m)
