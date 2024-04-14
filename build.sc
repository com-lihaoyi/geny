// plugins
import $ivy.`de.tototec::de.tobiasroeser.mill.vcs.version::0.4.0`
import $ivy.`com.github.lolgab::mill-mima::0.0.23`

// imports
import mill._, scalalib._, scalajslib._, scalanativelib._, publish._
import de.tobiasroeser.mill.vcs.version.VcsVersion
import com.github.lolgab.mill.mima._
import mill.scalalib.api.ZincWorkerUtil

val communityBuildDottyVersion = sys.props.get("dottyVersion").toList

val scalaVersions = Seq(
  "3.3.1",
  "2.13.8",
  "2.12.17",
) ++ communityBuildDottyVersion

trait MimaCheck extends Mima {
  override def mimaPreviousVersions = T{
    val sv = scalaVersion()
    val is3 = ZincWorkerUtil.isScala3(sv)
    val is211 = sv.startsWith("2.11.")
    val isNative = this.isInstanceOf[ScalaNativeModule]
    Seq(
      Seq("0.6.0", "0.6.2").filter(_ => !is3 && !is211 && !isNative),
      Seq("0.6.4", "0.6.5", "0.6.6").filter(_ => !is3 && !is211),
      Seq("0.6.7", "0.6.8", "0.6.9").filter(_ => !is3),
      Seq("0.6.10", "0.7.0").filter(_ => !is3 || !isNative),
      Seq("0.7.1", "1.0.0")
    ).flatten
  }

  override def mimaBinaryIssueFilters: T[Seq[ProblemFilter]] = Seq(
    // deprecated since its inception in 0.3.0
    ProblemFilter.exclude[DirectMissingMethodProblem]("geny.ByteData.string"),
    ProblemFilter.exclude[DirectMissingMethodProblem]("geny.ByteData#Chunks.string")
  )

  def mimaReportBinaryIssues() =
    if (this.isInstanceOf[ScalaNativeModule] || this.isInstanceOf[ScalaJSModule]) T.command()
    else super.mimaReportBinaryIssues()
}


trait GenyPublishModule extends PublishModule with MimaCheck {
  override def artifactName = "geny"

  override def publishVersion: T[String] = VcsVersion.vcsState().format()

  override def versionScheme: T[Option[VersionScheme]] = Some(VersionScheme.EarlySemVer)

  override def pomSettings: T[PomSettings] = PomSettings(
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
  override def ivyDeps = Agg(ivy"com.lihaoyi::utest::0.8.3")
}

object geny extends Module {
  object jvm extends Cross[JvmGenyModule](scalaVersions)
  trait JvmGenyModule extends Common with ScalaModule with GenyPublishModule {
    object test extends ScalaTests with CommonTestModule
  }

  object js extends Cross[JSGenyModule](scalaVersions)
  trait JSGenyModule extends Common with ScalaJSModule with GenyPublishModule {
    def scalaJSVersion = "1.12.0"
    private def sourceMapOptions = T.task {
      val vcsState = VcsVersion.vcsState()
      vcsState.lastTag.collect {
        case tag if vcsState.commitsSinceLastTag == 0 =>
          val baseUrl = pomSettings().url.replace("github.com", "raw.githubusercontent.com")
          val sourcesOptionName = if(ZincWorkerUtil.isScala3(crossScalaVersion)) "-scalajs-mapSourceURI" else "-P:scalajs:mapSourceURI"
          s"$sourcesOptionName:${T.workspace.toIO.toURI}->$baseUrl/$tag/"
      }
    }
    override def scalacOptions = super.scalacOptions() ++ sourceMapOptions()

    object test extends ScalaJSTests with CommonTestModule
  }

  object native extends Cross[NativeGenyModule](scalaVersions)
  trait NativeGenyModule extends Common with ScalaNativeModule with GenyPublishModule
  {
    def scalaNativeVersion = "0.5.0"
    object test extends ScalaNativeTests with CommonTestModule
  }
}
