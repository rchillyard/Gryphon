///*
// * Copyright (c) 2023. Phasmid Software
// */
//
//package com.phasmidsoftware.gryphon.oldvisit
//
//import com.phasmidsoftware.gryphon.core.OrderedEdge
//import com.phasmidsoftware.gryphon.util.PriorityQueueImmutable
//
//import java.io.FileWriter
//import scala.collection.immutable.Queue
//import scala.collection.mutable
//
///**
// * Type-class trait to define the behavior of a Journal of `Z` elements.
// * It is used in the `Visitor` trait as something that can receive the `A`
// * instances which can be converted into `Z` elements.
// * `Journal` does not support an `Iterator`, since many journals might simply be a log file of some sort.
// *
// * @tparam J the type of the journal
// * @tparam A the type to be converted
// * @tparam Z the target type after conversion
// */
//trait Journal[A, Z] extends Appendable[Z] with AutoCloseable {
//  
//  def append(z: Z): Journal[A, Z]
//  
//  /**
//   * Converts an instance of type `A` to an instance of type `Z`.
//   *
//   * @param a the input value of type `A` to be converted.
//   * @return the resulting value of type `Z` after conversion.
//   */
//  def convert(a: A): Z
//  
//  
//
//  /**
//   * Adds an entry of type `A` to the journal of type `J` by first converting it to the target type `Z`
//   * and then appending it to the journal.
//   *
//   * @param j the journal of type `J` where the entry will be added.
//   * @param a the input value of type `A` to be added to the journal after conversion.
//   * @return an updated journal of type `J` containing the newly added entry.
//   */
//  def addEntry(a: A): Journal[A, Z] = append(convert(a))
//}
//
///**
// * A simplified implementation of the `Journal` type-class that enforces
// * the input type `A` and output type `Z` to be the same.
// *
// * This trait is particularly useful for scenarios where no transformation
// * of the journal entries is required, and the input type `A` is retained.
// *
// * @tparam J the type of the journal
// * @tparam A the type of the entries in the journal
// */
//trait SimpleJournal[A] extends Journal[A, A] {
//  /**
//   * Identity method.
//   *
//   * @param a the input value of type `A` to be converted.
//   * @return the input value.
//   */
//  def convert(a: A): A = a
//}
//
///**
// * A type-class trait that extends the `Journal` trait, specifically designed to handle journals where
// * each record is represented as a tuple containing a key of type `K` and a corresponding value of type `X`.
// * The `KeyedJournal` introduces additional behavior to manage and process elements based on their keys.
// *
// * @tparam J The type of the journal.
// * @tparam K The type of the key used for each entry in the journal.
// * @tparam X The type of the value associated with a key in the journal.
// */
//trait KeyedJournal[K, X] extends Journal[K, (K, X)] {
//  /**
//   * Retrieves a value that can then be associated with the given key in the journal.
//   *
//   * @param k The key of type `K` for which the associated value is to be retrieved.
//   * @return The value of type `X` associated with the provided key.
//   */
//  def fulfill(k: K): X
//
//  /**
//   * Converts an instance of type `A` to an instance of type `Z`.
//   *
//   * @param a the input value of type `A` to be converted.
//   * @return the resulting value of type `Z` after conversion.
//   */
//  def convert(a: K): (K, X) = a -> fulfill(a)
//}
//
///**
// * Trait which combines the behaviors of `Journal` and `HasIterator`.
// *
// * @tparam J the (iterable) journal type for this `IterableJournal` (`J` must be a subclass of `Iterable[X]`).
// * @tparam X the underlying type of the journal.
// */
//trait IterableJournal[X] extends SimpleJournal[X] with Iterable[X]
//
///**
// * Trait which combines the behaviors of `Journal` and `HasIterator`.
// *
// * @tparam J the (iterable) journal type for this `IterableJournal` (`J` must be a subclass of `Iterable[X]`).
// * @tparam X the underlying type of the journal.
// */
//trait MappedJournal[K, X] extends KeyedJournal[K, X] with Iterable[(K, X)]
//
///**
// * A trait that represents a mapped journal specifically for `Map` data structures.
// * This combines the behavior of a `MappedJournal` with the use of a key-value structure.
// *
// * @tparam V the type of keys in the map.
// * @tparam T the type of values in the map.
// */
//trait MappedJournalMap[V, T] extends MappedJournal[Map[V, T], V, T] {
//  /**
//   * An empty journal.
//   */
//  def empty: Map[V, T] =
//    Map.empty
//
//  /**
//   * Method to append a `V` value to this `Journal`.
//   *
//   * @param j the journal to be appended to.
//   * @param z an instance of `(V, T)` to be appended to `j`.
//   * @return a new `Journal`.
//   */
//  def append(j: Map[V, T], z: (V, T)): Map[V, T] =
//    j + z
//}
//
///**
// * A trait that extends the functionality of `IterableJournal` to work with `Queue` as the journal type.
// *
// * @tparam Z the type of elements stored in the queue.
// */
//trait IterableJournalQueue[Z] extends IterableJournal[Queue[Z], Z] {
//
//  /**
//   * Represents an empty `Queue` of type `Z`.
//   * This defines the base case or starting state for operations
//   * performed on the queue within the `IterableJournalQueue` trait.
//   */
//  val empty: Queue[Z] =
//    Queue.empty
//
//  /**
//   * Appends an element to the end of the specified queue.
//   *
//   * @param q the queue to which the element will be added
//   * @param z the element to append to the queue
//   * @return a new queue with the element appended
//   */
//  def append(q: Queue[Z], z: Z): Queue[Z] =
//    q.enqueue(z)
//}
//
///**
// * A trait that extends the behavior of `IterableJournal` specifically for a stack-like data structure
// * using `List[Z]` as the journal type.
// * This trait provides stack-specific implementations
// * such as appending elements in a last-in-first-out (LIFO) manner.
// *
// * @tparam Z the type of the elements in the journal.
// */
//trait IterableJournalStack[Z] extends IterableJournal[List[Z], Z] {
//  /**
//   * An empty journal of type `List[Z]`.
//   * Represents the initial state of the stack, containing no elements.
//   */
//  val empty: List[Z] =
//    List.empty
//
//  /**
//   * Appends an element to the beginning of the given list in a last-in-first-out (LIFO) manner.
//   *
//   * @param q the list to which the element will be appended
//   * @param z the value to append to the list
//   * @return a new list with the value appended to the beginning
//   */
//  def append(q: List[Z], z: Z): List[Z] =
//    z :: q
//}
//
///**
// * A type-class trait extending `Journal` to provide specific behavior for journals 
// * using `StringBuilder` as the journal type to store values of type `Z`.
// *
// * @tparam Z the underlying type of the values stored in the journal.
// */
//trait StringBuilderJournal[Z] extends Appendable[StringBuilder, Z] {
//
//  /**
//   * Creates and returns a new, empty StringBuilder instance.
//   *
//   * @return an empty StringBuilder.
//   */
//  def empty: StringBuilder =
//    new StringBuilder()
//
//  /**
//   * Appends a value of type `Z` to the provided `StringBuilder` journal.
//   *
//   * @param j the `StringBuilder` instance to which the value will be appended
//   * @param z the value of type `Z` to append to the `StringBuilder`
//   * @return the updated `StringBuilder` instance with the appended value
//   */
//  def append(j: StringBuilder, z: Z): StringBuilder =
//    j.append(s"$z\n")
//}
//
///**
// * Trait representing a Journal specifically for `FileWriter` and values of type `Z`.
// *
// * This implementation uses a file named "FileWriterJournal.txt" when no explicit journal is provided.
// *
// * @tparam Z the type of the elements to be written to the journal.
// */
//trait FileWriterJournal[Z] extends Appendable[FileWriter, Z] {
//  /**
//   * This method is used only when no explicit Journal is defined for a Visitor[FileWriter, Int].
//   *
//   * @return a new FileWriter based on the file called "FileWriterJournal.txt".
//   */
//  def empty: FileWriter =
//    new FileWriter("FileWriterJournal.txt")
//
//  /**
//   * Method to append a Z value to a journal.
//   *
//   * @param j the journal to be appended to.
//   * @param z an instance of Z to be appended to the journal j.
//   * @return a new journal.
//   */
//  def append(j: FileWriter, z: Z): FileWriter = {
//    j.append(s"$z\n")
//    j
//  }
//}
//
///**
// * A specialization of the `Journal` type class for maintaining an immutable priority queue of `OrderedEdge` elements.
// *
// * This trait provides functionality for creating an empty immutable priority queue and appending
// * `OrderedEdge` elements to it. The underlying data structure is a priority queue that respects the ordering
// * defined for `OrderedEdge`.
// *
// * @tparam E the type of the attribute associated with edges, used for determining their priority.
// * @tparam V the type of vertices connected by the edges.
// */
//trait PriorityQueueJournal[E, V] extends Journal[PriorityQueueImmutable[OrderedEdge[E, V]], E, OrderedEdge[E, V]] {
//  def empty: PriorityQueueImmutable[OrderedEdge[E, V]] =
//    PriorityQueueImmutable(scala.collection.mutable.PriorityQueue.empty[OrderedEdge[E, V]])
//
//  def append(j: PriorityQueueImmutable[OrderedEdge[E, V]], z: OrderedEdge[E, V]): PriorityQueueImmutable[OrderedEdge[E, V]] =
//    j.insert(z)
//}
//
//
///**
// * Companion object to Journal.
// *
// * Here, you may find the various implicit objects that extend `Journal[J, X]`.
// */
//object Journal {
//  /**
//   * An implicit object that extends the `StringBuilderJournal` trait for `Int` values.
//   *
//   * This implementation provides functionality to manage journals based on `StringBuilder`
//   * for storing and appending integer values.
//   *
//   * It uses a `StringBuilder` as the underlying journal structure and appends `Int` values
//   * in a string representation, separated by newlines.
//   */
//  implicit object StringBuilderJournalInt$$ extends StringBuilderJournal[Int]
//
//  /**
//   * An implicit object that provides a default implementation of `FileWriterJournal` for `Int`.
//   *
//   * It defines the methods specific to journaling integer values using a `FileWriter`.
//   *
//   * The `empty` method in this implementation creates a new `FileWriter` bound to 
//   * "FileWriterJournal.txt" when no explicit journal is defined for `Visitor[FileWriter, Int]`.
//   *
//   * The `append` method allows appending integer values to the journal represented 
//   * by a `FileWriter` instance.
//   */
//  implicit object FileWriterJournalInt$$ extends FileWriterJournal[Int]
//
//  /**
//   * Implicit object extending `IterableJournalQueue[Int]` to provide functionality 
//   * for working with `Queue[Int]` as the journal type.
//   *
//   * This object enables the integration of journal-like operations for `Int` values 
//   * using a `Queue` as the underlying structure.
//   */
//  implicit object IterableJournalQueueInt$$ extends IterableJournalQueue[Int]
//
//  /**
//   * An implicit object representing an implementation of `IterableJournalStack` for `Int` types.
//   *
//   * This object extends the `IterableJournalStack` trait where the underlying journal
//   * is a stack-like data structure (`List[Int]`).
//   * The operations are handled in a
//   * last-in-first-out (LIFO) manner specific to stacks.
//   */
//  implicit object IterableJournalStackInt$$ extends IterableJournalStack[Int]
//
//  /**
//   * Implicit object for managing a `Journal` instance that uses `StringBuilder` as the journal type
//   * and `String` as the value type.
//   *
//   * This implementation defines how to initialize an empty journal and append `String` values
//   * to the `StringBuilder` journal.
//   */
//  implicit object StringBuilderJournalString$$ extends StringBuilderJournal[String]
//
//  /**
//   * An implicit object implementing `FileWriterJournal[String]`.
//   * This serves as a concrete instance of `FileWriterJournal` specialized for `String` values. 
//   *
//   * It provides functionality to handle journals represented by `FileWriter` and allows 
//   * appending `String` values to them.
//   */
//  implicit object FileWriterJournalString$$ extends FileWriterJournal[String]
//
//  /**
//   * Implicit object for providing `IterableJournalQueue` functionality for `String` type.
//   * It allows journal operations using a `Queue[String]` as the underlying data structure.
//   */
//  implicit object IterableJournalQueueString$$ extends IterableJournalQueue[String]
//
//  /**
//   * Implicit object that provides a stack-like journal implementation for `String` values
//   * using the `IterableJournalStack` trait.
//   * This uses `List[String]` to maintain a 
//   * last-in-first-out (LIFO) order for journal entries.
//   */
//  implicit object IterableJournalStackString$$ extends IterableJournalStack[String]
//}
