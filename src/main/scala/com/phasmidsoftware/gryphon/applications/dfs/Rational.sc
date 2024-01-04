import scala.annotation.tailrec

case class Rational(n: Long, d: Long) {
  require(d >= 0, "Denominator should be non-negative")
  require(Rational.gcd(n,d)==1, "n and d must be co-prime")

  def +(that: Rational): Rational = Rational(that.d*n+d*that.n, d*that.d)

  override def toString: String = d match {
    case 1L => s"$n"
    case _ => s"$n/$d"
  }
}

object Rational {
  def apply(n: Long, d: Long): Rational = {
    val g = gcd(n,d)
    new Rational(n/g,d/g)
  }
  def apply(n: Long, d: Int): Rational = apply(n, d.toLong)
  def apply(n: Int, d: Int): Rational = apply(n.toLong, d)
  def apply(x: Int): Rational = apply(x,1)

  def parse(s: String, maybeD: Option[String]): Option[Rational] =
    for {
      n <- s.toLongOption
      w <- maybeD orElse Some("1")
      d <- w.toLongOption
    } yield Rational(n, d)

  @tailrec
  def gcd(a: Long, b: Long): Long = if (b == 0L) a else gcd (b, a % b)

  // First, easy, method for sum
  def sum(xs: Seq[Rational]): Rational = xs reduce (_ + _)

  // Alternative, better, definition which allows us to write "xs sum" where xs has type Seq[Rational]
  trait RationalIsNumeric extends Numeric[Rational] {
    def plus(x: Rational, y: Rational): Rational = x + y

    def fromInt(x: Int): Rational = Rational(x)

    // For this exam, all of the following can simply return ???
    def minus(x: Rational, y: Rational): Rational = ???
    def times(x: Rational, y: Rational): Rational = ???
    def negate(x: Rational): Rational = ???
    def parseString(str: String): Option[Rational] = ???
    def toInt(x: Rational): Int = ???
    def toLong(x: Rational): Long = ???
    def toFloat(x: Rational): Float = ???
    def toDouble(x: Rational): Double = ???
    def compare(x: Rational, y: Rational): Int = ???
  }
  implicit object RationalIsNumeric extends RationalIsNumeric
}

Rational(1,2)+Rational(1,3)+Rational(1,6) // should be 1
Rational.sum(Seq(Rational(1,2),Rational(1,3),Rational(1,6))) // should be 1

Rational.parse("1",Some("2"))
Rational.parse("x",Some("2"))
Rational.parse("2",Some("x"))
Rational.parse("2",Some("1"))
Rational.parse("2",None)
