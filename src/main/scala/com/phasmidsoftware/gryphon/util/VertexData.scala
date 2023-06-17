/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.util

import com.phasmidsoftware.parse._
import com.phasmidsoftware.table.{HeadedTable, Header, Table}
import scala.util.Try

/**
 * Trait to model the behavior of a transitional vertex, i.e. during the input (parsing) process.
 *
 * This is used by the Traveling Salesman Problem (TSP).
 *
 * @tparam V the vertex attribute type.
 */
trait VertexData[V] {
    def attribute: V
}

/**
 * Case class to represent a transitional edge, i.e. during the input (parsing) process.
 *
 * In particular, this class is used to parse from a CSV file when an edge is shows in the form "v1 v2 e".
 *
 * @param attribute one vertex's attribute.
 * @tparam V the vertex attribute type (a label, typically).
 */
case class VertexDataTSP[V: Ordering : CellParser](attribute: V) extends VertexData[V]

/**
 * Parser to parse vertex data.
 *
 * @tparam V requires implicit evidence of Ordering and CellParser.
 */
class VertexDataParser[V: Ordering : CellParser] {

    private object VertexDataTSPParser extends CellParsers {

        implicit val columnHelper: ColumnHelper[V] = columnHelper()

        implicit val vertexDataMSTParser: CellParser[VertexDataTSP[V]] = cellParser1(VertexDataTSP[V])

        implicit object VertexDataTSPConfig extends DefaultRowConfig {
            override val listEnclosure: String = ""
        }

        val parser: StandardRowParser[VertexDataTSP[V]] = StandardRowParser.create[VertexDataTSP[V]]
    }

    trait VertexDataTSPTableParser extends StringTableParser[Table[VertexDataTSP[V]]] {
        type Row = VertexDataTSP[V]

        val maybeFixedHeader: Option[Header] = None

        val headerRowsToRead: Int = 1

        override val forgiving: Boolean = false

        val rowParser: RowParser[VertexDataTSP[V], String] = VertexDataTSPParser.parser

        protected def builder(rows: Iterable[VertexDataTSP[V]], header: Header): Table[VertexDataTSP[V]] = HeadedTable(rows, header)
    }

    implicit object VertexDataTSPTableParser extends VertexDataTSPTableParser

    /**
     * TESTME
     *
     * @param resource the name of the resource.
     * @return a Try of Iterable[V].
     */
    def parseVerticesFromCsv(resource: String): Try[Iterable[V]] = {
        val dty: Try[Table[VertexDataTSP[V]]] = Table.parseResource[Table[VertexDataTSP[V]]](resource)

        for (vt <- dty) yield vt.rows.map(d => d.attribute)
    }
}