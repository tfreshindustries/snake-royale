lazy val commonSettings = Seq(
  scalaVersion := "2.12.6",
  version := "0.1"
)

lazy val akkaStreamVersion = "2.5.14"
lazy val akkaHttpVersion = "10.1.3"

lazy val common = project
  .settings(PB.targets in Compile := Seq(
    PB.gens.java -> (sourceManaged in Compile).value
  ))

lazy val server = project
  .dependsOn(common)
  .settings(libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-stream" % akkaStreamVersion,
    "com.typesafe.akka" %% "akka-stream-testkit" % akkaStreamVersion % Test,
    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
    "com.typesafe.akka" %% "akka-slf4j" % "2.5.14",
    "com.google.guava" % "guava" % "25.1-jre",
    "ch.qos.logback" % "logback-classic" % "1.2.3",
    "org.slf4j" % "slf4j-api" % "1.7.25"
  ))

lazy val royale = project
  .aggregate(common, server)
  .settings(commonSettings)
  .settings(
    name := "snake-royale"
  )