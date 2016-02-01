import AssemblyKeys._

name := "dj"

version := "0.0.1"

scalaVersion := "2.11.7"

libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test"

libraryDependencies += "com.hazelcast" % "hazelcast" % "3.4.2"

javacOptions += "-g"

scalacOptions += "-target:jvm-1.8"

scalacOptions += "-g:vars"

//libraryDependencies += "org.javassist" % "javassist" % "3.19.0-GA"
// need to add the jdk jar or include it to build the hotswapper, not sure if will want that


assemblySettings

mainClass in (Compile,run) := Some("edu.berkeley.dj.rt.Main")

mainClass in assembly := Some("edu.berkeley.dj.rt.Main")

// adding the tools.jar to the unmanaged-jars seq
unmanagedJars in Compile ~= {uj =>
  Seq(Attributed.blank(file(System.getProperty("java.home").dropRight(3)+"lib/tools.jar"))) ++ uj
}

// exluding the tools.jar file from the build
excludedJars in assembly <<= (fullClasspath in assembly) map { cp =>
  cp filter {_.data.getName == "tools.jar"}
}

// TODO: have an artifact that contains useful classes for the public api such as: DJIO, JITInterface


// TODO: remove
// these are for the examples which are currently being built at the same time
// normally they wouldn't have their code included
libraryDependencies += "org.eclipse.jetty" % "jetty-server" % "9.3.6.v20151106"
libraryDependencies += "net.imglib2" % "imglib2" % "2.6.0"