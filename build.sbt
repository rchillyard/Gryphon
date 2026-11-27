// Gryphon project build file.

ThisBuild / version := "1.14.0"

ThisBuild / organization := "com.phasmidsoftware"

ThisBuild / javacOptions ++= Seq("--release", "21")

val scalaVersionNumber  = "3.9.0" // the current LTS
val scalaTestVersion    = "3.2.20"
val jupiterVersion      = "6.0.3"
// cats-kernel only: Eq, Order, Semigroup and Monoid, with no transitive dependencies of its own.
// Gryphon needs none of cats-core, so the dependency list grows by exactly one small jar.
val catsKernelVersion   = "2.13.0"
val jupiterInterfaceVersion = "0.19.0"
val visitorVersion      = "1.9.0"
val flogVersion         = "1.1.0"
val logbackVersion      = "1.5.32"
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
val previousRelease = "1.13.0"
mimaPreviousArtifacts := Set(organization.value %% moduleName.value % previousRelease)

// Deliberate breaks are listed in src/main/mima-filters/<previousRelease>.backwards.excludes,
// which MiMa reads automatically. The version in the name records which release the breaks belong
// to; it is NOT a condition on when the file applies. sbt-mima reads EVERY *.backwards.excludes in
// that directory whatever it is called, so a filter is a permanent, unconditional exemption for the
// symbol it names and stays in force until the file is deleted by hand. Moving previousRelease on
// does not retire anything. Deleting the filters for release N, once previousRelease has reached N,
// is therefore a required step of the post-release and not tidying. See #107, where three files
// were live and 81 filters in force where this comment had implied 36.
//
// There are none at present, and no mima-filters directory: 1.12.0's fourteen were deleted at its
// post-release (#124), the first time this rule was actually applied here. Recreate the directory
// when a release next needs a filter, and delete it again afterwards.

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
            "org.typelevel"          %% "cats-kernel"              % catsKernelVersion,
            "com.phasmidsoftware"    %% "visitor"                  % visitorVersion,
            // flog is a DEBUGGING tool: you add calls while chasing something and take them out
            // before committing. Zero call sites is therefore its normal, healthy state and is NOT
            // evidence the dependency is unused. #102 proposed dropping it on exactly that basis;
            // Robin's answer was no, and this comment is here so the question is not reopened.
            "com.phasmidsoftware"    %% "flog"                     % flogVersion,
            // slf4j-api is gone in 1.13.0 (#102). It had one user, a public `val logger` on
            // VertexMap that nothing called, and after #57 made the orientation warning a Failure
            // there is no logging call anywhere in src/main. Put it back the day something logs.
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
// JAVADOC FOR THE JAVA FAÇADE
// ============================================================================
// `sbt doc` runs Scaladoc, which documents Scala sources only. The Java façade in src/main/java —
// the interface this library offers Java callers — therefore had no published API documentation at
// all, and neither did the -javadoc.jar on Maven Central, which is Scaladoc output. This task runs
// the JDK's javadoc over src/main/java; the site setting below publishes the result next to the
// Scaladoc, at https://rchillyard.github.io/Gryphon/latest/javadoc/.

lazy val javaApiDoc = taskKey[File]("Generates Javadoc for the Java façade in src/main/java.")

javaApiDoc := {
  val log = streams.value.log
  val _ = (Compile / compile).value // the façade's Javadoc needs the compiled Scala classes on the classpath
  val out = target.value / "javadoc"
  IO.delete(out)
  IO.createDirectory(out)
  val javaSources = ((Compile / javaSource).value ** "*.java").get
  if (javaSources.isEmpty) sys.error("No Java sources found in " + (Compile / javaSource).value)
  val classpath =
    ((Compile / dependencyClasspath).value.files :+ (Compile / classDirectory).value)
            .mkString(java.io.File.pathSeparator)
  val args = Seq(
    "-d", out.getAbsolutePath,
    "-classpath", classpath,
    "-quiet",
    // Report malformed HTML and broken @link targets, but not undocumented members: the façade
    // documents its public API, while its package-private plumbing is deliberately bare.
    "-Xdoclint:all,-missing",
    "-windowtitle", s"${name.value} ${version.value} Java API",
    "-doctitle", s"${name.value} ${version.value} — Java API"
  ) ++ javaSources.map(_.getAbsolutePath)
  val exitCode = scala.sys.process.Process("javadoc" +: args).!(log)
  if (exitCode != 0) sys.error(s"javadoc failed with exit code $exitCode")
  log.info(s"Javadoc for the Java façade written to $out")
  out
}

// addMappingsToSiteDir needs a SettingKey, not a plain value, for the destination directory.
lazy val javaApiDocSubdir = settingKey[String]("Where the Java façade's Javadoc goes within the site.")

javaApiDocSubdir := "latest/javadoc"

addMappingsToSiteDir(Def.task(Path.allSubpaths(javaApiDoc.value).toSeq), javaApiDocSubdir)

// ============================================================================
// MUTATION TESTING (Stryker4s)
// ============================================================================

// Stryker4s parses the sources with scalameta before instrumenting them. The default
// dialect is Scala 2.13, which cannot parse `given`/`using`, `enum`, or indentation
// syntax, so Scala 3 must be stated explicitly or every file fails to parse.
// Scope and thresholds live in stryker4s.conf. Run with `sbt stryker`.
strykerScalaDialect := scala.meta.dialects.Scala3

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
