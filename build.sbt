name := """timeshift"""

version := "1.0"

scalaVersion := "2.11.5"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.9",
  "com.sksamuel.elastic4s" %% "elastic4s" % "1.4.13" excludeAll(
    ExclusionRule(organization = "log4j"),
    ExclusionRule(organization = "org.slf4j")),
  "ch.qos.logback" % "logback-classic" % "1.1.2",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.9" % "test",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test",
  "org.mockito" % "mockito-all" % "1.9.5" % "test"
)


fork in run := true