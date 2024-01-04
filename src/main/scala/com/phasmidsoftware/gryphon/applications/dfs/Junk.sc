import scala.language.implicitConversions

def myMap[X,Y](xs: Seq[X])(f: X=>Y): Seq[Y] = for (x <- xs) yield f(x)
def show[X](x: X): String = x.toString

myMap(Seq(1,2))(show)
myMap(Seq(1,2))(show(_))
myMap(Seq(1,2))(x => show(x))

implicit val convert: T => U = ???
trait T {
  val t: T
}
trait U {
  val u: U
}
val t: T = ???
t.u