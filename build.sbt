name := "AkkaStreamDynamicThrottle"

version := "0.1"

scalaVersion := "2.13.3"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream-contrib" % "0.11",
  Cinnamon.library.cinnamonAkkaStream,
  Cinnamon.library.cinnamonPrometheus,
  Cinnamon.library.cinnamonPrometheusHttpServer
)

lazy val base = (project in file("."))
  .enablePlugins(Cinnamon)

cinnamon in run := true
cinnamonLogLevel := "INFO"
