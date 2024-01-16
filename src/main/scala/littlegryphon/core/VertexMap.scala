package littlegryphon.core

import littlegryphon.visit.Visitor

case class VertexMap[V](map: Map[V, Bag[Connexion[V]]]) {

  /**
   * Method to run depth-first-search on this VertexMap.
   *
   * @param visitor the visitor, of type Visitor[V, J].
   * @param v       the starting vertex.
   * @tparam J the journal type.
   * @return a new Visitor[V, J].
   */
  def dfs[J](visitor: Visitor[V, J])(v: V): Visitor[V, J] = {
    initializeVisits(Some(v))
    val result = recursiveDFS(visitor, v)
    result.close()
    result
  }

  def get(key: V): Option[Bag[Connexion[V]]] = map.get(key)

  def getOrElse[V1 >: Bag[Connexion[V]]](key: V, default: => V1): V1 = map.getOrElse(key, default)

  @throws[NoSuchElementException]
  def apply(key: V): Bag[Connexion[V]] = map.apply(key)

  def applyOrElse[K1 <: V, V1 >: Bag[Connexion[V]]](x: K1, default: K1 => V1): V1 = map.applyOrElse(x, default)

  def +[V1 >: Bag[Connexion[V]]](kv: (V, V1)): Map[V, V1] = map.+(kv)

  def keySet: Set[V] = map.keySet

  private def initializeVisits[J](vo: Option[V]): Unit = {
//    vertexMap.values foreach (_.reset())
//    VertexMap.findAndMarkVertex(vertexMap, { _: Vertex[V, X, P] => () }, vo, s"initializeVisits")
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

  private def recurseOnVertex[J](v: V, visitor: Visitor[V, J]) =
    get(v) match {
      case Some(xa) => xa.iterator.foldLeft(visitor)((q, x) => recurseOnConnexion(v, q, x))
      case None => throw littlegryphon.util.GraphException(s"DFS logic error 0: recursiveDFS(v = $v)")
    }


  private def recurseOnConnexion[J](v: V, visitor: Visitor[V, J], x: Connexion[V]): Visitor[V, J] = {
//    s"recurseOnEdgeX: $v, $x" !!
    findAndMarkVertex({ w => w.discovered = true }, x.v2, s"DFS logic error 1: findAndMarkVertex(v = $v, x = $x") match {
      case Some(z) => recursiveDFS(visitor, z)
      case None => visitor
    }
  }

  private def findAndMarkVertex(f: Vertex[V] => Unit, z: Vertex[V], errorMessage: String): Option[V] = {
    val xXvo: Option[Vertex[V]] = Option.when(z.discovered)(z)
    xXvo foreach f
    xXvo map (_.attribute)
  }
}

trait Bag[+X] extends Iterable[X]

case class ListBag[+X](xs: Seq[X]) extends Bag[X] {
  def iterator: Iterator[X] = xs.iterator
}