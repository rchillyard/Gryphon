  case class Rational private (n: Long, d: Long) {
    require(d != 0, "Denominator must not be zero")

    // Find the greatest common divisor (gcd) using Euclid's algorithm
    @scala.annotation.tailrec
    private def gcd(a: Long, b: Long): Long = {
      if (b == 0) a else gcd(b, a % b)
    }

    // Calculate the gcd of numerator and denominator
    private val commonGcd: Long = gcd(n.abs, d.abs)

    // Reduce the rational number by dividing both numerator and denominator by their gcd
    private val reducedNumerator: Long = n / commonGcd
    private val reducedDenominator: Long = d / commonGcd

    // Ensure that the denominator is not negative
    require(d >= 0, "Denominator must not be negative")

    // Define the + method to add two Rational numbers
    def +(other: Rational): Rational = {
      val newNumerator = reducedNumerator * other.reducedDenominator + other.reducedNumerator * reducedDenominator
      val newDenominator = reducedDenominator * other.reducedDenominator
      Rational(newNumerator, newDenominator)
    }

    override def toString: String = {
      if (reducedDenominator == 1) reducedNumerator.toString
      else s"$reducedNumerator/$reducedDenominator"
    }
  }

  // Define the companion object with the apply method
  object Rational {
    // Companion object's apply method to create Rational instances
    def apply(n: Long, d: Long): Rational = {
      require(d != 0, "Denominator must not be zero")
      new Rational(n, d)
    }

    def apply(n: Long, d: Int): Rational = apply(n, d.toLong)

    def apply(n: Int, d: Int): Rational = apply(n.toLong, d.toLong)

    def apply(n: Int): Rational = apply(n.toLong, 1L)

    // Define the sum method for a sequence of Rationals
    def sum(xs: Seq[Rational]): Rational = xs.foldLeft(Rational(0))(_ + _)

    // Define the parse method
    def parse(s: String, maybeD: Option[String]): Option[Rational] = {
      val maybeNumerator = s.toLongOption

      // Use pattern matching to handle the case where the denominator fails to parse
      maybeD match {
        case Some(d) =>
          d.toLongOption.flatMap { denominator =>
            if (denominator != 0) {
              maybeNumerator.map(n => Rational(n, denominator))
            } else {
              None
            }
          }
        case None =>
          maybeNumerator.map(n => Rational(n, 1))
      }
    }
  }

  Rational.parse("1",Some("0"))

  // Test for the provided input parameters as per the question:
  val parsedRational1 = Rational.parse("1", Some("2"))
  val parsedRational2 = Rational.parse("x", Some("2"))
  val parsedRational3 = Rational.parse("2", Some("x"))
  val parsedRational4 = Rational.parse("2", Some("1"))
  val parsedRational5 = Rational.parse("2", None)

  // Ensure that the results of the last two tests are identical
  val resultComparison = parsedRational4 == parsedRational5

  // Print the results
  println(s"Parsed Rational 1: $parsedRational1")
  println(s"Parsed Rational 2: $parsedRational2")
  println(s"Parsed Rational 3: $parsedRational3")
  println(s"Parsed Rational 4: $parsedRational4")
  println(s"Parsed Rational 5: $parsedRational5")

/*
Results:
/Users/shreemoynanda/Library/Java/JavaVirtualMachines/openjdk-21.0.1/Contents/Home/bin/java -javaagent:/Applications/IntelliJ IDEA.app/Contents/lib/idea_rt.jar=53104:/Applications/IntelliJ IDEA.app/Contents/bin -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -classpath /Users/shreemoynanda/Desktop/NEU/CYSE7200_Scala/scala_final/target/scala-3.3.1/classes:/Users/shreemoynanda/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala-library/2.13.10/scala-library-2.13.10.jar:/Users/shreemoynanda/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala3-library_3/3.3.1/scala3-library_3-3.3.1.jar CYSE7200_Final_Part_3
Parsed Rational 1: Some(1/2)
Parsed Rational 2: None
Parsed Rational 3: None
Parsed Rational 4: Some(2)
Parsed Rational 5: Some(2)

Process finished with exit code 0
 */