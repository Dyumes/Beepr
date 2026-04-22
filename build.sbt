val scala3Version = "3.8.1"

lazy val root = project
  .in(file("."))
  .settings(
    name := "social-network-simulator",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit" % "1.3.0" % Test,
      "org.neo4j.driver" % "neo4j-java-driver" % "5.18.0",
      "com.lihaoyi" %% "upickle" % "3.3.1"
    ))
