lazy val root = project
  .in(file("."))
  .enablePlugins(ScalaJSPlugin, ScalaJSJUnitPlugin)
  .settings(
    organization := "com.augustnagro",
    name := "native-converter",
    version := "0.1.1-SNAPSHOT",
    
    scalaVersion := "3.0.0-RC1",
    scalacOptions ++= Seq(
      "-Ycheck-init",
      "-Yexplicit-nulls",
      "-Yindent-colons"
    ),
    
    // publishing settings
    homepage := Some(url("https://github.com/AugustNagro/native-converter")),
    licenses += ("Apache-2.0", url("https://opensource.org/licenses/Apache-2.0")),
    scmInfo := Some(ScmInfo(
      url("https://github.com/AugustNagro/native-converter"),
      "scm:git:git@github.com/AugustNagro/native-converter.git",
      Some("scm:git:git@github.com/AugustNagro/native-converter.git")
    )),
    developers := List(
      Developer(
        id = "augustnagro@gmail.com",
        name = "August Nagro",
        email = "augustnagro@gmail.com",
        url = url("https://augustnagro.com")
      )
    ),
    publishMavenStyle := true,
    publishArtifact.in(Test) := false,
    pomIncludeRepository := { _ => false },
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases" at nexus + "service/local/staging/deploy/maven2")
    },
    credentials += Credentials(Path.userHome / ".sbt" / ".credentials")
  )
