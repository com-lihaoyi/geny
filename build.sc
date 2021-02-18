import mill._, scalalib._, scalajslib._, scalanativelib._, publish._

val dottyCustomVersion = sys.props.get("dottyVersion")

val scala212 = "2.12.13"
val scala213 = "2.13.4"
val scala3 = "3.0.0-RC1"

val scalaJSVersions = for {
  scalaV <- Seq(scala213, scala212)
  scalaJSV <- Seq("0.6.33", "1.4.0")
} yield (scalaV, scalaJSV)

val scalaNativeVersions = for {
  scalaV <- Seq(scala213, scala212)
  scalaNativeV <- Seq("0.4.0")
} yield (scalaV, scalaNativeV)

trait GenyPublishModule extends PublishModule {
  def artifactName = "geny"

  def publishVersion = "0.6.5"

  def pomSettings = PomSettings(
    description = artifactName(),
    organization = "com.lihaoyi",
    url = "https://github.com/lihaoyi/geny",
    licenses = Seq(License.MIT),
    scm = SCM(
      "git://github.com/lihaoyi/geny.git",
      "scm:git://github.com/lihaoyi/geny.git"
    ),
    developers = Seq(
      Developer("lihaoyi", "Li Haoyi", "https://github.com/lihaoyi")
    )
  )
}

trait Common extends CrossScalaModule {
  def millSourcePath = build.millSourcePath / "geny"
  def sources = T.sources(millSourcePath / "src")
}

trait CommonTestModule extends ScalaModule with TestModule {
  def ivyDeps = Agg(ivy"com.lihaoyi::utest::0.7.7")
  def testFrameworks = Seq("utest.runner.Framework")
}


object geny extends Module {
  object jvm extends Cross[JvmGenyModule]((scala212 :: scala213 :: scala3 :: dottyCustomVersion.toList): _*)
  class JvmGenyModule(val crossScalaVersion: String)
    extends Common with ScalaModule with GenyPublishModule
  {
    object test extends Tests with CommonTestModule
  }

  object js extends Cross[JSGenyModule](scalaJSVersions: _*)
  class JSGenyModule(val crossScalaVersion: String, crossJSVersion: String)
    extends Common with ScalaJSModule with GenyPublishModule
  {
    def scalaJSVersion = crossJSVersion
    object test extends Tests with CommonTestModule
  }

  object native extends Cross[NativeGenyModule](scalaNativeVersions: _*)
  class NativeGenyModule(val crossScalaVersion: String, crossScalaNativeVersion: String)
    extends Common with ScalaNativeModule with GenyPublishModule
  {
    def scalaNativeVersion = crossScalaNativeVersion
    object test extends Tests with CommonTestModule
  }
}
