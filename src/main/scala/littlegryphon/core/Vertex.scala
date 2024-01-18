package littlegryphon.core

case class Vertex[V](attribute: V)(val connexions: Bag[Connexion[Vertex[V]]])(var discovered: Boolean = false) extends Attribute[V] {

  def +(connexion: Connexion[Vertex[V]]): Vertex[V] = Vertex(attribute)(connexions + connexion)(discovered)

  /**
   * Method to perform strict equality between this and another Vertex.
   *
   * NOTE: the auto-generated equals method ONLY compares the attribute.
   *
   * @param v the other Vertex to compare.
   * @return true if and only if all members and parameters are equal.
   */
  def equalsStrict(v: Vertex[V]): Boolean = this == v && connexions == v.connexions && discovered == v.discovered

  override def toString: String = s"v$attribute"
}

object Vertex {
  def create[V](attribute: V, connexions: Bag[Connexion[Vertex[V]]] = Bag.empty): Vertex[V] = new Vertex[V](attribute)(connexions)()
}
