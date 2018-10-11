name := "scala-futures-promises"

version := "1.0"

organization := "tdauth"

scalaVersion := "2.12.6"

// set the main class for 'sbt run'
mainClass in (Compile, run) := Some("tdauth.futuresandpromises.benchmarks.Benchmarks")
// set the main class for packaging the main jar
mainClass in (Compile, packageBin) := Some("tdauth.futuresandpromises.benchmarks.Benchmarks")

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

libraryDependencies += "com.twitter" %% "util-collection" % "18.9.1"

libraryDependencies += "org.scala-stm" %% "scala-stm" % "0.8"

libraryDependencies += "org.scala-lang.modules" %% "scala-async" % "0.9.6"

libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.5"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % "test"

libraryDependencies += "com.storm-enroute" %% "scalameter" % "0.8.2" % "test"

testFrameworks += new TestFramework("org.scalameter.ScalaMeterFramework")

parallelExecution in Test := false

logBuffered in Test := false

coverageEnabled := true

coverageMinimum := 100

coverageFailOnMinimum := false

coverageHighlighting := true