package com.phasmidsoftware.gryphon.core

import com.phasmidsoftware.gryphon.parse.Parseable
import com.phasmidsoftware.gryphon.util.FP
import scala.util.Try

/**
 * A graph described as a list of vertex pairs, each carrying an edge type but no attribute.
 *
 * One of the three shapes a graph can be unbuilt into: the attributeless corner, where only the
 * connections and their direction matter and `E` is therefore `Unit`. See #135.
 *
 * It has been here before. #70 moved it to the test tree in 1.11.0 because in the published
 * library it was reachable but inert, exactly as `EdgeList` was: its one consumer,
 * `VertexMap.createFromVertexPairList`, did nothing but read `.pairs` and hand them to
 * `addVertexPairs`, which already took the raw sequence. That consumer is still gone and is not
 * coming back — `VertexMap[V].addVertexPairs(pairs)` says the same thing. What brings this type
 * back is the other direction: rendering a graph, not building one.
 *
 * Until #137 this type had a twin, `ProtoConnexions`, holding the same `Seq[(V, V, Z)]` and
 * defining `triplets` with the same line — the difference being only that this one fixed `Z` to
 * `EdgeType` and extended `Pairs`. They were one type written twice, and the 2x2 that #135
 * documented (attributed or not, against edge objects or raw tuples) therefore had one cell
 * occupied twice. `ProtoConnexions` was the name nothing used: its sole call site was
 * `SerializableGraph.createFromPairs`, and its `parse` had none at all. This type kept the name
 * and took the generality, so `createFromPairs` need not narrow.
 *
 * @tparam V the type of the vertices.
 * @tparam Z the edge-type parameter. Fixed to `EdgeType` by every caller in this library, but
 *           left open because `createFromPairs` is generic in it and narrowing published API to
 *           suit the common case would be a loss for nothing.
 * @param pairs a sequence of tuples, each holding two vertices and the type of the connexion
 *              between them.
 */
case class VertexPairList[V, Z](pairs: Seq[(V, V, Z)]) extends SerializableGraph[V, Unit, Z] with Pairs[V, Z]:
  /**
   * Transforms the pairs of vertices into a sequence of triplets.
   *
   * The attribute is `None` throughout: this shape carries none, which is what distinguishes it
   * from `Triplets`. `serialize` renders an absent attribute by omitting it, so a pair becomes
   * `0 = 1` rather than `0 = 1 ()`.
   *
   * @return a sequence of triplets, each holding a source vertex, a target vertex, `None` as the
   *         attribute, and the edge type.
   */
  def triplets: Seq[Triplet[V, Unit, Z]] =
    pairs.map(p => Triplet(p._1, p._2, None, p._3))

/**
 * Companion object for `VertexPairList`, providing the parsing direction.
 */
object VertexPairList {
  /**
   * Builds a `VertexPairList` from pairs whose three components are still strings.
   *
   * This is the string-parsing entry to the pair-shaped description, and so the counterpart of the
   * rendering direction that #135 gave `SerializableGraph.serialize`. It came across from
   * `ProtoConnexions` in #137 — where nothing called it and nothing tested it — and gained a test
   * on the way.
   *
   * Failure is propagated rather than skipped: one unparseable component fails the whole sequence,
   * which is `FP.sequence`'s doing and is the right behaviour for a description that is meant to
   * round-trip.
   *
   * @param pairs the pairs, as `(from, to, edgeType)` strings.
   * @tparam V the vertex type, which must be parseable.
   * @tparam Z the edge-type parameter, which must be parseable.
   * @return the parsed `VertexPairList` in a `Success`, or the first failure encountered.
   */
  def parse[V: Parseable, Z: Parseable](pairs: Seq[(String, String, String)]): Try[VertexPairList[V, Z]] = {
    val tys: Seq[Try[(V, V, Z)]] = for {
      (x, y, z) <- pairs
    } yield for {
      vx <- implicitly[Parseable[V]].parse(x)
      vy <- implicitly[Parseable[V]].parse(y)
      q <- implicitly[Parseable[Z]].parse(z)
    } yield (vx, vy, q)
    FP.sequence(tys) map (VertexPairList(_))
  }
}
