// This project is its own plugin :)
//unmanagedSourceDirectories in Compile += baseDirectory.value.getParentFile / "src" / "main" / "scala"

libraryDependencies += "org.scala-sbt" % "scripted-plugin" % sbtVersion.value
