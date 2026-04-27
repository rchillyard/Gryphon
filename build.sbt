// Gryphon project build file.

ThisBuild / version := "1.6.0"

ThisBuild / organization := "com.phasmidsoftware"

ThisBuild / javacOptions ++= Seq("--release", "21")

val scalaVersionNumber  = "3.7.4"
val scalaTestVersion    = "3.2.20"
val jupiterVersion      = "6.0.3"
val visitorVersion      = "1.6.0"
val flogVersion         = "1.0.13"
val logbackVersion      = "1.5.32"
val slf4jVersion        = "2.0.17"
lazy val scalaParserCombinatorsVersion = "2.4.0"

// ============================================================================
// COMPILER OPTIONS
// ============================================================================

val commonScalacOptions = Seq(
  "-encoding", "UTF-8",
  "-unchecked",
  "-deprecation",
  "-feature"
)

val scala3Options = Seq(
  // "-Xfatal-warnings",         // Turn warnings into errors (uncomment to enable)
  "-Wvalue-discard",              // Error on discarded non-Unit values
  "-Wnonunit-statement",          // Error when non-Unit expressions used as statements
  "-explain",                     // Detailed error explanations
  "-Wunused:imports",             // Catch unused imports
  "-Wunused:privates",            // Catch unused private members
  "-Wunused:locals",              // Catch unused local definitions
  "-Wconf:msg=Couldn't resolve a member:s" // Suppress linker warnings in doc generation
)

// ScalaTest assertions return Assertion (not Unit), so -Wnonunit-statement
// would fire on every multi-assertion test. Filter it out for test compilation.
val scala3TestSettings = Seq(
  Test / scalacOptions := scalacOptions.value.filterNot(_ == "-Wnonunit-statement")
)

// ============================================================================
// MODULE DEFINITION
// Java sources live in src/main/java and src/test/java alongside the Scala
// sources — no separate subproject needed.
// ============================================================================

lazy val root = (project in file("."))
        .settings(
          name         := "Gryphon",
          scalaVersion := scalaVersionNumber,
          scalacOptions ++= commonScalacOptions ++ scala3Options,

          libraryDependencies ++= Seq(
            "org.scala-lang.modules" %% "scala-parser-combinators" % scalaParserCombinatorsVersion,
            "com.phasmidsoftware"    %% "visitor"                  % visitorVersion,
            "com.phasmidsoftware"    %% "flog"                     % flogVersion,
            "org.slf4j"               % "slf4j-api"                % slf4jVersion,
            "ch.qos.logback"          % "logback-classic"          % logbackVersion % Runtime,
            "org.scalatest"          %% "scalatest"                % scalaTestVersion % Test,
            // JUnit 5 — for Java façade tests in src/test/java
            "org.junit.jupiter"       % "junit-jupiter-api"        % jupiterVersion % Test,
            "org.junit.jupiter"       % "junit-jupiter-engine"     % jupiterVersion % Test
          ),

          // Run both ScalaTest and JUnit suites with sbt test
          testOptions ++= Seq(
            Tests.Argument(TestFrameworks.ScalaTest, "-u", "target/test-reports"),
            Tests.Argument(TestFrameworks.JUnit, "-v")
          )
        )
        .settings(scala3TestSettings)

// ============================================================================
// GLOBAL SETTINGS
// ============================================================================

//ScalaUnidoc / unidoc / scalacOptions ++= Seq(
//  "-doc-logo", (baseDirectory.value / "gryphon-logo.svg").toString
//)

Test / parallelExecution := false

// ============================================================================
// USAGE NOTES
// ============================================================================
// Compiler options mirror those used in the Visitor project for consistency.
//
// Key options:
//   -Wvalue-discard      : catches silently discarded non-Unit values
//   -Wnonunit-statement  : catches non-Unit expressions used as statements
//                          (filtered out for test code — see scala3TestSettings)
//   -Wunused:*           : catches unused imports, privates, locals
//
// To treat all warnings as errors, uncomment -Xfatal-warnings above.
// ============================================================================
