name := "Model DB Spark Client"

version := "1.0"

scalaVersion := "2.11.7"

test in assembly := {}
assemblyJarName in assembly := "ml.jar"

libraryDependencies += "org.apache.spark" %% "spark-core" % "2.0.0" % "provided"
libraryDependencies += "org.apache.spark" %% "spark-sql" % "2.0.0" % "provided"
libraryDependencies += "org.apache.spark" %% "spark-mllib" % "2.0.0" % "provided"
libraryDependencies += "org.apache.spark" %% "spark-hive" % "2.0.0" % "provided"

libraryDependencies += "org.apache.thrift" % "libthrift" % "0.8.0"
libraryDependencies += "com.twitter" %% "scrooge-core" % "4.8.0"
libraryDependencies += "com.twitter" %% "finagle-thrift" % "6.36.0"



resolvers += "Akka Repository" at "http://repo.akka.io/releases/"
resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
