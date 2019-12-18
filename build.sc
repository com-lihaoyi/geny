import mill._, scalalib._, scalajslib._, scalanativelib._, publish._


trait GenyPublishModule extends PublishModule {
  def artifactName = "geny"

  def publishVersion = "0.2.0"

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
  def ivyDeps = Agg(ivy"com.lihaoyi::utest::0.7.1")
  def testFrameworks = Seq("utest.runner.Framework")
}


object geny extends Module {
  object jvm extends Cross[JvmGenyModule]("2.12.8", "2.13.0")
  class JvmGenyModule(val crossScalaVersion: String)
    extends Common with ScalaModule with GenyPublishModule
  {
    object test extends Tests with CommonTestModule
  }

  object js extends Cross[JSGenyModule](("2.12.8", "0.6.26"), ("2.13.0", "0.6.28"))
  class JSGenyModule(val crossScalaVersion: String, crossJSVersion: String)
    extends Common with ScalaJSModule with GenyPublishModule
  {
    def scalaJSVersion = crossJSVersion
    object test extends Tests with CommonTestModule
  }

  object native extends Cross[NativeGenyModule](("2.11.12", "0.3.8"))
  class NativeGenyModule(val crossScalaVersion: String, crossScalaNativeVersion: String)
    extends Common with ScalaNativeModule with GenyPublishModule
  {
    def scalaNativeVersion = crossScalaNativeVersion
    object test extends Tests with CommonTestModule
  }
}
