name := "blablacar-mower"

version := "0.1"

scalaVersion := "2.13.3"

val akkaVersion = "2.6.10"
val logbackVersion = "1.1.7"

libraryDependencies ++= Seq(
  // ACTOR
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,

  // LOGGING
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "ch.qos.logback" % "logback-classic" % logbackVersion,

  // TESTING
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
  "org.scalatest" %% "scalatest" % "3.2.0" % "test"
)

assemblyOutputPath in assembly := file("blablacar-mower.jar")