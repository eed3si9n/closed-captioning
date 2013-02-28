import sbt._


object Builds extends Build {
  import Keys._
  import spray.revolver.RevolverPlugin._
  lazy val buildSettings = Defaults.defaultSettings ++ Seq(
    organization := "com.eed3si9n",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := "2.9.1",
    resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

  )

  lazy val root = Project("root", file("."),
    settings = buildSettings
  )
  lazy val pricbot = Project("pircbot", file("pircbot"),
    settings = buildSettings ++ Seq(
      name := "pircbot"
    ))
  lazy val app = Project("closed-captioning", file("app"),
    settings = buildSettings ++ Revolver.settings ++ seq(
      name := "closed-captioning", 
      libraryDependencies ++= Seq(
        "net.databinder" %% "unfiltered-netty-websockets" % "0.6.5",
        "se.scalablesolutions.akka" % "akka-actor" % "1.3.1",
        "net.databinder" %% "dispatch-core" % "0.8.9",
        "net.databinder" %% "dispatch-http-json" % "0.8.9",
        "net.databinder" %% "dispatch-oauth" % "0.8.9",
        // "pircbot" % "pircbot" % "1.5.0",
        "com.typesafe" % "config" % "1.0.0",
        "org.clapper" %% "grizzled-slf4j" % "0.6.10",
        "org.clapper" %% "avsl" % "0.4"
      ),
      unmanagedClasspath in Runtime <+= (baseDirectory) map { (dir) =>
        dir / "conf"
      }
    )) dependsOn(pricbot)
}
