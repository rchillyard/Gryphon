// Gryphon project build file.

ThisBuild / version := "1.6.1"

ThisBuild / organization := "com.phasmidsoftware"

ThisBuild / javacOptions ++= Seq("--release", "21")

val scalaVersionNumber  = "3.9.0" // the current LTS
val scalaTestVersion    = "3.2.20"
val jupiterVersion      = "6.0.3"
val jupiterInterfaceVersion = "0.19.0"
val visitorVersion      = "1.7.0"
val flogVersion         = "1.1.0"
val logbackVersion      = "1.5.32"
val slf4jVersion        = "2.0.17"
lazy val scalaParserCombinatorsVersion = "2.4.0"

// ============================================================================
// COMPILER OPTIONS
// ============================================================================

// The compiler options that all the Phasmid libraries share.
scalacOptions ++= Seq(
  "-encoding", "UTF-8",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Wunused:all",
  "-Wvalue-discard",
  "-Wnonunit-statement",
  "-Werror"
)
// Tests may use a value as a statement (for example, in a matcher), so they skip -Wnonunit-statement.
Test / scalacOptions ~= (_.filterNot(_ == "-Wnonunit-statement"))

// ============================================================================
// BINARY COMPATIBILITY
// ============================================================================

// MiMa checks the published artifact for binary compatibility with the last release: `sbt mimaReportBinaryIssues`.
// The version scheme is PVP (see publish.sbt): a patch release must pass; a minor release may break compatibility,
// on purpose and with a release note, by listing the breaks in mimaBinaryIssueFilters (or, after the release, by
// moving previousRelease on). Note moduleName, not name: the published artifact is gryphon_3, not Gryphon_3.
val previousRelease = "1.6.0"
mimaPreviousArtifacts := Set(organization.value %% moduleName.value % previousRelease)

// ============================================================================
// MODULE DEFINITION
// Java sources live in src/main/java and src/test/java alongside the Scala
// sources — no separate subproject needed.
// ============================================================================

lazy val root = (project in file("."))
        .settings(
          name         := "Gryphon",
          scalaVersion := scalaVersionNumber,

          libraryDependencies ++= Seq(
            "org.scala-lang.modules" %% "scala-parser-combinators" % scalaParserCombinatorsVersion,
            "com.phasmidsoftware"    %% "visitor"                  % visitorVersion,
            "com.phasmidsoftware"    %% "flog"                     % flogVersion,
            "org.slf4j"               % "slf4j-api"                % slf4jVersion,
            // Test scope only: a library must not publish a logging backend for its users.
            "ch.qos.logback"          % "logback-classic"          % logbackVersion % Test,
            "org.scalatest"          %% "scalatest"                % scalaTestVersion % Test,
            // JUnit 5 — for Java façade tests in src/test/java.
            // jupiter-interface is the sbt test interface: without it sbt compiles these tests and
            // never runs them, because sbt only discovers tests through a framework on the classpath.
            "org.junit.jupiter"       % "junit-jupiter-api"        % jupiterVersion % Test,
            "org.junit.jupiter"       % "junit-jupiter-engine"     % jupiterVersion % Test,
            "com.github.sbt.junit"    % "jupiter-interface"        % jupiterInterfaceVersion % Test
          )
        )
        // Scaladoc is published to https://rchillyard.github.io/Gryphon/latest/api/ by `sbt ghpagesPushSite`
        .enablePlugins(GhpagesPlugin, SiteScaladocPlugin)
        .settings(git.remoteRepo := "git@github.com:rchillyard/Gryphon.git")

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
// The compiler options are the ones that all the Phasmid libraries share.
//
// Key options:
//   -Wvalue-discard      : catches silently discarded non-Unit values
//   -Wnonunit-statement  : catches non-Unit expressions used as statements
//                          (filtered out for test code)
//   -Wunused:all         : catches unused imports, privates, locals, parameters, etc.
// ============================================================================
