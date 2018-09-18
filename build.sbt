name := "scala-futures-promises"

version := "1.0"

organization := "tdauth"

scalaVersion := "2.12.6"

libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.5"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % "test"

libraryDependencies += ("org.scala-stm" %% "scala-stm" % "0.8")

logBuffered in Test := false

coverageEnabled := true

coverageMinimum := 100

coverageFailOnMinimum := false

coverageHighlighting := true