organization := "com.eed3si9n"

name := "closed-captioning"

scalaVersion := "2.9.1"

version := "0.1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  "net.databinder" %% "unfiltered-netty-websockets" % "0.6.5",
  "se.scalablesolutions.akka" % "akka-actor" % "1.3.1",
  "net.databinder" %% "dispatch-core" % "0.8.9",
  "net.databinder" %% "dispatch-http-json" % "0.8.9",
  "net.databinder" %% "dispatch-oauth" % "0.8.9",
  "pircbot" % "pircbot" % "1.5.0",
  "com.typesafe" % "config" % "1.0.0"
)

Revolver.settings

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

unmanagedClasspath in Runtime <+= (baseDirectory) map { (dir) =>
  dir / "conf"
}
