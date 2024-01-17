import littlegryphon.core.{Vertex, VertexMap, VertexPair}

val v0: Vertex[Int] = Vertex.create(0)
val v1: Vertex[Int] = Vertex.create(1)


new VertexPair[Int](v0, v1)

VertexMap.empty()