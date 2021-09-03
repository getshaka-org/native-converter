lazy val root = project
  .in(file("."))
  .enablePlugins(ScalaJSPlugin, ScalaJSJUnitPlugin)
  .settings(
    organization := "org.getshaka",
    name := "native-converter",
    versionScheme := Some("early-semver"),

    scalaVersion := "3.0.1",

    // publishing settings
    homepage := Some(url("https://github.com/getshaka-org/native-converter")),
    licenses += ("Apache-2.0", url("https://opensource.org/licenses/Apache-2.0")),
    developers := List(
      Developer(
        id = "augustnagro@gmail.com",
        name = "August Nagro",
        email = "augustnagro@gmail.com",
        url = url("https://augustnagro.com")
      )
    ),
    Test / publishArtifact := false,
    publishTo := {
      val nexus = "https://s01.oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases" at nexus + "service/local/staging/deploy/maven2")
    }
  )
