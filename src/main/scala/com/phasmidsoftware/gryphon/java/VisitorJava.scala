package com.phasmidsoftware.gryphon.java

import com.phasmidsoftware.gryphon.core._
import java.util
import java.util.concurrent.LinkedBlockingQueue
import java.util.{Collection, Stack}

/**
 * Visitor adapted for use by Java.
 * Visitor functions are delegated to an actual Visitor.
 * This type of visitor is only suitable for mutating journals.
 * If you want to use an immutable journal, then use the Scala visitors.
 *
 * @tparam V the underlying type of this Visitor.
 */
trait VisitorJava[V] {
    /**
     * Method to traverse the given graph with this VisitorJava.
     *
     * @param graph the Graph to be traversed.
     * @param v     the starting vertex.
     * @tparam E the edge attribute type.
     * @tparam X the edge type.
     * @tparam P the property type.
     * @return a Collection. WARNING: ideally, you should ignore this result.
     *         Instead, use the iterator or the journal of the visitor.
     *         It's a quirk of the fact that the journal is always mutable for a VisitorJava.
     */
    def dfs[E, X <: Edge[V, E], P](graph: Graph[V, E, X, P], v: V): Collection[V]

    /**
     * Method to get an iterator from this visitor after it has been run.
     *
     * @return the true iterator (do not iterate on the return value from dfs).
     */
    def iterator: util.Iterator[V]

}

case class VisitorJavaQueue[V](visitor: Visitor[V, util.Queue[V]]) extends VisitorJava[V] {

    def dfs[E, X <: Edge[V, E], P](graph: Graph[V, E, X, P], v: V): util.Queue[V] = graph.dfs(visitor)(v).journal

    def iterator: util.Iterator[V] = visitor.journal.iterator()
}

case class VisitorJavaStack[V](visitor: Visitor[V, util.Stack[V]]) extends VisitorJava[V] {

    def dfs[E, X <: Edge[V, E], P](graph: Graph[V, E, X, P], v: V): util.Stack[V] = {
        val value = graph.dfs(visitor)(v)
        value.journal
    }

    /**
     * Correct implementation of iterator for a stack.
     * The Stack class in Java does not iterate according to the popped order.
     *
     * @return the true iterator (do not iterate on the return value from dfs).
     */
    def iterator: util.Iterator[V] = {
        val stack: Stack[V] = visitor.journal
        new util.Iterator[V] {
            def hasNext: Boolean = !stack.empty()

            def next(): V = stack.pop
        }
    }
}

object VisitorJava {

    private trait JournalJavaQueue[V] extends Journal[java.util.Queue[V], V] {
        def empty: util.Queue[V] = new LinkedBlockingQueue[V]()

        def append(j: util.Queue[V], v: V): util.Queue[V] = if (j.offer(v)) j else throw GraphException(s"VisitorJavaQueue: unable to append to queue")
    }

    private trait JournalJavaStack[V] extends Journal[java.util.Stack[V], V] {
        def empty: util.Stack[V] = new Stack[V]()

        def append(j: util.Stack[V], v: V): util.Stack[V] = {
            j.push(v)
            j
        }
    }

    def createPre[V]: VisitorJavaQueue[V] = {
        implicit object JournalV extends JournalJavaQueue[V]

        VisitorJavaQueue(PreVisitor[V, util.Queue[V]]())
    }

    def createPost[V]: VisitorJavaQueue[V] = {
        implicit object JournalV extends JournalJavaQueue[V]

        VisitorJavaQueue(PostVisitor[V, util.Queue[V]]())
    }

    def createReversePost[V]: VisitorJavaStack[V] = {
        implicit object JournalV extends JournalJavaStack[V]

        VisitorJavaStack(PostVisitor[V, util.Stack[V]]())
    }
}
