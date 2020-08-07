name := """biblioteca"""

version := "1.0"

import com.github.play2war.plugin._

//WAR plugins
Play2WarPlugin.play2WarSettings

//Servlet 3.1: Tomcat 8, Wildfly 8, Glassfish 4, Jetty 9, ...
//Play2WarKeys.servletVersion := "3.1"

//Servlet 3.0: Tomcat 7, JBoss 7, JBoss EAP 6, Glassfish 3, Jetty 8, ...
//Play2WarKeys.servletVersion := "3.0"

//Servlet 2.5: Tomcat 6, JBoss AS 5/6, JBoss EAP 5, Glassfish 2, Jetty 7, ...
Play2WarKeys.servletVersion := "2.5"

//Application without plugin
lazy val root = (project in file(".")).enablePlugins(PlayJava, PlayEbean)

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  javaJdbc,
  cache,
  javaWs,
  filters,
  "org.postgresql" % "postgresql" % "9.4.1211",
  "com.typesafe.play" %% "play-mailer" % "4.0.0",
  "org.julienrf" %% "play-jsmessages" % "2.0.0",
  "commons-io" % "commons-io" % "2.4",
  "commons-validator" % "commons-validator" % "1.5.1",
  "org.mindrot" % "jbcrypt" % "0.4",
  "eu.bitwalker" % "UserAgentUtils" % "1.20",
  "it.innove" % "play2-pdf" % "1.4.0",
  "org.apache.poi" % "poi" % "4.1.2",
  "com.google.code.gson" % "gson" % "2.8.6"
)

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator