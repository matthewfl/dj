import AssemblyKeys._

name := "dj"

version := "0.0.1"

scalaVersion := "2.11.5"

assemblySettings

mainClass in (Compile,run) := Some("edu.berkeley.dj.rt.Main")

mainClass in assembly := Some("edu.berkeley.dj.rt.Main")
