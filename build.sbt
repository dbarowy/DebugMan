scalaVersion := "2.11.4"

name := "AutoManDebugger"

version := "0.1-SNAPSHOT"

organization := "edu.umass.cs.plasma"

organizationName := "PLASMA Lab @ University of Massachusetts Amherst"

organizationHomepage := Some(url("http://plasma.cs.umass.edu"))

homepage := Some(url("https://bitbucket.org/btamaskar/automan-debugger"))

exportJars := true

libraryDependencies ++= {
  val akkaVer   = "2.3.7"
  val sprayVer  = "1.3.2"
  Seq(
    "edu.umass.cs"	    %%	"automan"	      % "0.5-SNAPSHOT",
    "io.spray"          %%  "spray-can"     % sprayVer,
    "io.spray"          %%  "spray-routing" % sprayVer,
    "io.spray"          %%  "spray-testkit" % sprayVer  % "test",
    "io.spray"          %%  "spray-json"    % "1.3.1",
    "com.typesafe.akka" %%  "akka-actor"    % akkaVer,
    "com.typesafe.akka" %%  "akka-testkit"  % akkaVer   % "test",
    "org.specs2"        %%  "specs2-core"   % "2.3.11"  % "test"
  )
}


