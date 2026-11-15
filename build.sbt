// Gryphon project build file.

import com.typesafe.tools.mima.core.{DirectMissingMethodProblem, IncompatibleMethTypeProblem, MissingClassProblem, ProblemFilters, ReversedMissingMethodProblem}

ThisBuild / version := "1.9.0"

ThisBuild / organization := "com.phasmidsoftware"

ThisBuild / javacOptions ++= Seq("--release", "21")

val scalaVersionNumber  = "3.9.0" // the current LTS
val scalaTestVersion    = "3.2.20"
val jupiterVersion      = "6.0.3"
// cats-kernel only: Eq, Order, Semigroup and Monoid, with no transitive dependencies of its own.
// Gryphon needs none of cats-core, so the dependency list grows by exactly one small jar.
val catsKernelVersion   = "2.13.0"
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
val previousRelease = "1.8.1"
mimaPreviousArtifacts := Set(organization.value %% moduleName.value % previousRelease)

// 1.8.0's deliberate breaks were filtered here while previousRelease was 1.7.0, and went with it when
// the baseline moved on. These are 1.9.0's, each with a release note in the README.
mimaBinaryIssueFilters ++= Seq(
  // RelaxableVertex is deleted, with its companion and the Vertex.createRelaxableWithSet factory.
  // It carried the Dijkstra relaxation cost as a `var` on the vertex itself. Commit 9e06526
  // (Gryphon 0.2.5) moved that state into the traversal — GraphTraversal.run owns `pred` and
  // `bestCost` as locals, and the frontier carries (cost, vertex) tuples — after which nothing in
  // src/main referenced it. Only its own tests did. Deleting it removes one of the two pieces of
  // mutable state in the permanent structure of a graph; VertexMap.edgeCount is the other.
  ProblemFilters.exclude[MissingClassProblem]("com.phasmidsoftware.gryphon.core.RelaxableVertex"),
  ProblemFilters.exclude[MissingClassProblem]("com.phasmidsoftware.gryphon.core.RelaxableVertex$"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("com.phasmidsoftware.gryphon.core.Vertex.createRelaxableWithSet(java.lang.Object,cats.kernel.Monoid,scala.math.Ordering)com.phasmidsoftware.gryphon.core.RelaxableVertex"),
  // addEdge moved to the new AbstractAdjunctGraph base, where it is written once as
  // `unit(vertexMap + edge)` instead of twice as `copy(vertexMap + edge)`. Source compatibility is
  // unaffected — each subclass still fixes `EdgeT` to its own edge kind and `G` to itself, so
  // `directedGraph.addEdge(e)` still takes a DirectedEdge and still returns a DirectedGraph — but
  // the *erased* signature is now (Edge)AbstractAdjunctGraph, declared on the base.
  ProblemFilters.exclude[IncompatibleMethTypeProblem]("com.phasmidsoftware.gryphon.adjunct.DirectedGraph.addEdge"),
  ProblemFilters.exclude[IncompatibleMethTypeProblem]("com.phasmidsoftware.gryphon.adjunct.UndirectedGraph.addEdge"),
  // `N` and `M` become `order` and `size` — the standard graph-theory terms, and what the Java
  // façade has always called them. `N` and `M` survive as deprecated aliases with bodies, so
  // calling code still compiles; what moves is the *abstract* member each trait requires.
  ProblemFilters.exclude[ReversedMissingMethodProblem]("com.phasmidsoftware.gryphon.core.Traversable.order"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("com.phasmidsoftware.gryphon.core.Traversable.M()Int"),
  // `size` moves from Traversable down to EdgeTraversable, because counting edges requires knowing
  // what an edge is. A VertexMap is a Traversable and does not know: it cannot tell whether an
  // adjacency it holds is half of an undirected edge or the whole of a directed one. Requiring the
  // count one level too high is precisely what forced VertexMap to carry a mutable `edgeCount`,
  // which `+` incremented on the receiver while returning a map whose own count was always 0.
  ProblemFilters.exclude[ReversedMissingMethodProblem]("com.phasmidsoftware.gryphon.core.EdgeTraversable.size"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("com.phasmidsoftware.gryphon.core.VertexMap.M()Int"),
  // AbstractGraph's own `M` went with it. It was `vertexMap.M`, so it inherited that wrongness;
  // both concrete graphs overrode it, which is the only reason nothing user-facing was broken.
  ProblemFilters.exclude[DirectMissingMethodProblem]("com.phasmidsoftware.gryphon.core.AbstractGraph.M()Int")
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

          libraryDependencies ++= Seq(
            "org.scala-lang.modules" %% "scala-parser-combinators" % scalaParserCombinatorsVersion,
            "org.typelevel"          %% "cats-kernel"              % catsKernelVersion,
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
