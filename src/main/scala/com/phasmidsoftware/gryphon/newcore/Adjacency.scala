/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.newcore

import com.phasmidsoftware.gryphon.visit.{MutableQueueable, Visitor}
import scala.collection.immutable.{HashMap, TreeMap}


/**
 * Trait to model the concept of adjacency.
 * BaseAdjacency is central to the representation of graphs and trees.
 * By using adjacency lists (bags), we can traverse a graph in time O(|E|) where E is the number of edges.
 * In terms of the number (n) of vertices, traversal takes O(nx) where x is the mean degree.
 * By referring to the Erdös-Rényi model, we can show that x ~ log(n), thus graph traversal becomes O(n log(n)).
 * See [[https://en.wikipedia.org/wiki/Erdős–Rényi_model]].
 *
 * @tparam V the attribute type of a vertex (node).
 * @tparam X the edge (connexion) type.
 */
trait Adjacency[V, X <: Connexion[V], P] extends Traversable[V, P] {
  /**
   * Method to yield all the connexions (as a Bag) from the vertex (node) identified by v.
   *
   * @param v a V.
   * @return a Bag[X].
   */
  def adjacent(v: V): Connexions[V, X, P]

  /**
   * Method to add a new key-value pair of V->Bag[X] to this BaseAdjacency.
   *
   * @param vx a tuple of V -> Bag[X].
   * @return a new BaseAdjacency which includes the new key-value pair (vBx).
   */
  def +(vx: (V, Connexions[V, X, P])): Adjacency[V, X, P]

  /**
   * Method to add a new connexion X at V.
   *
   * @param v a node (vertex) identifier [V].
   * @param x a connexion [X].
   * @return a new BaseAdjacency which includes the connexion x at v.
   */
  def connect(v: V, x: X): Adjacency[V, X, P]

  /**
   * Method to construct a new BaseAdjacency based on this.
   *
   * @param map the map to be used.
   * @return an BaseAdjacency[V, X].
   */
  def unit(map: Map[V, Connexions[V, X, P]]): Adjacency[V, X, P]
}


/**
 * Trait to model the concept of adjacency.
 * BaseAdjacency is central to the representation of graphs and trees.
 * By using adjacency lists (bags), we can traverse a graph in time O(|E|) where E is the number of edges.
 * In terms of the number (n) of vertices, traversal takes O(nx) where x is the mean degree.
 * By referring to the Erdös-Rényi model, we can show that x ~ log(n), thus graph traversal becomes O(n log(n)).
 * See [[https://en.wikipedia.org/wiki/Erdős–Rényi_model]].
 *
 * @tparam V the attribute type of a vertex (node).
 * @tparam X the edge (connexion) type.
 */
trait BaseAdjacency[V, X <: Connexion[V]] extends Adjacency[V, X, Unit] {
  /**
   * Method to yield all the connexions (as a Bag) from the vertex (node) identified by v.
   *
   * @param v a V.
   * @return a Bag[X].
   */
  def adjacent(v: V): BaseConnexions[V, X]

  /**
   * Method to add a new key-value pair of V->Bag[X] to this BaseAdjacency.
   *
   * @param vx a tuple of V -> Bag[X].
   * @return a new BaseAdjacency which includes the new key-value pair (vBx).
   */
  def +(vx: (V, BaseConnexions[V, X])): BaseAdjacency[V, X]

  /**
   * Method to add a new connexion X at V.
   *
   * @param v a node (vertex) identifier [V].
   * @param x a connexion [X].
   * @return a new BaseAdjacency which includes the connexion x at v.
   */
  def connect(v: V, x: X): BaseAdjacency[V, X]

  /**
   * Method to construct a new BaseAdjacency based on this.
   *
   * @param map the map to be used.
   * @return an BaseAdjacency[V, X].
   */
  def unit(map: Map[V, BaseConnexions[V, X]]): BaseAdjacency[V, X]

  /**
   * Method to decorate this BaseAdjacency as a Adjacency.
   *
   * @param v the vertex/node identifier.
   * @param p the property.
   * @tparam P the property type.
   * @return
   */
  def adjacency[P](v: V, p: Unit => P): Adjacency[V, X, P]

}

abstract class AbstractBaseAdjacency[V, X <: Connexion[V]](map: Map[V, BaseConnexions[V, X]]) extends AbstractAdjacency[V, X, Unit](map) {
//
//  /**
//   * Method to run depth-first-search on this Traversable.
//   * Vertices will not be visited if they are not reachable from v.
//   *
//   * @param visitor the visitor, of type Visitor[V, J].
//   * @param v       the starting vertex.
//   * @tparam J the journal type.
//   * @return a new Visitor[V, J].
//   */
//  def dfs[J](visitor: Visitor[V, J])(v: V)(implicit evP: Properties[V, Unit]): Visitor[V, J] = {
//    val z: Adjacency[V, X, Unit] = adjacency(v, null) // TODO get this right
//    z.dfs(visitor)(v)
//  }

  /**
   * Method to yield all the connexions from the vertex (node) identified by v.
   *
   * @param v a V.
   * @return a Bag[X].
   */
  override def adjacent(v: V): BaseConnexions[V, X] = super.adjacent(v).asInstanceOf[BaseConnexions[V, X]]

  /**
   * Method to decorate this BaseAdjacency as a Adjacency.
   *
   * @param v the vertex/node identifier.
   * @param p the property.
   * @tparam P the property type.
   * @return
   */
  def adjacency[P: Initializable : Discoverable](v: V, p: Unit => P): Adjacency[V, X, P]

  // TESTME
  def adjacencyDFS[P: Initializable : Discoverable](v: V): Adjacency[V, X, Unit] =
    Adjacency(for ((v, c) <- map) yield v -> Vertex(v, c.connexions, implicitly[Initializable[P]].initialize))

}

abstract class AbstractAdjacency[V, X <: Connexion[V], P: Initializable : Discoverable](map: Map[V, Connexions[V, X, P]]) extends Adjacency[V, X, P] {
  /**
   * Method to yield all the connexions from the vertex (node) identified by v.
   *
   * @param v a V.
   * @return a Bag[X].
   */
  def adjacent(v: V): Connexions[V, X, P] = map(v)

  /**
   * Method to add a new key-value pair of V->Bag[X] to this BaseAdjacency.
   * Note that if the key value exists in this, its current bag will not be present in the result.
   *
   * @param vBx a tuple of V -> Bag[X].
   * @return a new BaseAdjacency which includes the new key-value pair (vBx).
   */
  def +(vBx: (V, Connexions[V, X, P])): Adjacency[V, X, P] = unit(map + vBx)

  /**
   * Method to add a new key-value pair of V->Bag[X] to this BaseAdjacency.
   * Note that if the key value exists in this, its current bag will not be present in the result.
   *
   * @param v a node (vertex) identifier [V].
   * @param x a connexion X.
   * @return a new BaseAdjacency which includes the new key-value pair (vBx).
   */
  def connect(v: V, x: X): Adjacency[V, X, P] = this + (v -> (map.getOrElse(v, Connexions.empty[V, X, P](implicitly[Initializable[P]].initialize)) + x))

  /**
   * Method to run depth-first-search on this Traversable.
   * Vertices will not be visited if they are not reachable from v.
   *
   * @param visitor the visitor, of type Visitor[V, J].
   * @param v       the starting vertex.
   * @param evP     (implicit) evidence of Properties[V,P].
   * @tparam J the journal type.
   * @return a new Visitor[V, J].
   */
  def dfs[J](visitor: Visitor[V, J])(v: V)(implicit evP: Properties[V, P]): Visitor[V, J] = {
    val result = recursiveDFS(visitor, v)
    result.close()
    result
  }

  /**
   * Method to get the AdjacencyList for vertex with key (attribute) v, if there is one.
   *
   * @param v the key (attribute) of the vertex whose adjacency list we require.
   * @return an Option of AdjacencyList[X].
   */
  def maybeConnexions(v: V): Option[Connexions[V, X, P]] = map.get(v)

  /**
   * Non-tail-recursive method to run DFS on the vertex V with the given Visitor.
   *
   * @param visitor the Visitor[V, J].
   * @param v       the vertex at which we run depth-first-search.
   * @tparam J the Journal type of the Visitor.
   * @return a new Visitor[V, J].
   */
  private def recursiveDFS[J](visitor: Visitor[V, J], v: V)(implicit evP: Properties[V, P]): Visitor[V, J] =
    recurseOnVertex(v, visitor.visitPre(v)).visitPost(v)

  private def recurseOnVertex[J](v: V, visitor: Visitor[V, J])(implicit evP: Properties[V, P]): Visitor[V, J] =
    maybeConnexions(v) match {
      case Some(xa) =>
        xa.connexions.iterator.foldLeft(visitor)((q, x) => recurseOnEdgeX(v, q, x))
      case None =>
        visitor
//        throw CoreException(s"DFS logic error 0: recursiveDFS(v = $v)")
    }

  private def recurseOnEdgeX[J](v: V, visitor: Visitor[V, J], y: X)(implicit evP: Properties[V, P]): Visitor[V, J] = {
    //    val dummy = { w: BaseConnexions[V, X] => w.setProperty(deriveProperty(v, y)) }
//    val f: (Connexions[V, X, P] => Unit) = {
//      case cp: Connexions[V, X, P] => {cp.property} // TODO need to update property
//      case c => throw CoreException(s"recurseOnEdgeX: ${c.getClass} is of wrong type--should be Connexions.")
//    }
//    AbstractBaseAdjacency.findAndMarkVertex(map, f, y.connexion(v), s"DFS logic error 1: findAndMarkVertex(v = $v, x = $y") match {
//      case Some(z) => recursiveDFS(visitor, z)
//      case None => visitor
//    }
val pd = implicitly[Discoverable[P]]

    val vo = for {
      z <- y.connexion(v)
      p = evP.getProperties(z)
      if !pd.isDiscovered(p)
      _ = pd.setDiscovered(p, b = true)
    } yield z

    vo match {
      case Some(v) => recurseOnVertex(v, visitor)
      case None => visitor
    }
  }
}
//
///**
// * Method to run depth-first-search on this Traversable, ensuring that every vertex is visited..
// *
// * @param visitor the visitor, of type Visitor[V, J].
// * @tparam J the journal type.
// * @return a new Visitor[V, J].
// */
//def dfsAll[J](visitor: Visitor[V, J]): Visitor[V, J] = ???
//
///**
// * Method to run breadth-first-search with a mutable queue on this Traversable.
// *
// * @param visitor the visitor, of type Visitor[V, J].
// * @param v       the starting vertex.
// * @tparam J the journal type.
// * @tparam Q the type of the mutable queue for navigating this Traversable.
// *           Requires implicit evidence of MutableQueueable[Q, V].
// * @return a new Visitor[V, J].
// */
//def bfsMutable[J, Q](visitor: Visitor[V, J])(v: V)(goal: V => Boolean)(implicit ev: MutableQueueable[Q, V]): Visitor[V, J] = ???
//}

// TESTME
object AbstractBaseAdjacency {
//  /**
//   * This method finds the vertex at the other end of x from v, checks to see if it is already discovered
//   * and, if not, marks it as discovered then returns it, wrapped in Some.
//   *
//   * @param connexionsMap the Map of V -> BaseConnexions[V, X, P] which represents the adjacencies of a graph.
//   * @param f             a Unit-function to be applied to the BaseConnexions corresponding to maybeV (if it exists).
//   * @param maybeV        an optional V value which is the attribute of the BaseConnexions to be found and marked.
//   * @param errorMessage  an error message which will be the message for the exception that arises when maybeV is None.
//   * @return Option[V]: the (optional) vertex to run dfs on next.
//   */
//  private[newcore] def findAndMarkVertex[V, X <: Connexion[V], P <: Discovered](connexionsMap: Map[V, Connexions[V, X, P]], f: Connexions[V, X, P] => Unit, maybeV: Option[V], errorMessage: String)(implicit evP: Properties[V, P]): Option[V] = maybeV match {
//    case Some(z) =>
//      val cs: Seq[Connexions[V, X, P]] = for {
//        q <- connexionsMap.get(z).toSeq
//        x <- q.connexions
//        v <- x.connexion(z).toSeq
//        t = evP.getProperties(v) if !t.isDiscovered
//      } yield q
//      cs foreach (c => f(c))
//      maybeV
//    case None => maybeV //  NOTE: this is not a problem. // throw GraphException(errorMessage)
//  }
}

object Adjacency {
  def apply[V, X <: Connexion[V], P](value: Map[V, Vertex[V, X, P]]): Adjacency[V, X, P] = ??? // TODO implement me
}

case class OrderedBaseAdjacency[V: Ordering, X <: Connexion[V]](map: TreeMap[V, BaseConnexions[V, X]]) extends AbstractBaseAdjacency[V, X](map) {

  /**
   * Method to decorate this BaseAdjacency as a Adjacency.
   *
   * @param v the vertex/node identifier.
   * @param p the property.
   * @tparam P the property type.
   *           Required to be Initializable and Discoverable.
   * @return
   */
  def adjacency[P: Initializable : Discoverable](v: V, p: Unit => P): Adjacency[V, X, P] = {
    val mm: TreeMap[V, Connexions[V, X, P]] = for ((k, v) <- map) yield k -> v.toConnexions(p)
    OrderedAdjacency(mm)
  }

  /**
   * Method to construct a new BaseAdjacency based on this.
   *
   * @param map the map to be used.
   * @return an BaseAdjacency[V, X].
   */
  def unit(map: Map[V, Connexions[V, X, Unit]]): Adjacency[V, X, Unit] = map match {
    case m: TreeMap[V, BaseConnexions[V, X]] => OrderedBaseAdjacency(m)
    case _ => throw CoreException(s"OrderedBaseAdjacency: unit: map must be a TreeMap")
  }

  /**
   * Method to run depth-first-search on this Traversable, ensuring that every vertex is visited..
   *
   * @param visitor the visitor, of type Visitor[V, J].
   * @tparam J the journal type.
   * @return a new Visitor[V, J].
   */
  def dfsAll[J](visitor: Visitor[V, J]): Visitor[V, J] = ??? // TODO implement me

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
  def bfsMutable[J, Q](visitor: Visitor[V, J])(v: V)(goal: V => Boolean)(implicit ev: MutableQueueable[Q, V]): Visitor[V, J] = ??? // TODO implement me
}

object OrderedBaseAdjacency {
  def empty[V: Ordering, X <: Connexion[V]]: OrderedBaseAdjacency[V, X] = OrderedBaseAdjacency(TreeMap.empty)
}

case class OrderedAdjacency[V, X <: Connexion[V], P: Initializable : Discoverable](map: TreeMap[V, Connexions[V, X, P]]) extends AbstractAdjacency[V, X, P](map) {

  /**
   * Method to construct a new BaseAdjacency based on this.
   *
   * @param map the map to be used.
   * @return an BaseAdjacency[V, X].
   */
  def unit(map: Map[V, Connexions[V, X, P]]): Adjacency[V, X, P] = map match {
    case m: TreeMap[V, Connexions[V, X, P]] => OrderedAdjacency(m)
    case _ => throw CoreException(s"OrderedAdjacency: unit: map must be a TreeMap")
  }

//
//  /**
//   * Method to run depth-first-search on this Traversable.
//   * Vertices will not be visited if they are not reachable from v.
//   *
//   * @param visitor the visitor, of type Visitor[V, J].
//   * @param v       the starting vertex.
//   * @tparam J the journal type.
//   * @return a new Visitor[V, J].
//   */
//  def dfs[J](visitor: Visitor[V, J])(v: V)(implicit evP: Properties[V, P]): Visitor[V, J] = ???

  /**
   * Method to run depth-first-search on this Traversable, ensuring that every vertex is visited..
   *
   * @param visitor the visitor, of type Visitor[V, J].
   * @tparam J the journal type.
   * @return a new Visitor[V, J].
   */
  def dfsAll[J](visitor: Visitor[V, J]): Visitor[V, J] = ??? // TODO implement me

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
  def bfsMutable[J, Q](visitor: Visitor[V, J])(v: V)(goal: V => Boolean)(implicit ev: MutableQueueable[Q, V]): Visitor[V, J] = ??? // TODO implement me

}

object OrderedAdjacency {
  def empty[V: Ordering, X <: Connexion[V], P: Initializable : Discoverable]: OrderedAdjacency[V, X, P] = OrderedAdjacency[V, X, P](TreeMap.empty[V, Connexions[V, X, P]])
}

case class UnorderedBaseAdjacency[V: Ordering, X <: Connexion[V]](map: HashMap[V, BaseConnexions[V, X]]) extends AbstractBaseAdjacency[V, X](map) {

  /**
   * Method to decorate this BaseAdjacency as a Adjacency.
   *
   * @param v the vertex/node identifier.
   * @param p the property.
   * @tparam P the property type.
   * @return an Adjacency[V, X, P].
   */
  def adjacency[P: Initializable : Discoverable](v: V, p: Unit => P): Adjacency[V, X, P] = {
    val mm: HashMap[V, Connexions[V, X, P]] = for ((k, v) <- map) yield k -> v.toConnexions(p)
    UnorderedAdjacency(mm)
  }

  /**
   * Method to construct a new BaseAdjacency based on this.
   *
   * @param map the map to be used.
   * @return an BaseAdjacency[V, X].
   */
  def unit(map: Map[V, Connexions[V, X, Unit]]): Adjacency[V, X, Unit] = map match {
    case m: TreeMap[V, BaseConnexions[V, X]] => OrderedBaseAdjacency(m)
    case _ => throw CoreException(s"OrderedBaseAdjacency: unit: map must be a TreeMap")
  }

  /**
   * Method to run depth-first-search on this Traversable, ensuring that every vertex is visited..
   *
   * @param visitor the visitor, of type Visitor[V, J].
   * @tparam J the journal type.
   * @return a new Visitor[V, J].
   */
  def dfsAll[J](visitor: Visitor[V, J]): Visitor[V, J] = ??? // TODO implement me

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
  def bfsMutable[J, Q](visitor: Visitor[V, J])(v: V)(goal: V => Boolean)(implicit ev: MutableQueueable[Q, V]): Visitor[V, J] = ??? // TODO implement me
}


case class UnorderedAdjacency[V, X <: Connexion[V], P: Initializable : Discoverable](map: HashMap[V, Connexions[V, X, P]]) extends AbstractAdjacency[V, X, P](map) {

  /**
   * Method to construct a new BaseAdjacency based on this.
   *
   * @param map the map to be used.
   * @return an BaseAdjacency[V, X].
   */
  def unit(map: Map[V, Connexions[V, X, P]]): Adjacency[V, X, P] = map match {
    case m: HashMap[V, Connexions[V, X, P]] => UnorderedAdjacency(m)
    case _ => throw CoreException(s"OrderedAdjacency: unit: map must be a HashMap")
  }

  /**
   * Method to run depth-first-search on this Traversable.
   * Vertices will not be visited if they are not reachable from v.
   *
   * @param visitor the visitor, of type Visitor[V, J].
   * @param v       the starting vertex.
   * @tparam J the journal type.
   * @return a new Visitor[V, J].
   */
  def dfs[J](visitor: Visitor[V, J])(v: V): Visitor[V, J] = ??? // TODO implement me

  /**
   * Method to run depth-first-search on this Traversable, ensuring that every vertex is visited..
   *
   * @param visitor the visitor, of type Visitor[V, J].
   * @tparam J the journal type.
   * @return a new Visitor[V, J].
   */
  def dfsAll[J](visitor: Visitor[V, J]): Visitor[V, J] = ??? // TODO implement me

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
  def bfsMutable[J, Q](visitor: Visitor[V, J])(v: V)(goal: V => Boolean)(implicit ev: MutableQueueable[Q, V]): Visitor[V, J] = ??? // TODO implement me

}

object UnorderedAdjacency {
  def empty[V: Ordering, X <: Connexion[V], P: Initializable : Discoverable]: UnorderedAdjacency[V, X, P] = UnorderedAdjacency[V, X, P](HashMap.empty[V, Connexions[V, X, P]])
}
