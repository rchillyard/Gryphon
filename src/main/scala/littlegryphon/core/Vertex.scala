package littlegryphon.core

/**
 * Represents a vertex in a graph structure with an associated attribute and a collection
 * of adjacencies.
 * Each vertex can optionally be marked as discovered during traversal, 
 * which is useful for graph algorithms.
 *
 * @tparam V the type representing the attribute of the vertex (invariant).
 * @param attribute   the data or value associated with this vertex.
 * @param adjacencies an unordered collection of adjacencies representing the connections
 *                    of this vertex to others in the graph.
 * @param discovered  an optional flag indicating whether this vertex has been discovered 
 *                    in a traversal.
 *                    Defaults to `false`.
 */
case class Vertex[V](attribute: V, adjacencies: Unordered[Adjacency[V]])(var discovered: Boolean = false) extends Attribute[V] {
        def +(a: Adjacency[V]): Vertex[V] = Vertex(attribute, adjacencies + a)(discovered)

        override def toString:String=s"v$attribute"
}

/**
 * Companion object for the `Vertex` class, providing a factory method for creating
 * instances of `Vertex`.
 * Designed to construct a vertex with an associated attribute
 * and a dynamically initialized collection of adjacencies.
 */
object Vertex {
        def create[V](f: () => Unordered[Adjacency[V]])(attribute: V): Vertex[V] = new Vertex[V](attribute, f())()
}
