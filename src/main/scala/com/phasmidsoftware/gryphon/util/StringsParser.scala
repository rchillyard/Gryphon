/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.util

import scala.util.parsing.combinator.JavaTokenParsers

class StringsParser extends JavaTokenParsers {

  case class Comment(w: String) extends Stringy

  case class Strings2(a: String, b: String) extends Stringy

  case class Strings3(a: String, b: String, c: String) extends Stringy

  def parse2(w: String): Either[String, (String, String)] = parseAll(tuple2, w) match {
    case Success(z, _) => z
    case Failure(_, z) => throw new Exception(s"parse2 failure at position $z")
    case Error(_, z) => throw new Exception(s"parse2 error at position $z")
  }

  def parse3(w: String): Either[String, (String, String, String)] = parseAll(tuple3, w) match {
    case Success(z, _) => z
    case Failure(_, z) => throw new Exception(s"parse3 failure at position $z")
    case Error(_, z) => throw new Exception(s"parse3 error at position $z")
  }

  def tuple2: Parser[Either[String, (String, String)]] = stringy ^^ {
    case Strings2(x, y) => Right(x -> y)
    case Comment(z) => Left(z)
  }

  def tuple3: Parser[Either[String, (String, String, String)]] = stringy ^^ {
    case Strings3(x, y, z) => Right((x, y, z))
    case Comment(z) => Left(z)
  }

  def stringy: Parser[Stringy] = triple | pair | comment | failure("invalid tuple")

  def comment: Parser[Comment] = """\s*//.*""".r ^^ (w => Comment(w))

  def triple: Parser[Strings3] = word ~ word ~ word ^^ { case a ~ b ~ c => Strings3(a, b, c) }

  def pair: Parser[Strings2] = word ~ word ^^ { case a ~ b => Strings2(a, b) }

  def word: Parser[String] = """[^/\s]+""".r
}

trait Stringy