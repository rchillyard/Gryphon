package littlegryphon.core

import littlegryphon.core.Edge

case class EdgeList[V, E](edges: Seq[Edge[E, V]])
