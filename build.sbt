import sbtcrossproject.CrossPlugin.autoImport.crossProject

lazy val geny = crossProject(JSPlatform, JVMPlatform)
  .settings(
    scalaVersion := "2.11.12",
    crossScalaVersions := Seq("2.11.12", "2.12.7", "2.13.0-M5"),
    organization := "com.lihaoyi",
    name := "geny",
    version := "0.1.5",
    scalacOptions ++= (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 11)) => Seq("-target:jvm-1.7")
      case _ => Seq()
    }),
    libraryDependencies ++= Seq(
      "com.lihaoyi" %%% "utest" % "0.6.6" % "test"
    ),
    testFrameworks := Seq(new TestFramework("utest.runner.Framework")),
    publishTo := Some("releases"  at "https://oss.sonatype.org/service/local/staging/deploy/maven2"),

    pomExtra :=
      <url>https://github.com/lihaoyi/geny</url>
        <licenses>
          <license>
            <name>MIT license</name>
            <url>http://www.opensource.org/licenses/mit-license.php</url>
          </license>
        </licenses>
        <scm>
          <url>git://github.com/lihaoyi/geny.git</url>
          <connection>scm:git://github.com/lihaoyi/geny.git</connection>
        </scm>
        <developers>
          <developer>
            <id>lihaoyi</id>
            <name>Li Haoyi</name>
            <url>https://github.com/lihaoyi</url>
          </developer>
        </developers>
  ).enablePlugins(BuildInfoPlugin).settings(
    buildInfoKeys := Seq[BuildInfoKey](scalaVersion, crossVersion),
    buildInfoPackage := "geny"
  )

lazy val genyJVM = geny.jvm
lazy val genyJS = geny.js
