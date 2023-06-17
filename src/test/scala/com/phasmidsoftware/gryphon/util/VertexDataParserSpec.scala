/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.util

import com.phasmidsoftware.parse.{CellParser, CellParsers, SingleCellParser}
import com.phasmidsoftware.table.Table
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import scala.util.{Success, Try}

class VertexDataParserSpec extends AnyFlatSpec with should.Matchers {

    case class Crime(id: BigInt, longitude: Double, latitude: Double)

    object Crime extends CellParsers {

        implicit object BigIntCellParser extends SingleCellParser[BigInt] {
            def convertString(w: String): Try[BigInt] = Try(BigInt(w, 16))
        }

        implicit val crimeCellParser: CellParser[Crime] = cellParser3(Crime.apply)

        implicit object CrimeOrdering extends Ordering[Crime] {
            def compare(x: Crime, y: Crime): Int = x.id.compare(y.id)
        }
    }

    behavior of "VertexDataParser"

    it should "parseVerticesFromCsv" in {
        val spring2023Project = "/info6205.spring2023.teamproject.csv"
        import Crime._
        val vertexDataParser: VertexDataParser[Crime] = new VertexDataParser[Crime]()
        implicit val tableParser: vertexDataParser.VertexDataTSPTableParser.type = vertexDataParser.VertexDataTSPTableParser
        val cvty: Try[Table[VertexDataTSP[Crime]]] = Table.parseResource[Table[VertexDataTSP[Crime]]](spring2023Project)
        cvty should matchPattern { case Success(_) => }
        val cvt = cvty.get
        cvt.rows.size shouldBe 585
        val firstId: BigInt = BigInt("447a81a19157c2f6ef97accacebaa66d8153e19ca43c16ca452e6d8d447823", 16)
        val firstCrime = Crime(firstId, -0.009691, 51.483548)
        val expected = VertexDataTSP(firstCrime)
        val head = cvt.head
        head shouldBe expected
    }

}
