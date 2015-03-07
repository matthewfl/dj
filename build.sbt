import AssemblyKeys._

name := "dj"

version := "0.0.1"

scalaVersion := "2.11.5"

libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test"

libraryDependencies += "org.javassist" % "javassist" % "3.19.0-GA"


assemblySettings

mainClass in (Compile,run) := Some("edu.berkeley.dj.rt.Main")

mainClass in assembly := Some("edu.berkeley.dj.rt.Main")
