package com.phasmidsoftware.gryphon.core

/**
 * Evidence about whether an edge-attribute type `E` can represent the absence of an attribute.
 *
 * A [[VertexPair]] is a perfectly good thing: a connection between two vertices carrying
 * nothing, which is all a graph needs unless its edges have an attribute such as a weight. The
 * difficulty is only what happens when such a connection is admitted into a graph whose edges
 * are supposed to carry an `E`. Up to 1.11.0 both graph flavours admitted it and then lied
 * about the type:
 *
 *   - `DirectedGraph` built a `VertexPair` and later cast `OrderedEdge(...)`, a
 *     `DirectedEdge[V, Unit]`, to `DirectedEdge[V, E]`;
 *   - `UndirectedGraph` built `UndirectedEdge(None, ...)`, putting `None` in a field typed `E`.
 *
 * Both compile, and both hand the caller a value of the wrong type: reading `.attribute` on
 * such an edge in a `Double`-weighted graph throws `ClassCastException` at the use site, far
 * from the construction that caused it. See issue #56.
 *
 * This type class answers the question the construction code could not: is `E` a type for which
 * "no attribute" is itself a legitimate value? For `Unit` it is, and the value is `()`. For any
 * other type it is not, and a triplet without an attribute is refused at construction rather
 * than becoming a graph that fails later.
 *
 * @tparam E the edge-attribute type.
 */
trait AbsentAttribute[E]:
  /**
   * The value standing for an absent attribute, if `E` has one.
   *
   * @return `Some(value)` when `E` can represent an absent attribute, otherwise `None`.
   */
  def maybeValue: Option[E]

/**
 * The fallback, kept at lower priority so that a more specific instance wins.
 *
 * It is deliberately total: resolution always succeeds, so adding the requirement to
 * `triplesToTryGraph` does not force every intermediate signature to carry it. Where `E` is
 * still abstract — as in `JavaFacadeBridge`'s weighted-from-resource methods — this is the
 * instance found, and refusing an attributeless triplet is the right answer there.
 */
trait LowPriorityAbsentAttribute:
  /**
   * For an arbitrary `E`, there is no value meaning "no attribute".
   */
  given cannotBeAbsent[E]: AbsentAttribute[E] with
    def maybeValue: Option[E] = None

object AbsentAttribute extends LowPriorityAbsentAttribute:
  /**
   * `Unit` can represent an absent attribute, and the value is `()`.
   *
   * This is what makes `DirectedGraph[V, Unit]` and `UndirectedGraph[V, Unit]` — the ordinary
   * unweighted graphs, such as the ones `dfsu.graph` parses to — accept triplets with no
   * attribute, which is exactly what they should do.
   */
  given unitCanBeAbsent: AbsentAttribute[Unit] with
    def maybeValue: Option[Unit] = Some(())
