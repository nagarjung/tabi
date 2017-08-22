name := "BGPSecurity"

version := "1.0"

scalaVersion := "2.11.7"


// https://mvnrepository.com/artifact/org.apache.spark/spark-streaming_2.11
libraryDependencies += "org.apache.spark" % "spark-streaming_2.11" % "2.1.0"

// https://mvnrepository.com/artifact/com.typesafe.slick/slick_2.11
libraryDependencies += "com.typesafe.slick" % "slick_2.11" % "3.1.1"

// https://mvnrepository.com/artifact/com.typesafe.slick/slick-hikaricp_2.11
libraryDependencies += "com.typesafe.slick" % "slick-hikaricp_2.11" % "3.1.1"

libraryDependencies += "org.slf4j" % "slf4j-nop" % "1.6.4"

// https://mvnrepository.com/artifact/mysql/mysql-connector-java
libraryDependencies += "mysql" % "mysql-connector-java" % "5.1.38"


libraryDependencies += "com.google.code.gson" % "gson" % "2.7"

libraryDependencies += "com.sparkjava" % "spark-core" % "2.5.3"


assemblyJarName in assembly := "BGPSecHistoryService.jar"

assemblyMergeStrategy in assembly := {

  case PathList(ps@_*) if ps.last endsWith "pom.xml" =>
    MergeStrategy.discard

  case PathList(ps@_*) if ps.last endsWith "pom.properties" =>
    MergeStrategy.discard

  case PathList("javax", "inject", xs@_*) => MergeStrategy.first

  case PathList("org", "aopalliance", xs@_*) => MergeStrategy.first

  case PathList("org", "apache", "commons", "beanutils", xs@_*) => MergeStrategy.first

  case PathList("org", "apache", "commons", "collections", xs@_*) => MergeStrategy.first

  case PathList("org", "apache", "hadoop", "yarn", xs@_*) => MergeStrategy.first

  case PathList("org", "apache", "spark", "unused", xs@_*) => MergeStrategy.first

  case PathList("org", "slf4j", "impl", xs@_*) => MergeStrategy.first


  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)

}

