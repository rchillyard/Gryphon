import com.phasmidsoftware.gryphon.core.{Connexion, Node, Pair, VertexMap}

val u0 = 0
val u1 = 1
val w0: Node[Int] = Node.create(u0)
val w1: Node[Int] = Node.create(u1)

val c01: Connexion[Int, Node] = Pair(u0, w1)

val e01: Pair[Int] = new Pair[Int](u0, w1)

val m_ : VertexMap[Int] = VertexMap.empty[Int]

val m01: VertexMap[Int] = m_ + c01

m01

