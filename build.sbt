ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.0"

lazy val root = (project in file("."))
  .settings(
    name := "LittleGryphon"
  )

val scalaTestVersion = "3.2.19"
lazy val scalaParserCombinatorsVersion = "2.4.0"

libraryDependencies += "org.slf4j" % "slf4j-api" % "2.0.17"
libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % scalaParserCombinatorsVersion
libraryDependencies += "com.phasmidsoftware" %% "visitor" % "0.0.10-SNAPSHOT"
libraryDependencies += "com.phasmidsoftware" % "flog_2.13" % "1.0.10"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.5.18" % "runtime"
libraryDependencies += "org.scalatest" %% "scalatest" % scalaTestVersion % "test"
