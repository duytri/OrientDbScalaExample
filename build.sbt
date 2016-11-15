name := "OrientDbScalaExample"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "com.orientechnologies" % "orientdb-graphdb" % "2.2.12",
  "com.orientechnologies" % "orientdb-core" % "2.2.12",
  "com.googlecode.concurrentlinkedhashmap" % "concurrentlinkedhashmap-lru" % "1.4.1",
  "com.tinkerpop.blueprints" % "blueprints-core" % "2.6.0"
)
