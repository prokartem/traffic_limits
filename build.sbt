name := "traffic_limits"

version := "0.1"

scalaVersion := "2.12.10"

assemblyMergeStrategy / assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x                             => MergeStrategy.first
}

lazy val catsCore         = "2.1.1"
lazy val doobieVersion    = "0.12.1"
lazy val logBack          = "1.2.3"
lazy val scalaLogging     = "3.9.3"
lazy val scalatestVersion = "3.2.9"
lazy val pcap4j           = "1.8.2"
lazy val kafka            = "2.8.0"
lazy val spark            = "3.1.2"

libraryDependencies ++= Seq(
  "org.apache.spark"            % "spark-core_2.12"                 % spark,
  "org.apache.spark"            % "spark-streaming_2.12"            % spark,
  "org.apache.spark"            % "spark-streaming-kafka-0-10_2.12" % spark,
  "org.pcap4j"                  % "pcap4j-distribution"             % pcap4j,
  "org.tpolecat"               %% "doobie-core"                     % doobieVersion,
  "org.tpolecat"               %% "doobie-postgres"                 % doobieVersion,
  "org.tpolecat"               %% "doobie-specs2"                   % doobieVersion,
  "ch.qos.logback"              % "logback-classic"                 % logBack,
  "com.typesafe.scala-logging" %% "scala-logging"                   % scalaLogging,
  "org.scalatest"              %% "scalatest"                       % scalatestVersion % "test",
  "org.typelevel"              %% "cats-core"                       % catsCore,
  "org.typelevel"              %% "cats-kernel"                     % catsCore,
)
