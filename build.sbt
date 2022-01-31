inThisBuild(
  List(
    organization := "org.getshaka",
    versionScheme := Some("early-semver"),

    scalaVersion := "3.1.0",

    // publishing settings
    homepage := Some(url("https://github.com/getshaka-org/native-converter")),
    licenses += ("Apache-2.0", url("https://opensource.org/licenses/Apache-2.0")),
    scmInfo := Some(ScmInfo(
      url("https://github.com/getshaka-org/native-converter"),
      "scm:git:git@github.com:getshaka-org/native-converter.git",
      Some("scm:git:git@github.com:getshaka-org/native-converter.git")
    )),
    developers := List(
      Developer(
        id = "augustnagro@gmail.com",
        name = "August Nagro",
        email = "augustnagro@gmail.com",
        url = url("https://augustnagro.com")
      )
    ),

    publish / skip := true,
    Test / publishArtifact := false,
    sonatypeCredentialHost := "s01.oss.sonatype.org"
  )
)

lazy val root = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Full)
  .in(file("."))
  .platformsEnablePlugins(JSPlatform)(ScalaJSJUnitPlugin)
  .settings(
    name := "native-converter",
    publish / skip := false
  )
  .jvmSettings(
    libraryDependencies ++= Seq(
      "com.novocode" % "junit-interface" % "0.11" % "test"
    )
  )
