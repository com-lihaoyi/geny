// plugins
import $ivy.`de.tototec::de.tobiasroeser.mill.vcs.version::0.2.0`
import $ivy.`com.github.lolgab::mill-mima::0.0.12`

// imports
import mill._, scalalib._, scalajslib._, scalanativelib._, publish._
import de.tobiasroeser.mill.vcs.version.VcsVersion
import com.github.lolgab.mill.mima._
import mill.scalalib.api.ZincWorkerUtil

val communityBuildDottyVersion = sys.props.get("dottyVersion").toList

val scalaVersions = Seq(
  "3.1.3",
  "2.13.8",
  "2.12.16",
  "2.11.12"
) ++ communityBuildDottyVersion

val scalaJSVersions = scalaVersions.map((_, "1.10.1"))
val scalaNativeVersions = scalaVersions.map((_, "0.4.5"))


trait MimaCheck extends Mima {
  override def mimaPreviousVersions = Seq("0.6.7", "0.6.8", "0.6.9", "0.6.10", "0.7.0", "0.7.1")

 // Temporary until the next version of Mima gets released with
  // https://github.com/lightbend/mima/issues/693 included in the release.
  override def mimaPreviousArtifacts = T{
    if (ZincWorkerUtil.isScala3(scalaVersion())) Agg.empty[Dep]
    else super.mimaPreviousArtifacts()
  }
}


trait GenyPublishModule extends PublishModule with MimaCheck {
  override def artifactName = "geny"

  def publishVersion = VcsVersion.vcsState().format()

  def pomSettings = PomSettings(
    description = artifactName(),
    organization = "com.lihaoyi",
    url = "https://github.com/com-lihaoyi/geny",
    licenses = Seq(License.MIT),
    versionControl = VersionControl.github(owner = "com-lihaoyi", repo = "geny"),
    developers = Seq(
      Developer("lihaoyi", "Li Haoyi", "https://github.com/lihaoyi")
    )
  )
}

trait Common extends CrossScalaModule {
  def millSourcePath = build.millSourcePath / "geny"
  def sources = T.sources(millSourcePath / "src")
}

trait CommonTestModule extends ScalaModule with TestModule.Utest {
  override def ivyDeps = Agg(ivy"com.lihaoyi::utest::0.8.1")
}

object geny extends Module {
  object jvm extends Cross[JvmGenyModule](scalaVersions: _*)
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
