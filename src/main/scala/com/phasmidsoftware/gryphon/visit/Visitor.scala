/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.visit

import scala.collection.immutable.Queue

/**
 * Trait to define the behavior of a visitor--used during depth-first-search, etc.
 * For DFS, `Visitor` supports two journal entries: pre- and post-recursion.
 * For BFS, only the "pre" visit is supported.
 *
 * Most journals are also iterable so that they can be retrieved after the traversal is complete.
 * However, it is perfectly possible to have a journal which simply writes to a file (or something similar).
 *
 * @tparam A the type to be visited, typically the (key) attribute type of `Vertex`.
 * @tparam J the type of the `Journal` for this `Visitor`.
 */
trait Visitor[A, J] extends AutoCloseable {

  /**
   * Method to visit BEFORE processing the (child) `A` values in DFS.
   * For BFS, this method is the only visit to be performed.
   *
   * @param a (`A`) the type being visited.
   * @return an updated `Visitor[A, J]`.
   */
  def visitPre(a: A): Visitor[A, J]

  /**
   * Method to visit AFTER processing the (child) A values in DFS.
   * For BFS, this method is a no-op.
   *
   * @param a (A) the value of this node (vertex).
   * @return an updated Visitor[A, J].
   */
  def visitPost(a: A): Visitor[A, J]

  /**
   * Function to process a vertex in pre-order.
   * NOTE This function is not intended for application usage.
   */
  val preFunc: A => J => Option[J]

  /**
   * Function to process a vertex in post-order.
   * NOTE This function is not intended for application usage.
   */
  val postFunc: A => J => Option[J]

  /**
   * The journal of all the pre- and post-invocations.
   */
  val journal: J

  /**
   * Closes any resources held by the implementing Visitor instance.
   *
   * This method is intended to release or clean up resources such as file writers,
   * journals, or other external dependencies associated with the Visitor object.
   *
   * @return Unit, indicating that the method performs a side effect without returning a value.
   */
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
   * @tparam A the type to be visited, typically the (key) attribute type of vertex.
   *           requires evidence of a Journal...
   * @return a PreVisitor of A and Queue[A]
   */
  def createPre[A](implicit ev: Journal[Queue[A], A]): PreVisitor[A, Queue[A]] =
    PreVisitor[A, Queue[A]]()

  /**
   * Method to create a reverse PreVisitor.
   *
   * TODO generalize this so that it doesn't require a List.
   *
   * @tparam A the type to be visited, typically the (key) attribute type of vertex.
   *           Requires implicit evidence of type Journal...
   * @return a PreVisitor of A and List[A]
   */
  def reversePre[A](implicit ev: Journal[List[A], A]): PreVisitor[A, List[A]] =
    PreVisitor(ev.empty)

  /**
   * Method to create a PostVisitor.
   *
   * TODO generalize this so that it doesn't require a Queue.
   *
   * @tparam A the type to be visited, typically the (key) attribute type of vertex.
   *           Requires implicit evidence of type Journal...
   * @return a PostVisitor of A and Queue[A]
   */
  def createPostQueue[A](implicit ev: Journal[Queue[A], A]): PostVisitor[A, Queue[A]] =
    PostVisitor[A, Queue[A]]()

  /**
   * Method to create a reverse PostVisitor.
   *
   * TODO generalize this so that it doesn't require a List.
   *
   * @tparam A the type to be visited, typically the (key) attribute type of vertex.
   *           Requires implicit evidence of type Journal...
   * @return a PostVisitor of A and List[A]
   */
  def reversePost[A](implicit ev: Journal[List[A], A]): PostVisitor[A, List[A]] =
    PostVisitor(ev.empty)

  /**
   * Method to create a PreVisitorIterable based on a Queue[A].
   *
   * @tparam A the type to be visited, typically the (key) attribute type of vertex.
   *           Requires evidence of type IterableJournal[Queue[A], A].
   * @return a PreVisitorIterable of A and Queue[A]
   */
  def createPreQueue[A](implicit ev: IterableJournal[Queue[A], A]): PreVisitorIterable[A, Queue[A]] =
    PreVisitorIterable[A, Queue[A]]()

  /**
   * Method to create a reverse PreVisitorIterable based on a List[A].
   *
   * @tparam A the type to be visited, typically the (key) attribute type of vertex.
   *           Requires evidence of an `IterableJournal[List[A], A]`.
   * @return a PreVisitorIterable of A and List[A]
   */
  def reversePreList[A](implicit ev: IterableJournal[List[A], A]): PreVisitorIterable[A, List[A]] =
    PreVisitorIterable(ev.empty)

  /**
   * Method to create a PostVisitorIterable based on a Queue[A].
   *
   * @tparam A the type to be visited, typically the (key) attribute type of a vertex.
   *           Requires evidence of a IterableJournal[Queue[A], A].
   * @return a PostVisitorIterable of A and Queue[A]
   */
  def createPostQueue[A](implicit ev: IterableJournal[Queue[A], A]): PostVisitorIterable[A, Queue[A]] =
    PostVisitorIterable[A, Queue[A]]()

  /**
   * Method to create a reverse PostVisitorIterable based on a List[A].
   *
   * @tparam A the type to be visited, typically the (key) attribute type of a vertex.
   *           Requires implicit evidence of a IterableJournal[List[A], A].
   * @return a PostVisitorIterable of A and List[A]
   */
  def reversePostList[A](implicit ev: IterableJournal[List[A], A]): PostVisitorIterable[A, List[A]] =
    PostVisitorIterable(ev.empty)

  /**
   * Method to create a composed pre- and post-visitor.
   *
   * @tparam A the type to be visited, typically the (key) attribute type of a vertex.
   * @return a Visitor[A, List of A].
   */
  def preAndPost[A](implicit ev: Journal[List[A], A]): Visitor[A, List[A]] =
    PreVisitor[A, List[A]]() join PostVisitor()
}

/**
 * Case class representing a generic PreVisitor.
 *
 * @param journal the journal which will record all visits.
 * @tparam A the type to be visited, typically the (key) attribute type of a vertex.
 * @tparam J the type of the journal.
 *           Requires implicit evidence of type Journal[J, A].
 */
case class PreVisitor[A, J](journal: J)(implicit val ev: Journal[J, A]) extends AbstractVisitor[A, J](journal) {

  /**
   * A function that represents the pre-visit behavior in the `PreVisitor` class.
   * This function takes a value of type `A` and returns a function that takes
   * a journal of type `J`, producing an optional updated journal containing the appended value.
   *
   * The actual logic for appending the value to the journal is encapsulated
   * within the `appendToJournal` method, which leverages the implicit `Journal` instance.
   */
  val preFunc: A => J => Option[J] =
    appendToJournal

  /**
   * A function representing the post-visit operation of a `PreVisitor`.
   *
   * This function is invoked after visiting an element of type `A` to potentially modify the journal `J`.
   * By default, it performs no operation and always returns `None` to signify that no journal changes occur.
   *
   * @return A function that takes an element of type `A` and returns
   *         a function which accepts the current journal of type `J`
   *         and produces an `Option[J]` representing the updated journal (or `None` if unchanged).
   */
  val postFunc: A => J => Option[J] =
    doNothing

  /**
   * Method to construct a new Visitor[A, J] based on this visitor and the given journal.
   *
   * @param journal the journal to keep track of the visited elements.
   * @return a new Visitor[A, J].
   */
  def unit(journal: J): Visitor[A, J] =
    PreVisitor(journal)
}

/**
 * Companion object to PreVisitor.
 */
object PreVisitor {
  /**
   * Factory method to create a new instance of `PreVisitor` using an empty journal.
   *
   * @param ev an implicit evidence parameter of type `Journal[J, A]` that provides the ability
   *           to manage a journal of type `J` containing elements of type `A`.
   * @tparam A the type of elements that the `PreVisitor` will process.
   * @tparam J the type of the journal used to record the processing of elements of type `A`.
   * @return a new `PreVisitor` instance initialized with an empty journal.
   */
  def apply[A, J]()(implicit ev: Journal[J, A]): PreVisitor[A, J] =
    new PreVisitor(ev.empty)
}

/**
 * Case class representing a generic PostVisitor.
 *
 * @param journal the journal which will record all visits.
 * @tparam A the type to be visited, typically the (key) attribute type of a vertex.
 * @tparam J the type of the journal.
 *           Requires implicit evidence of type Journal[J, A].
 */
case class PostVisitor[A, J](journal: J)(implicit val ev: Journal[J, A]) extends AbstractVisitor[A, J](journal) {

  /**
   * A function that, given a value of type `A`, returns another function.
   * The inner function, when provided a journal of type `J`, will
   * produce an empty `Option[J]` (i.e., `None`) signifying that no operation
   * was performed on the journal.
   *
   * This is typically used as a "do-nothing" function in contexts where
   * pre-visit actions are optional or require no effect on the journal.
   */

  val preFunc: A => J => Option[J] =
    doNothing

  /**
   * A function that represents the "post-visit" behavior for a `PostVisitor`.
   *
   * The function, when applied, utilizes the `appendToJournal` mechanism to potentially modify the journal
   * after visiting a node or element of type `A`. The implicit `Journal[J, A]` instance determines
   * how the value `A` is appended to the journal `J`.
   *
   * It takes a value of type `A` as input and returns another function, which takes a journal of type `J`
   * and produces an updated journal wrapped in `Option`, reflecting how the visit is recorded.
   */
  val postFunc: A => J => Option[J] =
    appendToJournal

  /**
   * Creates a `PostVisitor` instance initialized with the provided journal.
   *
   * @param journal the journal instance of type `J` to be used by the `PostVisitor`.
   * @return a `PostVisitor` instance configured with the given journal.
   */
  def unit(journal: J): Visitor[A, J] =
    PostVisitor(journal)
}

/**
 * Companion object to PostVisitor.
 */
object PostVisitor {
  /**
   * Creates a new instance of `PostVisitor` with an empty journal.
   *
   * @param ev implicit evidence of type `Journal[J, A]`, which provides the behavior of the journal.
   * @tparam A the type to be visited, typically the attribute type of a vertex.
   * @tparam J the type of the journal.
   * @return a new `PostVisitor` instance with an empty journal.
   */
  def apply[A, J]()(implicit ev: Journal[J, A]): PostVisitor[A, J] =
    new PostVisitor(ev.empty)
}

/**
 * Case class representing an iterable PreVisitor.
 *
 * @param journal the journal which will record all visits.
 * @tparam A the type to be visited, typically the (key) attribute type of a vertex.
 * @tparam J the type of the journal.
 *           Requires implicit evidence of type IterableJournal[J, A].
 */
case class PreVisitorIterable[A, J <: Iterable[A]](journal: J)(implicit val ev: IterableJournal[J, A]) extends AbstractIterableVisitor[A, J](journal) {

  /**
   * A function that represents a pre-visit behavior to append a value of type `A` to a journal of type `J`.
   * It uses the `appendToJournal` method, which relies on the implicit `Journal` type class to handle
   * the appending of values and updates the journal accordingly.
   *
   * @return a curried function that first takes a value of type `A`, then
   *         a journal of type `J`, and finally returns an optional updated journal.
   */
  val preFunc: A => J => Option[J] =
    appendToJournal
  /**
   * A post-visit function defined for a `PreVisitorIterable`.
   * This function determines the behavior to be executed after visiting an element.
   *
   * It is a no-operation (no-op) function that effectively performs no action
   * and always results in `None`.
   *
   * @return a function that takes a value of type `A` and produces another function
   *         which, given a journal of type `J`, returns `Option[J]` (always `None`).
   */
  val postFunc: A => J => Option[J] =
    doNothing

  /**
   * Method to construct a new Visitor[A, J] based on this visitor and the given journal.
   *
   * @param journal the journal to keep track of the visited elements.
   * @return a new Visitor[A, J].
   */
  def unit(journal: J): Visitor[A, J] =
    PreVisitorIterable(journal)
}

/**
 * Companion object to PreVisitorIterable.
 */
object PreVisitorIterable {
  /**
   * Method to construct a new PreVisitorIterable based on an empty journal.
   *
   * @tparam A the underlying type of the visitor and the journal.
   * @tparam J the journal type, for example Queue[A].
   *           Must provide implicit evidence of type IterableJournal[J, A].
   * @return a PreVisitorIterable[A, J]
   */
  def apply[A, J <: Iterable[A]]()(implicit ev: IterableJournal[J, A]): PreVisitorIterable[A, J] =
    new PreVisitorIterable(ev.empty)

  /**
   * Method to create an PreVisitorIterable based on a Queue -- without having to provide evidence of an IterableJournal[J, A].
   *
   * @tparam A the underlying type of the visitor and the resulting queue.
   * @return an IterableVisitor based on a Queue.
   */
  def create[A]: PreVisitorIterable[A, Queue[A]] = {
    implicit val vijq: IterableJournalQueue[A] = new IterableJournalQueue[A] {}
    apply()
  }
}

/**
 * Case class representing an iterable PostVisitor.
 *
 * @param journal the journal which will record all visits.
 * @tparam A the type to be visited, typically the (key) attribute type of a vertex.
 * @tparam J the type of the journal.
 *           Requires implicit evidence of type IterableJournal[J, A].
 */
case class PostVisitorIterable[A, J <: Iterable[A]](journal: J)(implicit val ev: IterableJournal[J, A]) extends AbstractIterableVisitor[A, J](journal) {

  /**
   * A function that encapsulates a pre-visit operation for elements of type `A` during a traversal.
   * This function takes a vertex of type `A`, returns a higher-order function
   * that accepts a journal of type `J`, and produces an optional updated journal.
   * The default implementation does nothing and always returns `None`.
   */
  val preFunc: A => J => Option[J] =
    doNothing

  /**
   * A function that appends a value of type `A` to a journal of type `J`.
   *
   * This function uses the implicit `Journal[J, A]` type class instance to append
   * the value to the journal. The function takes a value of type `A` and returns
   * another function which takes a journal of type `J`, producing an optionally updated journal.
   */
  val postFunc: A => J => Option[J] =
    appendToJournal

  /**
   * Method to construct a new Visitor[A, J] based on this visitor and the given journal.
   *
   * @param journal the journal to keep track of the visited elements.
   * @return a new Visitor[A, J].
   */
  def unit(journal: J): Visitor[A, J] =
    PostVisitorIterable(journal)
}

/**
 * Companion object to PostVisitorIterable.
 */
object PostVisitorIterable {
  /**
   * Method to construct a new PostVisitorIterable based on an empty journal.
   *
   * @tparam A the underlying type of the visitor and the journal.
   * @tparam J the journal type, for example Queue[A].
   *           Must provide implicit evidence of type IterableJournal[J, A].
   * @return a PostVisitorIterable[A, J]
   */
  def apply[A, J <: Iterable[A]]()(implicit ev: IterableJournal[J, A]): PostVisitorIterable[A, J] =
    new PostVisitorIterable(ev.empty)

  /**
   * Method to create an PostVisitorIterable based on a Queue -- without having to provide evidence of an IterableJournal[J, A].
   *
   * @tparam A the underlying type of the visitor and the resulting queue.
   * @return an IterableVisitor based on a Queue.
   */
  def create[A]: PostVisitorIterable[A, Queue[A]] = {
    implicit val vijq: IterableJournalQueue[A] = new IterableJournalQueue[A] {}
    apply()
  }
}

/**
 * A case class representing a "post-keyed visitor" that performs specific actions
 * on a journal associated with visited elements after processing them.
 *
 * This visitor operates in the context of a keyed journal, leveraging the implicit
 * `KeyedJournal` type class instance to manage the journal and its entries.
 *
 * CONSIDER added a PreKeyedVisitor (although such would be less likely to be useful).
 *
 * @tparam A The type of the value being visited, typically a key or attribute of a vertex.
 * @tparam X The type of the associated value in the keyed journal entries.
 * @tparam J The type of the journal used to record the visited elements.
 * @param journal The journal instance used for tracking visited elements during processing.
 * @param ev      An implicit instance of the `KeyedJournal` type class, providing operations for keyed journals.
 */
case class PostKeyedVisitor[A, X, J](journal: J)(implicit val ev: KeyedJournal[J, A, X]) extends AbstractKeyedVisitor[A, X, J](journal) {

  /**
   * A function that, given a value of type `A`, returns another function.
   * The inner function, when provided a journal of type `J`, will
   * produce an empty `Option[J]` (i.e., `None`) signifying that no operation
   * was performed on the journal.
   *
   * This is typically used as a "do-nothing" function in contexts where
   * pre-visit actions are optional or require no effect on the journal.
   */

  val preFunc: A => J => Option[J] =
    doNothing

  /**
   * A function that represents the "post-visit" behavior for a `PostVisitor`.
   *
   * The function, when applied, utilizes the `appendToJournal` mechanism to potentially modify the journal
   * after visiting a node or element of type `A`. The implicit `Journal[J, A]` instance determines
   * how the value `A` is appended to the journal `J`.
   *
   * It takes a value of type `A` as input and returns another function, which takes a journal of type `J`
   * and produces an updated journal wrapped in `Option`, reflecting how the visit is recorded.
   */
  val postFunc: A => J => Option[J] =
    appendToJournal

  /**
   * Method to construct a new Visitor[A, J] based on this visitor and the given journal.
   *
   * @param journal the journal to keep track of the visited elements.
   * @return a new Visitor[A, J].
   */
  def unit(journal: J): AbstractKeyedVisitor[A, X, J] =
    PostKeyedVisitor(journal)
}

/**
 * Companion object for the `PostKeyedVisitor` class, providing utility methods for constructing instances
 * of `PostKeyedVisitor` with specific journal and value types.
 */
object PostKeyedVisitor {

  /**
   * Creates a new instance of `PostKeyedVisitor` with an empty journal, utilizing the implicit
   * `KeyedJournal` instance for journal operations.
   *
   * @param ev An implicit instance of the `KeyedJournal` type class, which provides the functionality
   *           for managing journals of type `J`, where each entry is associated with a value of type `A` and
   *           an associated value of type `X`.
   * @return A new instance of `PostKeyedVisitor` initialized with an empty journal.
   */
  def create[A, X, J]()(implicit ev: KeyedJournal[J, A, X]): PostKeyedVisitor[A, X, J] =
    new PostKeyedVisitor(ev.empty)

}

/**
 * Concrete Visitor which is defined by its provided pre and post functions.
 *
 * @param preFunc  the function to be invoked in pre-order.
 * @param postFunc the function to be invoked in post-order.
 * @param journal  the journal which will record all visits.
 * @tparam A the type to be visited, typically the (key) attribute type of a vertex.
 * @tparam J the Journal type.
 *           Requires implicit evidence of type Journal[J, A].
 */
class GenericVisitor[A, J](val preFunc: A => J => Option[J], val postFunc: A => J => Option[J])(val journal: J)(implicit val ev: Journal[J, A]) extends AbstractVisitor[A, J](journal) {
  /**
   * Method to create a new GenericVisitor.
   *
   * @param journal the journal to keep track of the visited elements.
   * @return a new Visitor[A, J].
   */
  def unit(journal: J): Visitor[A, J] =
    new GenericVisitor(preFunc, postFunc)(journal)
}

/**
 * Trait to define the behavior of a Visitor which can be iterated.
 *
 * @tparam A the type to be visited, typically the (key) attribute type of a vertex.
 * @tparam J the type of the journal for this visitor.
 */
trait IterableVisitor[A, J <: Iterable[A]] extends Visitor[A, J] with IterableOnce[A] {

  /**
   * Method to visit BEFORE processing the (child) A values.
   *
   * @param a (A) the value of this node (vertex).
   * @return an updated Visitor[A, J].
   */
  def visitPre(a: A): IterableVisitor[A, J]

  /**
   * Method to visit AFTER processing the (child) A values.
   *
   * @param a (A) the value of this node (vertex).
   * @return an updated Visitor[A, J].
   */
  def visitPost(a: A): IterableVisitor[A, J]
}

/**
 * Abstract base class which extends Visitor[A, J].
 *
 * @param journal the journal which will record all visits.
 * @tparam A the type to be visited, typically the (key) attribute type of a vertex.
 * @tparam J the type of the journal for this visitor.
 *           Requires implicit evidence of type Journal[J, A].
 */
abstract class AbstractVisitor[A, J](journal: J)(implicit val ava: Journal[J, A]) extends Visitor[A, J] {
  self =>

  /**
   * Method to visit before processing the (child) A values.
   *
   * @param a (A) the value of this node (vertex).
   * @return an updated Visitor[A, J].
   */
  def visitPre(a: A): Visitor[A, J] =
    unit(preFunc(a)(journal) getOrElse journal)

  /**
   * Method to visit after processing the (child) A values.
   *
   * @param a (A) the value of this node (vertex).
   * @return an updated Visitor[A, J].
   */
  def visitPost(a: A): Visitor[A, J] =
    unit(postFunc(a)(journal) getOrElse journal)

  /**
   * Method to append a value of type `A` to a journal of type `J`, utilizing the implicit `Journal` type class instance.
   *
   * @return a function that takes a value of type `A`, then a journal of type `J`, and produces an optional updated journal.
   */
  protected def appendToJournal: A => J => Option[J] =
    v => a => Some(ava.append(a, v))

  /**
   * A method that represents a no-operation for updating a journal of type `J` with a value of type `A`.
   * It always returns `None` regardless of the inputs.
   *
   * @return a function that takes a value of type `A` and a journal of type `J`, always producing `None`.
   */
  //noinspection MutatorLikeMethodIsParameterless
  protected def doNothing: A => J => Option[J] =
    _ => _ => None

  /**
   * Method to construct a new Visitor[A, J] based on this visitor and the given journal.
   *
   * @param journal the journal to keep track of the visited elements.
   * @return a new Visitor[A, J].
   */
  def unit(journal: J): Visitor[A, J]

  /**
   * Non-pure method to close this Visitor.
   */
  def close(): Unit =
    journal match {
      case x: AutoCloseable =>
        x.close()
      case _ =>
    }

  /**
   * Method to compose two Visitors into one.
   *
   * TODO move this into GenericVisitor or use unit
   *
   * NOTE that the types (A, J) of the other visitor MUST be consistent with the types of this visitor.
   * See VisitorSpec to see how you might work around this limitation.
   *
   * @param visitor a Visitor[A, J].
   * @return a new GenericVisitor[A, J].
   */
  def join(visitor: Visitor[A, J]): Visitor[A, J] =
    new GenericVisitor[A, J](v => a => joinFunc(a, self.preFunc(v), visitor.preFunc(v)), v => a => joinFunc(a, self.postFunc(v), visitor.postFunc(v)))(self.journal)

  /**
   * Combines and applies two functions sequentially on a given value of type `J`, returning the first non-empty result.
   *
   * @param a  the initial journal value of type `J` to be processed by the functions.
   * @param f1 a function that takes a `J` and produces an `Option[J]`. It is applied first to the input `a`.
   * @param f2 a function that takes a `J` and produces an `Option[J]`. It is applied to the result of `f1` if `f1` produces a value,
   *           or directly to `a` if `f1` produces `None`.
   * @return the first `Some` result encountered during the application of `f1` or `f2`, or `None` if both return `None`.
   */
  private def joinFunc(a: J, f1: J => Option[J], f2: J => Option[J]) =
    f1(a) match {
      case x@Some(b) =>
        f2(b) orElse x
      case None =>
        f2(a)
    }
}

/**
 * An abstract class providing a framework for implementing a visitor pattern with keyed journals.
 * It extends the `Visitor` trait and introduces additional functionality for journal management
 * with implicit support for a `KeyedJournal` type class instance.
 *
 * TODO try to extend AbstractVisitor (although this might not be easy)
 *
 * CONSIDER can we extract a KeyedVisitor trait from this?
 *
 * @tparam A the type of value being visited, typically the key or attribute of a vertex.
 * @tparam X the secondary type associated with the keyed journal entries.
 * @tparam J the type of journal used to record the visited elements.
 * @param journal an instance of the journal used to keep track of visited elements during traversal.
 * @param ava     an implicit instance of `KeyedJournal[J, A, X]`, providing additional operations for keyed journals.
 */
abstract class AbstractKeyedVisitor[A, X, J](journal: J)(implicit val ava: KeyedJournal[J, A, X]) extends Visitor[A, J] {
  self =>

  /**
   * Method to visit before processing the (child) A values.
   *
   * @param a (A) the value of this node (vertex).
   * @return an updated Visitor[A, J].
   */
  def visitPre(a: A): Visitor[A, J] =
    unit(preFunc(a)(journal) getOrElse journal)

  /**
   * Method to visit after processing the (child) A values.
   *
   * @param a (A) the value of this node (vertex).
   * @return an updated Visitor[A, J].
   */
  def visitPost(a: A): Visitor[A, J] =
    unit(postFunc(a)(journal) getOrElse journal)

  /**
   * Method to append a value of type `A` to a journal of type `J`, utilizing the implicit `Journal` type class instance.
   *
   * @return a function that takes a value of type `A`, then a journal of type `J`, and produces an optional updated journal.
   */
  protected def appendToJournal: A => J => Option[J] =
    v => a => Some(ava.appendKey(a, v))

  /**
   * A method that represents a no-operation for updating a journal of type `J` with a value of type `A`.
   * It always returns `None` regardless of the inputs.
   *
   * @return a function that takes a value of type `A` and a journal of type `J`, always producing `None`.
   */
  //noinspection MutatorLikeMethodIsParameterless
  protected def doNothing: A => J => Option[J] =
    _ => _ => None

  /**
   * Method to construct a new Visitor[A, J] based on this visitor and the given journal.
   *
   * @param journal the journal to keep track of the visited elements.
   * @return a new Visitor[A, J].
   */
  def unit(journal: J): AbstractKeyedVisitor[A, X, J]

  /**
   * Non-pure method to close this Visitor.
   */
  def close(): Unit =
    journal match {
      case x: AutoCloseable =>
        x.close()
      case _ =>
    }
}

/**
 * Abstract base class which extends IterableVisitor[A, J].
 *
 * @param journal the journal which will record all visits.
 * @tparam A the type to be visited, typically the (key) attribute type of a vertex.
 * @tparam J the type of the journal for this visitor.
 *           Requires implicit evidence of type IterableJournal[J, A].
 */
abstract class AbstractIterableVisitor[A, J <: Iterable[A]](journal: J)(implicit val avai: IterableJournal[J, A]) extends AbstractVisitor[A, J](journal) with IterableVisitor[A, J] {
  self =>

  /**
   * Returns an iterator over the elements in the journal.
   *
   * @return an Iterator of type A, representing the elements in the journal.
   */
  def iterator: Iterator[A] =
    avai.iterator(journal)

  /**
   * Method to visit before processing the (child) A values.
   *
   * @param a (A) the value of this node (vertex).
   * @return an updated IterableVisitor[A, J].
   */
  // CONSIDER eliminate this asInstanceOf
  override def visitPre(a: A): IterableVisitor[A, J] =
    super.visitPre(a).asInstanceOf[IterableVisitor[A, J]]

  /**
   * Method to visit after processing the (child) A values.
   *
   * @param a (A) the value of this node (vertex).
   * @return an updated IterableVisitor[A, J].
   */
  // TODO eliminate this asInstanceOf
  override def visitPost(a: A): IterableVisitor[A, J] =
    super.visitPost(a).asInstanceOf[IterableVisitor[A, J]]
}

/**
 * A type-class trait that provides functionality to allow iteration over elements for collections
 * that are subtypes of `Iterable`. The trait is designed to enable iterating over a journal-like
 * data structure, where the journal is itself an instance of `Iterable[A]`.
 *
 * @tparam J the journal type, which must be a subtype of `Iterable[A]`.
 * @tparam A the type of elements contained in the journal.
 */
trait HasIterator[J <: Iterable[A], A] {
  /**
   * Returns an iterator over the elements in the given journal.
   *
   * @param journal the journal of type `J` to be iterated over.
   * @return an iterator of type `Iterator[A]` for the elements in the journal.
   */
  def iterator(journal: J): Iterator[A] =
    journal.iterator
}

/**
 * A type-class trait that represents a type-safe abstraction to interact with a Map-like structure.
 *
 * @tparam J the type of the map-like structure that this trait can operate on
 * @tparam K the type of the keys in the map
 * @tparam X the type of the values in the map
 */
trait IsMap[J <: Map[K, X], K, X] {
  /**
   * Retrieves the value associated with the specified key from the given map-like structure.
   *
   * @param j the map-like structure to retrieve the value from
   * @param k the key whose associated value is to be returned
   * @return an Option containing the value associated with the given key if it exists, or None if the key is not present
   */
  def get(j: J, k: K): Option[X] = j.get(k)
}
