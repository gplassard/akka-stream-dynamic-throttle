name := "AkkaStreamDynamicThrottle"

version := "0.1"

scalaVersion := "2.13.3"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream-contrib" % "0.11",
  "com.lightbend.akka" %% "akka-stream-alpakka-dynamodb" % "2.0.2",
  "software.amazon.awssdk" % "cloudwatch" % "2.15.26"
)
