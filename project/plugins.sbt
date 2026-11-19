/*
 * Copyright (c) 2026. Phasmid Software
 */

addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.3.2")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.12.2")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.4.4")
addSbtPlugin("com.github.sbt" % "sbt-ghpages" % "0.10.0")
addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "1.2.1")
addSbtPlugin("com.github.sbt.junit" % "sbt-jupiter-interface" % "0.19.0")
// Mutation testing: `sbt stryker`. Asks whether a test *asserts* a behaviour, which
// scoverage cannot — it only shows the line ran. Configured in stryker4s.conf.
addSbtPlugin("io.stryker-mutator" % "sbt-stryker4s" % "1.1.1")
