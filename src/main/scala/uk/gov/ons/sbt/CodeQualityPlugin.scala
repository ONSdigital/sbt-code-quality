package uk.gov.ons.sbt

import scala.collection.JavaConversions._

import sbt.Keys._
import sbt._
import org.scalastyle.sbt.ScalastylePlugin
import org.scalastyle.sbt.ScalastylePlugin.autoImport.{scalastyle, scalastyleConfig, scalastyleFailOnError, scalastyleTarget}
import com.sksamuel.scapegoat.sbt.ScapegoatSbtPlugin
import com.sksamuel.scapegoat.sbt.ScapegoatSbtPlugin.autoImport.{scapegoatConsoleOutput, scapegoatOutputPath, scapegoatVersion}

import scoverage.ScoverageKeys.{coverageExcludedPackages, coverageMinimum}
import scoverage.ScoverageSbtPlugin

object CodeQualityPlugin extends AutoPlugin {

  private val scalastyleConfigFilename = "scalastyle-config.xml"

  override def trigger = allRequirements
  override def requires = ScoverageSbtPlugin && ScalastylePlugin && ScapegoatSbtPlugin

  object autoImport {
        val importScalastyleConfig = taskKey[Unit]("Imports Scalastyle config from this plugin to the project target path")
  }

  import autoImport._

  override lazy val projectSettings = {
    val staticAnalysisOutputDir = "code-quality/style"
    Seq(
      scapegoatVersion := "1.1.0",
      coverageExcludedPackages := ".*Routes.*;.*ReverseRoutes.*;.*javascript.*",
      coverageMinimum := 80,
      scalastyleConfig := baseDirectory.value / "project" / scalastyleConfigFilename,
      scalastyleTarget := (target.value / staticAnalysisOutputDir / "code-quality/style/scalastyle-result.xml"),
      scalastyleFailOnError := true,
      scapegoatOutputPath := "target/" + staticAnalysisOutputDir,
      scapegoatConsoleOutput := false
    ) ++
    configSettings ++
    Seq(
      // Run importScalastyleConfig task before scalastyle task.
      scalastyle.in(Compile) := {
        scalastyle.in(Compile).dependsOn(importScalastyleConfig).evaluated
      },
      scalastyle.in(Test) := {
        scalastyle.in(Compile).dependsOn(importScalastyleConfig).evaluated
      }
    )
  }

  // Settings used for a particular configuration (such as Compile).
  def configSettings: Seq[Setting[_]] = Seq(importScalastyleConfigTask)


  val importScalastyleConfigTask = importScalastyleConfig := {
    val targetPath = baseDirectory.value / "project" / scalastyleConfigFilename
    extractFileFromJar(getClass.getResource(s"/$scalastyleConfigFilename"), targetPath)
  }

  private[this] def extractFileFromJar(url: java.net.URL, target: File): Unit = {
    url.openConnection match {
      case connection: java.net.JarURLConnection => {
        val entryName = connection.getEntryName
        val jarFile = connection.getJarFile

        jarFile.entries.filter(_.getName == entryName).foreach { e =>
          IO.transfer(jarFile.getInputStream(e), target)
        }
      }
      case _ =>
        throw new IllegalStateException(s"Could not open JAR connection to obtain $scalastyleConfigFilename.")
    }
  }

}
