/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.visit

import scala.collection.immutable.Queue

/**
 * Represents a general-purpose visitor structure for traversals and transformations.
 *
 * This trait is designed to process elements of type `A` while maintaining a journal of type `J`.
 * It supports two types of visits: "pre" (before visiting children in a traversal)
 * and "post" (after visiting children in a traversal). The visitor provides mechanisms
 * for resource cleanup, combining visitor states, and journal management.
 *
 * Instances of this trait typically implement functionality for depth-first or
 * breadth-first search traversals in graph-like or hierarchical structures, while
 * maintaining flexibility for custom behaviors.
 *
 * @tparam A The type of elements being visited.
 * @tparam J The type of journal used to record or store results during traversal.
 */
trait Visitor[A, J] extends AutoCloseable {

  /**
   * Indicates whether the visitor responds to "pre" visits.
   *
   * @return true if the visitor is in the "pre" state, false otherwise.
   */
  def isPre: Boolean

  /**
   * Indicates whether the visitor responds to "post" visits.
   * A Visitor must respond to either pre- or post-visits.
   *
   * @return true if the visitor is in the "post" state, false otherwise.
   */
  private def isPost: Boolean = !isPre

  /**
   * Defines the function that is the basis of this Visitor.
   *
   * @return A curried function that takes a value of type `A` and then a value of type `J`,
   *         returning an optional `J` representing the result of the transformation.
   */
  def function: A => J => Option[J]

  /**
   * Method to visit BEFORE processing the (child) `A` values in DFS.
   * For BFS, this method is the only visit to be performed.
   *
   * @param a (`A`) the type being visited.
   * @return an updated `Visitor[A, J]`.
   */
  def visitPre(a: A): Visitor[A, J] =
    doVisit(a, isPre, true)

  /**
   * Method to visit AFTER processing the (child) A values in DFS.
   * For BFS, this method is a no-op.
   *
   * @param a (A) the value of this node (vertex).
   * @return an updated Visitor[A, J].
   */
  def visitPost(a: A): Visitor[A, J] =
    doVisit(a, isPost, false)

  /**
   * The journal of all the pre- and post-invocations.
   */
  protected val journal: J

  /**
   * Retrieves a list of journals accumulated throughout the visitor's traversal.
   *
   * If the `next` visitor exists, its journals are appended with the current
   * visitor's journal. Otherwise, only the current journal is returned.
   *
   * @return a list of journals of type `J` collected by this visitor.
   */
  def journals: List[J] =
    next.fold(List(journal))(_.journals :+ journal) // TODO check this

  /**
   * Creates a new `Visitor` instance with the provided journal and an optional existing visitor as `next`.
   *
   * @param journal      The journal of type `J` to initialize or update the visitor with.
   * @param maybeVisitor An optional existing `Visitor[A, J]` instance to be replaced or updated.
   * @return A new or updated `Visitor[A, J]` instance.
   */
  def unit(journal: J, maybeVisitor: Option[Visitor[A, J]]): Visitor[A, J]

  /**
   * Retrieves the next `Visitor` instance, if available.
   *
   * The `next` visitor represents the next stage in the traversal or
   * processing sequence. If no further visitor is available, it returns `None`.
   *
   * @return an `Option` containing the next `Visitor[A, J]`, or `None` if no next visitor exists.
   */
  def next: Option[Visitor[A, J]]

  /**
   * Combines the current `Visitor` instance with the provided `Visitor` instance.
   *
   * This method creates a new `Visitor` using the journal of the provided visitor
   * and recursively combines it with the `next` visitor of the current instance
   * if one exists.
   *
   * @param visitor The `Visitor[A, J]` instance to combine with the current visitor.
   * @return A new `Visitor[A, J]` instance that represents the combined state of the two visitors.
   */
  def join(visitor: Visitor[A, J]): Visitor[A, J] =
    unit(visitor.journal, (next map (v => v.join(visitor))) orElse Some(visitor))

  /**
   * Closes any resources held by the implementing Visitor instance.
   *
   * This method is intended to release or clean up resources such as file writers,
   * journals, or other external dependencies associated with the Visitor object.
   *
   * @return Unit, indicating that the method performs a side effect without returning a value.
   */
  def close(): Unit = {
    next foreach (_.close())
    journal match {
      case x: AutoCloseable =>
        x.close()
      case _ =>
    }
  }

  /**
   * Performs a visit operation on the given value `a`, with optional application of a function.
   *
   * Depending on the `doFunction` flag, this method applies the visitor's function to `a` and updates
   * the visitor's journal. Additionally, if a `next` visitor is available, it invokes its `visitPre`
   * or `visitPost` as appropriate for further processing.
   *
   * @param a          The value of type `A` to be visited.
   * @param doFunction A boolean indicating whether the visitor's function should be applied to `a`.
   * @param pre        True if this is a pre-visit, otherwise false.
   */
  private def doVisit(a: A, doFunction: Boolean, pre: Boolean) =
    unit(
      if (doFunction)
        function(a)(journal) getOrElse journal
      else
        journal,
      next map (v => if (pre) v.visitPre(a) else v.visitPost(a))
    )
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
  def createPre[A](implicit ev: SimpleJournal[Queue[A], A]): PreVisitor[A, Queue[A]] =
    PreVisitor[A, Queue[A]]()
  //
  //  /**
  //   * Method to create a PreVisitor based on a PriorityQueue.
  //   *
  //   * @tparam A the type to be visited, typically the (key) attribute type of vertex.
  //   *           requires evidence of a Journal...
  //   * @return a PreVisitor of A and PriorityQueue[A]
  //   */
  //  def createPrioritizedPre[A: Ordering](implicit ev: Journal[PriorityQueueImmutable[A], A, A]): PreVisitor[A, PriorityQueueImmutable[A]] =
  //    PreVisitor[A, PriorityQueueImmutable[A]]()

  /**
   * Method to create a reverse PreVisitor.
   *
   * TODO generalize this so that it doesn't require a List.
   *
   * @tparam A the type to be visited, typically the (key) attribute type of vertex.
   *           Requires implicit evidence of type Journal...
   * @return a PreVisitor of A and List[A]
   */
  def reversePre[A](implicit ev: SimpleJournal[List[A], A]): PreVisitor[A, List[A]] =
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
  def createPostQueue[A](implicit ev: SimpleJournal[Queue[A], A]): PostVisitor[A, Queue[A]] =
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
  def reversePost[A](implicit ev: SimpleJournal[List[A], A]): PostVisitor[A, List[A]] =
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
   *           Requires evidence of an IterableJournal[Queue[A], A].
   * @return a PostVisitorIterable of A and Queue[A]
   */
  def createPostQueue[A](implicit ev: IterableJournal[Queue[A], A]): PostVisitorIterable[A, Queue[A]] =
    PostVisitorIterable[A, Queue[A]]()

  /**
   * Method to create a reverse PostVisitorIterable based on a List[A].
   *
   * @tparam A the type to be visited, typically the (key) attribute type of a vertex.
   *           Requires implicit evidence of an IterableJournal[List[A], A].
   * @return a PostVisitorIterable of A and List[A]
   */
  def reversePostList[A](implicit ev: IterableJournal[List[A], A]): PostVisitorIterable[A, List[A]] =
    PostVisitorIterable(ev.empty)
}

/**
 * Case class representing a generic PreVisitor.
 *
 * @param journal the journal which will record all visits.
 * @tparam A the type to be visited, typically the (key) attribute type of a vertex.
 * @tparam J the type of the journal.
 *           Requires implicit evidence of type Journal[J, A].
 */
case class PreVisitor[A, J](journal: J, next: Option[Visitor[A, J]] = None)(implicit val ev: SimpleJournal[J, A]) extends AbstractVisitor[A, J](journal, next) {

  /**
   * Indicates whether this visitor is a pre-order visitor.
   *
   * @return true if the visitor is pre-order, false otherwise.
   */
  val isPre: Boolean = true

  /**
   * A function that represents the pre-visit behavior in the `PreVisitor` class.
   * This function takes a value of type `A` and returns a function that takes
   * a journal of type `J`, producing an optional updated journal containing the appended value.
   *
   * The actual logic for appending the value to the journal is encapsulated
   * within the `appendToJournal` method, which leverages the implicit `Journal` instance.
   */
  val function: A => J => Option[J] = appendToJournal

  /**
   * Creates a new `Visitor` with the provided journal and optional next visitor.
   *
   * @param journal      the journal to keep track of visited elements.
   * @param maybeVisitor an optional next visitor to use for further processing.
   * @return a new `Visitor[A, J]` instance with the updated journal and next visitor.
   */
  def unit(journal: J, maybeVisitor: Option[Visitor[A, J]]): Visitor[A, J] =
    copy(journal = journal, next = maybeVisitor)

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
  def apply[A, J]()(implicit ev: SimpleJournal[J, A]): PreVisitor[A, J] =
    PreVisitor(ev.empty)
}

/**
 * Case class representing a generic PostVisitor.
 *
 * @param journal the journal which will record all visits.
 * @tparam A the type to be visited, typically the (key) attribute type of a vertex.
 * @tparam J the type of the journal.
 *           Requires implicit evidence of type Journal[J, A].
 */
case class PostVisitor[A, J](journal: J, next: Option[Visitor[A, J]] = None)(implicit val ev: SimpleJournal[J, A]) extends AbstractVisitor[A, J](journal, next) {

  /**
   * Determines if the visitor operates in 'pre-visit' mode.
   *
   * @return false, indicating that this visitor does not operate in a 'pre-visit' mode.
   */
  val isPre: Boolean = false

  /**
   * A function that appends a value of type `A` to a journal of type `J`, producing an optional updated journal.
   *
   * This function is defined by delegating to `appendToJournal`, which leverages the implicit `Journal` type class instance
   * to perform the append operation.
   *
   * @return a function taking a value of type `A` and a journal of type `J` to produce an optional updated journal.
   */
  val function: A => J => Option[J] = appendToJournal

  /**
   * Creates a new `Visitor` instance with the provided journal and an optional next visitor.
   *
   * This method allows updating or initializing a Visitor by setting its journal
   * and linking it to another Visitor as the next in the processing chain.
   *
   * @param journal      The journal of type `J` to initialize or update the Visitor with.
   * @param maybeVisitor An optional `Visitor[A, J]` instance to be used as the next visitor in the chain.
   * @return A new or updated `Visitor[A, J]` instance with the specified journal and next visitor.
   */
  def unit(journal: J, maybeVisitor: Option[Visitor[A, J]]): Visitor[A, J] =
    copy(journal = journal, next = maybeVisitor)
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
  def apply[A, J]()(implicit ev: SimpleJournal[J, A]): PostVisitor[A, J] =
    PostVisitor(ev.empty)
}

/**
 * Case class representing an iterable PreVisitor.
 *
 * @param journal the journal which will record all visits.
 * @tparam A the type to be visited, typically the (key) attribute type of a vertex.
 * @tparam J the type of the journal.
 *           Requires implicit evidence of type IterableJournal[J, A].
 */
case class PreVisitorIterable[A, J <: Iterable[A]](journal: J, next: Option[Visitor[A, J]] = None)(implicit val ev: IterableJournal[J, A]) extends AbstractIterableVisitor[A, J](journal, next) {
  /**
   * Flag indicating whether this visitor represents a pre-visit operation.
   *
   * Pre-visit operations are executed before performing the main operation associated with the visitor.
   */
  val isPre: Boolean = true

  /**
   * A curried function that appends a value of type `A` to a journal of type `J`.
   *
   * This function employs the `appendToJournal` method, which is based on the
   * implicit `IterableJournal` type class instance. It represents the pre-visit
   * behavior of the `PreVisitorIterable`, enabling updates to the journal during
   * the visiting process.
   *
   * @return A curried function that first takes a value of type `A`, then takes
   *         a journal of type `J`, and finally returns an optional updated journal.
   */
  val function: A => J => Option[J] = appendToJournal

  /**
   * Creates a new `Visitor` instance with the provided journal and an optional existing visitor as `next`.
   *
   * This method allows the creation or updating of a `Visitor` by substituting the journal
   * and optionally providing another `Visitor` instance to represent the next stage of traversal or processing.
   *
   * @param journal      The journal of type `J` to initialize or update the visitor with.
   * @param maybeVisitor An optional existing `Visitor[A, J]` instance to be used as the next visitor.
   * @return A new or updated `Visitor[A, J]` instance.
   */
  def unit(journal: J, maybeVisitor: Option[Visitor[A, J]]): Visitor[A, J] =
    copy(journal = journal, next = maybeVisitor)
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
    PreVisitorIterable(ev.empty)

  /**
   * Method to create a PreVisitorIterable based on a Queue -- without having to provide evidence of an IterableJournal[J, A].
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
case class PostVisitorIterable[A, J <: Iterable[A]](journal: J, next: Option[Visitor[A, J]] = None)(implicit val ev: IterableJournal[J, A]) extends AbstractIterableVisitor[A, J](journal, next) {
  /**
   * Indicates whether the visitor operates in pre-visit mode.
   *
   * This value is set to `false`, representing that the visitor operates in post-visit mode.
   */
  val isPre: Boolean = false

  /**
   * A higher-order function used to append an element of type `A` to a journal of type `J`.
   *
   * This function utilizes the `appendToJournal` method. It accepts a value of type `A` and produces
   * a function that takes a journal of type `J`, returning an optionally updated journal.
   */
  val function: A => J => Option[J] = appendToJournal

  /**
   * Creates a new `Visitor` instance with an updated journal and an optional next visitor.
   *
   * @param journal      The journal of type `J` to set for the new `Visitor` instance.
   * @param maybeVisitor An optional `Visitor[A, J]` instance to be set as the next visitor.
   * @return A new `Visitor[A, J]` instance with the specified journal and next visitor.
   */
  def unit(journal: J, maybeVisitor: Option[Visitor[A, J]]): Visitor[A, J] =
    copy(journal = journal, next = maybeVisitor)
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
    PostVisitorIterable(ev.empty)

  /**
   * Method to create a PostVisitorIterable based on a Queue -- without having to provide evidence of an IterableJournal[J, A].
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
 * A case class representing a "pre-keyed visitor" that performs specific actions
 * on a journal associated with visited elements before processing them.
 *
 * This visitor operates in the context of a keyed journal, leveraging the implicit
 * `KeyedJournal` type class instance to manage the journal and its entries.
 *
 * @tparam A The type of the value being visited, typically a key or attribute of a vertex.
 * @tparam X The type of the associated value in the keyed journal entries.
 * @tparam J The type of the journal used to record the visited elements.
 * @param journal The journal instance used for tracking visited elements during processing.
 * @param ev      An implicit instance of the `KeyedJournal` type class, providing operations for keyed journals.
 */
case class PreKeyedVisitor[A, X, J](journal: J, next: Option[Visitor[A, J]] = None)(implicit val ev: KeyedJournal[J, A, X]) extends AbstractKeyedVisitor[A, X, J](journal, next) {
  /**
   * A flag indicating whether the current visitor instance represents the "pre" state in the visiting process.
   * This is used to manage the state and behavior of the visitor during traversal.
   */
  val isPre: Boolean = true

  /**
   * A function that appends a value of type `A` to a journal of type `J`, producing an optional updated journal.
   * This is defined using the `appendToJournal` method, relying on the implicit `Journal` type class instance.
   */
  val function: A => J => Option[J] = appendToJournal

  /**
   * Creates a new `Visitor` instance with the updated journal and an optional next visitor.
   *
   * This method is used to create a copy of the current visitor while replacing the journal
   * or specifying a subsequent visitor in the process chain. It can be used to update the
   * state of the visitor during traversal or transformation.
   *
   * @param journal      The updated journal of type `J` to replace the existing one in the visitor.
   * @param maybeVisitor An optional next `Visitor[A, J]` to define the subsequent processing step.
   * @return A new `Visitor[A, J]` instance with the specified journal and next visitor.
   */
  def unit(journal: J, maybeVisitor: Option[Visitor[A, J]]): Visitor[A, J] =
    copy(journal = journal, next = maybeVisitor)
}

/**
 * Companion object for the `PreKeyedVisitor` class, providing utility methods for constructing instances
 * of `PreKeyedVisitor` with specific journal and value types.
 */
object PreKeyedVisitor {

  /**
   * Creates a new instance of `PostKeyedVisitor` with an empty journal, utilizing the implicit
   * `KeyedJournal` instance for journal operations.
   *
   * @param ev An implicit instance of the `KeyedJournal` type class, which provides the functionality
   *           for managing journals of type `J`, where each entry is associated with a value of type `A` and
   *           an associated value of type `X`.
   * @return A new instance of `PostKeyedVisitor` initialized with an empty journal.
   */
  def create[A, X, J]()(implicit ev: KeyedJournal[J, A, X]): PreKeyedVisitor[A, X, J] =
    PreKeyedVisitor(ev.empty)
}

/**
 * A case class representing a "post-keyed visitor" that performs specific actions
 * on a journal associated with visited elements after processing them.
 *
 * This visitor operates in the context of a keyed journal, leveraging the implicit
 * `KeyedJournal` type class instance to manage the journal and its entries.
 *
 * @tparam A The type of the value being visited, typically a key or attribute of a vertex.
 * @tparam X The type of the associated value in the keyed journal entries.
 * @tparam J The type of the journal used to record the visited elements.
 * @param journal The journal instance used for tracking visited elements during processing.
 * @param ev      An implicit instance of the `KeyedJournal` type class, providing operations for keyed journals.
 */
case class PostKeyedVisitor[A, X, J](journal: J, next: Option[Visitor[A, J]] = None)(implicit val ev: KeyedJournal[J, A, X]) extends AbstractKeyedVisitor[A, X, J](journal, next) {
  /**
   * A flag indicating whether the current visitor instance represents the "pre" state in the visiting process.
   * This is used to manage the state and behavior of the visitor during traversal.
   */
  val isPre: Boolean = false

  /**
   * A function that appends a value of type `A` to a journal of type `J`, producing an optional updated journal.
   * This is defined using the `appendToJournal` method, relying on the implicit `Journal` type class instance.
   */
  val function: A => J => Option[J] = appendToJournal

  /**
   * Creates a new `Visitor` instance with the updated journal and an optional next visitor.
   *
   * This method is used to create a copy of the current visitor while replacing the journal
   * or specifying a subsequent visitor in the process chain. It can be used to update the
   * state of the visitor during traversal or transformation.
   *
   * @param journal      The updated journal of type `J` to replace the existing one in the visitor.
   * @param maybeVisitor An optional next `Visitor[A, J]` to define the subsequent processing step.
   * @return A new `Visitor[A, J]` instance with the specified journal and next visitor.
   */
  def unit(journal: J, maybeVisitor: Option[Visitor[A, J]]): Visitor[A, J] =
    copy(journal = journal, next = maybeVisitor)
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
    PostKeyedVisitor(ev.empty)
}

/**
 * Trait to define the behavior of a Visitor which can be iterated.
 *
 * @tparam A the type to be visited, typically the (key) attribute type of a vertex.
 * @tparam J the type of the journal for this visitor.
 */
trait IterableVisitor[A, J <: Iterable[A]] extends Visitor[A, J] with IterableOnce[A] {
  /**
   * Visits the element of type `A` before its children during a depth-first traversal.
   * Converts the result to `IterableVisitor[A, J]`.
   *
   * @param a the element of type `A` to be visited.
   * @return an updated instance of `IterableVisitor[A, J]`.
   */
  override def visitPre(a: A): IterableVisitor[A, J] =
    super.visitPre(a).asInstanceOf[IterableVisitor[A, J]]

  /**
   * Visits the current node after processing its child nodes in a depth-first traversal.
   * For breadth-first traversal, this method does not perform any operations.
   *
   * @param a the value of the current node to be visited.
   * @return an updated IterableVisitor[A, J] representing the new state of the visitor.
   */
  override def visitPost(a: A): IterableVisitor[A, J] =
    super.visitPost(a).asInstanceOf[IterableVisitor[A, J]]
}

/**
 * Abstract base class which extends Visitor[A, J].
 *
 * @param journal the journal which will record all visits.
 * @tparam A the type to be visited, typically the (key) attribute type of a vertex.
 * @tparam J the type of the journal for this visitor.
 *           Requires implicit evidence of type Journal[J, A].
 */
abstract class AbstractVisitor[A, J](journal: J, next: Option[Visitor[A, J]])(implicit val ava: SimpleJournal[J, A]) extends Visitor[A, J] {
  self =>
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
abstract class AbstractKeyedVisitor[A, X, J](journal: J, next: Option[Visitor[A, J]])(implicit val ava: KeyedJournal[J, A, X]) extends Visitor[A, J] {
  self =>

  /**
   * Method to append a value of type `A` to a journal of type `J`, utilizing the implicit `Journal` type class instance.
   *
   * @return a function that takes a value of type `A`, then a journal of type `J`, and produces an optional updated journal.
   */
  protected def appendToJournal: A => J => Option[J] =
    v => a => Some(ava.appendKey(a, v))
}

/**
 * Abstract base class which extends IterableVisitor[A, J].
 *
 * @param journal the journal which will record all visits.
 * @tparam A the type to be visited, typically the (key) attribute type of a vertex.
 * @tparam J the type of the journal for this visitor.
 *           Requires implicit evidence of type IterableJournal[J, A].
 */
abstract class AbstractIterableVisitor[A, J <: Iterable[A]](journal: J, next: Option[Visitor[A, J]] = None)(implicit val avai: IterableJournal[J, A]) extends AbstractVisitor[A, J](journal, next) with IterableVisitor[A, J] {
  self =>

  /**
   * Returns an iterator over the elements in the journal.
   *
   * @return an Iterator of type A, representing the elements in the journal.
   */
  def iterator: Iterator[A] =
    avai.iterator(journal)
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
