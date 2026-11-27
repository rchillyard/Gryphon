package com.phasmidsoftware.gryphon.traverse

import com.phasmidsoftware.gryphon.adjunct.{AttributedDirectedEdge, DirectedEdge, UndirectedEdge}
import com.phasmidsoftware.gryphon.core
import com.phasmidsoftware.gryphon.core.{Connexion, Edge, GraphException}
import scala.util.Random

/**
 * A trait that defines the behavior of traversal over vertices and edges within a graph-like structure.
 *
 * @tparam V the type representing a vertex in the graph.
 * @tparam T the resulting type after traversing a vertex or an edge.
 */
trait TraversalResult[V, T] {
  /**
   * Returns the number of key-value pairs in the map.
   *
   * @return the size of the map as an integer
   */
  def size: Int

  /**
   * Retrieves the set of keys from the underlying map that represents the traversal result.
   *
   * @return a set of keys of type `V` contained in the underlying map.
   */
  def keySet: Set[V]

  /**
   * Traverses a given vertex in the graph and returns the result of the traversal.
   *
   * @param v the vertex to be traversed.
   * @return the result of the traversal for the provided vertex.
   */
  def vertexTraverse(v: V): Option[T]
}

/**
 * Provides graph traversal functionality using depth-first search (DFS) for vertex traversal.
 */
object TraversalResult {
  /**
   * Performs a traversal over the edges of a graph and applies a function to each edge, returning the results
   * as a `TraversalResult` object.
   *
   * @param f           a function applied to each edge, transforming each `Edge[V, E]` into a value of type `T`
   * @param traversable the graph or data structure implementing the `core.Traversable` interface over edge type `E`
   * @tparam V the type of vertices connected by the edges in the graph
   * @tparam E the type of the attributes associated with the edges in the graph
   * @tparam T the resulting type after applying the function `f` to each edge
   * @return an `AbstractEdgeTraversalResult[T]` containing the results of applying `f` to each edge of the graph.
   *         NOTE that the static type is the edge-keyed one, not `TraversalResult`: `edgeTraverse` lives only on
   *         that branch, so returning the general type would hide the one method callers want (#71).
   */
  def edgeTraversal[V, E, T](f: Edge[V, E] => T)(traversable: core.EdgeTraversable[V, E]): AbstractEdgeTraversalResult[T] =
    EdgeTraversalResult(traversable.edges.foldLeft(List[T]())((list, edge) => f(edge) :: list))

  /**
   * The edges of the path ending at `target`, in order from the root.
   *
   * Several results in this library are parent maps — each reached vertex against the edge by
   * which it was reached — which is the `edgeTo[]` array of Sedgewick & Wayne's `DepthFirstPaths`
   * and `BreadthFirstPaths`. The structure was here and the walk back along it was not, so every
   * caller wanting a path had to write the walk. Issue #112.
   *
   * One implementation serves all of them, because they share the shape rather than the type:
   * `Connexions` from `getConnexions`, the `VertexTraversalResult` from `ShortestPaths.dijkstra`,
   * and the one from `ShortestPaths.acyclic` and `longestPaths`.
   *
   * @param result the parent map to walk.
   * @param target the vertex to walk back from.
   * @tparam V the vertex type.
   * @tparam E the edge attribute type.
   * @tparam X the concrete edge type the result holds.
   * @return the edges from the root to `target`, nearest the root first; empty when `target` has
   *         no recorded incoming edge.
   */
  def pathEdgesTo[V, E, X <: Edge[V, E]](result: TraversalResult[V, X])(target: V): Seq[X] =
    // The visited set is not decoration. Every map this library produces is acyclic -- a vertex is
    // recorded once, at first discovery -- but the TYPE permits a cycle, and these are public case
    // classes anybody can construct. Without the guard a hand-built map would hang here, which is
    // the failure mode #117 has just shown is worth taking seriously.
    @scala.annotation.tailrec
    def loop(v: V, acc: List[X], seen: Set[V]): List[X] =
      if seen.contains(v) then acc
      else
        result.vertexTraverse(v) match
          case None => acc
          case Some(edge) => loop(edge.white, edge :: acc, seen + v)

    loop(target, Nil, Set.empty)

  /**
   * The vertices of the path ending at `target`, in order from the root.
   *
   * Always ends with `target`, so the result is never empty. A single-element `Seq(target)` means
   * `target` has no recorded incoming edge — which is either that it is the root, or that it was
   * never reached.
   *
   * **Those two cases are indistinguishable from a parent map alone**, because the root is simply
   * absent from the map: it is the one reached vertex with no incoming edge. A caller who needs to
   * tell them apart must compare against the start vertex it passed in. That is a real limitation
   * of the representation rather than of this method, and is part of what
   * [[https://github.com/rchillyard/Gryphon/issues/76 #76]] is about.
   *
   * @param result the parent map to walk.
   * @param target the vertex to walk back from.
   * @tparam V the vertex type.
   * @tparam E the edge attribute type.
   * @tparam X the concrete edge type the result holds.
   * @return the vertices from the root to `target` inclusive.
   */
  def pathTo[V, E, X <: Edge[V, E]](result: TraversalResult[V, X])(target: V): Seq[V] =
    pathEdgesTo[V, E, X](result)(target) match
      case Nil => Seq(target)
      case edges => edges.map(_.white) :+ target
}

/**
 * Abstract base class for edge traversal within a graph.
 *
 * This is the edge-keyed branch of the result type, and the only one carrying `edgeTraverse`.
 * `vertexTraverse` returns `None` here, since a vertex key means nothing to an edge-keyed
 * result; it does not throw.
 *
 * @tparam T the type representing the result of the edge traversal
 * @param ts a list of elements of type T representing traversal data, typically associated with edges
 */
abstract class AbstractEdgeTraversalResult[T](ts: List[T]) extends TraversalResult[Int, T] with Seq[T] {
  /**
   * Returns the number of key-value pairs in the map.
   *
   * @return the size of the map as an integer
   */
  // No `size` here: it is final in SeqOps, defined as `length`, which this class supplies below.
  // That inherited member also satisfies `TraversalResult.size`, so nothing is lost.

  /**
   * Retrieves the set of keys from the underlying map that represents the traversal result.
   *
   * @return a set of keys of type `V` contained in the underlying map.
   */
  def keySet: Set[Int] = Range(0, ts.size).toSet

  /**
   * Traverses a given vertex in the graph and returns the result of the traversal.
   *
   * @param v the vertex to be traversed.
   * @return the result of the traversal for the provided vertex.
   */
  def vertexTraverse(v: Int): Option[T] = None

  /**
   * Traverses a specified edge within a graph-like structure and produces a result of type T.
   *
   * @param x the index of the edge to be traversed
   * @return the result of the traversal
   */
  def edgeTraverse(x: Int): T = ts(x)

  /**
   * The results in order. Extending `Iterable[T]` is what makes this branch usable, and was a
   * precondition of #130.
   *
   * Before it, the branch offered only `size`, `keySet` (the indices) and `edgeTraverse(i)` —
   * positional, O(n) into a `List`. Moving `MST.kruskal` and `MST.boruvka` onto it would then
   * have cost every caller the `mst.map(_.attribute).sum` they all write: 46 sites in the test
   * suite alone needed `map`, `exists` or `head`. Adding `.toSeq` at each of them would have been
   * churn advertising an awkward type. `Seq` gives all of those directly — and `sorted`, which
   * `Iterable` does not — so the move is a gain in type precision at NO cost in usability, which
   * is the only basis on which it was worth making.
   *
   * It also makes #130 very nearly source-compatible: a caller who had a `Seq[Edge[V, E]]` from
   * `MST.kruskal` keeps every operation they were using. Only the declared type changed.
   */
  def iterator: Iterator[T] = ts.iterator

  /**
   * Positional access, as `Seq` requires — and the same operation as `edgeTraverse`, which is
   * what the edge-keyed branch has always meant by a key. The two are deliberately the same.
   */
  def apply(i: Int): T = ts(i)

  /** The number of results, as `Seq` requires. */
  def length: Int = ts.size
}

/**
 * Abstract base class for edge traversal within a graph.
 *
 * This is the edge-keyed branch of the result type, and the only one carrying `edgeTraverse`.
 * `vertexTraverse` returns `None` here, since a vertex key means nothing to an edge-keyed
 * result; it does not throw.
 *
 * @tparam V the type representing a vertex in the graph
 * @tparam T the type representing the result of the edge traversal
 * @param map the underlying map of vertex -> T elements
 */
abstract class AbstractVertexTraversalResult[V, T](map: Map[V, T]) extends TraversalResult[V, T] {
  /**
   * Returns the number of key-value pairs in the map.
   *
   * @return the size of the map as an integer
   */
  def size: Int = map.size

  /**
   * Retrieves the set of keys from the underlying map that represents the traversal result.
   *
   * @return a set of keys of type `V` contained in the underlying map.
   */
  def keySet: Set[V] = map.keySet

  /**
   * Traverses the specified vertex in the graph and retrieves the associated traversal result
   * from the underlying map. If the vertex does not exist, a `NoSuchElementException` is thrown.
   *
   * @param v the vertex to be traversed and looked up in the map.
   * @return the traversal result of type `T` associated with the provided vertex.
   * @throws NoSuchElementException if the vertex does not exist in the map.
   */
  def vertexTraverse(v: V): Option[T] =
    map.get(v)

  /**
   * Constructs a traversal instance using the provided map of vertices and their associated traversal results.
   *
   * @param map a mapping where keys represent vertices of type `V`, and values represent traversal results of type `T`.
   * @return a new `TraversalResult[V, T]` instance built from the specified map.
   */
  def unit(map: Map[V, T]): TraversalResult[V, T]

  /**
   * Adds a new mapping of a vertex to its corresponding traversal result to the existing map,
   * returning a new `VertexTraversalResult` instance with the updated mapping.
   *
   * @param t a tuple where the first element is a vertex of type `V`, and the second element is its associated
   *          traversal result of type `T`.
   *
   * @return a new `VertexTraversalResult` instance of type `VertexTraversalResult[V, E, T]` containing the updated map.
   */
  def +(t: (V, T)): TraversalResult[V, T] =
    unit(map + t)
}

/**
 * A case class representing the concrete implementation of edge traversal for a graph-like structure.
 *
 * This class enables the traversal of edges by providing a list of precomputed results of type `T`
 * associated with each edge.
 *
 * Edge traversal uses the defined behavior from the `AbstractEdgeTraversalResult` base class,
 * where `vertexTraverse` returns `None`, and `edgeTraverse` retrieves the traversal result
 * from the precomputed list based on the edge index.
 *
 * @tparam V the type representing a vertex in the graph.
 * @tparam E the type representing an edge in the graph.
 * @tparam T the type of results for the edge traversal.
 * @param ts a list of elements of type `T` representing the computed results of edge traversal.
 */
case class EdgeTraversalResult[V, E, T](ts: List[T]) extends AbstractEdgeTraversalResult[T](ts)

/**
 * A concrete implementation of the `TraversalResult` trait that uses a map to represent
 * vertex traversals. Each vertex in the graph is associated with a traversal result.
 *
 * @constructor Creates a `VertexTraversalResult` with the given map of vertices and their associated traversal outputs.
 * @param map A map where each key is a vertex of type `V`, and the value is the corresponding traversal result of type `T`.
 * @tparam V The type representing a vertex in the graph.
 * @tparam T The resulting type after traversing a vertex or an edge.
 */
/**
  * A decomposition of a graph's vertices into components, each labelled with an integer id.
  *
  * Two algorithms produce one of these: `ConnectedComponents.components` for weak connectivity
  * and `StronglyConnectedComponents.kosaraju` for strong. They answer the same KIND of question,
  * and before 1.15.0 they shared no type at all -- one returned a rich result, the other a bare
  * `Map`. See #168.
  *
  * ==What this type is really for: saying which view to trust==
  *
  * It exists less to share the one-line `partition` than to state a contract that is subtle,
  * identical for both, and easy to get wrong in different ways on each:
  *
  *  - `partition` is '''a property of the graph'''. Which vertices share a component does not depend
  *    on how the search ran. Invariant, without qualification. '''Compare this.'''
  *  - `map` is '''a labelling''', and its stability is an implementation detail rather than a promise.
  *    For connected components it happens not to vary, because ids are handed out by walking a
  *    `Set`. For strongly connected components it '''does''' vary -- measured over forty seeds on a
  *    DAG of singleton components: two distinct numberings, one grouping. '''Do not compare this.'''
  *
  * That asymmetry is exactly the sort of thing that gets documented on one type and forgotten on
  * the other. Here it is written once, and `partition` is visibly the member the contract is about.
  *
  * ==A trap inherited from above==
  *
  * `size` comes from [[AbstractVertexTraversalResult]] and counts '''vertices''', as on every
  * `TraversalResult`. On a type about components it is tempting to read it as the component
  * count. That is `partition.size`.
  *
  * @param map every vertex against its component id (0, 1, 2, ...).
  * @tparam V the vertex type.
  */
abstract class AbstractComponentResult[V](map: Map[V, Int]) extends AbstractVertexTraversalResult[V, Int](map) {

  /**
    * The components themselves, as a partition of the vertices.
    *
    * Derived from `map` rather than stored beside it, so the two cannot drift apart. The number
    * of components is `partition.size`.
    *
    * @return one `Set[V]` per component.
    */
  def partition: Set[Set[V]] = map.groupBy((_, id) => id).values.map(_.keySet).toSet
}

case class VertexTraversalResult[V, T](map: Map[V, T]) extends AbstractVertexTraversalResult[V, T](map) {
  /**
   * Adds a new mapping of a vertex to its corresponding traversal result to the existing map,
   * returning a new `VertexTraversalResult` instance with the updated mapping.
   *
   * @param t a tuple where the first element is a vertex of type `V`, and the second element is its associated
   *          traversal result of type `T`.
   *
   * @return a new `VertexTraversalResult` instance of type `VertexTraversalResult[V, E, T]` containing the updated map.
   */
  override def +(t: (V, T)): VertexTraversalResult[V, T] =
    unit(map + t)

  /**
   * Creates a new traversal instance based on the provided map of vertices and their associated traversal results.
   *
   * @param map a map where the keys are vertices of type `V` and the values are the associated traversal results of type `T`.
   * @return a new traversal instance of type `TraversalResult[V, T]` constructed with the given map.
   */
  // Declared as the concrete type, not TraversalResult[V, T]: `unit` is the single construction
  // point, so widening it here forced `+` to cast its own result straight back down. Same shape as
  // the Graph.unit bug fixed in 1.8.1.
  def unit(map: Map[V, T]): VertexTraversalResult[V, T] =
    VertexTraversalResult(map)
}

/**
 * Provides utility methods for creating and working with instances of `VertexTraversalResult`.
 *
 * A `VertexTraversalResult` is a concrete implementation of the `TraversalResult` trait
 * that uses a map to represent the traversal results for graph vertices.
 * This object contains a factory method for creating an empty `VertexTraversalResult` instance.
 */
object VertexTraversalResult {
  /**
   * Creates and returns an empty `VertexTraversalResult` instance.
   *
   * An empty `VertexTraversalResult` contains no mappings of vertices to their traversal results.
   * This method is useful as a starting point for building a traversal object incrementally.
   *
   * @tparam V The type representing a vertex in the graph.
   * @tparam T The resulting type after traversing a vertex or an edge.
   * @return A new `VertexTraversalResult` instance with an empty map.
   */
  def empty[V, T]: VertexTraversalResult[V, T] =
    VertexTraversalResult(Map.empty[V, T])
}

/**
 * Represents a particular traversal of vertices within a graph resulting from a depth-first-search
 * starting at a particular vertex.
 * Each vertex that is reached (not including the start vertex) is represented in the `connexions` `Map`
 * by a `DirectedEdge` where the "white" (first) vertex is the start of a followed edge and the "black" (second)
 * vertex will be the same as the key vertex.
 * NOTE that not all edges will be followed and different random seeds will result in different results.
 *
 * CONSIDER returning a directed, rooted tree of DirectedEdge. See issue #76, which is
 * sequenced after #53 (how this is computed) and #67 (which tree you get).
 *
 * @param connexions a map where each key is a vertex of type `V` and its value is a tuple `(V, V)`
 *                   representing the result associated with traversal from this vertex.
 *
 * @tparam V         the type representing a vertex in the graph.
 */
case class Connexions[V, E](connexions: Map[V, DirectedEdge[V, E]]) extends AbstractVertexTraversalResult[V, DirectedEdge[V, E]](connexions) {
  /**
   * Creates a new `TraversalResult` instance with the specified map of vertices and their associated traversal results.
   *
   * @param map a map where each key is a vertex of type `V` and its value is the `DirectedEdge[V, E]`
   *            by which that vertex was reached.
   *
   * @return a new `Connexions[V, E]` initialized with the provided map.
   */
  // As in VertexTraversalResult: the concrete type, so `+` need not cast.
  def unit(map: Map[V, DirectedEdge[V, E]]): Connexions[V, E] =
    copy(connexions = map)

  /**
   * Adds a connexion between the specified vertex and another vertex defined in the given `Connexion` instance.
   * If the `Connexion` is directed, it is added as-is. If the `Connexion` is undirected, it is converted into a directed connexion
   * pointing from the specified vertex to the other vertex before being added.
   *
   * @param v         the vertex to which the connexion is to be added.
   * @param connexion the `Connexion` instance representing the connexion details (directed or undirected) between the vertices.
   * @return a new `Connexions[V, E]` instance with the updated connexion added.
   * @throws com.phasmidsoftware.gryphon.core.GraphException if the provided connexion type is unexpected.
   */
  def addConnexion(v: V, connexion: Connexion[V]): Connexions[V, E] = connexion match {
    case d: AttributedDirectedEdge[V, E] @unchecked =>
      copy(connexions = connexions + (v -> d))
    case u: UndirectedEdge[V, E] @unchecked =>
      copy(connexions = connexions + (v -> AttributedDirectedEdge(u.attribute, u.other(v), v)))
    case _ =>
      throw GraphException(s"getConnexions: unexpected connexion: $connexion")
  }

  /**
   * Adds a new mapping of a vertex to its corresponding traversal result to the existing map,
   * returning a new `VertexTraversalResult` instance with the updated mapping.
   *
   * @param t a tuple where the first element is a vertex of type `V`, and the second element is its associated
   *          traversal result of type `T`.
   *
   * @return a new `VertexTraversalResult` instance of type `VertexTraversalResult[V, E, T]` containing the updated map.
   */
  override def +(t: (V, DirectedEdge[V, E])): Connexions[V, E] =
    unit(connexions + t)
}

/**
 * Provides utility methods for creating `Connexions` instances, which represent a specific
 * traversal of vertices within a graph resulting from a depth-first search starting at a
 * particular vertex.
 *
 * NOTE this object documented itself as being about `ProtoConnexions` until #133. That was a
 * different type entirely -- a `SerializableGraph` of vertex pairs, not a traversal result -- and
 * the similarity of the names was the whole of the confusion. #137 then removed it as a duplicate
 * of `VertexPairList`, so the name no longer resolves to anything; it is left here because the
 * note only makes sense if it says what the wrong name was.
 */
object Connexions {
  /**
   * Creates a `Connexions` instance representing a traversal of a graph using depth-first search (DFS).
   * The traversal starts from the specified vertex, processes each vertex using the provided function,
   * and returns the resulting connexions.
   *
   * @param graph  the graph-like structure to be traversed, of type `core.Traversable[V]`.
   * @param start  the starting vertex for the DFS traversal, of type `V`.
   * @param random the generator determining adjacency order, forwarded to `getConnexions`.
   *               Without it this method could not be seeded at all: a `given Random` at the
   *               call site stopped here, and the traversal inside used a fresh one (#67).
   *
   * @tparam V the type representing a vertex in the graph.
   * @tparam E the type representing the edge attribute in the traversal.
   * @return a `Connexions[V, E]` encapsulating the result of the traversal, mapping each reached
   *         vertex to the directed edge by which it was reached.
   */
  def create[V, E](graph: core.Traversable[V])(start: V)(using random: Random = Random()): Connexions[V, E] = {
    graph.getConnexions(start)
  }

  /**
   * Creates an empty `Connexions`, representing a traversal with no vertices or edges.
   *
   * @tparam V the type representing a vertex in the graph.
   * @return an empty `Connexions[V, E]`.
   */
  def empty[V, E]: Connexions[V, E] =
    Connexions(Map.empty[V, DirectedEdge[V, E]])
}
