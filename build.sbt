val tapirVersion = "1.4.0"

lazy val rootProject = (project in file(".")).settings(
  Seq(
    name := "gwi-csv-converter",
    version := "0.1.0-SNAPSHOT",
    organization := "eu.ww86",
    scalaVersion := "3.2.2",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % tapirVersion,
      "org.http4s" %% "http4s-ember-server" % "0.23.19",
      "com.softwaremill.sttp.tapir" %% "tapir-prometheus-metrics" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % tapirVersion,
      "ch.qos.logback" % "logback-classic" % "1.4.7",
      "com.softwaremill.sttp.tapir" %% "tapir-sttp-stub-server" % tapirVersion % Test,
      "org.scalatest" %% "scalatest" % "3.2.16" % Test,
      "com.softwaremill.sttp.client3" %% "circe" % "3.8.15" % Test
    )
  )
)
