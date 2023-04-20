package com.phasmidsoftware.gryphon.util

import com.phasmidsoftware.gryphon.core.{UndirectedOrderedEdge, UndirectedOrderedEdgeCase}
import com.phasmidsoftware.parse._
import com.phasmidsoftware.table.{HeadedTable, Header, Table}
import scala.util.Try
import scala.util.matching.Regex

/**
 * Trait to model the behavior of a transitional edge, i.e. during the input (parsing) process.
 *
 * The purpose of this module is to allow parsing (using TableParser) of CSV files made up of
 * one edge per line (or something similar to that).
 *
 * This kind of data is a standard way of defining a graph from an input file.
 * For example, we wish to determine the MST for a graph.
 *
 * @tparam V the vertex attribute type.
 * @tparam E the edge attribute type.
 */
trait EdgeData[V, E] {
    def vertex1: V

    def vertex2: V

    def edge: E
}

/**
 * Case class to represent a transitional edge, i.e. during the input (parsing) process.
 *
 * In particular, this class is used to parse from a CSV file when an edge is shows in the form "v1 v2 e".
 *
 * @param vertex1 one vertex's attribute.
 * @param vertex2 the other vertex's attribute.
 * @param edge    the edge attribute.
 * @tparam V the vertex attribute type.
 * @tparam E the edge attribute type.
 */
case class EdgeDataMST[V: Ordering : CellParser, E: Ordering : CellParser](vertex1: V, vertex2: V, edge: E) extends EdgeData[V, E]

/**
 * Class to represent a parser for EdgeData.
 *
 * @tparam V the (key) vertex attribute.
 * @tparam E the edge attribute.
 *
 *           Implicit evidence required:
 *           V: Ordering and CellParser
 *           E: Ordering and CellParser
 */
class EdgeDataParser[V: Ordering : CellParser, E: Ordering : CellParser] {
    object EdgeDataMSTParser extends CellParsers {

        implicit val edgeDataMSTParser: CellParser[EdgeDataMST[V, E]] = cellParser3(EdgeDataMST[V, E])

        implicit object EdgeDataMSTConfig extends DefaultRowConfig {
            override val listEnclosure: String = ""
            override val delimiter: Regex = """\s+""".r
            override val string: Regex = """[^ "]*""".r
        }
        val parser: StandardRowParser[EdgeDataMST[V, E]] = StandardRowParser.create[EdgeDataMST[V, E]]
    }

    trait EdgeDataMSTTableParser extends StringTableParser[Table[EdgeDataMST[V, E]]] {
        type Row = EdgeDataMST[V, E]

        val maybeFixedHeader: Option[Header] = Some(Header.create("vertex1", "vertex2", "edge"))
        val headerRowsToRead: Int = 0
        override val forgiving: Boolean = false
        val rowParser: RowParser[EdgeDataMST[V, E], String] = EdgeDataMSTParser.parser

        protected def builder(rows: Iterable[EdgeDataMST[V, E]], header: Header): Table[EdgeDataMST[V, E]] = HeadedTable(rows, header)
    }

    implicit object EdgeDataMSTTableParser extends EdgeDataMSTTableParser

    /**
     * Method to parse edges from a CSV resource.
     *
     * @param resource the name of the resource.
     * @return a Try of an Iterable of edges
     */
    def parseEdgesFromCsv(resource: String): Try[Iterable[UndirectedOrderedEdge[V, E]]] = {
        val dty: Try[Table[EdgeDataMST[V, E]]] = Table.parseResource[Table[EdgeDataMST[V, E]]](resource)

        for (et <- dty) yield et.map(e => UndirectedOrderedEdgeCase(e.vertex1, e.vertex2, e.edge)).rows
    }
}