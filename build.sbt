ThisBuild / publish / skip := true

lazy val root = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Full)
  .in(file("."))
  .platformsEnablePlugins(JSPlatform)(ScalaJSJUnitPlugin)
  .settings(
    organization := "org.getshaka",
    name := "native-converter",
    version := "0.8.1-SNAPSHOT",
    versionScheme := Some("early-semver"),
    scalaVersion := "3.3.0",

    // publishing settings
    homepage := Some(url("https://github.com/getshaka-org/native-converter")),
    licenses += ("Apache-2.0", url("https://opensource.org/licenses/Apache-2.0")),
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/getshaka-org/native-converter"),
        "scm:git:git@github.com:getshaka-org/native-converter.git",
        Some("scm:git:git@github.com:getshaka-org/native-converter.git")
      )
    ),
    developers := List(
      Developer(
        id = "augustnagro@gmail.com",
        name = "August Nagro",
        email = "augustnagro@gmail.com",
        url = url("https://augustnagro.com")
      )
    ),
    publish / skip := false,
    Test / publishArtifact := false,
    publishMavenStyle := true,
    pomIncludeRepository := { _ => false },
    publishTo := {
      val nexus = "https://s01.oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases" at nexus + "service/local/staging/deploy/maven2")
    },
    credentials += Credentials(Path.userHome / ".sbt" / ".credentials")
  )
  .jvmSettings(
    libraryDependencies ++= Seq(
      "com.novocode" % "junit-interface" % "0.11" % "test"
    )
  )
