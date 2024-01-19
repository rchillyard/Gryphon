import com.phasmidsoftware.gryphon.core.{Connexion, Vertex, VertexMap}

val u0 = 0
val u1 = 1
val w0: Vertex[Int] = Vertex.create(u0)
val w1: Vertex[Int] = Vertex.create(u1)

val c01: Connexion[Int] = Pair(u0, u1)

val e01: VertexPair[Int] = new VertexPair[Int](w0, w1)

val m_ : VertexMap[Int] = VertexMap.empty[Int]

val m01: VertexMap[Int] = m_ + c01

m01

