ThisBuild / version := "0.1.2"

ThisBuild / scalaVersion := "3.7.4"

lazy val root = (project in file("."))
  .settings(
    name := "LittleGryphon"
  )

val scalaTestVersion = "3.2.19"
lazy val scalaParserCombinatorsVersion = "2.4.0"

libraryDependencies += "org.slf4j" % "slf4j-api" % "2.0.17"
libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % scalaParserCombinatorsVersion
libraryDependencies += "com.phasmidsoftware" %% "visitor" % "1.2.0"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.5.32" % "runtime"
libraryDependencies += "org.scalatest" %% "scalatest" % scalaTestVersion % "test"
