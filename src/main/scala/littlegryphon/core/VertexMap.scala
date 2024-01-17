package littlegryphon.core

import littlegryphon.visit.Visitor

case class VertexMap[V](map: Map[V, Vertex[V]]) {

  /**
   * Method to run depth-first-search on this VertexMap.
   *
   * @param visitor the visitor, of type Visitor[V, J].
   * @param v       the starting vertex.
   * @tparam J the journal type.
   * @return a new Visitor[V, J].
   */
  def dfs[J](visitor: Visitor[V, J])(v: V)(implicit discoverable: Discoverable[Vertex[V]]): Visitor[V, J] = {
    val vertex: Option[Vertex[V]] = initializeVisits(Some(v))
    vertex match {
      case None => println(s"dfs: v=$v unknown"); visitor
      case Some(x) =>
        val result = recursiveDFS(visitor, x)
        result.close()
        result
    }
  }

  def get(key: V): Option[Vertex[V]] = map.get(key)

//  def getOrElse[V1 >: Bag[Connexion[V]]](key: V, default: => V1): V1 = connexions.getOrElse(key, default)

  @throws[NoSuchElementException]
  def apply(key: V): Vertex[V] =
    map.apply(key)

//  def applyOrElse[K1 <: V, V1 >: Bag[Connexion[V]]](x: K1, default: K1 => V1): V1 = connexions.applyOrElse(x, default)

  def +[V1 >: Vertex[V]](kv: (V, V1)): Map[V, V1] = map.+(kv)

//  def keySet: Set[V] = connexions.keySet

  private def initializeVisits[J](vo: Option[V])(implicit discoverable: Discoverable[Vertex[V]]): Option[Vertex[V]] = {
    map.values foreach (discoverable.setDiscovered(_, b = false))
    val result: Option[Vertex[V]] = for {v <- vo; vertex <- map.get(v)} yield vertex
    result foreach (vertex => discoverable.setDiscovered(vertex, b = true))
    result
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
  private def recursiveDFS[J](visitor: Visitor[V, J], v: Vertex[V]): Visitor[V, J] =
    recurseOnVertex(v, visitor.visitPre(v.attribute)).visitPost(v.attribute)

  private def recurseOnVertex[J](v: Vertex[V], visitor: Visitor[V, J]) =
    v.connexions.foldLeft(visitor)((q, x) => recurseOnConnexion(v, q, x))


  private def recurseOnConnexion[J](v: Vertex[V], visitor: Visitor[V, J], vwx: Connexion[Vertex[V]]): Visitor[V, J] =
//    s"recurseOnEdgeX: $v, $x" !!
    markUndiscoveredVertex({ w => w.discovered = true }, vwx.v2) match {
      case Some(z) => recursiveDFS(visitor, z)
      case None => visitor
    }

  private def markUndiscoveredVertex(f: Vertex[V] => Unit, vw: Vertex[V]) = {
    val vwo: Option[Vertex[V]] = Option.when(vw.discovered)(vw)
    vwo foreach f
    vwo
  }
}

object VertexMap {
  def empty[V]: VertexMap[V] = new VertexMap[V](Map.empty)
}

trait Bag[+X] extends Iterable[X]

object Bag {
  def empty: Bag[Nothing] = ListBag.apply
}

case class ListBag[+X](xs: Seq[X]) extends Bag[X] {
  def iterator: Iterator[X] = xs.iterator
}

object ListBag {
  def apply: ListBag[Nothing] = apply(Nil)
}