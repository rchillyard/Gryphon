package littlegryphon.core

case class Vertex[V](attribute: V, connexions: Bag[Connexion[Vertex[V]]])(var discovered: Boolean = false) extends Attribute[V] {
  override def toString: String = s"v$attribute"
}

object Vertex {
  def create[V](attribute: V, connexions: Bag[Connexion[Vertex[V]]] = Bag.empty): Vertex[V] = new Vertex[V](attribute, connexions)()
}
