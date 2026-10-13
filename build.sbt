name := "geny"

scalaVersion := "3.9.0"

// geny uses Mill's layout, so point sbt at it rather than moving the sources.
Compile / scalaSource := baseDirectory.value / "geny" / "src"

Test / scalaSource := baseDirectory.value / "geny" / "test" / "src"

libraryDependencies += "com.lihaoyi" %% "utest" % "0.8.5" % Test

testFrameworks += new TestFramework("utest.runner.Framework")
