ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.0"

lazy val root = (project in file("."))
  .settings(
    name := "LittleGryphon"
  )

val scalaTestVersion = "3.2.19"

libraryDependencies += "org.slf4j" % "slf4j-api" % "2.0.17"

libraryDependencies += "org.scalatest" %% "scalatest" % scalaTestVersion % "test"
