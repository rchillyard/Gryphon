/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.applications.mst

import com.phasmidsoftware.gryphon.core.{GraphException, UndirectedGraph, UndirectedOrderedEdge, UndirectedOrderedEdgeCase}
import com.phasmidsoftware.gryphon.util.{GraphBuilder, VertexDataParser, VertexDataTSP}
import com.phasmidsoftware.parse.{CellParser, CellParsers, SingleCellParser}
import com.phasmidsoftware.table.Table
import com.phasmidsoftware.util.FP.resource
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import scala.util.{Failure, Success, Try}

class LazyPrimSpec extends AnyFlatSpec with should.Matchers {

  behavior of "LazyPrim"

  it should "mst of trivial graph" in {
    val edge1 = UndirectedOrderedEdgeCase("A", "B", 1)
    val edge3 = UndirectedOrderedEdgeCase("A", "D", 3)
    val edge2 = UndirectedOrderedEdgeCase("A", "C", 2)
    val value1: UndirectedGraph[String, Int, UndirectedOrderedEdge[String, Int], Unit] = UndirectedGraph.createUnordered[String, Int, UndirectedOrderedEdge[String, Int], Unit]("Prim test").addEdge(edge1).addEdge(edge3).addEdge(edge2)
    // TODO eliminate this asInstanceOf
    val graph: UndirectedGraph[String, Int, UndirectedOrderedEdge[String, Int], Unit] = value1
    val target: LazyPrim[String, Int, UndirectedOrderedEdge[String, Int]] = new LazyPrimHelper[String, Int, UndirectedOrderedEdge[String, Int]]().createFromGraph(graph)
    target.edges shouldBe List(edge3, edge2, edge1)
  }

  it should "mst of Prim demo from Sedgewick & Wayne" in {
    val uy = resource("/prim.graph")
    val graphBuilder = new GraphBuilder[Int, Double, Unit]()

    val esy = graphBuilder.createEdgeListTriple(uy)(UndirectedOrderedEdgeCase(_, _, _))
    graphBuilder.createGraphFromEdges(UndirectedGraph.createUnordered[Int, Double, UndirectedOrderedEdge[Int, Double], Unit]("no title"))(esy) match {
      case Success(graph: UndirectedGraph[Int, Double, UndirectedOrderedEdge[Int, Double], Unit]) =>
        val prim = new LazyPrimHelper[Int, Double, UndirectedOrderedEdge[Int, Double]]().createFromGraph(graph)
        prim.edges.size shouldBe 7
        prim.mst.vertices.size shouldBe 8
        prim.edges map (_.attribute) shouldBe List(0.26, 0.16, 0.4, 0.17, 0.35, 0.28, 0.19)
      case Failure(x) => throw x
      case x => throw GraphException(s"graph of wrong type: ${x.get.getClass}")
    }
  }

  /**
   * This is data from the Kaggle UK Crime dataset.
   *
   * @param id        crime ID.
   * @param longitude longitude
   * @param latitude  latitude
   */
  case class Crime(id: BigInt, longitude: Double, latitude: Double) {
    override def toString: String = s"$briefId"

    private def briefId = {
      val str = id.toString(16)
      str.substring(str.length - 5)
    }
  }

  object Crime extends CellParsers {

    implicit def distance(crime1: Crime, crime2: Crime): Double = {
      val r = 6365082 // Radius of the earth in m
      val (lat1, lat2) = (crime1.latitude, crime2.latitude)
      val (lon1, lon2) = (crime1.longitude, crime2.longitude)
      val dLat = deg2rad(lat2 - lat1)
      val dLon = deg2rad(lon2 - lon1)
      val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
              Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) *
                      Math.sin(dLon / 2) * Math.sin(dLon / 2)
      r * (2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)))
    }

    implicit object BigIntCellParser extends SingleCellParser[BigInt] {
      def convertString(w: String): Try[BigInt] = Try(BigInt(w, 16))
    }

    implicit val crimeCellParser: CellParser[Crime] = cellParser3(Crime.apply)

    implicit object CrimeOrdering extends Ordering[Crime] {
      def compare(x: Crime, y: Crime): Int = x.id.compare(y.id)
    }

    private def deg2rad(deg: Double): Double = deg * (Math.PI / 180)
  }

  it should "mst the traveling salesman problem for INFO6205 Spring 2023" in {
    val spring2023Project = "/info6205.spring2023.teamproject.csv"
    import Crime._
    val vertexDataParser: VertexDataParser[Crime] = new VertexDataParser[Crime]()
    implicit val tableParser: vertexDataParser.VertexDataTSPTableParser.type = vertexDataParser.VertexDataTSPTableParser
    val cvty: Try[Table[VertexDataTSP[Crime]]] = Table.parseResource[Table[VertexDataTSP[Crime]]](spring2023Project)
    val csy: Try[Iterable[Crime]] = cvty map (cvt => for (r <- cvt.rows) yield r.attribute)
    csy map (new LazyPrimHelper[Crime, Double, UndirectedOrderedEdge[Crime, Double]]().createFromVertices(_)) match {
      case Success(mst: MST[Crime, Double, UndirectedOrderedEdge[Crime, Double]]) =>
        mst.edges.size shouldBe 584
        val total = mst.total
        println(total)
        total shouldBe 512849.2 +- 0.2
        mst.edges.toSeq.sortBy(e => e.vertex) foreach println
      case Failure(x) => throw x
    }
  }
}
