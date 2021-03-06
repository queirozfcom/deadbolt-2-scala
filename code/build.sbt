name := "deadbolt-scala"

organization := "be.objectify"

version := "2.4.0.3-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(play.PlayScala)

scalaVersion := "2.11.6"

crossScalaVersions := Seq("2.11.6", "2.10.5")

libraryDependencies ++= Seq(
  cache,
  specs2 % Test,
  "be.objectify" %% "deadbolt-core" % "2.4.1"
)

resolvers += Resolver.sonatypeRepo("snapshots")

parallelExecution in Test := false

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

fork in Test := true
