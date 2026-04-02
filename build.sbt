// Gryphon

ThisBuild / version := "0.2.7"

ThisBuild / scalaVersion := "3.7.4"

lazy val root = (project in file("."))
  .settings(
    name := "Gryphon"
  )

val scalaTestVersion = "3.2.20"
lazy val scalaParserCombinatorsVersion = "2.4.0"

libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-api" % "2.0.17",
  "org.scala-lang.modules" %% "scala-parser-combinators" % scalaParserCombinatorsVersion,
  ("com.phasmidsoftware" % "tableparser-core_2.13" % "1.5.1")
          .exclude("com.phasmidsoftware", "flog_2.13")
          .exclude("org.scala-lang.modules", "scala-parser-combinators_2.13"),
  "com.phasmidsoftware" %% "visitor" % "1.4.0",
  "com.phasmidsoftware" %% "flog" % "1.0.13",
  "ch.qos.logback" % "logback-classic" % "1.5.32" % "runtime",
  "org.scalatest" %% "scalatest" % scalaTestVersion % "test"
)
