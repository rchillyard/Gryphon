package com.phasmidsoftware.gryphon.traverse

import com.phasmidsoftware.gryphon.core
import com.phasmidsoftware.gryphon.util.PriorityQueueImmutable
import com.phasmidsoftware.gryphon.visit.{Journal, Visitor}

object ShortestPaths {

  //  def dijkstra[V, E: Ordering](traversable: core.Traversable[V]): Traversal[V, (E, V)] = {
  //    implicit object PriorityQueueJournal extends Journal[PriorityQueueImmutable[E], E] {}
  //    val visitor = Visitor.createPrioritizedPre[E]
  //    traversable.bfs(visitor)
  //
  //  }
}
