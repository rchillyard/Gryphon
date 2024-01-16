import com.phasmidsoftware.gryphon.tccore.{Attributed, Edge, EdgeLike, VertexPair}

trait WeightedVertexPair[V,E] extends EdgeLike[V] with Attributed[E]

implicit object WeightedVertexPairIntString extends WeightedVertexPair[Int, String] {
  def attribute(e: EdgeLike[Int]): String = vertices.toString()
  val vertices: (Int, Int) = ???
}
val v01: EdgeLike[Int] = new EdgeLike[Int] {
  val vertices: (Int, Int) = (0, 1)
}
v01.vertices

def followWeightedEdge[V, E <: EdgeLike[V] : Attributed](e: E)(v: V) = {
  println(v)
  println(e.other(v))
}

followWeightedEdge(v01)