import AssemblyKeys._

name := "dj"

version := "0.0.1"

scalaVersion := "2.11.6"

libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test"

libraryDependencies += "com.hazelcast" % "hazelcast" % "3.4.2"

javacOptions += "-g"

scalacOptions += "-target:jvm-1.8"

//libraryDependencies += "org.javassist" % "javassist" % "3.19.0-GA"
// need to add the jdk jar or include it to build the hotswapper, not sure if will want that


assemblySettings

mainClass in (Compile,run) := Some("edu.berkeley.dj.rt.Main")

mainClass in assembly := Some("edu.berkeley.dj.rt.Main")


