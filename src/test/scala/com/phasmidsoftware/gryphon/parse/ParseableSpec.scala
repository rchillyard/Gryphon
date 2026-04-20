package com.phasmidsoftware.gryphon.parse

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scala.util.{Failure, Success, Try}

class ParseableSpec extends AnyFlatSpec with Matchers {

  behavior of "Parseable"

  it should "parse Boolean" in {
    val parseable = implicitly[Parseable[Boolean]]
    parseable.parse("true") shouldBe Success(true)
    parseable.parse("tRuE") shouldBe Success(true)
  }
  it should "fail Boolean 1" in {
    val parseable = implicitly[Parseable[Boolean]]
    val triedBoolean = parseable.parse("X")
    triedBoolean should matchPattern { case Failure(_) => }
    a[IllegalArgumentException] should be thrownBy triedBoolean.get
    triedBoolean match {
      case Failure(e) => e.getLocalizedMessage shouldBe "For input string: \"X\""
      case _ => fail("parse succeeded when it should have failed")
    }
  }
  it should "fail Boolean 2" in {
    val booleanParser: String => Boolean = Parseable.parser[Boolean]
    a[ParseException] should be thrownBy booleanParser("X")
  }

  it should "fail Boolean 3" in {
    val booleanParser: String => Boolean = Parseable.parser[Boolean]
    Try(booleanParser("X")) match {
      case Failure(e) =>
        e.getLocalizedMessage shouldBe "Failed to parse \"X\" as Boolean (This is most likely an inconsistency in the logic of the implicit instance of Parseable[Boolean]). The underlying cause is:"
        e.getCause.getLocalizedMessage shouldBe "For input string: \"X\""
      case _ => fail("parse succeeded when it should have failed")
    }
  }

  it should "parse Int" in {
    val parseable = implicitly[Parseable[Int]]
    parseable.parse("1") shouldBe Success(1)
  }
  it should "fail Int" in {
    val parseable = implicitly[Parseable[Int]]
    parseable.parse("X") should matchPattern { case Failure(_) => }
    a[IllegalArgumentException] should be thrownBy parseable.parse("X").get
  }

  it should "parse Double" in {
    val parseable = implicitly[Parseable[Double]]
    parseable.parse("3.1415927") shouldBe Success(3.1415927)
  }
  it should "fail Double" in {
    val parseable = implicitly[Parseable[Double]]
    parseable.parse("X") should matchPattern { case Failure(_) => }
    a[IllegalArgumentException] should be thrownBy parseable.parse("X").get
  }

  it should "parse Unit" in {
    val parseable = implicitly[Parseable[Unit]]
    parseable.parse("()") shouldBe Success(())
  }
  it should "fail Unit" in {
    val parseable = implicitly[Parseable[Unit]]
    val triedUnit = parseable.parse("X")
    triedUnit should matchPattern { case Failure(_) => }
    a[AssertionError] should be thrownBy parseable.parse("X").get
  }

  behavior of "ParseableLong"

  it should "parse a plain integer string" in :
    Parseable.ParseableLong.parse("12345") shouldBe Success(12345L)

  it should "parse a string with uppercase L suffix" in :
    Parseable.ParseableLong.parse("12345L") shouldBe a[Success[?]]
    Parseable.ParseableLong.parse("12345L").get shouldBe 12345L

  it should "parse a string with lowercase l suffix" in :
    Parseable.ParseableLong.parse("12345l") shouldBe a[Success[?]]
    Parseable.ParseableLong.parse("12345l").get shouldBe 12345L

  it should "parse zero" in :
    Parseable.ParseableLong.parse("0") shouldBe Success(0L)

  it should "parse Long.MaxValue" in :
    Parseable.ParseableLong.parse(Long.MaxValue.toString) shouldBe Success(Long.MaxValue)

  it should "fail on a non-numeric string" in :
    Parseable.ParseableLong.parse("abc") shouldBe a[Failure[?]]

  it should "have none == 0L" in :
    Parseable.ParseableLong.none shouldBe 0L

  it should "have message == Long" in :
    Parseable.ParseableLong.message shouldBe "Long"

  behavior of "GraphBuilder with ParseableLong — longs.graph"

  it should "load longs.graph as UndirectedGraph[Int, Long]" in :
    import com.phasmidsoftware.gryphon.builder.GraphBuilder
    GraphBuilder.undirected[Int, Long].fromResource("longs.graph") shouldBe a[scala.util.Success[?]]

  it should "produce 4 vertices and 4 edges from longs.graph" in :
    import com.phasmidsoftware.gryphon.builder.GraphBuilder
    GraphBuilder.undirected[Int, Long].fromResource("longs.graph") match
      case scala.util.Success(g) =>
        g.N shouldBe 4
        g.M shouldBe 4
      case scala.util.Failure(x) => fail("failed to load longs.graph", x)

  it should "parse edge weights as Long from longs.graph" in :
    import com.phasmidsoftware.gryphon.builder.GraphBuilder
    import com.phasmidsoftware.gryphon.traverse.Kruskal
    given Ordering[Long] = Ordering.Long

    GraphBuilder.undirected[Int, Long].fromResource("longs.graph") match
      case scala.util.Success(g) =>
        val mst = Kruskal.mst(g)
        mst.size shouldBe 3
        mst.map(_.attribute).sum shouldBe 450L  // 100 + 150 + 200
      case scala.util.Failure(x) => fail("failed to load longs.graph", x)

}
