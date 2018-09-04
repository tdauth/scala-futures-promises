name := "scala-futures-promises"

version := "1.0"

organization := "tdauth"

scalaVersion := "2.12.3"

sbtVersion := "0.13"

libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.5"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % "test"

logBuffered in Test := false