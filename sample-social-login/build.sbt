name := "sample-social-login"
organization := "com.example"
version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.14"

libraryDependencies ++= Seq(
  guice,
  ws,
  jdbc,
  "mysql" % "mysql-connector-java" % "8.0.31",
  "com.typesafe.play" %% "play-slick" % "5.0.2",
  "com.typesafe.play" %% "play-slick-evolutions" % "5.0.2",
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % Test
)

dependencyOverrides += "org.scala-lang.modules" %% "scala-xml" % "1.2.0"

