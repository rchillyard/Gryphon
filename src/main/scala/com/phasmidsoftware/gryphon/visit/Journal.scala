/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.visit

import java.io.FileWriter
import scala.collection.immutable.Queue

/**
 * Type-class trait to define the behavior of a Journal of `V` elements.
 * It is used in the `Visitor` trait as something that can receive the `V` elements.
 * `Journal` does not support an `Iterator`, since many journals might simply be a log file of some sort.
 *
 * @tparam J the journal type.
 * @tparam V the underlying type of the journal.
 */
trait Journal[J, V] {

  /**
   * An empty journal.
   */
  def empty: J

  /**
   * Method to append a `V` value to this `Journal`.
   *
   * @param j the journal to be appended to.
   * @param v an instance of `V` to be appended to `j`.
   * @return a new `Journal`.
   */
  def append(j: J, v: V): J
}

/**
 * Trait which combines the behaviors of `Journal` and `HasIterator`.
 *
 * @tparam J the (iterable) journal type for this `IterableJournal` (`J` must be a subclass of `Iterable[V]`).
 * @tparam V the underlying type of the journal.
 */
trait IterableJournal[J <: Iterable[V], V] extends Journal[J, V] with HasIterator[J, V]

/**
 * A trait that extends the functionality of `IterableJournal` to work with `Queue` as the journal type.
 *
 * @tparam V the type of elements stored in the queue.
 */
trait IterableJournalQueue[V] extends IterableJournal[Queue[V], V] {
  /**
   * Represents an empty `Queue` of type `V`.
   * This defines the base case or starting state for operations
   * performed on the queue within the `IterableJournalQueue` trait.
   */
  val empty: Queue[V] =
    Queue.empty

  /**
   * Appends an element to the end of the specified queue.
   *
   * @param q the queue to which the element will be added
   * @param v the element to append to the queue
   * @return a new queue with the element appended
   */
  def append(q: Queue[V], v: V): Queue[V] =
    q.enqueue(v)
}

/**
 * A trait that extends the behavior of `IterableJournal` specifically for a stack-like data structure
 * using `List[V]` as the journal type.
 * This trait provides stack-specific implementations
 * such as appending elements in a last-in-first-out (LIFO) manner.
 *
 * @tparam V the type of the elements in the journal.
 */
trait IterableJournalStack[V] extends IterableJournal[List[V], V] {
  /**
   * An empty journal of type `List[V]`.
   * Represents the initial state of the stack, containing no elements.
   */
  val empty: List[V] =
    List.empty

  /**
   * Appends an element to the beginning of the given list in a last-in-first-out (LIFO) manner.
   *
   * @param q the list to which the element will be appended
   * @param v the value to append to the list
   * @return a new list with the value appended to the beginning
   */
  def append(q: List[V], v: V): List[V] =
    v :: q
}

/**
 * A type-class trait extending `Journal` to provide specific behavior for journals 
 * using `StringBuilder` as the journal type to store values of type `V`.
 *
 * @tparam V the underlying type of the values stored in the journal.
 */
trait StringBuilderJournal[V] extends Journal[StringBuilder, V] {
  /**
   * Creates and returns a new, empty StringBuilder instance.
   *
   * @return an empty StringBuilder.
   */
  def empty: StringBuilder =
    new StringBuilder()

  /**
   * Appends a value of type `V` to the provided `StringBuilder` journal.
   *
   * @param j the `StringBuilder` instance to which the value will be appended
   * @param v the value of type `V` to append to the `StringBuilder`
   * @return the updated `StringBuilder` instance with the appended value
   */
  def append(j: StringBuilder, v: V): StringBuilder =
    j.append(s"$v\n")
}

trait FileWriterJournal[V] extends Journal[FileWriter, V] {
  /**
   * This method is used only when no explicit Journal is defined for a Visitor[FileWriter, Int].
   *
   * @return a new FileWriter based on the file called "FileWriterJournal.txt".
   */
  def empty: FileWriter =
    new FileWriter("FileWriterJournal.txt")

  /**
   * Method to append a V value to a journal.
   *
   * @param j the journal to be appended to.
   * @param v an instance of V to be appended to the journal j.
   * @return a new journal.
   */
  def append(j: FileWriter, v: V): FileWriter = {
    j.append(s"$v\n"); j
  }
}

/**
 * Companion object to Journal.
 *
 * Here, you may find the various implicit objects that extend `Journal[J, V]`.
 */
object Journal {
  /**
   * An implicit object that extends the `StringBuilderJournal` trait for `Int` values.
   *
   * This implementation provides functionality to manage journals based on `StringBuilder`
   * for storing and appending integer values.
   *
   * It uses a `StringBuilder` as the underlying journal structure and appends `Int` values
   * in a string representation, separated by newlines.
   */
  implicit object StringBuilderJournalInt$$ extends StringBuilderJournal[Int]

  /**
   * An implicit object that provides a default implementation of `FileWriterJournal` for `Int`.
   *
   * It defines the methods specific to journaling integer values using a `FileWriter`.
   *
   * The `empty` method in this implementation creates a new `FileWriter` bound to 
   * "FileWriterJournal.txt" when no explicit journal is defined for `Visitor[FileWriter, Int]`.
   *
   * The `append` method allows appending integer values to the journal represented 
   * by a `FileWriter` instance.
   */
  implicit object FileWriterJournalInt$$ extends FileWriterJournal[Int]

  /**
   * Implicit object extending `IterableJournalQueue[Int]` to provide functionality 
   * for working with `Queue[Int]` as the journal type.
   *
   * This object enables the integration of journal-like operations for `Int` values 
   * using a `Queue` as the underlying structure.
   */
  implicit object IterableJournalQueueInt$$ extends IterableJournalQueue[Int]

  /**
   * An implicit object representing an implementation of `IterableJournalStack` for `Int` types.
   *
   * This object extends the `IterableJournalStack` trait where the underlying journal
   * is a stack-like data structure (`List[Int]`).
   * The operations are handled in a
   * last-in-first-out (LIFO) manner specific to stacks.
   */
  implicit object IterableJournalStackInt$$ extends IterableJournalStack[Int]

  /**
   * Implicit object for managing a `Journal` instance that uses `StringBuilder` as the journal type
   * and `String` as the value type.
   *
   * This implementation defines how to initialize an empty journal and append `String` values
   * to the `StringBuilder` journal.
   */
  implicit object StringBuilderJournalString$$ extends StringBuilderJournal[String]

  /**
   * An implicit object implementing `FileWriterJournal[String]`.
   * This serves as a concrete instance of `FileWriterJournal` specialized for `String` values. 
   *
   * It provides functionality to handle journals represented by `FileWriter` and allows 
   * appending `String` values to them.
   */
  implicit object FileWriterJournalString$$ extends FileWriterJournal[String]

  /**
   * Implicit object for providing `IterableJournalQueue` functionality for `String` type.
   * It allows journal operations using a `Queue[String]` as the underlying data structure.
   */
  implicit object IterableJournalQueueString$$ extends IterableJournalQueue[String]

  /**
   * Implicit object that provides a stack-like journal implementation for `String` values
   * using the `IterableJournalStack` trait.
   * This uses `List[String]` to maintain a 
   * last-in-first-out (LIFO) order for journal entries.
   */
  implicit object IterableJournalStackString$$ extends IterableJournalStack[String]
}
