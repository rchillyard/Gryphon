ThisBuild / version := "0.1.1-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.10"

lazy val root = (project in file("."))
  .settings(
    name := "Gryphon"
  )

organization := "com.phasmidsoftware"

scalacOptions += "-deprecation"

resolvers += "Typesafe Repository" at "https://repo.typesafe.com/typesafe/releases/"

lazy val scalaModules = "org.scala-lang.modules"
lazy val scalaTestVersion = "3.2.15"

// NOTE: Issue #44: this library is not currently compatible with version 2.x.x of the parser-combinators library
lazy val scalaParserCombinatorsVersion = "1.1.2"
lazy val nScalaTimeVersion = "2.32.0"
lazy val tsecVersion = "0.4.0"

libraryDependencies ++= Seq(
    "com.phasmidsoftware" %% "tableparser" % "1.1.1",
    "org.scala-lang.modules" %% "scala-java8-compat" % "1.0.2",
    "org.typelevel" %% "cats-effect" % "3.4.8",
    "com.phasmidsoftware" %% "flog" % "1.0.8",
    "ch.qos.logback" % "logback-classic" % "1.4.6" % "runtime",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
    "junit" % "junit" % "4.13.2" % "test",
    "com.novocode" % "junit-interface" % "0.11" % "test",
    "org.scalatest" %% "scalatest" % scalaTestVersion % "test"
)
