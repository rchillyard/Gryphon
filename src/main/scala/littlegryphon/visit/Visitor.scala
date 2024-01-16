/*
 * Copyright (c) 2023. Phasmid Software
 */

package littlegryphon.visit

import scala.collection.immutable.Queue

/**
 * Trait to define the behavior of a visitor--used during depth-first-search, etc.
 * The Visitor supports two journal entries: pre- and post- recursion.
 * Most journals are also iterable so that they can be retrieved after DFS is complete.
 * However, it is perfectly possible to have a journal which simply writes to a file (or something similar).
 *
 * CONSIDER do we need an edge-visitor?
 *
 * @tparam V the type to be visited, typically the (key) attribute type of a vertex.
 * @tparam J the type of the journal for this visitor.
 */
trait Visitor[V, J] extends AutoCloseable {

  /**
   * Method to visit BEFORE processing the (child) V values.
   *
   * @param v (V) the value of this node (vertex).
   * @return an updated Visitor[V, J].
   */
  def visitPre(v: V): Visitor[V, J]

  /**
   * Method to visit AFTER processing the (child) V values.
   *
   * @param v (V) the value of this node (vertex).
   * @return an updated Visitor[V, J].
   */
  def visitPost(v: V): Visitor[V, J]

  /**
   * Function to process a vertex in pre-order.
   * NOTE This function is not intended for application usage.
   */
  val preFunc: V => J => Option[J]

  /**
   * Function to process a vertex in post-order.
   * NOTE This function is not intended for application usage.
   */
  val postFunc: V => J => Option[J]

  /**
   * The journal of all of the pre- and post- invocations.
   */
  val journal: J

  def close(): Unit
}

/**
 * Companion object of Visitor case class.
 * Contains factory methods to create various types of Visitor.
 */
object Visitor {

  /**
   * Method to create a PreVisitor.
   *
   * TODO generalize this so that it doesn't require a Queue.
   *
   * @tparam V the type to be visited, typically the (key) attribute type of a vertex.
   *           requires evidence of a Journal...
   * @return a PreVisitor of V and Queue[V]
   */
  def createPre[V](implicit ev: Journal[Queue[V], V]): PreVisitor[V, Queue[V]] = PreVisitor[V, Queue[V]]()

  /**
   * Method to create a reverse PreVisitor.
   *
   * TODO generalize this so that it doesn't require a List.
   *
   * @tparam V the type to be visited, typically the (key) attribute type of a vertex.
   *           Requires implicit evidence of type Journal...
   * @return a PreVisitor of V and List[V]
   */
  def reversePre[V](implicit ev: Journal[List[V], V]): PreVisitor[V, List[V]] = PreVisitor(ev.empty)

  /**
   * Method to create a PostVisitor.
   *
   * TODO generalize this so that it doesn't require a Queue.
   *
   * @tparam V the type to be visited, typically the (key) attribute type of a vertex.
   *           Requires implicit evidence of type Journal...
   * @return a PostVisitor of V and Queue[V]
   */
  def createPost[V](implicit ev: Journal[Queue[V], V]): PostVisitor[V, Queue[V]] = PostVisitor[V, Queue[V]]()

  /**
   * Method to create a reverse PostVisitor.
   *
   * TODO generalize this so that it doesn't require a List.
   *
   * @tparam V the type to be visited, typically the (key) attribute type of a vertex.
   *           Requires implicit evidence of type Journal...
   * @return a PostVisitor of V and List[V]
   */
  def reversePost[V](implicit ev: Journal[List[V], V]): PostVisitor[V, List[V]] = PostVisitor(ev.empty)

  /**
   * Method to create a PreVisitorIterable based on a Queue[V].
   *
   * @tparam V the type to be visited, typically the (key) attribute type of a vertex.
   *           Requires evidence of type IterableJournal[Queue[V], V].
   * @return a PreVisitorIterable of V and Queue[V]
   */
  def createPreQueue[V](implicit ev: IterableJournal[Queue[V], V]): PreVisitorIterable[V, Queue[V]] = PreVisitorIterable[V, Queue[V]]()

  /**
   * Method to create a reverse PreVisitorIterable based on a List[V].
   *
   * @tparam V the type to be visited, typically the (key) attribute type of a vertex.
   *           Requires evidence of a IterableJournal[List[V], V].
   * @return a PreVisitorIterable of V and List[V]
   */
  def reversePreList[V](implicit ev: IterableJournal[List[V], V]): PreVisitorIterable[V, List[V]] = PreVisitorIterable(ev.empty)

  /**
   * Method to create a PostVisitorIterable based on a Queue[V].
   *
   * @tparam V the type to be visited, typically the (key) attribute type of a vertex.
   *           Requires evidence of a IterableJournal[Queue[V], V].
   * @return a PostVisitorIterable of V and Queue[V]
   */
  def createPostQueue[V](implicit ev: IterableJournal[Queue[V], V]): PostVisitorIterable[V, Queue[V]] = PostVisitorIterable[V, Queue[V]]()

  /**
   * Method to create a reverse PostVisitorIterable based on a List[V].
   *
   * @tparam V the type to be visited, typically the (key) attribute type of a vertex.
   *           Requires implicit evidence of a IterableJournal[List[V], V].
   * @return a PostVisitorIterable of V and List[V]
   */
  def reversePostList[V](implicit ev: IterableJournal[List[V], V]): PostVisitorIterable[V, List[V]] = PostVisitorIterable(ev.empty)

  /**
   * Method to create a composed pre- and post-visitor.
   *
   * @tparam V the type to be visited, typically the (key) attribute type of a vertex.
   * @return a Visitor[V, List of V].
   */
  def preAndPost[V](implicit ev: Journal[List[V], V]): Visitor[V, List[V]] = PreVisitor[V, List[V]]() join PostVisitor()
}

/**
 * Case class representing a generic PreVisitor.
 *
 * @param journal the journal which will record all visits.
 * @tparam V the type to be visited, typically the (key) attribute type of a vertex.
 * @tparam J the type of the journal.
 *           Requires implicit evidence of type Journal[J, V].
 */
case class PreVisitor[V, J](journal: J)(implicit val ev: Journal[J, V]) extends BaseVisitor[V, J](journal) {

  val preFunc: V => J => Option[J] = appendToJournal
  val postFunc: V => J => Option[J] = doNothing

  /**
   * Method to construct a new Visitor[V, J] based on this visitor and the given journal.
   *
   * @param journal the journal to keep track of the visited elements.
   * @return a new Visitor[V, J].
   */
  def unit(journal: J): Visitor[V, J] = PreVisitor(journal)
}

/**
 * Companion object to PreVisitor.
 */
object PreVisitor {
  def apply[V, J]()(implicit ev: Journal[J, V]): PreVisitor[V, J] = new PreVisitor(ev.empty)
}

/**
 * Case class representing a generic PostVisitor.
 *
 * @param journal the journal which will record all visits.
 * @tparam V the type to be visited, typically the (key) attribute type of a vertex.
 * @tparam J the type of the journal.
 *           Requires implicit evidence of type Journal[J, V].
 */
case class PostVisitor[V, J](journal: J)(implicit val ev: Journal[J, V]) extends BaseVisitor[V, J](journal) {

  val preFunc: V => J => Option[J] = doNothing
  val postFunc: V => J => Option[J] = appendToJournal

  def unit(journal: J): Visitor[V, J] = PostVisitor(journal)
}

/**
 * Companion object to PostVisitor.
 */
object PostVisitor {
  def apply[V, J]()(implicit ev: Journal[J, V]): PostVisitor[V, J] = new PostVisitor(ev.empty)
}

/**
 * Case class representing an iterable PreVisitor.
 *
 * @param journal the journal which will record all visits.
 * @tparam V the type to be visited, typically the (key) attribute type of a vertex.
 * @tparam J the type of the journal.
 *           Requires implicit evidence of type IterableJournal[J, V].
 */
case class PreVisitorIterable[V, J <: Iterable[V]](journal: J)(implicit val ev: IterableJournal[J, V]) extends BaseIterableVisitor[V, J](journal) {

  val preFunc: V => J => Option[J] = appendToJournal
  val postFunc: V => J => Option[J] = doNothing

  /**
   * Method to construct a new Visitor[V, J] based on this visitor and the given journal.
   *
   * @param journal the journal to keep track of the visited elements.
   * @return a new Visitor[V, J].
   */
  def unit(journal: J): Visitor[V, J] = PreVisitorIterable(journal)
}

/**
 * Companion object to PreVisitorIterable.
 */
object PreVisitorIterable {
  /**
   * Method to construct a new PreVisitorIterable based on an empty journal.
   *
   * @tparam V the underlying type of the visitor and the journal.
   * @tparam J the journal type, for example Queue[V].
   *           Must provide implicit evidence of type IterableJournal[J, V].
   * @return a PreVisitorIterable[V, J]
   */
  def apply[V, J <: Iterable[V]]()(implicit ev: IterableJournal[J, V]): PreVisitorIterable[V, J] = new PreVisitorIterable(ev.empty)

  /**
   * Method to create an PreVisitorIterable based on a Queue -- without having to provide evidence of an IterableJournal[J, V].
   *
   * @tparam V the underlying type of the visitor and the resulting queue.
   * @return an IterableVisitor based on a Queue.
   */
  def create[V]: PreVisitorIterable[V, Queue[V]] = {
    implicit val vijq: IterableJournalQueue[V] = new IterableJournalQueue[V] {}
    apply()
  }
}

/**
 * Case class representing an iterable PostVisitor.
 *
 * @param journal the journal which will record all visits.
 * @tparam V the type to be visited, typically the (key) attribute type of a vertex.
 * @tparam J the type of the journal.
 *           Requires implicit evidence of type IterableJournal[J, V].
 */
case class PostVisitorIterable[V, J <: Iterable[V]](journal: J)(implicit val ev: IterableJournal[J, V]) extends BaseIterableVisitor[V, J](journal) {

  val preFunc: V => J => Option[J] = doNothing
  val postFunc: V => J => Option[J] = appendToJournal

  /**
   * Method to construct a new Visitor[V, J] based on this visitor and the given journal.
   *
   * @param journal the journal to keep track of the visited elements.
   * @return a new Visitor[V, J].
   */
  def unit(journal: J): Visitor[V, J] = PostVisitorIterable(journal)
}

/**
 * Companion object to PostVisitorIterable.
 */
object PostVisitorIterable {
  /**
   * Method to construct a new PostVisitorIterable based on an empty journal.
   *
   * @tparam V the underlying type of the visitor and the journal.
   * @tparam J the journal type, for example Queue[V].
   *           Must provide implicit evidence of type IterableJournal[J, V].
   * @return a PostVisitorIterable[V, J]
   */
  def apply[V, J <: Iterable[V]]()(implicit ev: IterableJournal[J, V]): PostVisitorIterable[V, J] = new PostVisitorIterable(ev.empty)

  /**
   * Method to create an PostVisitorIterable based on a Queue -- without having to provide evidence of an IterableJournal[J, V].
   *
   * @tparam V the underlying type of the visitor and the resulting queue.
   * @return an IterableVisitor based on a Queue.
   */
  def create[V]: PostVisitorIterable[V, Queue[V]] = {
    implicit val vijq: IterableJournalQueue[V] = new IterableJournalQueue[V] {}
    apply()
  }
}

/**
 * Concrete Visitor which is defined by its provided pre and post functions.
 *
 * @param preFunc  the function to be invoked in pre-order.
 * @param postFunc the function to be invoked in post-order.
 * @param journal  the journal which will record all visits.
 * @tparam V the type to be visited, typically the (key) attribute type of a vertex.
 * @tparam J the Journal type.
 *           Requires implicit evidence of type Journal[J, V].
 */
class GenericVisitor[V, J](val preFunc: V => J => Option[J], val postFunc: V => J => Option[J])(val journal: J)(implicit val ev: Journal[J, V]) extends BaseVisitor[V, J](journal) {
  /**
   * Method to create a new GenericVisitor.
   *
   * @param journal the journal to keep track of the visited elements.
   * @return a new Visitor[V, J].
   */
  def unit(journal: J): Visitor[V, J] = new GenericVisitor(preFunc, postFunc)(journal)
}

/**
 * Trait to define the behavior of a Visitor which can be iterated.
 *
 * @tparam V the type to be visited, typically the (key) attribute type of a vertex.
 * @tparam J the type of the journal for this visitor.
 */
trait IterableVisitor[V, J <: Iterable[V]] extends Visitor[V, J] {
  def iterator: Iterator[V]

  /**
   * Method to visit BEFORE processing the (child) V values.
   *
   * @param v (V) the value of this node (vertex).
   * @return an updated Visitor[V, J].
   */
  def visitPre(v: V): IterableVisitor[V, J]

  /**
   * Method to visit AFTER processing the (child) V values.
   *
   * @param v (V) the value of this node (vertex).
   * @return an updated Visitor[V, J].
   */
  def visitPost(v: V): IterableVisitor[V, J]

}

/**
 * Abstract base class which extends Visitor[V, J].
 *
 * @param journal the journal which will record all visits.
 * @tparam V the type to be visited, typically the (key) attribute type of a vertex.
 * @tparam J the type of the journal for this visitor.
 *           Requires implicit evidence of type Journal[J, V].
 */
abstract class BaseVisitor[V, J](journal: J)(implicit val ava: Journal[J, V]) extends Visitor[V, J] {
  self =>

  /**
   * Method to visit before processing the (child) V values.
   *
   * @param v (V) the value of this node (vertex).
   * @return an updated Visitor[V, J].
   */
  def visitPre(v: V): Visitor[V, J] = unit(preFunc(v)(journal) getOrElse journal)

  /**
   * Method to visit after processing the (child) V values.
   *
   * @param v (V) the value of this node (vertex).
   * @return an updated Visitor[V, J].
   */
  def visitPost(v: V): Visitor[V, J] = unit(postFunc(v)(journal) getOrElse journal)

  protected def appendToJournal(implicit ev: Journal[J, V]): V => J => Option[J] = v => a => Some(ev.append(a, v))

  //noinspection MutatorLikeMethodIsParameterless
  protected def doNothing: V => J => Option[J] = _ => _ => None

  /**
   * Method to construct a new Visitor[V, J] based on this visitor and the given journal.
   *
   * @param journal the journal to keep track of the visited elements.
   * @return a new Visitor[V, J].
   */
  def unit(journal: J): Visitor[V, J]

  /**
   * Non-pure method to close this Visitor.
   */
  def close(): Unit = journal match {
    case x: AutoCloseable => x.close()
    case _ =>
  }

  /**
   * Method to compose two Visitors into one.
   *
   * NOTE that the types (V, J) of the other visitor MUST be consistent with the types of this visitor.
   * See VisitorSpec to see how you might work around this limitation.
   *
   * @param visitor a Visitor[V, J].
   * @return a new GenericVisitor[V, J].
   */
  def join(visitor: Visitor[V, J]): Visitor[V, J] =
    new GenericVisitor[V, J](v => a => joinFunc(a, self.preFunc(v), visitor.preFunc(v)), v => a => joinFunc(a, self.postFunc(v), visitor.postFunc(v)))(self.journal)

  private def joinFunc(a: J, f1: J => Option[J], f2: J => Option[J]) = f1(a) match {
    case x@Some(b) => f2(b) orElse x
    case None => f2(a)
  }
}

/**
 * Abstract base class which extends IterableVisitor[V, J].
 *
 * @param journal the journal which will record all visits.
 * @tparam V the type to be visited, typically the (key) attribute type of a vertex.
 * @tparam J the type of the journal for this visitor.
 *           Requires implicit evidence of type IterableJournal[J, V].
 */
abstract class BaseIterableVisitor[V, J <: Iterable[V]](journal: J)(implicit val avai: IterableJournal[J, V]) extends BaseVisitor[V, J](journal) with IterableVisitor[V, J] {
  self =>

  def iterator: Iterator[V] = avai.iterator(journal)

  /**
   * Method to visit before processing the (child) V values.
   *
   * @param v (V) the value of this node (vertex).
   * @return an updated IterableVisitor[V, J].
   */
  // TODO eliminate this asInstanceOf
  override def visitPre(v: V): IterableVisitor[V, J] = super.visitPre(v).asInstanceOf[IterableVisitor[V, J]]

  /**
   * Method to visit after processing the (child) V values.
   *
   * @param v (V) the value of this node (vertex).
   * @return an updated IterableVisitor[V, J].
   */
  // TODO eliminate this asInstanceOf
  override def visitPost(v: V): IterableVisitor[V, J] = super.visitPost(v).asInstanceOf[IterableVisitor[V, J]]
}

trait HasIterator[J <: Iterable[V], V] {
  def iterator(journal: J): Iterator[V] = journal.iterator
}
