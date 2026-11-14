// Gryphon project build file.

import com.typesafe.tools.mima.core.{DirectMissingMethodProblem, IncompatibleMethTypeProblem, IncompatibleResultTypeProblem, MissingClassProblem, ProblemFilters, ReversedMissingMethodProblem}

ThisBuild / version := "2.0.0"

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
val previousRelease = "1.7.0"
mimaPreviousArtifacts := Set(organization.value %% moduleName.value % previousRelease)

// 1.7.0's deliberate breaks — the erased util.GraphException alias and the removal of core.DAG — were
// filtered here while previousRelease was 1.6.0. They are part of 1.7.0 now, so the filters have gone with
// them. Add new ones here as 2.0.0 breaks compatibility, each with a release note in the README.
mimaBinaryIssueFilters ++= Seq(
  // inDegree and outDegree are new *abstract* members of the public EdgeGraph trait, so anyone who
  // implemented that trait outside Gryphon would no longer compile. Deliberate: a default correct
  // for a directed graph (count edges whose tail is v) is silently wrong for an undirected one,
  // where both endpoints count, so leaving them abstract makes every implementor answer.
  ProblemFilters.exclude[ReversedMissingMethodProblem]("com.phasmidsoftware.gryphon.core.EdgeGraph.inDegree"),
  ProblemFilters.exclude[ReversedMissingMethodProblem]("com.phasmidsoftware.gryphon.core.EdgeGraph.outDegree"),
  // addEdge now takes the graph's own edge kind (EdgeGraph#EdgeT) rather than any Edge, so the
  // erased signature changed. Deliberate, and the point of the change: an UndirectedGraph used to
  // accept a DirectedEdge and absorb it silently, leaving the graph inconsistent with itself —
  // degree counted the adjacency while edges and M, which select by edge type, did not.
  ProblemFilters.exclude[IncompatibleResultTypeProblem]("com.phasmidsoftware.gryphon.adjunct.DirectedGraph.addEdge(com.phasmidsoftware.gryphon.core.Edge)com.phasmidsoftware.gryphon.adjunct.DirectedGraph"),
  ProblemFilters.exclude[IncompatibleResultTypeProblem]("com.phasmidsoftware.gryphon.adjunct.UndirectedGraph.addEdge(com.phasmidsoftware.gryphon.core.Edge)com.phasmidsoftware.gryphon.adjunct.UndirectedGraph"),
  // isConnected and isBipartite are gone from the Graph trait and from DirectedGraph, where they
  // were implemented as `throw UnsupportedOperationException`. They are meaningful only for an
  // undirected graph, so `digraph.isConnected` is now a compile error rather than a run-time one.
  ProblemFilters.exclude[DirectMissingMethodProblem]("com.phasmidsoftware.gryphon.core.Graph.isConnected()Boolean"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("com.phasmidsoftware.gryphon.core.Graph.isBipartite()Boolean"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("com.phasmidsoftware.gryphon.adjunct.DirectedGraph.isConnected()Boolean"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("com.phasmidsoftware.gryphon.adjunct.DirectedGraph.isBipartite()Boolean"),
  // prim and dijkstra took Traversable[V], which says nothing about orientation, while the other
  // seven algorithms already demanded DirectedGraph or UndirectedGraph. They now match: prim takes
  // an UndirectedGraph and dijkstra a DirectedGraph. The $default$5 entries are the synthetic
  // accessors for their default Random argument, whose signatures move with them.
  ProblemFilters.exclude[IncompatibleMethTypeProblem]("com.phasmidsoftware.gryphon.traverse.MST.prim"),
  ProblemFilters.exclude[IncompatibleMethTypeProblem]("com.phasmidsoftware.gryphon.traverse.MST.prim$default$5"),
  ProblemFilters.exclude[IncompatibleMethTypeProblem]("com.phasmidsoftware.gryphon.traverse.ShortestPaths.dijkstra"),
  ProblemFilters.exclude[IncompatibleMethTypeProblem]("com.phasmidsoftware.gryphon.traverse.ShortestPaths.dijkstra$default$5"),
  // The typeclasses moved to cats-kernel, so every signature with a Monoid or Zero context bound
  // changed: Visitor's Monoid/Zero became cats.kernel.Monoid. Nothing about the algorithms needed
  // the bespoke versions; the two Monoids are the same abstraction, Cats' with laws and instances.
  // Prim and Borůvka are slightly over-constrained as a result — they compare weights but never
  // accumulate, so Zero was enough — but Cats has no identity-only class, deliberately, since a
  // lone identity satisfies no law.
  ProblemFilters.exclude[IncompatibleMethTypeProblem]("com.phasmidsoftware.gryphon.traverse.AcyclicShortestPaths.shortestPaths"),
  ProblemFilters.exclude[IncompatibleMethTypeProblem]("com.phasmidsoftware.gryphon.traverse.BellmanFord.shortestPaths"),
  ProblemFilters.exclude[IncompatibleMethTypeProblem]("com.phasmidsoftware.gryphon.traverse.Boruvka.mst"),
  ProblemFilters.exclude[IncompatibleMethTypeProblem]("com.phasmidsoftware.gryphon.adjunct.DirectedGraph.shortestPaths"),
  ProblemFilters.exclude[IncompatibleMethTypeProblem]("com.phasmidsoftware.gryphon.traverse.DijkstraTraversal.apply"),
  ProblemFilters.exclude[IncompatibleMethTypeProblem]("com.phasmidsoftware.gryphon.traverse.DijkstraTraversal.copy"),
  ProblemFilters.exclude[IncompatibleMethTypeProblem]("com.phasmidsoftware.gryphon.traverse.DijkstraTraversal.this"),
  ProblemFilters.exclude[IncompatibleMethTypeProblem]("com.phasmidsoftware.gryphon.traverse.PrimTraversal.apply"),
  ProblemFilters.exclude[IncompatibleMethTypeProblem]("com.phasmidsoftware.gryphon.traverse.PrimTraversal.copy"),
  ProblemFilters.exclude[IncompatibleMethTypeProblem]("com.phasmidsoftware.gryphon.traverse.PrimTraversal.this"),
  ProblemFilters.exclude[IncompatibleMethTypeProblem]("com.phasmidsoftware.gryphon.traverse.WeightedTraversal.this"),
  ProblemFilters.exclude[IncompatibleMethTypeProblem]("com.phasmidsoftware.gryphon.core.RelaxableVertex.apply"),
  ProblemFilters.exclude[IncompatibleMethTypeProblem]("com.phasmidsoftware.gryphon.core.Vertex.createRelaxableWithSet"),
  // util.HasZero is deleted. It was an identity-only type class whose own comment read "I suppose
  // we could get it from Cats but that seems like overkill", and nothing outside its own file ever
  // referenced it. cats.kernel.Monoid is the Cats answer it anticipated.
  ProblemFilters.exclude[MissingClassProblem]("com.phasmidsoftware.gryphon.util.HasZero"),
  ProblemFilters.exclude[MissingClassProblem]("com.phasmidsoftware.gryphon.util.HasZero$"),
  ProblemFilters.exclude[MissingClassProblem]("com.phasmidsoftware.gryphon.util.HasZero$HasZeroUnit"),
  ProblemFilters.exclude[MissingClassProblem]("com.phasmidsoftware.gryphon.util.HasZero$HasZeroUnit$"),
  ProblemFilters.exclude[MissingClassProblem]("com.phasmidsoftware.gryphon.util.HasZero$HasZeroBoolean"),
  ProblemFilters.exclude[MissingClassProblem]("com.phasmidsoftware.gryphon.util.HasZero$HasZeroBoolean$"),
  ProblemFilters.exclude[MissingClassProblem]("com.phasmidsoftware.gryphon.util.HasZero$HasZeroInt"),
  ProblemFilters.exclude[MissingClassProblem]("com.phasmidsoftware.gryphon.util.HasZero$HasZeroInt$"),
  ProblemFilters.exclude[MissingClassProblem]("com.phasmidsoftware.gryphon.util.HasZero$HasZeroDouble"),
  ProblemFilters.exclude[MissingClassProblem]("com.phasmidsoftware.gryphon.util.HasZero$HasZeroDouble$")
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
