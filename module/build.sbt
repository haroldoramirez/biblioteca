name := "twitter-module"

version := "1.0"

description := "PlayFramework 2.x module to fetch, cache, and display tweets from Twitter"

organization := "com.fizzed"

organizationName := "Fizzed, Inc."

organizationHomepage := Some(new URL("http://fizzed.com"))

lazy val `module` = (project in file(".")).enablePlugins(PlayJava)

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"
      
scalaVersion := "2.11.6"

libraryDependencies ++= Seq(javaJdbc,
  cache,
  javaWs,
  "org.twitter4j" % "twitter4j-core" % "4.0.6",
  "com.twitter" % "twitter-text" % "1.14.7"
)

      