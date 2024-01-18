package littlegryphon.core

import littlegryphon.core.VertexMap.addVertexPair
import littlegryphon.util.GraphException
import littlegryphon.visit.Visitor
import scala.util.Using

sealed trait Connectible[V] {

  /**
   * Method to add a connexion to this VertexMap.
   *
   * @param pair a Pair[V]
   * @return the updated Connectible[V].
   */
  def +(pair: Pair[V]): Connectible[V]

//  def +(vertexPair: VertexPair[V]): VertexMap[V]
}

case class VertexMap[V](map: Map[V, Vertex[V]]) extends Connectible[V] {
//  /**
//   * Method to add a VertexPair to this VertexMap.
//   *
//   * @param vertexPair a VertexPair[V].
//   * @return the updated VertexMap[V]
//   */
//  def +(vertexPair: VertexPair[V]): VertexMap[V] = copy(map = addVertexPair(map, vertexPair.v1, vertexPair.v2))

  /**
   * Method to add a connexion to this VertexMap.
   *
   * @param pair a Pair[V]
   * @return the updated VertexMap[V].
   */
  def +(pair: Pair[V]): VertexMap[V] = {

    val xo1: Option[Vertex[V]] = map.get(pair.v1)
    val x2: Vertex[V] = pair.v2

    val m: Map[V, Vertex[V]] = xo1 match {
      case Some(x1) =>
        addVertexPair(map, x1, x2)
      case None =>
        val x1: Vertex[V] = Vertex.create[V](pair.v1)
        addVertexPair(map, x1 + Pair[V](x1.attribute, x2), x2)

//        map + (x1 + VertexPair[V](x1, x2)).keyValue + x2.keyValue
    }

    copy(map = m)
  }

  private def keyValue(x: Vertex[V]): (V, Vertex[V]) = x.attribute -> x

  /**
   * Method to run depth-first-search on this VertexMap.
   *
   * @param visitor the visitor, of type Visitor[V, J].
   * @param v       the starting vertex.
   * @tparam J the journal type.
   * @return a new Visitor[V, J].
   */
  def dfs[J](visitor: Visitor[V, J])(v: V)(implicit discoverable: Discoverable[Vertex[V]]): Visitor[V, J] = {
    initializeVisits(Some(v)) match {
      case None => throw GraphException(s"dfs: starting vertex $v unknown")
      case Some(x) => Using.resource(recursiveDFS(visitor, x))(identity)
    }
  }

  def get(key: V): Option[Vertex[V]] = map.get(key)

  @throws[NoSuchElementException]
  def apply(key: V): Vertex[V] = map.apply(key)

  def +[V1 >: V](kv: (V1, Vertex[V1])): VertexMap[V1] = VertexMap[V1](map.asInstanceOf[Map[V1, Vertex[V1]]] + kv)

  private def initializeVisits[J](vo: Option[V])(implicit discoverable: Discoverable[Vertex[V]]): Option[Vertex[V]] = {
    val f: Boolean => Vertex[V] => Unit = b => v => discoverable.setDiscovered(v, b)
    map.values foreach f(false)
    val result = for {v <- vo; vertex <- map.get(v)} yield vertex
    result foreach f(true)
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
  private def recursiveDFS[J](visitor: Visitor[V, J], v: Vertex[V]): Visitor[V, J] =
    recurseOnVertex(v, visitor.visitPre(v.attribute)).visitPost(v.attribute)

  private def recurseOnVertex[J](v: Vertex[V], visitor: Visitor[V, J]) =
    v.connexions.foldLeft(visitor)((q, x) => recurseOnConnexion(v, q, x))


  private def recurseOnConnexion[J](v: Vertex[V], visitor: Visitor[V, J], vwx: Connexion[V, Vertex]): Visitor[V, J] =
    markUndiscoveredVertex({ w => w.discovered = true }, vwx.v2) match {
      case Some(z) => recursiveDFS(visitor, z)
      case None => visitor
    }

  private def markUndiscoveredVertex(f: Vertex[V] => Unit, vx: Vertex[V]) = {
    val vwo: Option[Vertex[V]] = Option.when(!vx.discovered)(vx)
    vwo foreach f
    vwo
  }

}

object VertexMap {
  def empty[V]: VertexMap[V] = new VertexMap[V](Map.empty)

  /**
   * Method to add a VertexPair to the given map.
   *
   * @param map a Map[V, Vertex of V].
   * @param x1  the first Vertex.
   * @param x2  the second Vertex.
   * @tparam V the underlying type.
   * @return an updated version of map.
   */
  def addVertexPair[V](map: Map[V, Vertex[V]], x1: Vertex[V], x2: Vertex[V]): Map[V, Vertex[V]] =
    map + (x1 + Pair[V](x1.attribute, x2)).keyValue + x2.keyValue

}
