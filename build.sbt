lazy val commonSettings = Seq(
  scalaVersion := "2.12.6",
  version := "0.1"
)

lazy val common = project
  .settings(PB.targets in Compile := Seq(
    PB.gens.java -> (sourceManaged in Compile).value
  ))

lazy val server = project
  .dependsOn(common)

lazy val royale = project
  .aggregate(common, server)
  .settings(commonSettings)
  .settings(
    name := "snake-royale"
  )