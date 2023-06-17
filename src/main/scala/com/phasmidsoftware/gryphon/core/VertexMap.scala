/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.core

import com.phasmidsoftware.flog.Flog
import com.phasmidsoftware.gryphon.core.VertexMap.findAndMarkVertex
import com.phasmidsoftware.gryphon.visit.Queueable.QueueableQueue
import com.phasmidsoftware.gryphon.visit.{MutableQueueable, Queueable, Visitor}
import scala.annotation.tailrec
import scala.collection.immutable.{HashMap, Queue, TreeMap}

/**
 * Trait to define the behavior of a "vertex map," i.e. the set of adjacency lists for a graph.
 *
 * The adjacency list (of type AdjacencyList[X]) for a vertex (type V) points to edges of type X which, in turn, reference
 * vertices of type Vertex[V, X].
 *
 * There are two distinct types of VertexMap:
 * <ol>
 * <li>Those that can be ordered according to type V (these will use a TreeMap)</li>
 * <li>Those that can't be ordered according to type V (these will use a HashMap)</li>
 * </ol>
 *
 * @tparam V the (key) vertex-type of a graph.
 * @tparam X the edge-type of a graph. A sub-type of EdgeLike[V].
 * @tparam P the property type (a mutable property currently only supported by the Vertex type).
 */
trait VertexMap[V, X <: EdgeLike[V], P] extends Traversable[V] {
    self =>

    /**
     * Method to determine if this VertexMap contains a vertex with attribute v.
     *
     * @param v the attribute of the vertex.
     * @return true if this VertexMap contains a vertex with attribute v.
     */
    def contains(v: V): Boolean

    /**
     * Method to yield the Vertex at v.
     *
     * @param v (a V) which is the vertex to look up.
     * @return Option of Vertex[V, X, P].
     */
    def get(v: V): Option[Vertex[V, X, P]]

    /**
     * Method to determine if this VertexMap contains a vertex at the other end of the given edge from the vertex with attribute v.
     *
     * @param v the attribute of the vertex.
     * @param x (Y >: X <: EdgeLike[V]) an edge.
     * @return true if this VertexMap contains the vertex at the other end of x from v.
     */
    def containsOther(v: V, x: X): Boolean = contains(x.otherVertex(v))

    /**
     * Method to yield the number of adjacency lists defined in this VertexMap.
     *
     * @return the size of this VertexMap.
     */
    def size: Int

    /**
     * Method to get the AdjacencyList for vertex with key (attribute) v, if there is one.
     *
     * @param v the key (attribute) of the vertex whose adjacency list we require.
     * @return an Option of AdjacencyList[X].
     */
    def optAdjacencyList(v: V): Option[AdjacencyList[X]]

    /**
     * Method to get a sequence of the adjacent edges for vertex with key (attribute) v.
     *
     * @param v the key (attribute) of the vertex whose adjacency list we require.
     * @return <code>optAdjacencyList(v).toSeq.flatMap(_.xs)</code>.
     */
    def adjacentEdges(v: V): Seq[X] = optAdjacencyList(v).toSeq.flatMap(_.xs)

    /**
     * Method to get a sequence of the adjacent edges for vertex with key (attribute) v.\
     * that also satisfy the predicate given.
     *
     * @param v the key (attribute) of the vertex whose adjacency list we require.
     * @param p a predicate of type X => Boolean.
     * @return <code>adjacentEdges(v) filter p</code>.
     */
    def adjacentEdgesWithFilter(v: V)(p: X => Boolean): Seq[X] = adjacentEdges(v) filter p

    /**
     * the vertex-type values, i.e. the keys, of this VertexMap.
     */
    val keys: Iterable[V]

    /**
     * the Vertex[V, X] values of this VertexMap.
     */
    def values: Iterable[Vertex[V, X, P]]

    /**
     * the X values of this VertexMap.
     */
    val edges: Iterable[X]

    /**
     * Method to get an optional value of type Q for a given vertex v, based on that vertex's property.
     *
     * @param f a function which transforms a P into a Q.
     * @param v the vertex (attribute) whose property we need.
     * @tparam Q the return type.
     * @return an Option[Q].
     */
    def processVertexProperty[Q](f: P => Q)(v: V): Option[Q] = for {
        vertex <- get(v)
        p <- vertex.getProperty
    } yield f(p)

    /**
     * Method to add a vertex to this VertexMap.
     *
     * @param v the (key) value of the vertex to be added.
     * @return a new VertexMap which includes all the original entries of <code>this</code> plus <code>v</code>.
     */
    def addVertex(v: V): VertexMap[V, X, P]

    /**
     * Method to add an edge to this VertexMap.
     *
     * @param v the (key) value of the vertex whose adjacency list we are adding to.
     * @param y the edge to be added to the adjacency list.
     * @return a new VertexMap which includes all the original entries of <code>this</code> plus <code>v -> x</code>.
     */
    def addEdge(v: V, y: X): VertexMap[V, X, P]

    /**
     * Method to copy the Vertexs from this VertexMap to "to".
     *
     * @param to the VertexMap to which we will copy the vertices of this VertexMap.
     * @tparam Xout the type of the edges in the resulting VertexMap.
     * @return
     */
    def copyVertices[Xout <: EdgeLike[V]](to: VertexMap[V, Xout, Unit]): VertexMap[V, Xout, Unit] = keys.foldLeft(to) {
        (mv, v) => mv.addVertex(v)
    }
}

object VertexMap {
    /**
     * This method finds the vertex at the other end of x from v, checks to see if it is already discovered
     * and, if not, marks it as discovered then returns it, wrapped in Some.
     *
     * @param vertexMap    the Map of V -> Vertex[V, X, P] which represents the adjacencies of a graph.
     * @param f            a Unit-function to be applied to the Vertex corresponding to maybeV (if it exists).
     * @param maybeV       an optional V value which is the attribute of the Vertex to be found and marked.
     * @param errorMessage an error message which will be the message for the exception that arises when maybeV is None.
     * @return Option[V]: the (optional) vertex to run dfs on next.
     */
    private[core] def findAndMarkVertex[V, X <: EdgeLike[V], P](vertexMap: Map[V, Vertex[V, X, P]], f: Vertex[V, X, P] => Unit, maybeV: Option[V], errorMessage: String): Option[V] = maybeV match {
        case Some(z) =>
            val xXvo: Option[Vertex[V, X, P]] = vertexMap.get(z) filterNot (_.discovered)
            xXvo foreach (vertex => f(vertex))
            xXvo map (_.attribute)
        case None => throw GraphException(errorMessage)
    }
}

/**
 * Trait to define the behavior of an OrderedVertexMap.
 *
 * @tparam V the (key) vertex-type of a graph.
 * @tparam X the edge-type of a graph. A sub-type of EdgeLike[V].
 * @tparam P the property type (a mutable property currently only supported by the Vertex type).
 */
trait OrderedVertexMap[V, X <: EdgeLike[V], P] extends BaseVertexMap[V, X, P] {

    /**
     * This method adds an edge y (Y) to this OrderedVertexMap and returns
     * a tuple formed from the new vertex and the new VertexMap.
     * This is particularly used in Prim's algorithm (and maybe Dijkstra's algorithm, too).
     *
     * @param y the edge to be added.
     * @return a tuple as described above.
     */
    def addEdgeWithVertex(y: X): (Some[V], OrderedVertexMap[V, X, P]) = {
        val (v1, v2) = y.vertices
        val (in, out) = if (contains(v1)) (v1, v2) else (v2, v1)
        // TODO eliminate this asInstanceOf
        Some(out) -> addVertex(out).addEdge(in, y).asInstanceOf[OrderedVertexMap[V, X, P]]
    }
}

/**
 * Case class to represent an ordered VertexMap.
 * that's to say a VertexMap where V is ordered, typically used for an ConcreteUndirectedGraph).
 * The ordering is based on the key (V) type.
 *
 * @param map a TreeMap of V -> Vertex[V, X].
 * @tparam V the (key) vertex-attribute type.
 *           Requires implicit evidence of type Ordering[V].
 * @tparam X the type of edge which connects two vertices. A sub-type of EdgeLike[V].
 * @tparam P the property type (a mutable property currently only supported by the Vertex type).
 */
case class OrderedVertexMapCase[V: Ordering, X <: EdgeLike[V], P](map: TreeMap[V, Vertex[V, X, P]]) extends BaseVertexMap(map) with OrderedVertexMap[V, X, P] {

    /**
     * Method to construct a new OrderedVertexMapCase from the given map.
     *
     * @param map a TreeMap. If it is not a TreeMap, it will be converted to one.
     * @return a new OrderedVertexMapCase[V, X].
     */
    def unit(map: Map[V, Vertex[V, X, P]]): VertexMap[V, X, P] = {
        val zz: TreeMap[V, Vertex[V, X, P]] = map.to(TreeMap)
        OrderedVertexMapCase[V, X, P](zz)
    }

    def deriveProperty(v: V, x: X): Option[P] = None

    /**
     * Method to add an edge to this VertexMap.
     *
     * CONSIDER re-writing this (without super implementation).
     *
     * @param v the (key) value of the vertex whose adjacency list we are adding to.
     * @param y the edge to be added to the adjacency list.
     * @return a new VertexMap which includes all the original entries of <code>this</code> plus <code>v -> x</code>.
     */
    override def addEdge(v: V, y: X): OrderedVertexMap[V, X, P] = super.addEdge(v, y).asInstanceOf[OrderedVertexMap[V, X, P]]
}

/**
 * Companion object to OrderedVertexMapCase.
 */
object OrderedVertexMap {
    /**
     * Method to create an OrderedVertexMap with exactly one vertex in it.
     *
     * @param v the V object to insert into an empty OrderedVertexMap.
     * @tparam V the (key) vertex-attribute type.
     *           Requires implicit evidence of type Ordering[V].
     * @tparam X the type of edge which connects two vertices. A sub-type of EdgeLike[V].
     * @tparam P the property type (a mutable property currently only supported by the Vertex type).
     * @return a new OrderedVertexMap[V,X,P] with exactly one vertex (V) in it: the given value v.
     */
    def apply[V: Ordering, X <: EdgeLike[V], P](v: V): VertexMap[V, X, P] = empty[V, X, P].addVertex(v)

    /**
     * Method to yield an empty OrderedVertexMapCase.
     *
     * @tparam V the (key) vertex-attribute type.
     *           Requires implicit evidence of type Ordering[V].
     * @tparam X the type of edge which connects two vertices. A sub-type of EdgeLike[V].
     * @tparam P the property type (a mutable property currently only supported by the Vertex type).
     * @return an empty OrderedVertexMapCase[V, X].
     */
    def empty[V: Ordering, X <: EdgeLike[V], P]: OrderedVertexMap[V, X, P] = OrderedVertexMapCase(TreeMap.empty[V, Vertex[V, X, P]])
}

trait UnorderedVertexMap[V, X <: EdgeLike[V], P] extends BaseVertexMap[V, X, P]

/**
 * Case class to represent an unordered VertexMap,
 * that's to say a VertexMap where V is unordered, typically used for a ConcreteDirectedGraph).
 *
 * @param map a HashMap of V -> Vertex[V, X].
 * @tparam V the (key) vertex-attribute type.
 * @tparam X the type of edge which connects two vertices. A sub-type of EdgeLike[V].
 * @tparam P the property type (a mutable property currently only supported by the Vertex type).
 */
case class UnorderedVertexMapCase[V, X <: EdgeLike[V], P](map: HashMap[V, Vertex[V, X, P]]) extends BaseVertexMap[V, X, P](map) with UnorderedVertexMap[V, X, P] {

    /**
     * Method to construct a new UnorderedVertexMapCase from the given map.
     *
     * @param map a HashMap. If it is not a HashMap, it will be converted to one.
     * @return a new UnorderedVertexMapCase[V, X].
     */
    def unit(map: Map[V, Vertex[V, X, P]]): VertexMap[V, X, P] = UnorderedVertexMapCase[V, X, P](map.to(HashMap))

    /**
     * CONSIDER making this the default behavior for deriveProperty.
     *
     * @param v a vertex.
     * @param x an edge.
     * @return an Option[P].
     */
    def deriveProperty(v: V, x: X): Option[P] = Some(x match {
        case z: P => z // TESTME and NOTE that P is unchecked.
        case _ => throw GraphException(s"types P and X are not the same")
    })

    /**
     * Method to add an edge to this VertexMap.
     *
     * @param v the (key) value of the vertex whose adjacency list we are adding to.
     * @param y the edge to be added to the adjacency list.
     * @return a new VertexMap which includes all the original entries of <code>this</code> plus <code>v -> x</code>.
     */
    override def addEdge(v: V, y: X): UnorderedVertexMap[V, X, P] = super.addEdge(v, y).asInstanceOf[UnorderedVertexMap[V, X, P]]
}

/**
 * Companion object to UnorderedVertexMapCase.
 */
object UnorderedVertexMap {
    /**
     * Method to create an UnorderedVertexMap with exactly one vertex in it.
     *
     * @param v the V object to insert into an empty UnorderedVertexMap.
     * @tparam V the (key) vertex-attribute type.
     * @tparam X the type of edge which connects two vertices. A sub-type of EdgeLike[V].
     * @tparam P the property type (a mutable property currently only supported by the Vertex type).
     * @return a new OrderedVertexMap[V,X,P] with exactly one vertex (V) in it: the given value v.
     */
    def apply[V, X <: EdgeLike[V], P](v: V): VertexMap[V, X, P] = empty[V, X, P].addVertex(v)

    /**
     * Method to yield an empty UnorderedVertexMapCase.
     *
     * @tparam V the (key) vertex-attribute type.
     * @tparam X the type of edge which connects two vertices. A sub-type of EdgeLike[V].
     * @tparam P the property type (a mutable property currently only supported by the Vertex type).
     * @return an empty UnorderedVertexMapCase[V, X].
     */
    def empty[V, X <: EdgeLike[V], P]: UnorderedVertexMap[V, X, P] = UnorderedVertexMapCase(HashMap.empty[V, Vertex[V, X, P]])
}

/**
 * VertexMap based on VertexPair.
 *
 * CONSIDER do we really need this as a separate class hierarchy?
 *
 * @tparam V the (key) vertex-type of a graph.
 * @tparam P the property type (a mutable property currently only supported by the Vertex type).
 */
trait PairVertexMap[V, P] extends VertexMap[V, VertexPair[V], P]

/**
 * Case class to represent an unordered VertexMap,
 * that's to say a VertexMap where V is unordered, typically used for a ConcreteDirectedGraph).
 *
 * TODO Not used
 *
 * @param map a HashMap of V -> Vertex[V, X].
 * @tparam V the (key) vertex-attribute type.
 * @tparam P the property type (a mutable property currently only supported by the Vertex type).
 */
case class PairVertexMapCase[V, P](map: HashMap[V, Vertex[V, VertexPair[V], P]]) extends AbstractVertexMap[V, VertexPair[V], P](map) with PairVertexMap[V, P] {

    /**
     * Method to add a vertex of (key) type V to this graph.
     * The vertex will have degree of zero.
     *
     * @param v the (key) attribute of the result.
     * @return a new AbstractGraph[V, E, X].
     */
    def addVertex(v: V): VertexMap[V, VertexPair[V], P] = unit(_map + (v -> (_map.get(v) match {
        case Some(vv) => vv
        case None => Vertex.empty(v)
    })))

    /**
     * Method to add an edge to this VertexMap.
     *
     * @param v the (key) value of the vertex whose adjacency list we are adding to.
     * @param y the edge to be added to the adjacency list.
     * @return a new VertexMap which includes all the original entries of <code>this</code> plus <code>v -> x</code>.
     */
    def addEdge(v: V, y: VertexPair[V]): PairVertexMap[V, P] = unit(
        _map.get(v) match {
            case Some(vv) => buildMap(_map - v, v, y, vv)
            case None => buildMap(_map, v, y, Vertex.empty(v))
        }
    ).asInstanceOf[PairVertexMap[V, P]]

    /**
     * Build a VertexMap from the given map (m) and the edge y at vertex v.
     * TODO revert to private.
     *
     * @param m  the existing Map.
     * @param v  the vertex (key) at which to update the adjacency list.
     * @param y  the edge to be added.
     * @param vv the existing adjacency list for vertex v.
     * @tparam Y the type of the ege to be added.
     * @return a new Map.
     */
    def buildMap[Y >: VertexPair[V] <: EdgeLike[V]](m: Map[V, Vertex[V, Y, P]], v: V, y: Y, vv: Vertex[V, Y, P]): Map[V, Vertex[V, Y, P]] = m + (v -> (vv addEdge y))

    /**
     * Method to construct a new UnorderedVertexMapCase from the given map.
     *
     * @param map a HashMap. If it is not a HashMap, it will be converted to one.
     * @return a new UnorderedVertexMapCase[V, X].
     */
    def unit(map: Map[V, Vertex[V, VertexPair[V], P]]): VertexMap[V, VertexPair[V], P] = PairVertexMapCase[V, P](map.to(HashMap))

    def deriveProperty(v: V, x: VertexPair[V]): Option[P] = Some(x match {
        case z: P => z // TESTME and NOTE that P is unchecked.
        case _ => throw GraphException(s"types P and X are not the same")
    })
}

/**
 * Companion object to PairVertexMapCase.
 *
 * NOTE: not used
 */
object PairVertexMap {
    /**
     * Method to create an PairVertexMap with exactly one vertex in it.
     *
     * @param v the V object to insert into an empty PairVertexMap.
     * @tparam V the (key) vertex-attribute type.
     * @tparam P the property type (a mutable property currently only supported by the Vertex type).
     * @return a new PairVertexMap[V,P] with exactly one vertex (V) in it: the given value v.
     */
    def apply[V, P](v: V): VertexMap[V, VertexPair[V], P] = empty[V, P].addVertex(v)

    /**
     * Method to yield an empty UnorderedVertexMapCase.
     *
     * @tparam V the (key) vertex-attribute type.
     * @tparam P the property type (a mutable property currently only supported by the Vertex type).
     * @return an empty UnorderedVertexMapCase[V, X].
     */
    def empty[V, P]: PairVertexMap[V, P] = PairVertexMapCase(HashMap.empty[V, Vertex[V, VertexPair[V], P]])
}

/**
 * Abstract base class to define general VertexMap properties.
 *
 * @param _map a Map of V -> Vertex[V, X].
 * @tparam V the (key) vertex-attribute type.
 * @tparam P the property type (a mutable property currently only supported by the Vertex type).
 * @tparam X the type of edge which connects two vertices. A sub-type of EdgeLike[V].
 */
abstract class AbstractVertexMap[V, X <: EdgeLike[V], P](val _map: Map[V, Vertex[V, X, P]]) extends VertexMap[V, X, P] {

    require(_map != null, "BaseVertexMap: _map is null")

    private val flog: Flog = Flog[AbstractVertexMap[V, X, P]]

    import flog._

    def contains(v: V): Boolean = _map.contains(v)

    def size: Int = _map.size

    /**
     * Method to get the AdjacencyList for vertex with key (attribute) v, if there is one.
     *
     * @param v the key (attribute) of the vertex whose adjacency list we require.
     * @return an Option of AdjacencyList[X].
     */
    def optAdjacencyList(v: V): Option[AdjacencyList[X]] = _map.get(v) map (_.adjacent)

    /**
     * The map of V -> Vertex[V, X] elements.
     */
    val vertexMap: Map[V, Vertex[V, X, P]] = _map

    /**
     * Method to yield the Vertex at v.
     *
     * @param v (a V) which is the vertex to look up.
     * @return Option of Vertex[V, X, P].
     */
    def get(v: V): Option[Vertex[V, X, P]] = _map.get(v)

    /**
     * the vertex-type values, i.e. the keys, of this VertexMap.
     */
    val keys: Iterable[V] = _map.keys

    /**
     * the Vertex[V, X] values of this VertexMap.
     */
    def values: Iterable[Vertex[V, X, P]] = _map.values

    /**
     * the X values of this VertexMap.
     */
    val edges: Iterable[X] = _map.values.flatMap(_.adjacent.xs)

    /**
     * Method to run depth-first-search on this VertexMap.
     *
     * @param visitor the visitor, of type Visitor[V, J].
     * @param v       the starting vertex.
     * @tparam J the journal type.
     * @return a new Visitor[V, J].
     */
    def dfs[J](visitor: Visitor[V, J])(v: V): Visitor[V, J] = {
        initializeVisits(v)
        val result = recursiveDFS(visitor, v)
        result.close()
        result
    }

    /**
     * Method to run breadth-first-search on this VertexMap.
     *
     * @param visitor the visitor, of type Visitor[V, J].
     * @param v       the starting vertex.
     * @tparam J the journal type.
     * @return a new Visitor[V, J].
     */
    def bfs[J](visitor: Visitor[V, J])(v: V): Visitor[V, J] = {
        initializeVisits(v)
        implicit object queuable extends QueueableQueue[V]
        val result: Visitor[V, J] = doBFSImmutable[J, Queue[V]](visitor, v)
        result.close()
        result
    }

    /**
     * Method to run breadth-first-search with a mutable queue on this Traversable.
     *
     * @param visitor the visitor, of type Visitor[V, J].
     * @param v       the starting vertex.
     * @tparam J the journal type.
     * @tparam Q the type of the mutable queue for navigating this Traversable.
     *           Requires implicit evidence of MutableQueueable[Q, V].
     * @return a new Visitor[V, J].
     */
    def bfsMutable[J, Q](visitor: Visitor[V, J])(v: V)(implicit ev: MutableQueueable[Q, V]): Visitor[V, J] = {
        initializeVisits(v)
        val result: Visitor[V, J] = doBFSMutable[J, Q](visitor, v)
        result.close()
        result
    }

    /**
     * Non-tail-recursive method to run DFS on the vertex V with the given Visitor.
     *
     * @param visitor the Visitor[V, J].
     * @param v       the vertex at which we run depth-first-search.
     * @tparam J the Journal type of the Visitor.
     * @return a new Visitor[V, J].
     */
    private def recursiveDFS[J](visitor: Visitor[V, J], v: V): Visitor[V, J] =
        recurseOnVertex(v, visitor.visitPre(v)).visitPost(v)

    private def recurseOnVertex[J](v: V, visitor: Visitor[V, J]) = optAdjacencyList(v) match {
        case Some(xa) => xa.xs.foldLeft(visitor)((q, x) => recurseOnEdgeX(v, q, x))
        case None => throw GraphException(s"DFS logic error 0: recursiveDFS(v = $v)")
    }

    /**
     * This should not be a method of VertexMap but instead should be passed in to VertexMap (or maybe into GraphBuilderHelper).
     *
     * @param v a vertex.
     * @param x an edge.
     * @return an Option[P].
     */
    def deriveProperty(v: V, x: X): Option[P]

    private def recurseOnEdgeX[J](v: V, visitor: Visitor[V, J], y: X) = {
        s"recurseOnEdgeX: $v, $y" !!
                VertexMap.findAndMarkVertex(vertexMap, { w: Vertex[V, X, P] => w.setProperty(deriveProperty(v, y)) }, y.other(v), s"DFS logic error 1: findAndMarkVertex(v = $v, x = $y") match {
            case Some(z) => recursiveDFS(visitor, z)
            case None => visitor
        }
    }

    private def enqueueUnvisitedVertices[Q](v: V, queue: Q)(implicit queueable: Queueable[Q, V]): Q = optAdjacencyList(v) match {
        case Some(xa) => xa.xs.foldLeft(queue)((q, x) => queueable.appendAll(q, getVertices(v, x)))
        case None => throw GraphException(s"BFS logic error 0: enqueueUnvisitedVertices(v = $v)")
    }

    private def getVertices(v: V, y: X): Seq[V] = findAndMarkVertex(vertexMap, { _: Vertex[V, X, P] => () }, y.other(v), "getVertices").toSeq

    private def doBFSImmutableX[J, Q](visitor: Visitor[V, J], queue: Q)(implicit queueable: Queueable[Q, V]): Visitor[V, J] = {
        @tailrec
        def inner(result: Visitor[V, J], work: Q): Visitor[V, J] = queueable.take(work) match {
            case Some((head, tail)) => inner(result.visitPre(head), enqueueUnvisitedVertices(head, tail))
            case _ => result
        }

        inner(visitor, queue)
    }

    private def doBFSImmutable[J, Q](visitor: Visitor[V, J], v: V)(implicit queueable: Queueable[Q, V]): Visitor[V, J] =
    // CONSIDER inlining this method
        doBFSImmutableX(visitor, queueable.append(queueable.empty, v))

    private def doBFSMutableX[J, Q](visitor: Visitor[V, J], queue: Q)(implicit queueable: MutableQueueable[Q, V]): Visitor[V, J] = {
        @tailrec
        def inner(result: Visitor[V, J], work: Q): Visitor[V, J] = {
            queueable.take(work) match {
                case Some(v) => inner(result.visitPre(v), enqueueMutableUnvisitedVertices(v, work))
                case _ => result
            }
        }

        inner(visitor, queue)
    }

    private def doBFSMutable[J, Q](visitor: Visitor[V, J], v: V)(implicit queueable: MutableQueueable[Q, V]): Visitor[V, J] = {
        val queue: Q = queueable.empty
        queueable.append(queue, v)
        // CONSIDER inlining this method
        doBFSMutableX(visitor, queue)
    }

    private def enqueueMutableUnvisitedVertices[Q](v: V, queue: Q)(implicit queueable: MutableQueueable[Q, V]): Q = optAdjacencyList(v) match {
        case Some(xa) => xa.xs.foldLeft(queue) { (q, x) => queueable.appendAll(q, getVertices(v, x)); queue }
        case None => throw GraphException(s"BFS logic error 0: enqueueUnvisitedVertices(v = $v)")
    }

    private def initializeVisits[J](v: V): Unit = {
        vertexMap.values foreach (_.reset())
        VertexMap.findAndMarkVertex(vertexMap, { _: Vertex[V, X, P] => () }, Some(v), s"initializeVisits")
    }
}

/**
 * Abstract base class to define general VertexMap properties.
 *
 * CONSIDER merge into AbstractVertexMap
 *
 * @param __map a Map of V -> Vertex[V, X].
 * @tparam V the (key) vertex-attribute type.
 * @tparam X the type of edge which connects two vertices. A sub-type of EdgeLike[V].
 */
abstract class BaseVertexMap[V, X <: EdgeLike[V], P](val __map: Map[V, Vertex[V, X, P]]) extends AbstractVertexMap[V, X, P](__map) {

    require(_map != null, "BaseVertexMap: _map is null")

    /**
     * Method to add a vertex of (key) type V to this graph.
     * The vertex will have degree of zero.
     *
     * @param v the (key) attribute of the result.
     * @return a new AbstractGraph[V, E, X].
     */
    def addVertex(v: V): VertexMap[V, X, P] = unit(_map + (v -> (_map.get(v) match {
        case Some(vv) => vv
        case None => Vertex.empty(v)
    })))

    /**
     * Method to add an edge to this VertexMap.
     *
     * @param v the (key) value of the vertex whose adjacency list we are adding to.
     * @param y the edge to be added to the adjacency list.
     * @return a new VertexMap which includes all the original entries of <code>this</code> plus <code>v -> x</code>.
     */
    def addEdge(v: V, y: X): VertexMap[V, X, P] = unit(
        _map.get(v) match {
            case Some(vv) => buildMap(_map - v, v, y, vv)
            case None => buildMap(_map, v, y, Vertex.empty(v))
        }
    )

    /**
     * (abstract) Method to construct a new VertexMap from the given map.
     *
     * @param map a Map (might be TreeMap or HashMap).
     * @return a new VertexMap[V, X].
     */
    def unit(map: Map[V, Vertex[V, X, P]]): VertexMap[V, X, P]

    /**
     * Build a VertexMap from the given map (m) and the edge y at vertex v.
     * TODO revert to private.
     *
     * @param m  the existing Map.
     * @param v  the vertex (key) at which to update the adjacency list.
     * @param y  the edge to be added.
     * @param vv the existing adjacency list for vertex v.
     * @return a new Map.
     */
    def buildMap(m: Map[V, Vertex[V, X, P]], v: V, y: X, vv: Vertex[V, X, P]): Map[V, Vertex[V, X, P]] = m + (v -> (vv addEdge y))
}
