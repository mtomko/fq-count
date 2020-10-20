ThisBuild / scalaVersion := "2.13.3"

lazy val versions = new {
  val cats = "2.2.0"
  val catsEffect = "2.2.0"
  val console4Cats = "0.8.1"
  val decline = "1.3.0"
  val fs2 = "2.4.4"
  val munit = "0.7.14"
}

lazy val `fq-count` = project
  .in(file("."))
  .settings(
    name := "fq-count",
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
    libraryDependencies ++= List(
      "org.typelevel"  %% "cats-core"        % versions.cats,
      "org.typelevel"  %% "cats-effect"      % versions.catsEffect,
      "dev.profunktor" %% "console4cats"     % versions.console4Cats,
      "com.monovore"   %% "decline"          % versions.decline,
      "com.monovore"   %% "decline-effect"   % versions.decline,
      "co.fs2"         %% "fs2-core"         % versions.fs2,
      "co.fs2"         %% "fs2-io"           % versions.fs2,
      "org.scalameta"  %% "munit"            % versions.munit % Test,
      "org.scalameta"  %% "munit-scalacheck" % versions.munit % Test
    ),
    buildInfoKeys    := Seq[BuildInfoKey](name, version),
    buildInfoPackage := "io.github.mtomko.fqcount",
    testFrameworks += new TestFramework("munit.Framework")
  )
  .enablePlugins(BuildInfoPlugin)
